package shared.state

import kotlinx.coroutines.*

/**
 * TODO 4: launch [workerCount] coroutines on Dispatchers.Default (a REAL thread pool - this is
 * the first module where that actually matters; every previous module's race-freedom came partly
 * from cooperative single-threaded scheduling, see theory.md section 2 for why that's no longer
 * true here), each calling increment() [incrementsPerWorker] times, against:
 *  - an UnsafeCounter
 *  - a MutexCounter
 *  - an AtomicCounter
 * Print the final count from each against the expected total (workerCount * incrementsPerWorker).
 * Run it a few times - UnsafeCounter's result should be inconsistent run-to-run and usually below
 * the expected total; the other two should always match exactly, every run.
 */

/**
 * TODO 5: same shape of test, against UnsafeLatencyStats vs MutexLatencyStats - have
 * [workerCount] coroutines each call record() [incrementsPerWorker] times with any latency value
 * you like, then print count/sum/averageMs from both. Add a comment: even if a given run doesn't
 * visibly produce a wrong average from UnsafeLatencyStats (compound-state races can be flakier to
 * trigger than the simple lost-update counter case), explain in your own words why it's still
 * unsafe and why swapping count/sum for independent atomics wouldn't fully fix it.
 */

const val workerCount = 1000
const val incrementsPerWorker = 1000

fun main() = runBlocking {
    // TODO 4
    val unsafeCounter = UnsafeCounter()
    val mutexCounter = MutexCounter()
    val atomicCounter = AtomicCounter()
    val jobs = mutableListOf<Job>()
    for (workerId in 1..workerCount) {
         jobs.add(launch(Dispatchers.Default) {
            for (incr in 1..incrementsPerWorker) {
                unsafeCounter.increment()
                mutexCounter.increment()
                atomicCounter.increment()
            }
        })
    }
    // wait for all of them to complete
    jobs.joinAll()
    println("Unsafe counter: ${unsafeCounter.count}")
    println("Mutex counter: ${mutexCounter.getCount()}")
    println("Atomic counter: ${atomicCounter.getCount()}")

    // TODO 5

    val unsafeLatencyStats = UnsafeLatencyStats()
    val mutexLatencyStats = MutexLatencyStats()

    jobs.clear()
    for (workerId in 1..workerCount) {
        jobs.add(launch(Dispatchers.Default) {
            repeat(incrementsPerWorker) {
                unsafeLatencyStats.record(100)
                mutexLatencyStats.record(100)
            }
        })
    }
    // wait for all of them to complete
    jobs.joinAll()
    println("Unsafe latency stats: ${unsafeLatencyStats.averageMs()}")
    println("Mutex latency stats: ${mutexLatencyStats.averageMs()}")
}
