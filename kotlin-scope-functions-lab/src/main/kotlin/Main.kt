package scope.functions

/**
 * TODO 7: build two configs - one from `validRaw` and one from `invalidRaw` (defined below) -
 * using buildConfig(...).logConfig(), then print summarize(...) for each. Confirm for yourself
 * that invalidRaw still produces a *usable* AppConfig (falling back to defaults) rather than
 * blowing up - that's the whole point of parsePort/parseHost returning null instead of throwing.
 */


/**
 * TODO 8 (use / AutoCloseable): construct a Connection for one of the configs above and drive it
 * with `use { }` - open it, send a message, and let `use` close it for you when the block ends.
 * Then, separately, prove `use` closes even on failure: construct a *second* Connection, and
 * inside a `use { }` block call `send(...)` WITHOUT calling `open()` first, wrapped in a
 * try/catch - confirm in the printed output that "closed" still prints even though `send` threw.
 */

/**
 * TODO 9 (anti-pattern reflection): write ONE line that chains at least three scope
 * functions/takeIf/takeUnless back to back against `invalidRaw` or `validRaw` (e.g.
 * `raw["port"]?.let { ... }?.takeIf { ... }?.also { ... } ?: ...`). Get it compiling, run it, then
 * add a comment right below it answering: would you actually ship this line, or would you break
 * it into named steps? Why?
 */

fun main() {
    // TODO 7

    val validRaw = mapOf("host" to "prod.example.com", "port" to "9090", "path" to "api/v2")
    val invalidRaw = mapOf("host" to "   ", "port" to "not-a-number")

    val rawConfig = buildConfig(validRaw).logConfig()
    val invalidRawConfig = buildConfig(invalidRaw).logConfig()

    println(summarize(rawConfig))
    println(summarize(invalidRawConfig))

    // TODO 8

    Connection(rawConfig).use {
        it.open();
        it.send("Test message")
    }

    try {
        Connection(invalidRawConfig).use {
            it.send("Test Message 2")
        }
    } catch (e: Exception) {
        println("send failed: ${e.message}")
    }

    // TODO 9
    val result = invalidRaw["port"]
        ?.let { it.toIntOrNull() }
        ?.takeIf { it in 1..65535 }
        ?.also { println("valid port: $it") }
        ?: -1
    println(result) // I am fine with shipping it like this

}
