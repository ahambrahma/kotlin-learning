package scope.functions

/**
 * Parses a raw, loosely-typed config map (like an untyped .properties file) into an AppConfig,
 * using each of the five scope functions where it's actually the right tool for that job - see
 * theory.md before implementing any of these.
 */

/**
 * TODO 1 (let + takeIf): safely parse the raw "port" string to an Int, returning null if it's
 * missing, unparsable, or outside 1..65535. Use `?.let { }` to chain off a null-safe parse, and
 * `takeIf { }` inside it to apply the range check without a separate if-statement.
 */
fun parsePort(raw: String?): Int? {
    return raw?.toIntOrNull()?.takeIf { it in 1..65535 }
}

/**
 * TODO 2 (takeUnless): return the raw "host" string trimmed, or null if it's blank after
 * trimming. Use `takeUnless { it.isBlank() }` rather than an if/else.
 */
fun parseHost(raw: String?): String? {
    return raw?.trim()?.takeUnless { it.isBlank() }
}

/**
 * TODO 3 (apply): build an AppConfig from a raw Map<String, String>, using `apply { }` to
 * configure the mutable properties in one chained block - the classic "builder" use of apply,
 * configuring an object and implicitly returning that same object.
 *  - host: parseHost(raw["host"]) ?: "localhost"
 *  - port: parsePort(raw["port"]) ?: 8080
 *  - path: raw["path"]?.let { if (it.startsWith("/")) it else "/$it" } ?: "/"
 */
fun buildConfig(raw: Map<String, String>): AppConfig {
    val parsedHost = parseHost(raw["host"]) ?: "localhost"
    val parsedPort = parsePort(raw["port"]) ?: 8080
    val parsedPath = raw["path"]?.let { if (it.startsWith("/")) it else "/$it" } ?: "/"
    return AppConfig().apply {
        this.host = parsedHost
        this.port = parsedPort
        this.path = parsedPath
    }
}

/**
 * TODO 4 (also): log this config (println) as a side effect WITHOUT breaking a chain - this
 * function should return the same config it was given, using `also { }`, so it composes as
 * `buildConfig(raw).logConfig().let { ... }` without disrupting the flow.
 */
fun AppConfig.logConfig(): AppConfig {
    return also { println(it) }
}

/**
 * TODO 5 (with): build a one-line summary string "host:port/path" using `with(config) { }` to
 * call host/port/path without repeating `config.` each time. Contrast with `run` below: `with`
 * is for several calls on a receiver you're not otherwise chaining anything off of.
 */
fun summarize(config: AppConfig): String {
    with(config) {
        return ("${this.host}:${this.port}${this.path}")
    }
}

/**
 * TODO 6 (run): open a Connection for this config and send a greeting message ("hello"),
 * returning whether it succeeded, using `config.let { }`-style scoping via `run { }` on a
 * freshly constructed Connection - i.e. `Connection(config).run { open(); send("hello"); true }`.
 * This is "compute a result via `this`," distinct from `with` above (`run` is called ON the
 * receiver, `with` takes it as an argument) and distinct from `apply` (you want the Boolean
 * result here, not the Connection back).
 */
fun greet(config: AppConfig): Boolean {
    return Connection(config).run {
        open();
        send("hello");
        true
    }
}
