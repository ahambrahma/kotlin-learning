package api.aggregator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val httpClient: HttpClient = HttpClient.newHttpClient()

/**
 * TODO 1: perform a single GET request to [url], with a hard [timeoutMs] budget for this one
 * attempt. Requirements (see theory.md section 1 for the reasoning behind each):
 *  - The actual blocking call (`httpClient.send(...)`) must run inside `withContext(Dispatchers.IO)`
 *    - never call it directly on whatever dispatcher the caller happens to be on.
 *  - Wrap the whole attempt in `withTimeoutOrNull(timeoutMs) { ... }`; if that returns null (the
 *    attempt didn't finish in time), return `ApiResult.Failure(ApiError.Timeout(timeoutMs))`.
 *  - If `httpClient.send(...)` throws an `IOException` (connection refused, etc.), catch that
 *    specific type and return `ApiResult.Failure(ApiError.NetworkError(e))` - catching the
 *    specific type rather than a blanket `Exception` also means you're not at risk of the
 *    CancellationException-swallowing anti-pattern from kotlin-health-checker's theory.md, since
 *    CancellationException isn't an IOException.
 *  - If the response arrives but its status code is not in 200..299, return
 *    `ApiResult.Failure(ApiError.HttpError(statusCode))`.
 *  - Otherwise, return `ApiResult.Success(responseBody)`.
 *
 * A `HttpRequest` for [url] with a GET method looks like:
 * `HttpRequest.newBuilder(URI.create(url)).GET().build()`. Send it with
 * `httpClient.send(request, HttpResponse.BodyHandlers.ofString())`, which gives you `.statusCode()`
 * and `.body()` on the result.
 */
suspend fun fetch(url: String, timeoutMs: Long): ApiResult<String> {
    val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
    return withContext(Dispatchers.IO) {
        withTimeoutOrNull(timeoutMs) {
            try {
                val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                val statusCode = httpResponse.statusCode()
                if (statusCode !in 200..299) {
                    ApiResult.Failure(ApiError.HttpError(statusCode))
                } else {
                    ApiResult.Success(httpResponse.body())
                }
            } catch (e: IOException) {
                ApiResult.Failure(ApiError.NetworkError(e))
            }
        } ?: ApiResult.Failure(ApiError.Timeout(timeoutMs))
    }
}
