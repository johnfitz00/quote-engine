# Learning Notes: Kotlin + Spring Boot (from PHP/Laravel)

These docs cover everything encountered in the `quoteEngine` project, explained for someone coming from PHP 8 and Laravel.

---

## Documents

| File | What it covers |
|------|----------------|
| [01-kotlin-syntax-vs-php.md](01-kotlin-syntax-vs-php.md) | Language syntax: `val`/`var`, types, nullability, `when`, lambdas, string templates, collections |
| [02-oop-kotlin-vs-php.md](02-oop-kotlin-vs-php.md) | OOP differences: `data class`, `enum class`, `open`/`final`, extension functions, `companion object`, scope functions (`apply`, `let`) |
| [03-spring-vs-laravel.md](03-spring-vs-laravel.md) | Framework concepts: routing annotations, DI, `@Transactional`, `@ControllerAdvice`, validation, `ResponseEntity`, repositories |
| [04-jpa-vs-eloquent.md](04-jpa-vs-eloquent.md) | Database/ORM: `@Entity`, `@Embedded`, Active Record vs Data Mapper, `Optional`, `saveAndFlush`, lazy loading |
| [05-testing-in-spring.md](05-testing-in-spring.md) | Testing: `@WebMvcTest` vs `@SpringBootTest`, MockK, `MockMvc`, `@Nested`, backtick test names |
| [06-architecture-patterns.md](06-architecture-patterns.md) | Layered architecture, DTO pattern, state machine in enum, `GlobalExceptionHandler`, `by lazy`, package structure |

---

## Quick reference: things that surprise PHP developers

| PHP | Kotlin | Note |
|-----|--------|------|
| `$variable` | `variable` | No `$` prefix |
| `string $x` | `x: String` | Type after the name |
| `?string` | `String?` | `?` position is mirrored |
| `function` | `fun` | |
| `public` default in class | default is `public` | Both default public; Kotlin you just omit it |
| `extends` | `: ParentClass()` | `:` replaces `extends`; note the `()` |
| `implements` | `: Interface` | Same `:`, no `()` for interfaces |
| `static` | `companion object` | No `static` keyword |
| `final class` | `class` | Final is the **default** in Kotlin |
| open class | `open class` | Must explicitly opt in to inheritance |
| `match($x)` | `when (x)` | Same idea; `when` can be exhaustive |
| `in_array($x, $arr)` | `x in collection` | Infix operator |
| `array_map(fn, arr)` | `list.map { it... }` | Collection method, not function |
| `new Foo()` | `Foo()` | No `new` keyword |
| `__construct` | primary constructor | In the class header |
| `null ?? 'default'` | `value ?: "default"` | Elvis operator — same! |
| `$obj?->method()` | `obj?.method()` | Nullsafe — same idea, different syntax |
| `throw new Foo()` | `throw Foo()` | No `new` |
