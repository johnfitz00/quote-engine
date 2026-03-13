# Testing in Spring / Kotlin vs PHP

---

## Two test types: unit (fast) and integration (full stack)

| | PHP / PHPUnit | Spring equivalent |
|-|--------------|-------------------|
| Fast, mocked | `TestCase` with mocked deps | `@WebMvcTest` |
| Full stack, real DB | `TestCase` + `RefreshDatabase` | `@SpringBootTest` + `@AutoConfigureMockMvc` |

This project has both:
- `QuoteControllerTest` — fast controller tests, no database
- `QuoteIntegrationTest` — full Spring context, H2 in-memory database

---

## `@WebMvcTest` — the fast controller test

```kotlin
@WebMvcTest(QuoteController::class)
class QuoteControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var quoteService: QuoteService
}
```

`@WebMvcTest(QuoteController::class)` boots only:
- The controller itself
- The serialisation layer (Jackson)
- The validation layer
- The `GlobalExceptionHandler` (`@ControllerAdvice`)

It does **not** boot:
- The database
- JPA/Hibernate
- The service or repository

The service is replaced with a mock: `@MockkBean` (from the `springmockk` library — the Kotlin-friendly mock library). This is equivalent to:

```php
// Laravel test with mocked service
$this->mock(QuoteService::class, function (MockInterface $mock) {
    $mock->shouldReceive('createQuote')->andReturn($fakeQuote);
});
```

---

## `MockMvc` — making fake HTTP requests

```kotlin
mockMvc.post("/v1/quotes") {
    contentType = MediaType.APPLICATION_JSON
    content = validCreateJson()
}.andExpect {
    status { isCreated() }
    header { string("Location", "http://localhost/v1/quotes/$id") }
    jsonPath("$.id") { value(id.toString()) }
    jsonPath("$.status") { value("DRAFT") }
}
```

```php
// Laravel equivalent
$this->postJson('/api/quotes', $data)
     ->assertStatus(201)
     ->assertHeader('Location', "/api/quotes/$id")
     ->assertJsonPath('id', $id)
     ->assertJsonPath('status', 'DRAFT');
```

`MockMvc` simulates HTTP requests without a real HTTP server. It's Laravel's `$this->postJson()` equivalent. The DSL is:
- `mockMvc.post(url) { ... }` — build the request
- `.andExpect { ... }` — assert the response
- `jsonPath("$.field")` — JSON path assertion (same syntax as Laravel's `assertJsonPath`)

---

## Setting up mocks: `every` and `justRun`

These come from MockK (the Kotlin mock library):

```kotlin
// Mock a method that returns a value
every { quoteService.createQuote(any()) } returns aQuoteResponse(id)

// Mock a method that throws
every { quoteService.getQuote(id) } throws QuoteNotFoundException(id)

// Mock a void method (no return value)
justRun { quoteService.decline(id) }
```

```php
// PHPUnit mocks
$this->quoteService
    ->expects($this->once())
    ->method('createQuote')
    ->willReturn($fakeQuote);

$this->quoteService
    ->method('getQuote')
    ->willThrowException(new QuoteNotFoundException($id));
```

`any()` is a matcher — "any argument of the right type." MockK's syntax is more readable than PHPUnit's mock builder.

---

## Backtick test names

```kotlin
@Test
fun `returns 201 with Location header`() { ... }

@Test
fun `quote not found returns 404 with error body`() { ... }
```

```php
/** @test */
public function it_returns_201_with_location_header(): void { ... }

#[Test]
public function quote_not_found_returns_404(): void { ... }
```

Kotlin allows any string as a function name if it's in backticks. This means test names can be full English sentences with spaces — far more readable than PHP's `snake_case_function_names`. This is purely cosmetic but widely used in Kotlin tests.

---

## `@Nested` — grouping tests by endpoint

```kotlin
@Nested
inner class CreateQuote {
    @Test
    fun `returns 201 with Location header`() { ... }

    @Test
    fun `invalid input returns 422 with field errors`() { ... }
}

@Nested
inner class GetQuoteById {
    @Test
    fun `returns 200 with quote body`() { ... }
}
```

```php
// PHP doesn't have @Nested — you use separate test classes or method prefixes
class QuoteControllerCreateTest extends TestCase { ... }
class QuoteControllerGetTest extends TestCase { ... }
```

`@Nested` + `inner class` groups related tests visually and in the test runner output. The output shows as "CreateQuote > returns 201 with Location header". PHP has no equivalent within a single class.

`inner class` is required (not just `class`) because a nested class in Kotlin doesn't have access to the outer class by default. `inner` grants that access.

---

## Test fixtures: private helper methods

```kotlin
private fun aQuoteResponse(
    id: UUID = UUID.randomUUID(),
    policyHolderName: String = "Alice Martin",
    status: String = "DRAFT"
) = QuoteResponse(
    id = id,
    policyHolderName = policyHolderName,
    vehicle = VehicleResponse(year = 2020, make = "Toyota", model = "Corolla", annualKm = 15000),
    driver = DriverResponse(age = 34, licenceYears = 12, atFaultClaims = 0),
    status = status,
    createdAt = Instant.now(),
    updatedAt = Instant.now()
)
```

This is a factory method with default parameter values. `UUID.randomUUID()` is the default for `id` — each call gets a fresh UUID unless you pass one. This is equivalent to PHP's model factories or `make()` helpers.

Default parameters in functions: when calling `aQuoteResponse(id)`, only `id` is specified; the rest use their defaults. This removes the need for method overloading.

---

## `@SpringBootTest` — the integration test

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class QuoteIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var quoteRepository: QuoteRepository

    @BeforeEach
    fun setUp() {
        quoteRepository.deleteAll()
    }
}
```

```php
class QuoteIntegrationTest extends TestCase {
    use RefreshDatabase;  // truncates tables before each test
}
```

`@SpringBootTest` starts the full application context — real service, real repository, real H2 database. `@AutoConfigureMockMvc` injects a `MockMvc` that routes through the full stack.

`@BeforeEach` (equivalent to PHPUnit's `setUp()`) deletes all quotes before each test so tests don't bleed into each other. `RefreshDatabase` in Laravel does the same via transaction rollback.

---

## `lateinit var` — deferred initialisation

```kotlin
@Autowired lateinit var mockMvc: MockMvc
@Autowired lateinit var quoteRepository: QuoteRepository
```

`lateinit` tells the compiler "I'll set this before I use it — trust me." Without it, you'd need to make these nullable (`MockMvc?`). `@Autowired` is how Spring injects the values. The compiler doesn't know Spring will inject them, so `lateinit` bridges that gap.

```php
// PHP — properties are nullable by default, no equivalent needed
private MockMvc $mockMvc;  // PHP 8 typed property, must be initialised before use
```

---

## `repeat(n) { ... }` — looping in tests

```kotlin
// QuoteIntegrationTest.kt
repeat(2) { createQuoteAndExtractId() }
```

```php
foreach (range(1, 2) as $_) {
    $this->createQuoteAndExtractId();
}
```

`repeat(n)` is a standard library function. It takes a lambda and calls it `n` times. Clean and readable.

---

## `substringAfterLast` — string utilities

```kotlin
val id = location.substringAfterLast("/")
```

```php
$id = substr($location, strrpos($location, '/') + 1);
// or:
$id = last(explode('/', $location));
```

Kotlin's standard library has a rich set of string extension functions. `substringAfterLast("/")` returns everything after the last `/`. PHP requires `strrpos` + `substr` or `explode`. These kinds of utility functions are available directly on the `String` type in Kotlin without importing anything.
