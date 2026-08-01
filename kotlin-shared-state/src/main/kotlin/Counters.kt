package shared.state

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Given, deliberately NOT thread-safe. `count++` looks like one operation but is actually three:
 * read [count], compute count + 1, write the result back. If two coroutines running on different
 * threads interleave between those steps, one increment gets silently lost - see theory.md
 * section 1 for the exact trace of how that happens with real numbers.
 */
class UnsafeCounter {
    var count = 0
        private set

    fun increment() {
        count++
    }
}

/**
 * TODO 1 (Mutex): fix the same race using a coroutine-aware lock. `increment()` must guarantee
 * only one coroutine at a time can read-modify-write [count] - use `mutex.withLock { }` around
 * the read-modify-write, not just around part of it. Both functions need to be `suspend` because
 * `withLock` suspends (it does not block a thread) while waiting for the lock to free up.
 */
class MutexCounter {
    private val mutex = Mutex()
    private var count = 0

    suspend fun increment() {
        mutex.withLock {
            count++
        }
    }

    suspend fun getCount(): Int {
        mutex.withLock {
            return count
        }
    }
}

/**
 * TODO 2 (AtomicInteger): fix the same race using a lock-free atomic instead of a lock. Neither
 * function needs to be `suspend` here - atomics never suspend, they succeed via a CPU-level
 * compare-and-swap (retrying internally if another thread won a race to update first), which is
 * why they're cheaper than a Mutex for protecting a single value like this one.
 */
class AtomicCounter {
    private val count = AtomicInteger(0)

    fun increment() {
        count.incrementAndGet()
    }

    fun getCount(): Int {
        return count.get()
    }
}
