package ingestion.pipeline

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlin.system.measureTimeMillis

/**
 * Sends [count] LogEvents into [channel] as fast as possible - no delay here, this is
 * deliberately a "fast producer." Whether it actually stays fast in practice depends entirely on
 * [channel]'s capacity, which you'll experiment with in Main.kt - that's the whole point of this
 * module.
 *
 * Behavior to implement:
 *  - send() exactly [count] LogEvents (ids 0 until count, any payload string you like)
 *  - once all events are sent, close the channel - this is what lets consumers' `for (event in
 *    channel)` loops end naturally instead of suspending forever waiting for one more element
 */
suspend fun produceLogEvents(channel: SendChannel<LogEvent>, count: Int) {
    val timeTaken = measureTimeMillis {
        for (i in 1..count) {
            val logEvent = LogEvent(i, "Message: $i")
            channel.send(logEvent)
            println("Produced message: $logEvent")
        }
        channel.close()
    }
    println("Time taken to send all messages is: $timeTaken")
}

/**
 * Consumes LogEvents from [channel] until it's closed and fully drained, simulating slower
 * processing than the producer via delay([processingDelayMs]) per event.
 *
 * Behavior to implement:
 *  - loop over the channel with `for (event in channel) { ... }` - the idiomatic way to drain a
 *    channel until closed; the loop ends automatically once the channel is closed AND empty, no
 *    manual "poison pill" value needed
 *  - delay(processingDelayMs) per event to simulate slow work
 *  - return a WorkerStats with how many events THIS worker processed and the total time it spent
 *    actively processing (i.e. summed delay time - not time spent suspended waiting for the next
 *    event to arrive)
 *
 * Important, worth confirming for yourself once this is wired up in Main.kt: if you launch
 * several of these against the SAME channel, each event goes to exactly ONE of them. Channels fan
 * work OUT across consumers - they don't broadcast the same event to every consumer.
 */
suspend fun consumeLogEvents(
    workerId: Int,
    channel: ReceiveChannel<LogEvent>,
    processingDelayMs: Long
): WorkerStats {
    var count = 0
    var totalProcessingMs = 0L
    for (event in channel) {
        totalProcessingMs += measureTimeMillis { delay(processingDelayMs) }
        println("WorkerId: $workerId, Consumed event: $event")
        count++
    }

    return WorkerStats(workerId, count, totalProcessingMs)
}
