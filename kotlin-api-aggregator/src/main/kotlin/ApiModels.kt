package api.aggregator

/**
 * The outcome of one logical "fetch this URL" operation, once retries (if any) have been
 * resolved one way or the other. Exactly one of [Success] or [Failure] - modeled as data so a
 * `when (result)` is exhaustive-checked by the compiler instead of relying on the caller to
 * remember to check for the unhappy path. See theory.md section 2.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
}

/**
 * The closed set of ways a single HTTP attempt (or a whole retry loop) can fail. See theory.md
 * section 2 for why this is a sealed class instead of catching a generic Exception.
 */
sealed class ApiError {
    /** The attempt exceeded its per-attempt timeout before a response arrived. */
    data class Timeout(val afterMs: Long) : ApiError()

    /** The server responded, but with a non-2xx status code. */
    data class HttpError(val statusCode: Int) : ApiError()

    /** The request never reached the server at all (connection refused, DNS failure, etc.). */
    data class NetworkError(val cause: Throwable) : ApiError()

    /** Every retry attempt was used up without a Success. Wraps whatever the final attempt saw. */
    data class RetriesExhausted(val attempts: Int, val lastError: ApiError) : ApiError()
}
