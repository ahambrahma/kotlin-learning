package org.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException

/***
 * Structured Concurrency means that coroutines are arranged in a hierarchy (a parent-child relationship):
 *
 * Parent Responsibility: The scope (parent) is responsible for the lifetime of all the coroutines (children) launched within it.
 *
 * Automatic Cancellation: If the parent scope is cancelled, all its children are automatically cancelled immediately.
 *
 * Automatic Completion: A parent coroutine cannot complete until all of its children have completed.
 *
 * This structure automatically solves resource leaks and prevents operations from leaving dangling background tasks when the user navigates away
 */

class CoroutineContextExample {
    fun demo() = runBlocking {
        println("Parent Coroutine Scope (runBlocking) started.")

        // 1. Define a custom scope (we get one automatically from runBlocking,
        // but let's create a nested one for demonstration)
        val parentScope = CoroutineScope(Dispatchers.Default)

        // 2. Launch two child coroutines within the 'parentScope'.
        val jobChildA = parentScope.launch {
            println("  Child A: Launched. Will run for 2000ms.")
            try {
                // delay() is a suspending function that is 'cancellable'
                delay(2000L)
                println("  Child A: Completed successfully (should not print).")
            } catch (e: CancellationException) {
                println("  Child A: Successfully caught CancellationException!")
            } finally {
                // cleanup logic goes here
                if (isActive) {
                    // This block is for non-cancellation cases
                } else {
                    println("  Child A: Cleanup due to cancellation.")
                }
            }
        }

        val jobChildB = parentScope.launch {
            println("  Child B: Launched. Will run for 500ms.")
            delay(500L)
            println("  Child B: Completed successfully.")
        }

        // Give the children a chance to start and for Job B to finish
        delay(600L)

        println("\nAction: Cancelling the PARENT SCOPE.")
        // 3. Cancelling the scope immediately cancels all its active children (Job A).
        parentScope.cancel()

        // Wait a moment to allow the cancellation and cleanup to finish
        delay(100L)

        println("\nFinal Status:")
        println("Parent Scope is active: ${parentScope.isActive}")
        println("Child A is active: ${jobChildA.isActive}") // Should be false
        println("Child B is active: ${jobChildB.isActive}") // Should be false (completed/cancelled)
        println("Parent Coroutine Scope (runBlocking) finished.")
    }
}