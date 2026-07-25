package order.intake

val sampleInputs = listOf(
    // Fully valid
    RawOrderInput(orderId = "ORD-1", customerId = "CUST-1", sku = "SKU-1", quantity = 3, notes = "Leave at door"),
    // Missing orderId entirely
    RawOrderInput(orderId = null, customerId = "CUST-2", sku = "SKU-2", quantity = 1, notes = null),
    // Blank customerId - passes the "is it a String" check but should fail the value class's init validation
    RawOrderInput(orderId = "ORD-3", customerId = "   ", sku = "SKU-3", quantity = 2, notes = null),
    // Quantity arrives as a numeric-looking String instead of an Int (e.g. from a query param)
    RawOrderInput(orderId = "ORD-4", customerId = "CUST-4", sku = "SKU-4", quantity = "5", notes = null),
    // Quantity is present but invalid (negative)
    RawOrderInput(orderId = "ORD-5", customerId = "CUST-5", sku = "SKU-5", quantity = -3, notes = null),
    // Quantity is the wrong type altogether
    RawOrderInput(orderId = "ORD-6", customerId = "CUST-6", sku = "SKU-6", quantity = true, notes = null),
    // sku missing, notes present and valid
    RawOrderInput(orderId = "ORD-7", customerId = "CUST-7", sku = null, quantity = 1, notes = "Gift wrap"),
    // notes present but wrong type
    RawOrderInput(orderId = "ORD-8", customerId = "CUST-8", sku = "SKU-8", quantity = 1, notes = 42)
)

/**
 * TODO: iterate over `results`, and for each one:
 *  - if Valid: print something like "OK  ORD-1 -> CUST-1 / SKU-1 x3"
 *  - if Invalid: print the order's approximate identity if you can recover any of it (this is
 *    optional - fine to just print "an order"), then every OrderError on its own line, using a
 *    `when` over OrderError's three subtypes to phrase each one distinctly (a MissingField
 *    message should read differently from an InvalidType or InvalidValue message)
 * Finish with a one-line summary: "X/Y orders valid".
 */
fun printReport(results: List<OrderValidationResult>) {
    var validOrderCount = 0
    for (result in results) {
        when (result) {
            is OrderValidationResult.Valid -> {
                val order = result.order
                println(order)
                validOrderCount++
            }
            is OrderValidationResult.Invalid -> {
                val errors = result.errors  // smart-cast to Invalid here, .errors resolves
                // errors is List<OrderError> - you'll want a second when, nested,
                // over OrderError's three subtypes to phrase each one distinctly
                for (error in errors) {
                    when (error) {
                        is OrderError.InvalidType -> println("  ${error.field}: expected ${error.expected}")
                        is OrderError.InvalidValue -> println("  ${error.field}: ${error.reason}")
                        is OrderError.MissingField -> println("  ${error.field}: missing")
                    }
                }
            }
        }
    }
    if (results.isNotEmpty()) {
        println("$validOrderCount/${results.size} orders valid")
    }
}

fun main() {
    val results = sampleInputs.map { validateOrder(it) }
    printReport(results)
}
