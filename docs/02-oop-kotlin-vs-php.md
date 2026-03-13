# OOP: Kotlin vs PHP 8

---

## Classes are `final` by default

This is the biggest mental shift coming from PHP.

In PHP, every class is open for extension unless you write `final`. In Kotlin, it's the **opposite** — every class is `final` (sealed from inheritance) unless you explicitly mark it `open`.

```kotlin
class QuoteService(...)      // final — nobody can extend this
open class BaseService(...)  // open — now extendable
```

```php
class QuoteService { }          // implicitly open — anyone can extend
final class QuoteService { }    // explicitly sealed
```

**Why this matters:** Kotlin's default forces you to consciously decide "should this be extended?" — a principle known as "design for extension or prohibit it." Spring works around this with the `allOpen` compiler plugin in `build.gradle.kts`, which re-opens classes annotated with `@Entity`, `@Service`, etc., because Spring's proxy mechanism requires them to be extendable.

---

## Primary constructors live in the class header

```kotlin
class QuoteService(
    private val quoteRepository: QuoteRepository,
    private val quoteMapper: QuoteMapper
)
```

```php
class QuoteService {
    public function __construct(
        private readonly QuoteRepository $quoteRepository,
        private readonly QuoteMapper $quoteMapper,
    ) {}
}
```

In Kotlin, the constructor parameters declared in the class header **become properties automatically** when marked `val` or `var`. There's no `__construct` method. The PHP equivalent is constructor property promotion (PHP 8.0+), which works similarly.

---

## `data class` — value objects for free

A `data class` auto-generates `equals()`, `hashCode()`, `toString()`, and `copy()` based on its constructor properties. No Eloquent, no boilerplate.

```kotlin
// Driver.kt
data class Driver(
    val age: Int,
    val licenceYears: Int,
    val atFaultClaims: Int
)
```

```php
// PHP equivalent — you write all this yourself or use readonly classes
readonly class Driver {
    public function __construct(
        public int $age,
        public int $licenceYears,
        public int $atFaultClaims,
    ) {}

    public function equals(Driver $other): bool {
        return $this->age === $other->age && ...;
    }
}
```

PHP 8.2 `readonly` classes get you immutability. But `equals()`, `hashCode()`, and `copy()` still need to be written manually. Kotlin gives all of this for free with two keywords.

---

## Regular class vs data class — when to use which

In this codebase:

| Class | Type | Why |
|-------|------|-----|
| `Quote` | regular `class` | JPA entity — needs mutation, complex lifecycle |
| `Driver`, `Vehicle` | `data class` | value objects — defined by their data |
| `QuoteResponse` | `data class` | pure output DTO — needs equality/toString |
| `CreateQuoteRequest` | `data class` | input DTO — no identity, just data |
| `QuoteService` | regular `class` | has behaviour and state (dependencies) |
| `QuoteMapper` | regular `class` | has behaviour, not value semantics |

Rule: if the class is just a bag of data with no meaningful identity beyond its contents, use `data class`.

---

## `enum class` — enums with behaviour

PHP 8.1 backed enums can have methods. Kotlin's enum classes are similar but richer — each case can carry state and there's a `when` expression that exhaustively covers every case.

```kotlin
// QuoteStatus.kt
enum class QuoteStatus {
    DRAFT, RATED, REFERRED, BOUND, EXPIRED, DECLINED;

    val allowedTransitions: Set<QuoteStatus> by lazy {
        when (this) {
            DRAFT    -> setOf(RATED, DECLINED)
            RATED    -> setOf(REFERRED, BOUND, DECLINED, EXPIRED)
            REFERRED -> setOf(BOUND, DECLINED, EXPIRED)
            BOUND, EXPIRED, DECLINED -> emptySet()
        }
    }

    fun canTransitionTo(next: QuoteStatus): Boolean = next in allowedTransitions
}
```

```php
enum QuoteStatus {
    case DRAFT;
    case RATED;
    // ...

    public function allowedTransitions(): array {
        return match($this) {
            self::DRAFT    => [self::RATED, self::DECLINED],
            self::RATED    => [self::REFERRED, self::BOUND, self::DECLINED, self::EXPIRED],
            default        => [],
        };
    }
}
```

**Key difference: `by lazy`**. The `allowedTransitions` property is computed once on first access and cached. PHP has no equivalent — you'd call the method every time or cache it yourself.

---

## `companion object` — where static lives

Kotlin has no `static` keyword. Static-like members live in a `companion object` block inside the class.

```kotlin
// QuoteControllerTest.kt
companion object {
    private const val BASE_URL = "/v1/quotes"
}
```

```php
class QuoteControllerTest {
    private const BASE_URL = '/v1/quotes';
}
```

The `companion object` is literally a singleton object that lives inside the class. `const` is for compile-time constants (only primitives and strings).

