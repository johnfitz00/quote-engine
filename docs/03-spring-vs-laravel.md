# Spring Boot vs Laravel

Both are opinionated frameworks. This maps the concepts you know from Laravel onto their Spring equivalents, using code from this project.

---

## The biggest conceptual difference: annotations vs configuration

Laravel uses PHP files for configuration and service binding. Spring uses **annotations** — special markers on classes and methods that the framework reads at startup to understand how to wire things together.

```kotlin
@RestController           // "this class handles HTTP requests"
@RequestMapping("/v1/quotes")
class QuoteController(...)
```

```php
// Laravel — route registration in routes/api.php
Route::apiResource('quotes', QuoteController::class);
```

Spring's annotations live directly on the class. Laravel's routing lives in a separate file. Both achieve the same result — Spring's approach is more self-contained.

---

## Routing: annotations vs `routes/api.php`

```kotlin
// QuoteController.kt
@RestController
@RequestMapping("/v1/quotes")
class QuoteController {

    @PostMapping           // POST /v1/quotes
    fun create(...) { }

    @GetMapping("/{id}")   // GET /v1/quotes/{id}
    fun getById(@PathVariable id: UUID) { }

    @PutMapping("/{id}")   // PUT /v1/quotes/{id}
    fun update(...) { }

    @DeleteMapping("/{id}") // DELETE /v1/quotes/{id}
    fun decline(...) { }

    @PostMapping("/{id}/rate")  // POST /v1/quotes/{id}/rate
    fun rate(...) { }
}
```

```php
// Laravel routes/api.php
Route::post('quotes', [QuoteController::class, 'create']);
Route::get('quotes/{id}', [QuoteController::class, 'show']);
Route::put('quotes/{id}', [QuoteController::class, 'update']);
Route::delete('quotes/{id}', [QuoteController::class, 'decline']);
Route::post('quotes/{id}/rate', [QuoteController::class, 'rate']);
```

---

## Path variables and query parameters

```kotlin
// @PathVariable — route segment: GET /v1/quotes/{id}
fun getById(@PathVariable id: UUID): ResponseEntity<QuoteResponse>

// @RequestParam — query string: GET /v1/quotes?status=DRAFT
fun list(@RequestParam(required = false) status: QuoteStatus?): ResponseEntity<List<QuoteResponse>>
```

```php
// Laravel — route model binding / route parameters
public function show(UUID $id): JsonResponse

// Query params accessed via Request object
public function index(Request $request): JsonResponse {
    $status = $request->query('status');
}
```

In Spring, parameters are declared in the method signature with annotations. In Laravel, you use the `Request` object or route model binding.

---

## Request body: `@RequestBody` and `@Valid`

```kotlin
@PostMapping
fun create(
    @Valid @RequestBody request: CreateQuoteRequest,
    uriBuilder: UriComponentsBuilder
): ResponseEntity<QuoteResponse>
```

```php
// Laravel — automatic resolution from request body via FormRequest
public function store(CreateQuoteRequest $request): JsonResponse
```

`@RequestBody` tells Spring to deserialise the JSON body into a `CreateQuoteRequest` object. `@Valid` triggers Bean Validation (the `@NotBlank`, `@Min` annotations on the DTO). Laravel does this automatically when you type-hint a `FormRequest`.

---

## Dependency injection: constructor injection

Both frameworks use dependency injection. Spring's preferred style (and the only style used in this project) is **constructor injection**:

```kotlin
// QuoteController.kt
@RestController
class QuoteController(private val quoteService: QuoteService)

// QuoteService.kt
@Service
class QuoteService(
    private val quoteRepository: QuoteRepository,
    private val quoteMapper: QuoteMapper
)
```

```php
// Laravel — constructor injection
class QuoteController extends Controller {
    public function __construct(
        private readonly QuoteService $quoteService
    ) {}
}
```

Spring automatically finds `QuoteService` because it's annotated `@Service`. Laravel finds it because it's registered in the service container (or auto-resolved via reflection). The result is the same: you declare what you need, the framework provides it.

---

## Stereotype annotations: `@Service`, `@Component`, `@Repository`

These tell Spring "register this class in the container":

| Spring | Laravel equivalent |
|--------|--------------------|
| `@Service` | `app()->bind(QuoteService::class, ...)` or just auto-resolution |
| `@Component` | Same — generic registration |
| `@Repository` | Same — specifically for data access |
| `@RestController` | `Route::...` + controller registration |

```kotlin
@Service     class QuoteService(...)    // business logic layer
@Component   class QuoteMapper(...)     // utility, doesn't fit other stereotypes
@Repository  interface QuoteRepository  // data access — Spring Data generates the implementation
```

You never call `new QuoteService()` yourself — Spring instantiates and injects everything.

---

## `@Transactional` — wrapping database work

```kotlin
// QuoteService.kt
@Transactional
fun createQuote(request: CreateQuoteRequest): QuoteResponse { ... }

@Transactional(readOnly = true)
fun getQuote(id: UUID): QuoteResponse { ... }
```

```php
// Laravel equivalent
DB::transaction(function () use ($request) {
    // ...
});
```

`@Transactional` wraps the method in a database transaction. If the method throws, the transaction rolls back. `readOnly = true` is an optimisation hint for read-only queries. In Laravel you'd call `DB::transaction()` manually. In Spring you annotate the method and it happens automatically.

---

## Global exception handling: `@ControllerAdvice` vs `app/Exceptions/Handler.php`

