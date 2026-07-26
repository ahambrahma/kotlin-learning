package scope.functions

data class AppConfig(
    var host: String = "",
    var port: Int = 0,
    var path: String = "/"
)

/**
 * A fake network connection - stands in for a real Socket/DB connection/file handle. Implements
 * AutoCloseable so it can be used with `use { }` (see theory.md section 3) - the same pattern
 * you'll reach for with real closeable resources later in this roadmap.
 */
class Connection(private val config: AppConfig) : AutoCloseable {
    var isOpen = false
        private set

    fun open(): Connection {
        isOpen = true
        println("Connection opened to ${config.host}:${config.port}")
        return this
    }

    fun send(message: String) {
        check(isOpen) { "Connection is not open" }
        println("Sending over ${config.host}:${config.port} -> $message")
    }

    override fun close() {
        isOpen = false
        println("Connection to ${config.host}:${config.port} closed")
    }
}
