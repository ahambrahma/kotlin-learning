package org.example

import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun main() = runBlocking {
    val urls = listOf(
        "https://service-a.internal",
        "https://service-b.internal",
        "https://service-c.internal",
        "https://service-d.internal",
        "https://service-e.internal",
        "https://service-f.internal",
        "https://service-g.internal",
        "https://service-h.internal"
    )

    val totalTime = measureTimeMillis {
        val results = checkAllEndpoints(urls, maxConcurrent = 3, perCheckTimeoutMs = 1500L)
        printReport(results)
    }

    println("\nChecked ${urls.size} endpoints in ${totalTime}ms")
    // Once this works: try maxConcurrent = 1 vs maxConcurrent = 8 and compare totalTime.
    // That difference IS the concurrency cap working.
}
