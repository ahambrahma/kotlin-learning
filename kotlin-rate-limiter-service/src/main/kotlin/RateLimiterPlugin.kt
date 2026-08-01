package ratelimiter.service

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey

/**
 * Given - the companion object / BaseApplicationPlugin wiring below is Ktor ceremony, not the
 * lesson. TODO 3 is the only part you write: the body of the intercepted block near the bottom.
 *
 * This is deliberately built on Ktor's lower-level "Base API" (BaseApplicationPlugin +
 * pipeline.intercept(...)) instead of the simplified createApplicationPlugin/onCall DSL you'll see
 * in most Ktor examples - see theory.md section 2 for why: the simplified onCall hook always lets
 * the call continue on to your routes afterward, no matter what you do inside it. The Base API's
 * intercepted block gives you that control directly, via `proceed()`.
 */
class RateLimiterPlugin private constructor(
    private val store: RateLimiterStore,
    private val keyExtractor: (ApplicationCall) -> String
) {

    class Configuration {
        var store: RateLimiterStore = InMemoryRateLimiterStore(capacity = 5, refillTokensPerSecond = 1.0)
        var keyExtractor: (ApplicationCall) -> String =
            { call -> call.request.headers["X-Client-Id"] ?: "anonymous" }
    }

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, RateLimiterPlugin> {
        override val key = AttributeKey<RateLimiterPlugin>("RateLimiterPlugin")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): RateLimiterPlugin {
            val configuration = Configuration().apply(configure)
            val plugin = RateLimiterPlugin(configuration.store, configuration.keyExtractor)

            /**
             * TODO 3: implement the actual rate-limit check inside this intercepted block. See
             * theory.md section 2 for the full reasoning. Requirements:
             *  - `val key = plugin.keyExtractor(call)`
             *  - `val allowed = plugin.store.tryConsume(key)`
             *  - If NOT allowed: `call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")`,
             *    and do NOT call `proceed()` - not calling it is exactly what stops this call from
             *    ever reaching routing, so the matched route's handler never runs for it.
             *  - If allowed: call `proceed()` yourself - unlike the simplified onCall DSL, the Base
             *    API does not do this automatically. Forgetting this line means every request hangs,
             *    since nothing would ever reach your routes, even the allowed ones.
             */
            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                val requestKey = plugin.keyExtractor(call)      // call: ApplicationCall, from PipelineContext
                val allowed = plugin.store.tryConsume(requestKey)  // suspend fun, fine to call here directly
                if (!allowed) {
                    call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
                } else {
                    proceed()
                }
            }
            return plugin
        }
    }
}