```kotlin
// GlobalExceptionHandler.kt
@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(QuoteNotFoundException::class)
    fun handleNotFound(ex: QuoteNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(ex.message!!))

    @ExceptionHandler(InvalidTransitionException::class)
    fun handleInvalidTransition(ex: InvalidTransitionException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message!!))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> { ... }
}
```

```php
// Laravel app/Exceptions/Handler.php
public function register(): void
{
    $this->renderable(function (QuoteNotFoundException $e) {
        return response()->json(['message' => $e->getMessage()], 404);
    });
}
```

`@ControllerAdvice` is a global interceptor. `@ExceptionHandler(Foo::class)` says "call this method when a `Foo` exception is thrown anywhere in any controller." The controller never catches exceptions — it just lets them bubble. The handler translates them to HTTP responses. This is exactly like Laravel's `Handler.php`.

---

## Validation: Bean Validation annotations vs Form Requests

```kotlin
// CreateQuoteRequest.kt
data class CreateQuoteRequest(
    @NotBlank val policyHolderName: String,
    @Valid @NotNull val vehicle: VehicleRequest,
    @Valid @NotNull val driver: DriverRequest
)

// DriverRequest.kt
data class DriverRequest(
    @Min(16) val age: Int,
    @Min(0) val licenceYears: Int,
    @Min(0) val atFaultClaims: Int
)
```

```php
// Laravel FormRequest
class CreateQuoteRequest extends FormRequest {
    public function rules(): array {
        return [
            'policyHolderName' => ['required', 'string'],
            'driver.age'       => ['required', 'integer', 'min:16'],
        ];
    }
}
```

In Spring, validation rules are annotations directly on the DTO class. In Laravel, rules are an array in a separate `FormRequest` class. When `@Valid` triggers and a constraint fails, Spring throws `MethodArgumentNotValidException` — which the `GlobalExceptionHandler` catches and returns as 422.

The `@Valid` annotation on a nested object (`vehicle`, `driver`) tells Spring to recurse into that object's annotations too. Without `@Valid`, only the top-level constraints are checked.

---

## Returning HTTP responses: `ResponseEntity`

```kotlin
// 201 Created with Location header
ResponseEntity.created(location).body(quote)

// 200 OK
ResponseEntity.ok(quoteService.getQuote(id))

// 202 Accepted
ResponseEntity.status(HttpStatus.ACCEPTED).body(quoteService.rateQuote(id))

// 204 No Content
ResponseEntity.noContent().build()
```

```php
// Laravel
return response()->json($quote, 201)->header('Location', $location);
return response()->json($quote, 200);
return response()->json($quote, 202);
return response()->noContent();
```

`ResponseEntity<T>` wraps a response body of type `T` plus status code and headers. It's the return type for controller methods. The generic `<T>` is type-checked at compile time — you can't accidentally return the wrong type.

---

## The repository pattern: Spring Data vs Eloquent

Laravel uses the Active Record pattern (models query themselves). Spring uses the Repository pattern (a separate class handles queries).

```kotlin
// QuoteRepository.kt — you only write the interface
interface QuoteRepository : JpaRepository<Quote, UUID> {
    fun findByStatus(status: QuoteStatus): List<Quote>
}
```

Spring Data generates the entire implementation — `findAll()`, `findById()`, `save()`, `delete()`, etc. are free. For `findByStatus`, Spring reads the method name and generates the query automatically ("find Quote where status = ?").

```php
// Laravel Eloquent — model IS the repository
Quote::all();
Quote::find($id);
Quote::where('status', $status)->get();
Quote::create($data);
```

In Laravel, `Quote::all()` is a static method on the model class itself. In Spring, you inject `QuoteRepository` and call instance methods. Neither approach is inherently better — they reflect different ORM philosophies.

---

## `saveAndFlush` vs `save()`

```kotlin
quoteRepository.saveAndFlush(quote)  // save AND immediately sync to DB
quoteRepository.save(quote)          // save, may be batched
```

```php
$quote->save();  // always writes immediately in Eloquent
```

`saveAndFlush` forces an immediate SQL `INSERT`/`UPDATE`. This matters in this project because we want the `createdAt`/`updatedAt` timestamps (set by the database) to be populated before we call `toResponse()`. Without `flush`, those timestamps might still be null inside the same transaction.

---

## `Optional` and `orElseThrow`

```kotlin
quoteRepository.findById(id)
    .orElseThrow { QuoteNotFoundException(id) }
```

```php
Quote::findOrFail($id);  // throws ModelNotFoundException
// or:
$quote = Quote::find($id);
if (!$quote) throw new QuoteNotFoundException($id);
```

`findById()` in Spring returns an `Optional<Quote>` — a container that may or may not hold a value. `orElseThrow { }` says "give me the value or execute this lambda and throw the exception it returns." Eloquent's `findOrFail()` is a direct equivalent.

---

## Testing: `@WebMvcTest` vs Laravel's `TestCase`

```kotlin
@WebMvcTest(QuoteController::class)
class QuoteControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @MockkBean lateinit var quoteService: QuoteService
}
```

```php
class QuoteControllerTest extends TestCase {
    use RefreshDatabase;

    public function test_create_quote(): void {
        $this->postJson('/api/quotes', [...])
             ->assertStatus(201);
    }
}
```

`@WebMvcTest` boots only the web layer (controller + serialisation). The service is mocked with `@MockkBean` (springmockk library). This is very fast — no database, no full Spring context. It's equivalent to testing a Laravel controller with a mocked service and no database.

`@SpringBootTest` (used in `QuoteIntegrationTest`) is the full equivalent of Laravel's `RefreshDatabase` tests — boots everything, hits a real H2 in-memory database.
