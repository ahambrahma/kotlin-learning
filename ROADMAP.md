# Kotlin Mastery Roadmap — From Java 8 / Go to Staff-Level Kotlin

Audience: strong Java 8 + Go background, new to Kotlin, wants intermediate/advanced idioms (not syntax basics), learned by building things a staff/principal engineer would actually use.

## Conventions (read this first)

- **One module = one standalone Gradle project.** Every project below gets its own folder with its own `build.gradle.kts`/`settings.gradle.kts`/wrapper — never a new file dropped into an existing module. Once a module is handed over, it is not edited again, even to patch a gap; a gap becomes its own small new project instead.
- **Projects are sized to be finished in one sitting, and build on each other conceptually, not by sharing code.** If project N needs a pattern from project N-1 (e.g. a circuit breaker), it re-implements a small version of it rather than depending on the old module. This is deliberate — re-implementing a familiar pattern in a new context is itself part of learning it, and it keeps every folder runnable in isolation.
- **Session flow per module:** I give a theory brief on the concepts that module needs → I hand over a skeleton (signatures + doc-comment TODOs, not full solutions) → you implement it in IntelliJ → you paste the code back → I review for correctness, idiom, and anti-patterns → we move on.
- **Every module ships with a `theory.md` at its project root**, written alongside the skeleton, covering the concepts that module's TODOs require. The point is to read it once up front and implement without needing back-and-forth for the core concepts — questions and reviews are still fair game for anything the doc doesn't cover or gets you stuck on.
- Projects are kept **decently sized and incremental** — if a project starts looking like it needs more than a handful of files/functions to be "done," it gets split into two sequential projects instead of becoming one large one-off.

## Project ledger

| # | Folder | Topic | Status |
|---|---|---|---|
| — | `kotlin-basics` | Pre-existing: types, control flow, classes, collections, scope functions, sealed types, delegation, data classes | done (pre-existing, not touched) |
| — | `kotlin-concurrency` | Pre-existing: suspend, launch/async, structured concurrency, cancellation | done (pre-existing, not touched) |
| 1 | `kotlin-order-intake` | Null safety gaps (`!!`, `as?`) + value classes (primitive obsession) | done |
| 2 | `kotlin-oop-idioms` | Sealed class vs sealed interface (properly reasoned this time), enums with per-constant behavior, companion object vs `object` vs object expression, operator overloading, property delegation (`by lazy`, `Delegates.observable`) | done |
| 3 | `kotlin-scope-functions-lab` | The scope-function decision framework (`let`/`also`/`apply`/`run`/`with` - context object as `it` vs `this`, lambda result vs receiver returned), `takeIf`/`takeUnless`, `use` for `AutoCloseable` resources, and the over-chaining anti-pattern | scaffolded, awaiting your implementation |
| 4 | `kotlin-generics-lab` | Variance (`in`/`out`), star projection, inline functions with reified type parameters, `crossinline`/`noinline` | not started |
| 5 | `kotlin-health-checker` | Coroutines fundamentals capstone: Dispatchers, exception isolation (coroutineScope vs supervisorScope), timeouts, concurrency limiting | scaffolded, awaiting your implementation |
| 6 | `kotlin-ingestion-pipeline` | Channel-based producer/consumer, backpressure | not started |
| 7 | `kotlin-shared-state` | Deliberately trigger + fix a race condition with Mutex/Atomic | not started |
| 8 | `kotlin-api-aggregator` | Real HTTP calls from coroutines, retry+backoff, timeouts, structured error modeling | not started |
| 9 | `kotlin-rate-limiter-service` | Ktor service, token-bucket middleware, in-memory then swappable backing store | not started |
| 10 | `kotlin-consistent-hash-ring` | Consistent hashing library + CLI | not started |
| 11 | `kotlin-leader-election` | Nodes-as-coroutines, channels-as-network, toy Bully/Raft-lite election, partition simulation | not started |
| 12 | `kotlin-concurrency-testing` | `kotlinx-coroutines-test`, virtual time, reproducing a race with a test | not started |
| 13a | `kotlin-task-queue-core` | Capstone stage 1: single-shard in-memory job queue, worker pool, rate limit + circuit breaker | not started |
| 13b | `kotlin-task-queue-sharded` | Capstone stage 2: add consistent-hash sharding across worker groups to 13a's design | not started |

