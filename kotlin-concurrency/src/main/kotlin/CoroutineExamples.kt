package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

class CoroutineExamples {
    fun demo() = runBlocking {
        val totalTime = measureTimeMillis {
            println("--- 1. Launch Example (Side-effects, Fire and Forget) ---")

            // Launch two tasks to run concurrently.
            // Each call returns a Job. We use the same thread for demonstration
            // purposes, showing how the coroutines interleave via suspension.
            // Job doesn't contain any values as such
            // If you need to return value, use async instead

            val jobA = launch(Dispatchers.Default) {
                println("Job A: Starting heavy task...")
                // Suspending call releases the thread
                delay(1000L)
                println("Job A: Task finished.")
            }

            val jobB = launch(Dispatchers.Default) {
                println("Job B: Starting medium task...")
                delay(500L)
                println("Job B: Task finished.")
            }

            // jobA.join() and jobB.join() suspend the 'runBlocking' coroutine
            // until the launched tasks are finished. This is the equivalent of waiting
            // for a thread to complete, but without blocking the underlying thread pool.
            jobA.join()
            jobB.join()

            println("\n--- 2. Async Example (Value Computation) ---")

            // Async starts a concurrent task that promises a result (Deferred<Int>).
            val deferredValue1 = async(Dispatchers.Default) {
                println("Async 1: Starting calculation.")
                delay(800L) // Suspend for 800ms
                println("Async 1: Returning 42.")
                42 // The result value
            }

            val deferredValue2 = async(Dispatchers.Default) {
                println("Async 2: Starting calculation.")
                delay(400L) // Suspend for 400ms
                println("Async 2: Returning 10.")
                10 // The result value
            }

            // 'await()' is a suspending call that waits for the deferred result.
            // Since both were started concurrently, the total wait time is only 800ms (max(800, 400)).
            val result1 = deferredValue1.await()
            val result2 = deferredValue2.await()

            println("Async Results: $result1 + $result2 = ${result1 + result2}")
        }

        // The entire concurrent block took slightly more than 1000ms (launch part)
        // plus 800ms (async part), demonstrating concurrency.
        println("\nTotal execution time: $totalTime ms")
    }
}