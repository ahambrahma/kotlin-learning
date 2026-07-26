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

    // TODO 2

    // TODO 3
}
