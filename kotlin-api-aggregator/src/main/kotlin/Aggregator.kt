package api.aggregator

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

/** One endpoint to aggregate, with its own per-attempt timeout and retry budget. */
data class Endpoint(
    val name: String,
    val url: String,
    val timeoutMs: Long = 500,
    val maxAttempts: Int = 4
)

/** The final outcome for one endpoint, after retries (if any) have been resolved. */
data class EndpointReport(val name: String, val result: ApiResult<String>)

/**
 * TODO 4: fetch every endpoint in [endpoints] concurrently, each wrapped in its own [withRetry]
 * call (using that endpoint's [Endpoint.timeoutMs] and [Endpoint.maxAttempts]), and return one
 * [EndpointReport] per endpoint, in any order. Requirements (see theory.md section 4):
 *  - Use `supervisorScope` so that one endpoint's retries being fully exhausted doesn't cancel the
 *    other endpoints still in flight - same reasoning as `checkAllEndpoints` in
 *    kotlin-health-checker.
 *  - Launch one `async` per endpoint inside that scope, then `awaitAll()` at the end - don't await
 *    endpoints one at a time in a loop, that would serialize work that should run concurrently.
 *  - Inside each `async`, call `withRetry(maxAttempts = endpoint.maxAttempts) { fetch(endpoint.url,
 *    endpoint.timeoutMs) }` and wrap the result into an `EndpointReport`.
 */
suspend fun aggregate(endpoints: List<Endpoint>): List<EndpointReport> = supervisorScope {
    val deferredList = endpoints.map { endpoint ->
        async {
            withRetry(endpoint.maxAttempts) {
                fetch(endpoint.url, endpoint.timeoutMs)
            }
        }
    }
    val outputList = deferredList.awaitAll()
    endpoints.zip(outputList) { endpoint, result -> EndpointReport(endpoint.name, result) }
}
