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

## kotlin-shared-state

Race conditions traced down to the exact interleaving that causes a lost update, why this is the
first module that needs a real multi-threaded dispatcher to observe, `Mutex` as a coroutine-aware
lock (and why `synchronized`/`ReentrantLock` are actively dangerous around suspending code),
`AtomicInteger` as the lock-free alternative, and the boundary between them (single value vs a
multi-field invariant) - via a counter and a latency-stats tracker, each with a deliberately racy
version and a version you fix. Read `theory.md` first, then fill in the TODOs across `Counters.kt`,
`LatencyStats.kt`, and `Main.kt` - run with `./gradlew run` once implemented.

## kotlin-api-aggregator

Real HTTP calls from coroutines against a tiny local test server: wrapping blocking
`java.net.http.HttpClient` I/O in `Dispatchers.IO`, structured error modeling with a sealed
`ApiError`/`ApiResult` hierarchy instead of caught exceptions, retry with exponential backoff +
full jitter (and the retryable-vs-not judgment call), and fan-out aggregation across several
endpoints via `supervisorScope`/`async`. Read `theory.md` first, then fill in the TODOs across
`HttpClientWrapper.kt`, `RetryPolicy.kt`, `Aggregator.kt`, and `Main.kt` - run with `./gradlew run`
once implemented.

## kotlin-rate-limiter-service

A real Ktor HTTP server for the first time, with a hand-rolled token-bucket rate limiter wired in
as request-intercepting middleware. Covers the token bucket algorithm (lazy refill via
`System.nanoTime()`), Ktor's plugin "Base API" (`BaseApplicationPlugin` + `pipeline.intercept(...)`
+ `proceed()`) versus the simplified `onCall` DSL and why the rate limiter specifically needs the
former, concurrency-safety for the shared per-client bucket state via `Mutex` (same reasoning as
`kotlin-shared-state`), and designing a `RateLimiterStore` interface so the in-memory backing store
can later be swapped for something else. Read `theory.md` first, then fill in the TODOs across
`TokenBucket.kt`, `RateLimiterStore.kt`, `RateLimiterPlugin.kt`, `Application.kt`, and `Main.kt` -
run with `./gradlew run` once implemented.

## kotlin-consistent-hash-ring

A change of pace - no coroutines, no HTTP, pure data structures and algorithms. Builds a
consistent hash ring over string node names: nodes and keys hashed onto the same numeric space,
`TreeMap.ceilingKey` + wraparound to find each key's owning node, and virtual nodes (replicas per
physical node) for even load distribution. `Main.kt` proves the actual payoff with real numbers -
comparing how many keys reshuffle when a node is added under consistent hashing versus naive
modulo hashing. Read `theory.md` first, then fill in the TODOs across `ConsistentHashRing.kt` and
`Main.kt` - run with `./gradlew run` once implemented.

## kotlin-leader-election

Distributed systems as a single-process simulation: nodes are coroutines, the network is a set of
`Channel`s, and the goal is the Bully leader-election algorithm - election/cascade-upward,
heartbeat-based failure detection, and the "adopt the highest claimed id" rule that resolves
split-brain automatically once a simulated network partition heals. Deliberately single-threaded
(no `Dispatchers.Default`) to keep this module's lesson - message timing/ordering - separate from
`kotlin-shared-state`'s lesson (thread-safety). Read `theory.md` first, then fill in the TODOs
across `Network.kt` and `Node.kt` - `Message.kt` and `Main.kt` are given. Run with `./gradlew run`
once implemented and watch the demo survive a leader crash and a network partition.

---

**Convention going forward:** every module in this repo is its own standalone Gradle project
(own `build.gradle.kts`/`settings.gradle.kts`), never edited once handed over - each represents
one project from the learning roadmap. See the roadmap doc for the full module list and how
projects build on each other conceptually (without sharing code).
