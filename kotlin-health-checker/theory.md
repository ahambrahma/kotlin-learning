# Coroutines Fundamentals — Theory

Read this once before touching the TODOs in `HealthChecker.kt`. This module is a fan-out/fan-in
concurrency exercise: check N endpoints at once, cap how many run simultaneously, time out slow
ones, and make sure one bad endpoint can't sink the whole batch.

## 1. `suspend` — what it actually means

A `suspend fun` can pause and resume without blocking the underlying OS thread. `delay(500)` looks
like `Thread.sleep(500)` but is fundamentally different: `Thread.sleep` parks a real thread (wasted,
can't do anything else) for 500ms; `delay` tells the coroutine machinery "wake me up in 500ms" and
frees the thread to go run other coroutines in the meantime. That's the entire value proposition of
coroutines — you can have thousands of "concurrent" suspended functions in flight backed by a
handful of real threads, because most of them are just waiting, not occupying a thread while doing
it.

The `suspend` keyword is a compile-time marker, not magic on its own — it's what *lets* a function
call other suspend functions (`delay`, `await`, another `suspend fun`) and lets the compiler
rewrite it (via continuation-passing under the hood) to support pausing. You can only call a
suspend function from another suspend function, or from inside a coroutine builder (`launch`,
`async`, `runBlocking`).

`checkEndpoint` in this module is `suspend` because it calls `delay()` to simulate network latency
— in a later module this becomes a real HTTP call, but the shape stays identical: suspend, don't
block, while waiting on I/O.

## 2. Coroutine builders: `launch` vs `async`

- **`launch`** starts a coroutine and returns a `Job` — fire-and-forget, you don't get a result back
  (only completion/cancellation state).
- **`async`** starts a coroutine and returns a `Deferred<T>` — a future-like handle you call
  `.await()` on to get the result (suspending until it's ready).

This module needs `async`, because you need the `EndpointResult` back from each check. The
fan-out/fan-in pattern is: kick off N `async { checkEndpoint(url) }` calls (fan-out — they all start
running "simultaneously"), collect them in a `List<Deferred<EndpointResult>>`, then
`.awaitAll()` (fan-in — suspend until every one of them has a result). Starting all the `async`
calls first, *then* awaiting, is what gives you concurrency — if you awaited each one immediately
after starting it, you'd just be running them sequentially with extra steps.

## 3. Structured concurrency: why everything needs a scope

You can't just call `launch`/`async` from nowhere — they're extension functions on
`CoroutineScope`. This is deliberate: Kotlin's structured concurrency guarantees that a scope
doesn't complete until all coroutines launched inside it have completed (or been cancelled). No
"fire and forget and lose track of it" — every child is tied to a parent, and a parent can't finish
before its children do. This is the direct fix for the classic problem with raw
`Thread`/`ExecutorService`/goroutine code: nothing accidentally outlives the scope that started it,
and cancellation propagates downward automatically (cancel the parent, all children get cancelled
too).

`coroutineScope { }` and `supervisorScope { }` are both ways to create a new scope tied to the
current suspend function — the function suspends until everything launched inside that block
finishes. They differ in exactly one thing: **failure propagation.**

## 4. `coroutineScope` vs `supervisorScope` — the core decision for this module

- **`coroutineScope { }`**: if any child fails (throws), the scope cancels *all other children*
  immediately, then rethrows that failure from the scope itself. Fail-fast — one bad child takes
  the whole group down. This is what you want when the children are all working toward one combined
  result and a partial result is meaningless (e.g., "fetch these 3 pieces of a single response;
  if any piece fails, the whole thing failed").
- **`supervisorScope { }`**: a failing child does *not* cancel its siblings. Each child's
  success/failure is isolated. This is what you want when the children are independent units of
  work and a failure in one tells you nothing about the others — exactly this module's situation:
  service-c being down has nothing to do with whether service-d is healthy, and the whole point of
  a health checker is "tell me the status of every endpoint," not "abort if any one is unhealthy."

So: **`supervisorScope` is the correct choice here.**

### The part that's easy to miss

`supervisorScope` stops a failing child from *cancelling its siblings* — but it does **not**
silently swallow the exception for you. If a child started with `async` throws, that exception is
stored on its `Deferred` and will be re-thrown the moment you call `.await()` on that specific
`Deferred` — regardless of `coroutineScope` vs `supervisorScope`. If you do a naive
`deferredList.awaitAll()` and the second endpoint's `Deferred` completed exceptionally, `awaitAll()`
throws when it reaches that one — and depending on how you've written the surrounding code, you can
still lose the results you'd already collected from the ones before it, or never even attempt the
ones after it if you're awaiting sequentially rather than truly all at once.

The robust fix — and the actual lesson here — is to make sure **no child coroutine ever completes
exceptionally in the first place.** `EndpointResult` already models failure as *data*
(`success = false, error = "..."`), so the right design is: catch anything that can go wrong
*inside* each `async` block itself (a thrown exception from `checkEndpoint`, a timeout from
`withTimeoutOrNull` coming back `null`) and turn it into a normal, successfully-returned
`EndpointResult` describing the failure. Nothing ever needs to propagate as a thrown exception
through `awaitAll()` at all. `supervisorScope` is then your defense-in-depth safety net for
anything that slips through that you didn't anticipate — not the primary mechanism you're relying
on to keep the batch alive.

## 5. Limiting concurrency: `Semaphore`

`kotlinx.coroutines.sync.Semaphore(n)` lets at most `n` coroutines past it at once; anything beyond
that suspends (not blocks) until a permit frees up. The idiomatic usage is
`semaphore.withPermit { ... }` — acquires a permit, runs the block, releases the permit when the
block finishes *even if it throws* (same "guaranteed cleanup" shape as `use { }` for
`AutoCloseable`, just for a concurrency slot instead of a resource handle). This is what caps
`maxConcurrent` in `checkAllEndpoints` — without it, launching 200 `async` blocks would try to run
all 200 "simultaneously" (well, cooperatively interleaved), which defeats the purpose of a
concurrency *cap*.

Where the `withPermit` call goes matters: wrap it around the actual check (`checkEndpoint` +
timeout), not around unrelated setup — you want the permit held only while the expensive work is
happening, released as soon as that specific check is done, not the whole batch.

## 6. Timeouts: `withTimeoutOrNull`

`withTimeout(ms) { ... }` runs a block and throws `TimeoutCancellationException` if it doesn't
finish in time. `withTimeoutOrNull(ms) { ... }` does the same but returns `null` instead of
throwing — much easier to fold into a normal result here: if it comes back `null`, you know that
specific check didn't finish in time, and you can construct an `EndpointResult(success = false,
error = "timeout", ...)` directly, no try/catch needed for the timeout case specifically (you still
want a try/catch around the whole thing for the "checkEndpoint threw for some other reason" case,
per section 4).

## 7. Dispatchers — why this module doesn't need to touch them much

A `Dispatcher` decides which thread(s) a coroutine actually runs on. `Dispatchers.Default` is for
CPU-bound work, `Dispatchers.IO` is for blocking I/O calls (file access, JDBC, anything that isn't
itself suspend-aware), `Dispatchers.Main` is UI-thread-only (not relevant here). Because
`checkEndpoint` uses `delay()` — a genuinely suspending, non-blocking operation — there's no thread
being tied up waiting, so there's no need to explicitly move this work onto `Dispatchers.IO` the way
you would if you were calling a blocking library. In a later module, once `checkEndpoint` becomes a
real HTTP call using a suspend-aware client (e.g. Ktor's client), the same logic holds — the client
handles the suspension, you still don't need to manually pick a dispatcher. You *would* need
`Dispatchers.IO` if you were wrapping a blocking call (e.g. `java.net.URL(...).readText()`) to avoid
starving the default dispatcher's limited thread pool — worth knowing, not needed for this module.

## 8. Small utilities used in `Main.kt`

- `measureTimeMillis { block }` — runs `block`, returns elapsed wall-clock time in ms. Already wired
  up for you to compare `maxConcurrent = 1` vs `maxConcurrent = 8`.
- `Random.nextLong(from, until)` / `Random.nextInt(...)` / `Random.nextBoolean()` — for the random
  latency and random success/failure simulation in `checkEndpoint`.

## Java-8 / Go callouts

- The closest Java-8 equivalent is `ExecutorService` + `Future`/`CompletableFuture` — but those give
  you no structured concurrency (a `Future` you forget to `.get()` just silently keeps running
  unsupervised), no cheap cancellation propagation, and blocking `.get()` ties up a real thread while
  waiting. `Deferred.await()` suspends instead of blocking, and `coroutineScope`/`supervisorScope`
  guarantee nothing outlives its parent.
- Since you know Go: `async { }` ≈ starting a goroutine that returns a value over a channel, and
  `awaitAll()` ≈ waiting on all of them — except the "does a failure cancel the others" question
  (which Go leaves entirely up to you to wire by hand with contexts/channels) is a first-class,
  explicit choice in Kotlin (`coroutineScope` vs `supervisorScope`), not something you re-implement
  every time.
- A `Semaphore` here plays the same role as a buffered channel used as a concurrency limiter in Go
  (`sem := make(chan struct{}, n)`) — same idea, built into the stdlib instead of a hand-rolled
  pattern.
