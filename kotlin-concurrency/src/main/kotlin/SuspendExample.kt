package org.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis


/***
 * What suspend Does:
 * 1. It's a Compiler Hint: Adding suspend to a function tells the Kotlin compiler, "Hey, this function might pause execution at some point."
 * 2. It Enables Suspending Calls: A function marked suspend is the only place you can call other suspending functions (like delay(), or an I/O function that returns a result later).
 * 3. It Allows State Saving: The compiler transforms the suspend function into a state machine behind the scenes.
 * When a suspension point is reached, the coroutine's state (all local variables, where it was in the code) is saved in a special object called a Continuation.
 *
 * What suspend Doesn't Do:
 * 1. It does NOT automatically make a function run on a background thread. A suspending function can run entirely on the main thread until it hits an actual suspension point.
 * 2. It does NOT make the function asynchronous. It simply makes it pausable. To make it truly concurrent or asynchronous, you need Coroutine Builders.
 */

class SuspendExample {
    fun blockingWait(time: Long) {
        println("  [T: ${Thread.currentThread().name}] Blocking wait started at time ${System.currentTimeMillis()}")
        // Thread.sleep() BLOCKS the thread, preventing it from doing other work.
        Thread.sleep(time)
        println("  [T: ${Thread.currentThread().name}] Blocking wait finished at time ${System.currentTimeMillis()}")
    }

    suspend fun suspendingWait(time: Long) {
        println("  [C: ${Thread.currentThread().name}] Suspending wait started at time ${System.currentTimeMillis()}")
        // delay() SUSPENDS the coroutine, freeing the thread to run other coroutines.
        delay(time)
        println("  [C: ${Thread.currentThread().name}] Suspending wait finished at time ${System.currentTimeMillis()}")
    }

    /**
     * The main function in Kotlin (like the main method in Java) is a standard, blocking entry point that runs on a normal JVM thread.
     * However, our coroutine builders like launch and async (and the function suspendingWait) are all suspending functions.
     *
     * Rule: You can only call a suspend function from another suspend function or a Coroutine Builder.
     *
     * Since main() is not a suspending function, we need a way to start the coroutine world from this blocking root.
     *
     * runBlocking acts as the necessary bridge. It is a Coroutine Builder that starts a new coroutine scope, allowing you to call suspending functions inside its block.
     */
    fun demo() = runBlocking {
        val totalTime = measureTimeMillis {
            println("--- Demonstrating Blocking (Thread.sleep) ---")

            // When we call blockingWait() twice, they must run sequentially,
            // blocking the main thread for the full duration.
            blockingWait(500L)
            blockingWait(500L)

            println("\n--- Demonstrating Suspending (delay) ---")

            // We use launch to start two concurrent coroutines.
            // Even though they run on the SAME thread (in this example),
            // when the first coroutine 'suspends' (pauses), the thread immediately
            // picks up the next coroutine, allowing them to run nearly simultaneously.
            val job1 = launch { suspendingWait(500L) }
            val job2 = launch { suspendingWait(500L) }

            job1.join()
            job2.join()

            println("\n--- Key Takeaway ---")
        }

        // We expect the blocking part to take around 1000ms and the suspending part to take around 500ms.
        println("Total execution time: $totalTime ms")
        println("Notice the coroutines finished in roughly the time of one, while the blocking calls added up.")
    }
}