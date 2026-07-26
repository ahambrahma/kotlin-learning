# Scope Functions & Functional Idioms — Theory

Read this once before touching the TODOs.

## 1. The five scope functions, as one decision, not five separate facts

`let`, `also`, `apply`, `run`, and `with` all do the same mechanical thing: take an object, open a block where that object is available under some name, and return *something*. They differ along exactly two axes — memorize the axes, not five isolated examples:

| Function | Refer to the object as | Block returns |
|---|---|---|
| `let` | `it` | the lambda's result |
| `also` | `it` | the object itself |
| `run` (as `x.run { }`) | `this` | the lambda's result |
| `apply` | `this` | the object itself |
| `with(x) { }` | `this` | the lambda's result |

That's the whole system. Two questions settle which one you want:

1. **Do you need the object as `it` (an explicit parameter) or `this` (implicit receiver)?** `it` is better when you're passing the object to something else (a function call, a constructor argument) or when a name makes a chain more readable. `this` is better when you're calling several members *on* the object itself, since you skip repeating `it.`/`config.` every time.
2. **Do you want the block's result, or the object back unchanged?** If you're computing something new (parsing, transforming, validating), you want the lambda's result. If you're doing a side effect (logging, configuring, sending) and want to keep chaining on the *original* object afterward, you want the object back.

Concretely, from those two questions:
- **Transform/compute + `it`** → `let`. Classic use: `nullableThing?.let { doSomethingWith(it) }` — null-safe execution, only runs if non-null.
- **Side effect + keep the object + `it`** → `also`. Classic use: logging or asserting mid-chain without breaking it: `buildThing().also { println("built: $it") }.useIt()`.
- **Configure + keep the object + `this`** → `apply`. Classic use: object configuration/builder style: `Config().apply { host = "x"; port = 1 }` — reads almost like a builder DSL, and returns the configured object.
- **Compute + `this`** → `run` (as `x.run { }`). Classic use: a multi-step computation scoped to one receiver, where you want the *result*, not the receiver back: `connection.run { open(); send("hi"); true }`.
- **Compute + `this`, but the receiver isn't the thing you're chaining off of** → `with(x) { }`. Classic use: several calls on the same object when you're not otherwise chaining anything — `with(config) { "$host:$port" }`. The difference from `run` is stylistic/structural: `with` takes the receiver as an argument (`with(x) { }`), `run` is called as a member on it (`x.run { }`) — reach for `with` when the receiver isn't part of some larger fluent chain.

`(x as T?).also/let/run` also work on non-null receivers, it's just less common to reach for them there since you don't need the null-safety benefit — but note that `also`/`let`/`apply` all work identically whether the receiver is nullable or not; nullability isn't what decides which function you pick, only `it`-vs-`this` and result-vs-receiver are.

## 2. `takeIf` / `takeUnless`

`x.takeIf { predicate }` returns `x` if the predicate is true, `null` otherwise. `x.takeUnless { predicate }` is the inverse — returns `x` if the predicate is *false*. These exist to replace an `if` used purely to decide "keep this value or turn it into null," inline, so it composes with `?.let`/`?:` instead of needing a separate statement:

```kotlin
val validPort = raw.toIntOrNull()?.takeIf { it in 1..65535 }
```

reads as "parse it, and only keep it if it's actually in range" in one expression — the alternative (`if (parsed != null && parsed in 1..65535) parsed else null`) says the same thing with more ceremony.

## 3. `use` for `AutoCloseable`

`resource.use { ... }` runs the block, then guarantees `resource.close()` is called afterward — even if the block throws. It's Kotlin's answer to Java's try-with-resources, and it's the idiomatic way to work with anything that implements `AutoCloseable`/`Closeable` (files, sockets, DB connections, and — in this project — the toy `Connection` class). Wrapping resource lifecycle management in `use` means you can't forget to close something, and you don't need a manual `try/finally` to guarantee it.

## 4. The over-chaining anti-pattern

Every scope function returns a value, which means they compose: `x?.let { }?.takeIf { }?.also { }` all chain together into one expression. That composability is exactly what makes them dangerous in excess. Three or four scope functions stacked in one line means a reader has to track, at every `.`, what the current implicit receiver is, what it's named (`it`? `this`?), and what got returned versus discarded — cognitive overhead that a couple of named local variables and a plain `if` would have avoided entirely. There's no hard rule for "too many," but a good gut check: if you can't tell at a glance what a chained expression evaluates to without mentally simulating each step, it's earned its complexity budget and should be broken apart. Prefer the named, sequential version unless the chained version is *obviously* clearer — "it compiles and does one thing" is not the same bar as "the next person can read it in five seconds."

## Java-8 callouts

- No real Java-8 equivalent for `let`/`also`/`apply`/`run`/`with` — the closest analogue is a fluent builder pattern (method chaining returning `this`), which Java requires you to hand-write per class; Kotlin gives you `apply` generically, for any type, for free.
- `use` is the direct answer to Java's try-with-resources (`try (var x = ...) { }`), just expressed as a function instead of special syntax.
- `takeIf`/`takeUnless` have no Java equivalent at all — closest is `Optional.filter` in modern Java, which only exists if you were already inside an `Optional` chain.
