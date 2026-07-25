package org.example

/**
 * The outcome of checking a single endpoint. Plain data holder - nothing coroutine-specific here.
 */
data class EndpointResult(
    val url: String,
    val success: Boolean,
    val latencyMs: Long,
    val error: String? = null
)