Not on the ledger yet, deliberately deferred: a dedicated testing/mocking-idioms project (JUnit5 + Kotlin, MockK, Kotest) — flagged as a real gap during review of `kotlin-order-intake`, but you chose to prioritize OOP idioms and generics first. Worth revisiting once 2–4 is done.

### What changed from the original plan, and why

- **Two new modules inserted after `kotlin-order-intake`**: `kotlin-oop-idioms` and `kotlin-generics-lab`. These came out of reviewing `kotlin-order-intake` itself — `FieldResult`/`OrderError` being `sealed class` while `OrderValidationResult` is `sealed interface` was never actually a deliberate contrast, which surfaced that the roadmap had no dedicated pass over intermediate Kotlin OOP idioms (enums with behavior, companion/object/object-expression, operator overloading, property delegation) or generics mechanics (variance, reified generics) beyond what came up incidentally. `kotlin-basics` (pre-existing) covers fundamentals, not this layer.
- **A third module, `kotlin-scope-functions-lab`, added between them.** `kotlin-basics` (pre-existing) has a `ScopeFunctions.kt` demonstrating `let`/`also`/`apply`/`run`/`with` syntactically, but not the decision framework for picking the right one, nor the common over-chaining anti-pattern — worth a dedicated pass rather than assuming that file's coverage is sufficient at the intermediate/advanced level this roadmap targets.
- **Channels/Flow (old Module 4) split into two projects** (`kotlin-ingestion-pipeline`, `kotlin-shared-state`) instead of one — backpressure and race-condition-hunting are each meaty enough to deserve their own focused build rather than being crammed together.
- **Testing (old Module 8) is now a fresh project, not a retrofit.** The original plan had you adding tests to earlier modules; that's no longer possible now that modules aren't edited after handoff, so it's a new small project built specifically to practice testing concurrent code.
- **The capstone (old Module 9) is now two incremental stages** instead of one big combine-everything project — stage 1 gets the core (queue + workers + resilience) working standalone; stage 2 adds sharding on top of that same design in a new project, rather than asking you to get everything right in one pass.

## Java-8-you-knew vs. what's changed (reference, come back to this)

| Java 8 world | What replaced/extended it | Kotlin equivalent |
|---|---|---|
| `Optional<T>`, null checks | Java `var`, records, sealed classes, pattern-matching `switch` (Java 17/21) | Null safety built into the type system (`String?` vs `String`) |
| POJOs + Lombok/`equals`/`hashCode` boilerplate | `record` (Java 16+) | `data class` |
| `interface` with no state hierarchy control | `sealed` interfaces/classes (Java 17) | `sealed class`/`sealed interface` (Kotlin had this first) |
| `Thread`, `ExecutorService`, `Future` | Virtual threads / Project Loom (Java 21) — cheap OS-thread-like blocking concurrency | Coroutines — cooperative, structured concurrency (different model, not a clone) |
| Streams API | Streams got minor improvements (`toList()`, teeing collector) | `Sequence` (lazy) vs `Collection` operators (eager) |
| Checked exceptions | Mostly unchanged | No checked exceptions in Kotlin — sealed `Result`-style types are idiomatic instead |

Since you know Go: a coroutine ≈ a goroutine that always belongs to a scope (no unscoped fire-and-forget by default), and `Channel` ≈ Go's `chan`, including the same footguns (unbounded channels, forgetting to close/cancel).

---

Say "let's start [project name]" whenever you're ready to begin the next one, or tell me if you want to reorder anything.
