# Kotlin Mastery Roadmap — From Java 8 / Go to Staff-Level Kotlin

Audience: strong Java 8 + Go background, new to Kotlin, wants intermediate/advanced idioms (not syntax basics), learned by building things a staff/principal engineer would actually use.

## Conventions (read this first)

- **One module = one standalone Gradle project.** Every project below gets its own folder with its own `build.gradle.kts`/`settings.gradle.kts`/wrapper — never a new file dropped into an existing module. Once a module is handed over, it is not edited again, even to patch a gap; a gap becomes its own small new project instead.
- **Projects are sized to be finished in one sitting, and build on each other conceptually, not by sharing code.** If project N needs a pattern from project N-1 (e.g. a circuit breaker), it re-implements a small version of it rather than depending on the old module. This is deliberate — re-implementing a familiar pattern in a new context is itself part of learning it, and it keeps every folder runnable in isolation.
- **Session flow per module:** I give a theory brief on the concepts that module needs → I hand over a skeleton (signatures + doc-comment TODOs, not full solutions) → you implement it in IntelliJ → you paste the code back → I review for correctness, idiom, and anti-patterns → we move on.
- Projects are kept **decently sized and incremental** — if a project starts looking like it needs more than a handful of files/functions to be "done," it gets split into two sequential projects instead of becoming one large one-off.

## Project ledger

| # | Folder | Topic | Status |
|---|---|---|---|
| — | `kotlin-basics` | Pre-existing: types, control flow, classes, collections, scope functions, sealed types, delegation, data classes | done (pre-existing, not touched) |
| — | `kotlin-concurrency` | Pre-existing: suspend, launch/async, structured concurrency, cancellation | done (pre-existing, not touched) |
| 1 | `kotlin-order-intake` | Null safety gaps (`!!`, `as?`) + value classes (primitive obsession) | scaffolded, awaiting your implementation |
| 2 | `kotlin-health-checker` | Coroutines fundamentals capstone: Dispatchers, exception isolation (coroutineScope vs supervisorScope), timeouts, concurrency limiting | scaffolded, awaiting your implementation |
| 3 | `kotlin-ingestion-pipeline` | Channel-based producer/consumer, backpressure | not started |
| 4 | `kotlin-shared-state` | Deliberately trigger + fix a race condition with Mutex/Atomic | not started |
| 5 | `kotlin-api-aggregator` | Real HTTP calls from coroutines, retry+backoff, timeouts, structured error modeling | not started |
| 6 | `kotlin-rate-limiter-service` | Ktor service, token-bucket middleware, in-memory then swappable backing store | not started |
| 7 | `kotlin-consistent-hash-ring` | Consistent hashing library + CLI | not started |
| 8 | `kotlin-leader-election` | Nodes-as-coroutines, channels-as-network, toy Bully/Raft-lite election, partition simulation | not started |
| 9 | `kotlin-concurrency-testing` | `kotlinx-coroutines-test`, virtual time, reproducing a race with a test | not started |
| 10a | `kotlin-task-queue-core` | Capstone stage 1: single-shard in-memory job queue, worker pool, rate limit + circuit breaker | not started |
| 10b | `kotlin-task-queue-sharded` | Capstone stage 2: add consistent-hash sharding across worker groups to 10a's design | not started |

(Numbering restarts from the original 9-module plan because two of those modules — null safety and value classes — turned out to need their own dedicated project instead of being folded into an existing one.)

### What changed from the original plan, and why

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
