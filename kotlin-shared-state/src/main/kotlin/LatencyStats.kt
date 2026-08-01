package shared.state

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Given, deliberately racy: tracks count and sum independently, with no synchronization on
 * either. Worth noticing something specific here: even if you replaced [count] and [sum] with
 * their own separate atomics (AtomicInteger, AtomicLong), a reader calling [averageMs] could still
 * observe [count] already bumped by some coroutine's [record] call while [sum] hasn't been
 * updated by that SAME call yet - atomicity of A and atomicity of B separately does not give you
 * atomicity of "A and B together." That's exactly the gap TODO 3 (Mutex) exists to close - see
 * theory.md's section on compound invariants.
 */
class UnsafeLatencyStats {
    var count = 0
        private set
    var sum = 0L
        private set

    fun record(latencyMs: Long) {
        count++
        sum += latencyMs
    }

    fun averageMs(): Double = if (count == 0) 0.0 else sum.toDouble() / count
}

/**
 * TODO 3 (Mutex protecting a compound invariant): implement the same behavior as
 * UnsafeLatencyStats, but route [record] AND [averageMs] through the SAME Mutex - not just each
 * field independently. The guarantee you're after: [count] and [sum] must only ever be observed
 * already-updated-together or not-yet-updated-together, never half-updated relative to each
 * other.
 */
class MutexLatencyStats {
    private val mutex = Mutex()
    private var count = 0
    private var sum = 0L

    suspend fun record(latencyMs: Long) {
        mutex.withLock {
            count++
            sum += latencyMs
        }
    }

    suspend fun averageMs(): Double {
        mutex.withLock {
            return if (count == 0) 0.0 else sum.toDouble() / count
        }
    }
}
