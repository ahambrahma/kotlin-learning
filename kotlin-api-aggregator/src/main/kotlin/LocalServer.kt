package api.aggregator

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Given - test harness, not part of the lesson. Spins up a tiny local HTTP server exposing four
 * endpoints that simulate the failure modes [withRetry]/[aggregate] need to handle:
 *  - /stable  - always succeeds immediately.
 *  - /flaky   - succeeds about half the time, otherwise returns 500 (retryable).
 *  - /slow    - always succeeds, but sometimes after a delay long enough to trip a short
 *               per-attempt timeout (retryable, via Timeout).
 *  - /broken  - always returns 404 (NOT retryable - retrying gets you the same 404 forever).
 *
 * Call [start] before you need it, [stop] when you're done - see Main.kt's TODO for where these
 * plug in. Uses a small fixed thread pool as the server's executor so concurrent requests (e.g.
 * multiple /slow calls in flight at once from aggregate()'s fan-out) are actually handled in
 * parallel rather than queued behind each other - the JDK's default HttpServer executor processes
 * requests sequentially on a single thread, which would silently defeat the concurrency this
 * module is about.
 */
class LocalServer(private val port: Int = 8089) {
    private val server: HttpServer = HttpServer.create(InetSocketAddress(port), 0)
    private val executor = Executors.newFixedThreadPool(8)

    init {
        server.executor = executor
        server.createContext("/stable") { exchange -> respond(exchange, 200, "stable-ok") }
        server.createContext("/flaky") { exchange ->
            if (Random.nextInt(100) < 50) respond(exchange, 200, "flaky-ok")
            else respond(exchange, 500, "flaky-fail")
        }
        server.createContext("/slow") { exchange ->
            val delayMs = Random.nextLong(0, 800)
            Thread.sleep(delayMs)
            respond(exchange, 200, "slow-ok-after-${delayMs}ms")
        }
        server.createContext("/broken") { exchange -> respond(exchange, 404, "not-found") }
    }

    fun start() = server.start()

    fun stop() {
        server.stop(0)
        executor.shutdown()
    }

    val baseUrl: String get() = "http://localhost:$port"

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
