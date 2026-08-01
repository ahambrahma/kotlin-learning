package ratelimiter.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The swappable half of this module - see theory.md section 4. RateLimiterPlugin only ever talks
 * to this interface, never to a concrete implementation directly, so a future backing store (say,
 * Redis-backed, to survive restarts or work correctly across multiple server instances) can be
 * substituted later without touching the plugin or routes at all.
 */
interface RateLimiterStore {
    suspend fun tryConsume(key: String): Boolean
}

/**
 * TODO 2: implement using a single Mutex guarding a mutable map of one TokenBucket per key. See
 * theory.md section 3 for why the locking lives here and not inside TokenBucket itself.
 * Requirements:
 *  - Inside `mutex.withLock { }`, get-or-create the bucket for `key` (using `getOrPut` on
 *    `buckets`, constructing a `new TokenBucket(capacity, refillTokensPerSecond)` if absent), then
 *    call `tryConsume()` on it and return the result - both the map's get-or-create AND the
 *    bucket's own read-modify-write need to happen inside the SAME lock acquisition, since neither
 *    `buckets` (a plain mutableMapOf, not a ConcurrentHashMap) nor `TokenBucket` itself is
 *    thread-safe on its own.
 */
class InMemoryRateLimiterStore(
    private val capacity: Long,
    private val refillTokensPerSecond: Double
) : RateLimiterStore {
    private val mutex = Mutex()
    private val buckets = mutableMapOf<String, TokenBucket>()

    override suspend fun tryConsume(key: String): Boolean {
        return mutex.withLock {
            val tokenBucket = buckets.getOrPut(key) { TokenBucket(capacity, refillTokensPerSecond) }
            tokenBucket.tryConsume()
        }
    }
}
