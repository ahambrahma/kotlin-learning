package order.intake

// This is the fail-fast pattern: reject a bad ID the moment it's constructed instead
// of letting a blank one silently flow through the system.
@JvmInline
value class OrderId(val value: String) {
    init {
       require (value.isNotBlank()) { "Value shouldn't be empty" }
    }
}

@JvmInline
value class CustomerId(val value: String) {
    init {
        require (value.isNotBlank()) { "Value shouldn't be empty" }
    }
}

@JvmInline
value class Sku(val value: String) {
    init {
        require (value.isNotBlank()) { "Value shouldn't be empty" }
    }
}

/**
 * Simulates a loosely-typed input - as if this came from parsing a query string, a CSV row, or a
 * JSON blob into a generic map before you know the real types are correct. Every field is `Any?`
 * on purpose: it might be missing (null), the wrong type (an Int where you expected a String, or
 * vice versa), or a String holding garbage. That messiness is the whole point - it's what real
 * input from outside your process actually looks like, and it's exactly what you'll deal with
 * again in Module 5 when the input comes from a real HTTP call instead of a hardcoded list.
 */
data class RawOrderInput(
    val orderId: Any?,
    val customerId: Any?,
    val sku: Any?,
    val quantity: Any?,
    val notes: Any?
)

data class ValidatedOrder(
    val orderId: OrderId,
    val customerId: CustomerId,
    val sku: Sku,
    val quantity: Int,
    val notes: String?
) {
    // OK  ORD-1 -> CUST-1 / SKU-1 x3
    override fun toString(): String {
        return "OK ${orderId.value} -> ${customerId.value} / ${sku.value} x$quantity"
    }
}

sealed class OrderError {
    data class MissingField(val field: String) : OrderError()
    data class InvalidType(val field: String, val expected: String) : OrderError()
    data class InvalidValue(val field: String, val reason: String) : OrderError()
}

sealed interface OrderValidationResult {
    data class Valid(val order: ValidatedOrder) : OrderValidationResult
    data class Invalid(val errors: List<OrderError>) : OrderValidationResult
}
