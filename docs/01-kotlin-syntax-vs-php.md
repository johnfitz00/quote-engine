# Kotlin Syntax vs PHP 8

Everything here is grounded in code you've already written in this project.

---

## Variables: `val` and `var` (not `$`)

Kotlin has two variable keywords. There is no `$` prefix.

```kotlin
val name = "Alice"   // immutable — like a PHP const at local scope, cannot be reassigned
var status = "DRAFT" // mutable — reassignable, like a normal PHP variable
```

```php
// PHP — everything is mutable, $ prefix always
$name = 'Alice';
$status = 'DRAFT';
```

**Rule of thumb:** always use `val` first. Only use `var` when you genuinely need to reassign it. In this project `Quote.kt` uses `var` on all fields because JPA needs to mutate them after construction.

---

## Types are written after the name, not before

```kotlin
val name: String = "Alice"
fun getQuote(id: UUID): QuoteResponse { ... }
```

```php
string $name = 'Alice';
function getQuote(UUID $id): QuoteResponse { ... }
```

Kotlin puts the type **after** a colon. PHP (since 7) puts the type **before** the variable.

---

## Type inference — the compiler fills it in

Kotlin infers the type when it's obvious, so you don't need to write it:

```kotlin
val name = "Alice"        // compiler knows it's String
val quotes = listOf(...)  // compiler knows it's List<QuoteResponse>
```

PHP has no type inference — types are either omitted (dynamic) or explicitly declared.

---

## Nullable types: `String?` vs `?string`

Both languages have nullable types, but the syntax is mirrored:

```kotlin
var id: UUID? = null         // Kotlin: ? comes AFTER the type
var name: String? = null
```

```php
?string $name = null;        // PHP: ? comes BEFORE the type
?UUID   $id   = null;
```

In `Quote.kt`, most fields start as `null` because JPA constructs the object first, then populates it:

```kotlin
var id: UUID? = null
var policyHolderName: String? = null
```

---

## The `!!` operator (non-null assertion)

This has no PHP equivalent. It tells the compiler "I promise this is not null — if it is, crash."

```kotlin
// QuoteMapper.kt
id = quote.id!!
policyHolderName = quote.policyHolderName!!
createdAt = quote.createdAt!!
```

If `quote.id` is `null` at runtime, this throws a `NullPointerException`. You use it when you know something can't be null in practice but the type system doesn't know that yet (e.g., JPA-populated fields after a save).

In PHP you would just use the value directly — the language doesn't track nullability at the type level the same way.

---

## Safe-call operator `?.` vs PHP's `?->`

Both languages use this to call a method only if the value isn't null:

```kotlin
val length = name?.length   // null if name is null, otherwise name.length
```

```php
$length = $name?->length(); // PHP 8 nullsafe operator
```

Almost identical. The difference: Kotlin's `?.` works on properties too (not just methods), since properties and method calls have the same syntax.

---

## String templates: `"$var"` and `"${expr}"`

```kotlin
"Quote $id not found"                         // simple variable
"Cannot transition quote from $from to $to"   // two variables
"Cannot edit a ${quote.status} quote"         // expression in braces
```

```php
"Quote $id not found"           // PHP also supports this
"Cannot edit a {$quote->status} quote"  // PHP uses {} for property access
```

Kotlin uses `${}` (dollar + braces). PHP uses `{}` alone. For simple variables both are just `$name`.

---

## Multi-line strings: `trimIndent()`

```kotlin
// QuoteControllerTest.kt
val json = """
    {
      "policyHolderName": "Alice",
      "vehicle": { "year": 2020 }
    }
""".trimIndent()
```

```php
$json = <<<JSON
{
  "policyHolderName": "Alice"
}
JSON;
```

Kotlin uses `"""triple quotes"""`. The `.trimIndent()` call strips the leading whitespace caused by code indentation — equivalent to PHP's heredoc which does this automatically.

---

## `when` expression vs PHP's `match`

