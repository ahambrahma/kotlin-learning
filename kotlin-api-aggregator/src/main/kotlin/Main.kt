package api.aggregator

import kotlinx.coroutines.runBlocking

/**
 * TODO 5: wire it all together.
 *  - Construct a `LocalServer` and call `start()` on it. Read its `baseUrl`.
 *  - Build a `List<Endpoint>` pointing at `$baseUrl/stable`, `/flaky`, `/slow`, and `/broken`.
 *    Pick a `timeoutMs` on the tighter side for `/slow` (e.g. 200-300ms) so some attempts actually
 *    trip the Timeout path, since `/slow` can take up to 800ms - that's the point of that endpoint.
 *  - Call `aggregate(endpoints)` and print each `EndpointReport`: for a `Success`, print the value;
 *    for a `Failure`, print which `ApiError` it terminated on via an exhaustive `when` (the
 *    compiler will tell you if you miss a case).
 *  - Stop the `LocalServer` in a `finally` block so it always shuts down, even if `aggregate()`
 *    throws something unexpected.
 */
fun main() = runBlocking {
    val server = LocalServer()
    try {
        server.start()
        val endpoints = listOf(
            Endpoint("stable","${server.baseUrl}/stable"),
            Endpoint("flaky","${server.baseUrl}/flaky"),
            Endpoint("slow","${server.baseUrl}/slow", 100),
            Endpoint("broken","${server.baseUrl}/broken")
        )
        val results = aggregate(endpoints)
        for (result in results) {
            print("Endpoint: ${result.name}  ")
            when (result.result) {
                is ApiResult.Failure -> {
                    when (result.result.error) {
                        is ApiError.Timeout -> println("Timeout: ${result.result.error}")
                        is ApiError.HttpError -> println("HttpError: ${result.result.error}")
                        is ApiError.NetworkError -> println("NetworkError: ${result.result.error}")
                        is ApiError.RetriesExhausted -> println("RetriesExhausted: ${result.result.error}")
                    }
                }
                is ApiResult.Success -> println("Success: ${result.result.value}")
            }
            println()
        }
    } finally {
        server.stop()
    }
}
