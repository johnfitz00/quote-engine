# Architecture Patterns in This Project

These are the structural patterns used in this codebase — why they exist and how they map to what you'd do in Laravel.

---

## Layered architecture

```
HTTP request
    ↓
QuoteController          (api layer)       — HTTP translation only
    ↓
QuoteService             (application)     — business logic, transactions
    ↓
QuoteRepository          (infrastructure)  — database access
    ↑
QuoteMapper              (application)     — converts between layers
    ↑
Quote / Driver / Vehicle (domain)          — pure business objects
    ↑
QuoteStatus              (domain)          — state machine
```

```
Laravel equivalent:
Request → Controller → Service (or directly to Model) → Eloquent Model → Database
```

The key discipline: **each layer only knows about the layer below it.** The controller never touches the repository. The domain objects never import Spring annotations (almost — `@Entity` is an exception forced by JPA).

---

## DTO pattern: Request → Domain → Response

Three different object shapes exist for one concept ("a quote"):

| Object | Lives in | Purpose |
|--------|----------|---------|
| `CreateQuoteRequest` | api/dto | Deserialised from HTTP body, validation annotations |
| `UpdateQuoteRequest` | api/dto | Same for PUT |
| `Quote` (entity) | domain | Persisted to database, JPA annotations |
| `QuoteResponse` | api/dto | Serialised to HTTP response, no annotations |

**Why separate objects?** Because each layer has different concerns:
- The request DTO needs validation rules
- The domain entity needs JPA annotations and business state
- The response DTO needs to be a stable public contract, immune to database schema changes

In a small Laravel app, you'd often use the model directly in responses (`$quote->toArray()`). That works until the model changes — then your API response changes too. The DTO pattern isolates changes.

---

## QuoteMapper — the translation layer

```kotlin
@Component
class QuoteMapper {
    fun toDomain(request: CreateQuoteRequest): Quote
    fun applyUpdate(request: UpdateQuoteRequest, quote: Quote)
    fun toResponse(quote: Quote): QuoteResponse
}
```

The mapper is the only place where domain objects and DTOs meet. This means:
- If `Quote` adds a field, only `QuoteMapper` needs updating — not every controller
- If the API response shape changes, only `QuoteMapper` needs updating — not the domain

Laravel doesn't have a standard mapper concept. You'd typically use `$model->only([...])`, API Resources (`QuoteResource extends JsonResource`), or Data Transfer Objects.

The `private` extension functions in `QuoteMapper` are a clean Kotlin idiom:

```kotlin
private fun VehicleRequest.toDomain() = Vehicle(year = year, make = make, model = model, annualKm = annualKm)
```

This reads as "VehicleRequest can convert itself to a Vehicle" — but the logic lives in the mapper, not in VehicleRequest itself.

---

## State machine in the domain

```kotlin
enum class QuoteStatus {
    DRAFT, RATED, REFERRED, BOUND, EXPIRED, DECLINED;

    val allowedTransitions: Set<QuoteStatus> by lazy { when (this) { ... } }
    fun canTransitionTo(next: QuoteStatus): Boolean = next in allowedTransitions
}
```

The transition rules live **on the enum itself**. When you need to know if a transition is valid, you ask the status — not the service, not the controller.

```kotlin
// QuoteService.kt
if (!quote.status.canTransitionTo(next)) {
    throw InvalidTransitionException(quote.status, next)
}
```

This is the principle of putting behaviour close to the data it operates on. In Laravel you might put this logic in the model or a dedicated `QuoteStateMachine` class.

---

## `GlobalExceptionHandler` — the error translation layer

The controllers throw domain exceptions (`QuoteNotFoundException`, `InvalidTransitionException`). They never deal with HTTP status codes for error cases. The `GlobalExceptionHandler` translates:

```
QuoteNotFoundException      → 404 + error body
InvalidTransitionException  → 409 + error body
MethodArgumentNotValidException → 422 + field errors
```

This keeps the service and domain layers clean — they don't know or care about HTTP. A `QuoteNotFoundException` makes sense even if you were using gRPC or a message queue instead of HTTP.

In Laravel, `app/Exceptions/Handler.php` serves the same purpose.

---

## `UriComponentsBuilder` — building the Location header

```kotlin
@PostMapping
fun create(
    @Valid @RequestBody request: CreateQuoteRequest,
    uriBuilder: UriComponentsBuilder
): ResponseEntity<QuoteResponse> {
    val quote = quoteService.createQuote(request)
    val location = uriBuilder.path("/v1/quotes/{id}").buildAndExpand(quote.id).toUri()
    return ResponseEntity.created(location).body(quote)
}
```

Spring injects `UriComponentsBuilder` automatically when it appears in a controller method signature. It knows the current request's host/port, so it can build an absolute URL. The `Location` header on a 201 response should point to the newly created resource — this is standard REST.

```php
// Laravel equivalent
return response()->json($quote, 201)
    ->header('Location', route('quotes.show', ['id' => $quote->id]));
```

---

## `by lazy` — computed once, cached forever

```kotlin
val allowedTransitions: Set<QuoteStatus> by lazy {
    when (this) {
        DRAFT -> setOf(RATED, DECLINED)
        // ...
    }
}
```

`by lazy { }` is a **property delegate**. The first time `allowedTransitions` is accessed, the lambda runs and the result is stored. Every subsequent access returns the cached result.

For an enum, this means the `Set` for each status is built once and reused. It's thread-safe by default.

```php
// PHP — no equivalent, typically just a method or static property
private static array $allowedTransitions = [
    self::DRAFT->name => [self::RATED, self::DECLINED],
];
```

---

## Package structure mirrors the architecture

```
com.example.quoteEngine
├── quote
│   ├── api              ← HTTP layer (controller, DTOs)
│   │   ├── dto
│   │   └── QuoteController.kt
│   ├── application      ← Business logic (service, mapper)
│   │   ├── QuoteService.kt
│   │   └── QuoteMapper.kt
│   ├── domain           ← Core model (no framework dependencies ideally)
│   │   ├── Quote.kt
│   │   ├── QuoteStatus.kt
│   │   ├── Driver.kt
│   │   ├── Vehicle.kt
│   │   └── exceptions...
│   └── infrastructure   ← External concerns (database)
│       └── QuoteRepository.kt
└── shared
    └── api              ← Cross-cutting (exception handler)
        └── GlobalExceptionHandler.kt
```

```
Laravel equivalent:
app/
├── Http/Controllers/
├── Http/Requests/
├── Http/Resources/      ← equivalent to dto/
├── Services/
├── Models/              ← domain + infrastructure merged (Eloquent)
└── Exceptions/
```

The key difference: Laravel's `Models/` merges domain and infrastructure. Spring keeps them separate because the domain shouldn't depend on JPA if you can avoid it (though `@Entity` makes this a leaky abstraction).

---

## `companion object { const val BASE_URL }` in tests

```kotlin
companion object {
    private const val BASE_URL = "/v1/quotes"
}
```

This is the test equivalent of defining a constant at the top of a PHP test class. It's in a `companion object` because Kotlin has no `static` — constants and static members live in companion objects. `const val` is a compile-time constant (only allowed for primitive types and String).
