package ratelimiter.service

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val httpClient: HttpClient = HttpClient.newHttpClient()

private suspend fun ping(clientId: String): Int {
    val request = HttpRequest.newBuilder(URI.create("http://localhost:8080/ping"))
        .header("X-Client-Id", clientId)
        .GET()
        .build()
    return withContext(Dispatchers.IO) {
        httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()
    }
}

/**
 * Given: starts the server on a background thread so the same process can act as both server and
 * demo client - same idea as kotlin-api-aggregator's LocalServer, just this time you built the
 * server-side logic (the plugin) yourself, not just a client hitting someone else's server.
 */
fun main() = runBlocking {
    val server = embeddedServer(Netty, port = 8080, module = Application::module)
    server.start(wait = false)

    try {
        /**
         * TODO 5: fire a burst of concurrent requests and prove the rate limiter works. Use the
         * given `ping(clientId)` helper - it returns the HTTP status code for one request.
         *  - Fire 10 concurrent requests with `clientId = "alice"` (use `async`/`awaitAll`, same
         *    fan-out pattern as kotlin-api-aggregator's `aggregate()`). With capacity=5 and a slow
         *    refill, expect roughly the first 5 to come back 200 and the rest 429 - count and print
         *    how many of each you got.
         *  - Then fire a few requests with `clientId = "bob"` instead, and confirm they succeed
         *    even though alice's bucket is empty - proving the two clients have independent
         *    buckets (different keys mean different TokenBucket instances in the store's map).
         */
        val deferredTasks = mutableListOf<Deferred<Int>>()
        for (i in 1..10) {
            deferredTasks.add(async { ping("alice") })
        }
        val aliceStatuses = deferredTasks.awaitAll()
        aliceStatuses.forEach { status -> println(status) }

        var successCount = aliceStatuses.count { it == 200 }
        var rejectedCount = aliceStatuses.count { it == 429 }
        println("alice: $successCount succeeded, $rejectedCount rejected")
        println()
        println()

        deferredTasks.clear()
        for (i in 1..10) {
            deferredTasks.add(async { ping("bob") })
        }
        val bobStatuses = deferredTasks.awaitAll()
        bobStatuses.forEach { status -> println(status) }

        successCount = bobStatuses.count { it == 200 }
        rejectedCount = bobStatuses.count { it == 429 }
        println("bob: $successCount succeeded, $rejectedCount rejected")
    } finally {
        server.stop(1000, 2000)
    }
}
