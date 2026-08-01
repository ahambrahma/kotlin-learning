package ratelimiter.service

import kotlin.math.min

/**
 * A single client's token bucket. Deliberately NOT thread-safe by itself - see theory.md section 3
 * for why that responsibility belongs to whatever wraps concurrent access to a given bucket (the
 * store), not to this class. Every call here assumes single-threaded access to one instance at a
 * time.
 */
class TokenBucket(
    private val capacity: Long,
    private val refillTokensPerSecond: Double
) {
    val capacityDouble = capacity.toDouble()
    private var availableTokens: Double = capacityDouble
    private var lastRefillNanos: Long = System.nanoTime()

    /**
     * TODO 1: attempt to consume [tokens] tokens from this bucket, refilling first based on
     * elapsed time. See theory.md section 1 for the exact reasoning. Requirements:
     *  - Use `System.nanoTime()` for elapsed-time math, never `System.currentTimeMillis()`.
     *  - Compute elapsed seconds since `lastRefillNanos`, multiply by `refillTokensPerSecond` to
     *    get how many tokens to add, and add them to `availableTokens` - capped at `capacity` (a
     *    bucket can never hold more than its capacity, no matter how long it's been idle).
     *  - Update `lastRefillNanos` to the current time every time you refill, whether or not the
     *    request ends up allowed - otherwise a rejected request would cause the next request to
     *    double-count the elapsed time.
     *  - If, after refilling, `availableTokens >= tokens`: deduct `tokens` and return true.
     *  - Otherwise: return false, and do NOT deduct anything - a rejected request costs nothing.
     */
    fun tryConsume(tokens: Long = 1): Boolean {
        val currentTime = System.nanoTime()
        val elapsedTimeSeconds = (currentTime - lastRefillNanos) / 1_000_000_000.0
        availableTokens = min(availableTokens + (elapsedTimeSeconds * refillTokensPerSecond), capacityDouble)
        lastRefillNanos = currentTime
        if (tokens > availableTokens) {
            return false
        }
        availableTokens -= tokens
        return true
    }
}
