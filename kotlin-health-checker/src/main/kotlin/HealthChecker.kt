package org.example

// You'll need most of these - add the rest as your IDE suggests them:
// import kotlinx.coroutines.coroutineScope
// import kotlinx.coroutines.supervisorScope
// import kotlinx.coroutines.async
// import kotlinx.coroutines.awaitAll
// import kotlinx.coroutines.delay
// import kotlinx.coroutines.withTimeoutOrNull
// import kotlinx.coroutines.sync.Semaphore
// import kotlinx.coroutines.sync.withPermit
// import kotlin.random.Random

/**
 * Simulates checking one "endpoint". In a later module we'll swap this out for a real HTTP call -
 * for now, delay() stands in for network latency so we can focus purely on the concurrency
 * mechanics (that's the whole point of this module).
 *
 * Behavior to implement:
 *  - suspend (don't block a thread) for a random duration between 100ms and 2000ms
 *  - ~80% of checks should "succeed", ~20% should "fail" (pick randomly)
 *  - measure and return the actual elapsed time as latencyMs
 *  - on failure, set success = false and put a short message in `error`
 */
suspend fun checkEndpoint(url: String): EndpointResult {
    TODO("implement me")
}

/**
 * Checks every url in [urls] concurrently, with two constraints:
 *
 * 1. Concurrency cap: never more than [maxConcurrent] checks running at the same time,
 *    even if `urls` has hundreds of entries. (Hint: kotlinx.coroutines.sync.Semaphore)
 *
 * 2. Per-check timeout: if a single check takes longer than [perCheckTimeoutMs], treat it as a
 *    failure with error = "timeout" instead of waiting forever. (Hint: withTimeoutOrNull)
 *
 * The tricky part: checkEndpoint() can, in principle, throw. All checks must still finish and be
 * reported even if one of them throws an unexpected exception - one bad endpoint should not take
 * down the whole batch. Structured concurrency has two flavors for this and they behave very
 * differently:
 *   - coroutineScope { }   -> a failing child cancels its siblings (fail-fast)
 *   - supervisorScope { }  -> a failing child does NOT cancel its siblings (isolated failures)
 * Pick the one that satisfies the requirement above, and be ready to explain why in review.
 */
suspend fun checkAllEndpoints(
    urls: List<String>,
    maxConcurrent: Int = 5,
    perCheckTimeoutMs: Long = 1500L
): List<EndpointResult> {
    TODO("implement me")
}

/**
 * Pure Kotlin, no coroutines needed. Print something like:
 *   5/6 endpoints healthy, avg latency 812ms
 *   FAILED: https://service-c.internal (timeout)
 */
fun printReport(results: List<EndpointResult>) {
    TODO("implement me")
}
