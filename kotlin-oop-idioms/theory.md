# Kotlin OOP Idioms — Theory

Read this once before touching the TODOs. Everything the skeleton asks you to write is covered below.

## 1. Enums with per-constant behavior

A Kotlin enum constant can override a member itself, instead of the enum class having one method that branches on `when (this)`:

```kotlin
enum class Currency(val symbol: String) {
    USD("$") {
        override fun format(cents: Long): String = "..."
    },
    EUR("€") {
        override fun format(cents: Long): String = "..."
    };              // <- semicolon required here: it separates the constant list from the
                     //    member declarations that follow, only needed when there ARE members after
    abstract fun format(cents: Long): String
}
```

This isn't new to Kotlin — Java has supported constant-specific class bodies since Java 5 — but it's reached for far more often in Kotlin, because the alternative (`when` over the enum with an `else`) loses exhaustiveness the moment someone adds a new constant and forgets a branch, whereas `abstract fun` forces every constant to implement it or the code doesn't compile.

**When to use this vs. a `sealed class`/`sealed interface`:** enums are right when every case has the *same shape* (same properties, e.g. every `Currency` has a `symbol: String`) and just differs in *behavior* or in a couple of constant values. Sealed types are right when different cases need genuinely different *shape* — different constructor parameters, some cases carrying data others don't. `OrderError`'s three variants (`MissingField(field)`, `InvalidType(field, expected)`, `InvalidValue(field, reason)`) needed a sealed class because they don't share a constructor shape; `Currency`'s cases do, so an enum is the better fit there.

## 2. Companion object vs. `object` declaration vs. object expression

Three different things that all use the `object` keyword, easy to conflate:

- **`object Foo { ... }`** (object *declaration*) — a standalone singleton. Lazily created on first access, thread-safe by default (the JVM guarantees a class is only initialized once, even under concurrent first access). Use it for a shared service with no natural "owning class" — `ExchangeRates` in this project is one: it's not a property of any particular `Money`, it's a shared lookup table.

- **`companion object` inside a class** — also a singleton, but scoped to and named through its class: `Money.of(...)`, `Money.zero(...)`. This is the direct idiomatic replacement for Java's static factory methods and constants. A class can have at most one companion object. Reach for this when the singleton behavior is specifically *about* the class it lives in (constructing instances of it, holding constants relevant to it) — that's the `Money.companion object` in this project.

- **Object expression** — `object : SomeInterface { ... }` used as an *expression*, not a declaration: an anonymous, throwaway implementation created inline, typically passed straight into a function call. This is Kotlin's equivalent of a Java anonymous inner class. You'll use one to build a one-off `Comparator<Money>` to sort a list a specific way, without giving that comparator a name or reusing it anywhere else.

Rule of thumb: needs a name and reuse and belongs to a class → companion object. Needs a name and reuse but doesn't belong to any one class → object declaration. Needed once, inline, thrown away → object expression.

## 3. Operator overloading

Kotlin lets specific function names, marked with the `operator` modifier, be invoked through symbolic syntax instead of a normal call:

| Function | Invoked as |
|---|---|
| `plus(other)` | `a + b` |
| `minus(other)` | `a - b` |
| `times(other)` | `a * b` |
| `unaryMinus()` | `-a` |
| `compareTo(other): Int` | `a < b`, `a > b`, `a <= b`, `a >= b` |
| `get(index)` / `set(index, value)` | `a[i]` / `a[i] = v` |
| `invoke(...)` | `a(...)` |
| `contains(item): Boolean` | `item in a` |

The `operator` modifier is mandatory and checked by the compiler — you can't accidentally make a function invokable as `+` by naming it `plus`; you have to opt in explicitly, which is a deliberate guard rail against "magic" behavior nobody asked for.

The judgment call is when to bother: operator overloading is good when the symbol's meaning is unsurprising (`Money + Money` reads exactly like it behaves) and a genuine anti-pattern when it isn't (overloading `+` to "merge" two `User` objects, say, is the kind of cleverness that makes code harder to read, not easier — a plain named function like `merge(other)` would be clearer). If you'd have to explain what the operator does in a code comment, it probably shouldn't be an operator.

