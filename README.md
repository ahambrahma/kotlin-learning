# kotlin-learning
Learning kotlin, one step at a time

## kotlin-basics 

This module contains basics of Kotlin including types, loops, if/else, functions, classes etc 

## kotlin-concurrency 

This module contains concurrency paradigms of Kotlin. It follows this detailed learning path:
https://docs.google.com/document/d/17mvgnDj-Ha3gUoLNDJ6QmNK7N7Or4fBG9yMJSQjnSVQ/edit?tab=t.0

## kotlin-oop-idioms

Sealed class vs sealed interface (properly reasoned, not just observed), enums with per-constant
behavior, companion object vs `object` declaration vs object expression, operator overloading,
property delegation (`by lazy`, `Delegates.observable`) - via a small Money/Currency/Wallet domain.
Read `theory.md` first, then fill in the TODOs across `Currency.kt`, `Money.kt`,
`ExchangeRates.kt`, `Wallet.kt`, and `Main.kt` - run with `./gradlew run`.

## kotlin-order-intake

Null safety (!! and its dangers, safe cast `as?`) + value classes (fixing primitive obsession),
via a small order-intake validator that parses loosely-typed input into a typed, validated domain
model or a list of typed errors. Skeleton with TODOs in `src/main/kotlin/Validation.kt` - run with
`./gradlew run` once implemented.

## kotlin-health-checker

Coroutines-fundamentals capstone: a concurrent endpoint health checker. Covers suspend functions,
launch/async, structured concurrency (coroutineScope vs supervisorScope), Dispatchers, cancellation,
timeouts (withTimeoutOrNull), and concurrency limiting (Semaphore). Skeleton with TODOs in
`src/main/kotlin/HealthChecker.kt` - run with `./gradlew run` once implemented.

## kotlin-scope-functions-lab

`let`/`also`/`apply`/`run`/`with`, `takeIf`/`takeUnless`, and `use` for `AutoCloseable`, taught as
one decision framework rather than five isolated facts, plus the over-chaining anti-pattern - via a
small config-loading + fake-connection domain. Read `theory.md` first, then fill in the TODOs
across `ConfigLoader.kt` and `Main.kt` - run with `./gradlew run` once implemented.

## kotlin-ingestion-pipeline

Channels as a producer/consumer primitive and backpressure as a real design decision - `Channel`
capacity (`RENDEZVOUS`/buffered/`UNLIMITED`/`CONFLATED`), `send`/`receive`/`close`, fan-out across
multiple consumers on one channel, and how this differs from the `Semaphore`-based concurrency
limiting from `kotlin-health-checker`. Read `theory.md` first, then fill in the TODOs across
`Pipeline.kt` and `Main.kt` - run with `./gradlew run` once implemented.

---

**Convention going forward:** every module in this repo is its own standalone Gradle project
(own `build.gradle.kts`/`settings.gradle.kts`), never edited once handed over - each represents
one project from the learning roadmap. See the roadmap doc for the full module list and how
projects build on each other conceptually (without sharing code).