Kotlin's `when` is PHP 8's `match`, but more powerful:

```kotlin
// QuoteStatus.kt
val allowedTransitions: Set<QuoteStatus> by lazy {
    when (this) {
        DRAFT    -> setOf(RATED, DECLINED)
        RATED    -> setOf(REFERRED, BOUND, DECLINED, EXPIRED)
        REFERRED -> setOf(BOUND, DECLINED, EXPIRED)
        BOUND, EXPIRED, DECLINED -> emptySet()
    }
}
```

```php
// PHP equivalent
$allowedTransitions = match($this) {
    self::DRAFT    => [self::RATED, self::DECLINED],
    self::RATED    => [self::REFERRED, self::BOUND, ...],
    default        => [],
};
```

Key differences:
- Kotlin `when` can be an **expression** (returns a value) or a **statement**
- Multiple values in one branch: `BOUND, EXPIRED, DECLINED -> ...` (PHP uses `match` too)
- Kotlin `when` is exhaustive when used as an expression — the compiler will error if you miss a case, which is why there's no `else` needed here

---

## `in` operator for collection membership

```kotlin
// QuoteStatus.kt
fun canTransitionTo(next: QuoteStatus): Boolean = next in allowedTransitions
```

```php
in_array($next, $allowedTransitions)
```

Kotlin's `in` reads like English. PHP needs `in_array()`.

---

## Collections are not arrays

PHP uses arrays for everything. Kotlin has distinct typed collections:

```kotlin
setOf(RATED, DECLINED)        // Set — no duplicates, unordered
listOf(quote1, quote2)        // List — ordered, duplicates allowed
mapOf("key" to "value")       // Map — key/value pairs
emptySet()                    // empty Set
emptyList()                   // empty List
```

These are all **immutable** by default. Mutable versions exist: `mutableListOf()`, `mutableSetOf()`.

The type is parameterised: `List<QuoteResponse>`, `Set<QuoteStatus>` — equivalent to PHP generics in docblocks but enforced by the compiler.

---

## Lambda syntax

```kotlin
// GlobalExceptionHandler.kt
val fieldErrors = ex.bindingResult.fieldErrors
    .map { "${it.field}: ${it.defaultMessage}" }
```

```php
$fieldErrors = array_map(
    fn($error) => "{$error->field}: {$error->defaultMessage}",
    $ex->bindingResult->fieldErrors
);
```

Kotlin lambdas use `{ }`. When a lambda has a single parameter, it's automatically named `it` — you don't need to declare it. PHP 8 uses `fn($x) => ...` arrow functions.

---

## Single-expression functions: `=` instead of `{ return }`

```kotlin
// QuoteStatus.kt
fun canTransitionTo(next: QuoteStatus): Boolean = next in allowedTransitions

// QuoteController.kt
fun getById(@PathVariable id: UUID): ResponseEntity<QuoteResponse> =
    ResponseEntity.ok(quoteService.getQuote(id))
```

```php
public function getById(UUID $id): ResponseEntity
{
    return ResponseEntity::ok($this->quoteService->getQuote($id));
}
```

When a function body is a single expression, Kotlin lets you use `=` and drop the `{}` and `return`. This is very common and worth getting comfortable with.

---

## `fun` keyword and no `public` noise

```kotlin
fun createQuote(request: CreateQuoteRequest): QuoteResponse { ... }
```

```php
public function createQuote(CreateQuoteRequest $request): QuoteResponse { ... }
```

`fun` replaces `function`. Kotlin methods default to `public` so you don't need to write it. You only write visibility when it's `private`, `protected`, or `internal`.

---

## Named arguments

```kotlin
// QuoteMapper.kt
Vehicle(year = year, make = make, model = model, annualKm = annualKm)
```

```php
new Vehicle(year: $year, make: $make, model: $model, annualKm: $annualKm)
```

Both PHP 8 and Kotlin support named arguments with the same intent. Syntax is `name = value` in Kotlin vs `name: value` in PHP.