Note that `compareTo` must return an `Int` (negative/zero/positive, same contract as Java's `Comparable`) — implementing it is what lets you use `<`/`>`/`<=`/`>=` directly on your type without implementing the full `Comparable` interface (though `operator fun compareTo` combined with `: Comparable<T>` is also common when you want the type usable with things like `sorted()` too).

## 4. Property delegation

`val`/`var` properties can hand off their getter (and setter, for `var`) to another object via `by`, instead of you writing the storage and access logic by hand:

```kotlin
val total: Money by lazy { /* computed once, cached forever after */ }
var lastTransaction: Money? by Delegates.observable(null) { property, old, new -> /* runs on every write */ }
```

**`by lazy { ... }`** is a standard-library delegate: the block runs exactly once, on the *first read* of the property, and the result is cached for every read after that. This is not "recompute if inputs changed" — it's "compute once, then never again," full stop. That means if the underlying data (say, a list `total` sums over) changes *after* `total` has already been read once, the cached value will not reflect the new data — reading `total` again just returns the stale cached number. This is a real, commonly-hit gotcha, not a bug in the concept: `by lazy` is the right tool for values that are expensive to compute and don't change after first use (or where you're deliberately caching a snapshot), and the wrong tool for anything that needs to reflect current, possibly-changing state on every read. (By default, `lazy` is also thread-safe — its initializer is synchronized so concurrent first-reads from multiple threads only run the block once — relevant background for when you get to coroutines shortly.)

**`Delegates.observable(initial) { property, old, new -> ... }`** (from `kotlin.properties.Delegates`) runs your callback after every assignment to the property, with the old and new values — handy for logging, validation, or reacting to state changes without scattering that logic across every call site that assigns the property. There's also `Delegates.vetoable`, which works the same way but your callback returns a `Boolean` deciding whether the assignment is allowed to actually happen.

**What `by` actually desugars to**, so a custom delegate later doesn't feel like magic: any class implementing the right `getValue`/`setValue` operator functions can be used with `by`. The stdlib's `ReadOnlyProperty<R, T>` / `ReadWriteProperty<R, T>` interfaces exist purely to formalize this shape:

```kotlin
class LoggingDelegate<T>(initial: T) : ReadWriteProperty<Any?, T> {
    private var current = initial
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = current
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        println("${property.name} changed from $current to $value")
        current = value
    }
}
```

`Delegates.observable` is just a pre-built version of exactly this. You won't need to write your own for this project, but knowing the shape means the next time you see `by someCustomThing`, you'll know exactly where to look.

## 5. Sealed class vs. sealed interface — the rule, properly this time

From reviewing `kotlin-order-intake`: the real distinguishing factor is whether the base type needs to hold **constructor state or shared implementation** that every subtype inherits.

- `sealed class Base(val sharedProp: ...)` — subtypes get `sharedProp` for free through inheritance, and the base can hold real logic. Costs you the subtype's one available superclass slot (a class can extend only one class).
- `sealed interface` — no constructor, no backing-field state, just a closed set of abstract (or default-implemented) members each subtype fulfills independently. Subtypes remain free to also extend some other class or implement other interfaces.

Default to `sealed interface` unless you specifically need shared constructor state — that's the majority case, including `MoneyOperationResult` in this project (its two variants, `Success` and `CurrencyMismatch`, share nothing structurally).

## Java-8 callouts

- Enum constant bodies: Java has had this since Java 5 — not a Kotlin invention, just used more idiomatically here.
- Companion objects: the direct answer to "how do I write a static factory method" without Java's `static` keyword existing in Kotlin at all.
- Operator overloading: Java has never allowed this (aside from a few built-in cases like `String +`); this is a genuine Kotlin-vs-Java-8 language capability gap.
- Property delegation: no Java equivalent — the closest Java analogue is manually writing getter/setter boilerplate, which `by` replaces entirely.
