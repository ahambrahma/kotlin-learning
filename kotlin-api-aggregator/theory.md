# Real HTTP Calls, Retries, and Structured Errors — Theory

Read this once before touching the TODOs. Every coroutine module so far has been either pure
simulation (`delay()` standing in for network time in `kotlin-health-checker`) or in-memory
(`Channel` in `kotlin-ingestion-pipeline`). This module makes an actual HTTP request over an actual
socket, to a tiny local server you spin up yourself (`LocalServer.kt`, given - not a TODO, it's
test harness, not the lesson). That's a deliberate choice: it's fast and fully offline, but it's a
real blocking I/O call underneath, with all the same properties a call to a real production API
would have - so everything you build here (the `Dispatchers.IO` wrapping, the retry/backoff logic,
the error modeling) transfers directly to hitting a real service.

## 1. Why a "real" HTTP call needs `Dispatchers.IO`, not `Default`

`java.net.http.HttpClient.send(...)` (the JDK's built-in HTTP client - no extra dependency needed)
is a **blocking** call: the calling thread parks until the response arrives, doing nothing useful
in the meantime. This is fundamentally different from every suspending call you've used so far.
`delay()`, `Mutex.withLock`, `channel.receive()` all genuinely *suspend* - the thread is released
back to the dispatcher and can go do other work while waiting. `HttpClient.send()` has no concept
of suspension; it occupies its thread for the entire round trip, the same way `Thread.sleep()`
would.

This is exactly the distinction `kotlin-shared-state`'s theory.md (section 3) drew between
dispatchers: `Dispatchers.Default` is a small pool sized to your CPU core count, meant for CPU-bound
work that keeps threads genuinely busy computing. If you call a blocking `send()` directly inside a
coroutine running on `Default`, you tie up one of those precious few threads doing nothing but
waiting on a socket - do that with a handful of concurrent requests and you can stall every other
coroutine that needs `Default` to run at all, including totally unrelated CPU work elsewhere in
your process. `Dispatchers.IO` exists specifically for this: a much larger, elastic pool designed
for threads that spend most of their time blocked, not computing. The fix is always the same
shape:

```kotlin
suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
    httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
}
```

`withContext` suspends the calling coroutine, moves execution to a thread on the `IO` dispatcher,
runs the blocking call there, and resumes the original coroutine with the result once it returns -
your calling code never blocks, only the `IO` thread temporarily does, and `IO`'s pool is sized to
absorb that. This is the single most important habit this module builds: **any time you call a
blocking Java API from a coroutine, it goes inside `withContext(Dispatchers.IO)`, never called bare
on whatever dispatcher happens to be active.**

## 2. Structured error modeling: making failure a value, not an exception

Every prior module either let exceptions propagate or converted them ad hoc (`checkAllEndpoints` in
`kotlin-health-checker` converted a timeout into a plain boolean `success = false`). A boolean
throws away *why* it failed. This module asks you to go one step further and model failure as a
proper typed value using a `sealed class` hierarchy - `ApiError` in `ApiModels.kt` - with one
variant per distinct failure category your `fetch` logic can actually produce: a network-level
failure (couldn't even connect), an HTTP-level failure (connected fine, server said no), and a
timeout. Wrapping the whole thing, `ApiResult<T>` is either `Success(value: T)` or
`Failure(error: ApiError)`.

Why bother, when a plain `try`/`catch` around a generic `Exception` would compile just as well?
Because `sealed class` gives you **exhaustiveness** - when you later `when (result)` over an
`ApiResult`, or `when (error)` over an `ApiError`, the compiler forces you to handle every variant,
and warns you the moment a new one is added anywhere it's matched. A caught generic `Exception`
gives you a stack trace and a `.message` string to guess at; a sealed `ApiError` gives you a closed,
enumerable set of *cases*, each carrying exactly the data relevant to that case (an HTTP error
carries a status code, a network error carries the underlying cause, a timeout carries nothing else
because there's nothing else to carry). This is precisely the "typed failure as data" idiom Kotlin
leans on instead of checked exceptions - you'll use this same shape again once you get to
`kotlin-consistent-hash-ring` and the task-queue capstone, so it's worth internalizing here.

## 3. Retry with backoff and jitter

Not every failure deserves a retry, and retrying blindly can make things *worse*, not better - so
this section has two parts: the backoff math, and the judgment call of what to retry at all.

**Retryable vs. not.** A `5xx` HTTP status or a network-level failure (connection refused, DNS
failure, or - in this exercise - the timeout your own client enforces) usually means "the server or
network hiccuped, try again." A `4xx` status means "the request itself was wrong" - retrying a
`404` or a `400` with the exact same request produces the exact same `404` or `400`, forever. Your
retry logic needs an explicit `isRetryable(error: ApiError): Boolean` check so it stops immediately
on a non-retryable error instead of burning through every attempt for no reason.

**Backoff.** If ten failing requests all retry after exactly the same fixed delay, you've just
recreated the same burst of load that may have caused the failure in the first place, on a
predictable clock tick - a classic case of a fleet of independent clients synchronizing themselves
by accident. Exponential backoff spreads retries out over increasing gaps: attempt 1 waits
`baseDelay`, attempt 2 waits `baseDelay * 2`, attempt 3 waits `baseDelay * 4`, and so on, capped at
some `maxDelay` so it doesn't grow unbounded. With `baseDelay = 200ms`, `maxDelay = 2000ms`: attempt
1 → 200ms, attempt 2 → 400ms, attempt 3 → 800ms, attempt 4 → 1600ms, attempt 5 → capped at 2000ms.

**Jitter.** Pure exponential backoff still has the synchronization problem, just less severe -
every client that failed at the same moment still retries at the same moments afterward (200ms
later, then 400ms later, in lockstep). Jitter fixes this by randomizing the actual delay within the
computed window instead of using it exactly - "full jitter" just picks a uniformly random value
between `0` and the computed exponential delay: `Random.nextLong(0, computedDelay)`. Now a fleet of
clients that all failed simultaneously fan back out across a spread of retry times instead of
hammering the recovering server in synchronized waves.

**Suspending, not blocking, between attempts.** The delay between retries is `delay(...)`, the same
suspending function you've used throughout this roadmap - it releases the thread while waiting, so
one coroutine backing off doesn't tie up a thread that could serve some other request in the
meantime.

## 4. Fan-out aggregation over several endpoints

The actual "aggregator" part of this module reuses a pattern you've already built once, in
`kotlin-health-checker`: `supervisorScope` plus `async` per endpoint, `awaitAll()` at the end. The
reason it's `supervisorScope` and not plain `coroutineScope` is unchanged from that module too - one
endpoint being permanently broken (like `/broken` returning `404` on every attempt) shouldn't cancel
the requests still in flight to the healthy endpoints. What's new here is that each of those `async`
blocks isn't a single `delay()` call anymore - it's `withRetry { fetch(url) }`, so a single logical
"check this endpoint" unit of work now internally contains its own bounded retry loop with backoff,
and the aggregator just needs the *final* `ApiResult` per endpoint once retries are exhausted or a
non-retryable failure is hit.

One more per-endpoint decision worth calling out explicitly: **per-attempt timeout vs. overall
deadline** are two different knobs, and this module asks you to think about both. A per-attempt
timeout (`withTimeoutOrNull` wrapped around a single `fetch` call, same as `kotlin-health-checker`)
bounds how long any *one* try can take before you give up on it and either retry or fail. An overall
deadline would bound the whole retry loop - all attempts combined - which this module doesn't
require you to implement, but is worth noticing is a distinct concept: a service with a generous
per-attempt timeout and a generous retry count can still take an unacceptably long time end-to-end
if nothing caps the total.

## Java-8 / Go callouts

- `java.net.http.HttpClient` has existed since Java 11 - if you've only used Java 8, this is new to
  you regardless of Kotlin; the synchronous `.send()` you're wrapping here is the same blocking
  call you'd get from `HttpURLConnection` in Java 8, just with a nicer API.
- Structured error modeling via `sealed class` is Kotlin's answer to something Go does differently:
  Go's idiomatic `(result, err)` two-return convention *is* a form of typed failure-as-value, just
  without exhaustiveness checking - nothing forces a Go caller to actually inspect `err`. A `when`
  over a sealed `ApiError` is the same instinct (don't throw failure away, make the caller look at
  it) with the compiler enforcing that the caller actually handles every case.
- Retry-with-backoff-and-jitter is a universal distributed-systems pattern, not Kotlin-specific -
  you've likely reimplemented some version of this by hand in Go with a manual `for` loop and
  `time.Sleep`. The only Kotlin-specific part is that the sleep is `delay()` (suspending) instead of
  a thread-blocking sleep, so backing off doesn't cost you a thread while you wait.
