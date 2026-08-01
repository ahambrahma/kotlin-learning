package api.aggregator

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

/**
 * TODO 2 (default retryability policy): a 5xx [ApiError.HttpError] and any [ApiError.NetworkError]
 * or [ApiError.Timeout] should be retryable - the failure looks transient. A 4xx
 * [ApiError.HttpError] should NOT be retryable - the request itself was wrong, and retrying it
 * unchanged will produce the exact same 4xx forever (see theory.md section 3). An
 * [ApiError.RetriesExhausted] should never actually be passed in here in practice (it's only ever
 * produced BY [withRetry] itself, as the terminal result), but treat it as not retryable if it
 * ever does show up, just to be safe.
 */
fun defaultIsRetryable(error: ApiError): Boolean {
    return when(error) {
        is ApiError.Timeout -> true
        is ApiError.HttpError -> error.statusCode in 500..599
        is ApiError.RetriesExhausted -> false
        is ApiError.NetworkError -> true
    }
}

/**
 * TODO 3: call [block] up to [maxAttempts] times (attempt numbers 1..maxAttempts), waiting
 * between attempts with exponential backoff plus full jitter. See theory.md section 3 for the
 * exact math. Requirements:
 *  - Stop and return immediately on the first `ApiResult.Success`.
 *  - Stop and return immediately on an `ApiResult.Failure` whose error [isRetryable] says is not
 *    retryable - don't burn remaining attempts on a failure that will never succeed.
 *  - If every attempt is exhausted without a Success, return
 *    `ApiResult.Failure(ApiError.RetriesExhausted(maxAttempts, lastError))`, where `lastError` is
 *    the error from the final attempt.
 *  - There is no delay before the very first attempt. Before attempt N+1 (after attempt N failed
 *    retryably), delay for `Random.nextLong(0, min(maxDelayMs, baseDelayMs * 2^(N-1)))` millis -
 *    full jitter, capped at [maxDelayMs].
 *  - Use `delay(...)` between attempts, never `Thread.sleep(...)` - this function runs on whatever
 *    dispatcher the caller is on, and a suspending delay is what keeps that dispatcher's thread
 *    free for other work while this one backs off.
 */
suspend fun <T> withRetry(
    maxAttempts: Int,
    baseDelayMs: Long = 200,
    maxDelayMs: Long = 2000,
    isRetryable: (ApiError) -> Boolean = ::defaultIsRetryable,
    block: suspend (attempt: Int) -> ApiResult<T>
): ApiResult<T> {
    var result = block(1)
    var error = when (result) {
        is ApiResult.Success -> return result
        is ApiResult.Failure -> result.error
    }

    if (!isRetryable(error)) {
        return result
    }

    for (retryAttempt in 1..maxAttempts-1) {
        val delayMs = Random.nextLong(0L, min(maxDelayMs, (baseDelayMs * (1L shl (retryAttempt-1)))))
        delay(delayMs)

        result = block(retryAttempt+1)

        error = when (result) {
            is ApiResult.Success -> return result
            is ApiResult.Failure -> result.error
        }

        if (!isRetryable(error)) {
            return result
        }
    }

    return ApiResult.Failure(ApiError.RetriesExhausted(maxAttempts, error))
}
