package ratelimiter.service

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * TODO 4: wire the plugin into the app.
 *  - `install(RateLimiterPlugin) { store = InMemoryRateLimiterStore(capacity = 5,
 *    refillTokensPerSecond = 1.0) }`
 *  - Define a `routing { }` block with a single `get("/ping")` route that responds with the plain
 *    text "pong" (`call.respondText("pong")`).
 */
fun Application.module() {
    install(RateLimiterPlugin) {
        store = InMemoryRateLimiterStore(capacity = 5, refillTokensPerSecond = 1.0)
    }
    routing {
        get("/ping") {
            call.respondText("pong")
        }
    }
}