---

## Extension functions — adding methods to classes you don't own

This has no PHP equivalent. You can add a function that appears to be a method on any class, including ones you can't modify.

```kotlin
// QuoteMapper.kt
private fun VehicleRequest.toDomain() = Vehicle(year = year, make = make, model = model, annualKm = annualKm)
private fun DriverRequest.toDomain() = Driver(age = age, licenceYears = licenceYears, atFaultClaims = atFaultClaims)
private fun Vehicle.toResponse() = VehicleResponse(year = year, make = make, model = model, annualKm = annualKm)
```

`fun VehicleRequest.toDomain()` means: "add a method called `toDomain()` to the `VehicleRequest` class." Inside the function body, `this` is the `VehicleRequest` instance, so `year`, `make`, etc. are available directly.

This lets you put conversion logic close to where it's used, without touching the original class — great for keeping DTOs and domain objects completely separate.

---

## Secondary constructors

When a class needs multiple ways to be constructed:

```kotlin
// InvalidTransitionException.kt
class InvalidTransitionException : RuntimeException {
    constructor(from: QuoteStatus, to: QuoteStatus) : super("Cannot transition quote from $from to $to")
    constructor(message: String) : super(message)
}
```

```php
// PHP — single constructor with overloading via defaults or named constructors
class InvalidTransitionException extends \RuntimeException {
    public static function fromTransition(QuoteStatus $from, QuoteStatus $to): self {
        return new self("Cannot transition quote from $from to $to");
    }
}
```

PHP can't overload constructors, so named static factory methods are the idiom. Kotlin supports multiple `constructor` declarations. Each calls `super(...)` to pass the message up to `RuntimeException`.

Notice `class Foo : RuntimeException` — that's inheritance. No `extends` keyword, just `:`.

---

## Inheritance syntax

```kotlin
class QuoteNotFoundException(id: UUID) : RuntimeException("Quote $id not found")
class InvalidTransitionException : RuntimeException { ... }
```

```php
class QuoteNotFoundException extends \RuntimeException { ... }
```

`:` replaces both `extends` and `implements`. If you're extending a class, the parent constructor is called with `()`:

```kotlin
class Foo : Bar("argument")   // calls Bar's constructor
class Foo : MyInterface       // implements interface — no ()
```

---

## `object` — singleton, not `static`

Kotlin has a first-class singleton with the `object` keyword:

```kotlin
object QuoteRegistry {
    val all = mutableListOf<Quote>()
}
```

Not used in this project, but good to know. PHP would use the Singleton pattern manually.

---

## Interfaces — same idea, cleaner syntax

```kotlin
interface Priceable {
    fun calculatePremium(): Double
    fun description(): String = "Standard policy"  // default implementation
}
```

```php
interface Priceable {
    public function calculatePremium(): float;
    // PHP 8 can't have default implementations in interfaces — use abstract classes
}
```

Kotlin interfaces can have default method implementations. PHP cannot — you'd use an abstract class for that.

---

## Scope functions: `apply`, `let`, `run`, `also`

These are standard library functions that let you run a block of code on an object and return either the object or the block's result. The most important ones:

### `apply` — configure an object, return the object

```kotlin
// QuoteMapper.kt
fun toDomain(request: CreateQuoteRequest): Quote = Quote().apply {
    policyHolderName = request.policyHolderName
    vehicle = request.vehicle.toDomain()
    driver = request.driver.toDomain()
    status = QuoteStatus.DRAFT
}
```

```php
// PHP equivalent — no built-in, write it manually
$quote = new Quote();
$quote->policyHolderName = $request->policyHolderName;
$quote->vehicle = $this->toDomain($request->vehicle);
$quote->driver = $this->toDomain($request->driver);
$quote->status = QuoteStatus::DRAFT;
return $quote;
```

Inside `apply { }`, `this` is the `Quote` object, so you can set properties directly. The block returns the object itself. This is the builder pattern without a builder class.

### `let` — run a block on a nullable, return the result

```kotlin
val upper = name?.let { it.uppercase() }  // null if name is null, otherwise uppercased
```

```php
$upper = $name !== null ? strtoupper($name) : null;
```

---

## Access modifiers — defaults differ

| PHP | Kotlin | Meaning |
|-----|--------|---------|
| `public` | _(default)_ | Visible everywhere |
| `protected` | `protected` | Visible in class and subclasses |
| `private` | `private` | Visible in class only |
| _(no equivalent)_ | `internal` | Visible within the same module |

Kotlin's default is `public`. PHP's default is also `public` for functions but you should always write it explicitly in PHP. In Kotlin, omitting visibility means public — you only write it when you want to restrict.

In this codebase, `private` is used for helpers like `transitionTo()` in `QuoteService` and the extension functions in `QuoteMapper`.
