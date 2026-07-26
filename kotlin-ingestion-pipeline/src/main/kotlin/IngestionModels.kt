package ingestion.pipeline

/**
 * One unit of work flowing through the pipeline. Plain data holder - nothing coroutine-specific.
 */
data class LogEvent(
    val id: Int,
    val payload: String
)

/**
 * Per-worker stats returned once a consumer has drained the channel. Plain data holder.
 */
data class WorkerStats(
    val workerId: Int,
    val processedCount: Int,
    val totalProcessingMs: Long
)
