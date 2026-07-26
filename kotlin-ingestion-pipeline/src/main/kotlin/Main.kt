package ingestion.pipeline

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.system.measureTimeMillis

/**
 * TODO 1: create a Channel<LogEvent> with capacity = Channel.RENDEZVOUS (the default - you can
 * write Channel<LogEvent>() with no argument), then launch, all running CONCURRENTLY:
 *  - one producer coroutine: produceLogEvents(channel, count = 200)
 *  - four consumer coroutines (workerId 1..4): consumeLogEvents(workerId, channel, processingDelayMs = 20)
 *
 * "Concurrently" is the key word - launch/async every one of these FIRST, then join/await them.
 * A bounded channel needs consumers actively draining it at the same time the producer is
 * sending, or the producer will suspend forever waiting for space that will never open up.
 *
 * Wrap the whole thing in measureTimeMillis, and print: total time, each worker's WorkerStats,
 * and the sum of processedCount across all four workers (should equal 200).
 */

/**
 * TODO 2: re-run the exact same setup but with Channel.UNLIMITED instead of Channel.RENDEZVOUS.
 * Compare the total times against TODO 1. Add a comment: what changed, and why? (Hint: think
 * about what, specifically, the producer is suspended waiting on in each case.)
 */

/**
 * TODO 3: re-run once more with Channel.CONFLATED. You'll notice processedCount no longer sums to
 * 200 across all workers. Add a comment explaining why, in terms of what CONFLATED actually
 * guarantees compared to RENDEZVOUS/buffered/UNLIMITED - don't just note that it happens, explain
 * the mechanism.
 */

fun main() = runBlocking {
    // TODO 1
    println("-----------------------------------")
    println("Trying out with default 0 capacity")
    println("-----------------------------------")
    val channel = Channel<LogEvent>()
    driver(channel)


    // TODO 2

    println("-----------------------------------")
    println("Trying out with unlimited capacity")
    println("-----------------------------------")

    /* In this case producer completes producing all events immediately within 5-10 ms and then consumption keeps happening
     Still, all the workers receive the same amount of messages
     */

    val channel2 = Channel<LogEvent>(capacity = Channel.UNLIMITED)
    driver(channel2)

    // TODO 3

    println("-----------------------------------")
    println("Trying out with conflated capacity")
    println("-----------------------------------")

    /*
    Since producer and consumer actually run on actually the same internal thread but on different coroutines.
    By the time consumers start reading events, they get overridden within the producer
     */
    val channel3 = Channel<LogEvent>(capacity = Channel.CONFLATED)
    driver(channel3)

}

private suspend fun driver(channel: Channel<LogEvent>) = coroutineScope {
    val consumerJobs = (1..4).map { workerId ->
        async {
            consumeLogEvents(workerId, channel, processingDelayMs = 20)
        }
    }
    val producerJob = launch { produceLogEvents(channel, 200) }
    producerJob.join()
    val stats2 = consumerJobs.awaitAll()
    println(stats2)
}
