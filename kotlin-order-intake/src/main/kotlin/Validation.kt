package order.intake

/**
 * Each parse* function below returns either the right value, or an OrderError explaining exactly
 * what went wrong - never null-with-no-explanation, never a thrown exception the caller has to
 * guess about. Kotlin's `Result<T>` type could hold this too, but a small sealed hierarchy gives
 * you room for multiple distinct failure reasons (see OrderError), which is why we're using one.
 *
 * Signature convention: each function takes the raw Any? value plus the field name (for error
 * messages) and returns Either<OrderError, T> - here we're modeling "Either" ourselves as a
 * two-branch sealed result rather than pulling in a library, since seeing it built once is the
 * point.
 */

sealed class FieldResult<out T> {
    data class Ok<T>(val value: T) : FieldResult<T>()
    data class Err(val error: OrderError) : FieldResult<Nothing>()
}

/**
 * TODO 1: implement using the safe cast `as?`.
 *  - cast raw to String using `as? String`
 *  - if the cast fails (raw was some other type, e.g. an Int), return
 *    FieldResult.Err(OrderError.InvalidType(field, expected = "String"))
 *  - if the cast succeeds but the string is blank, return
 *    FieldResult.Err(OrderError.InvalidValue(field, reason = "must not be blank"))
 *  - if raw was null in the first place, return FieldResult.Err(OrderError.MissingField(field))
 *  - otherwise return FieldResult.Ok(the string)
 */
fun parseRequiredString(raw: Any?, field: String): FieldResult<String> {
    if (raw == null) {
        return FieldResult.Err(OrderError.MissingField(field))
    }

    val str = raw as? String ?: return FieldResult.Err(OrderError.InvalidType(field, expected="String"))
    if (str.isBlank()) {
        return FieldResult.Err(OrderError.InvalidValue(field, reason = "must not be blank"))
    }

    return FieldResult.Ok(str)
}

/**
 * TODO 2: build OrderId from a parsed string. You'll need parseRequiredString first, then
 * construct OrderId(value) - remember its `init` block (from Domain.kt) can still throw even
 * after your blank check above passes for other reasons you add later, so wrap the construction
 * in a try/catch and convert any IllegalArgumentException into
 * FieldResult.Err(OrderError.InvalidValue(field, e.message ?: "invalid")).
 */
fun parseOrderId(raw: Any?): FieldResult<OrderId> {
    val orderIdResult = parseRequiredString(raw, "OrderId")
    // For Sealed Classes, when is the getter mechanism
    val orderIdStr = when (orderIdResult) {
        is FieldResult.Ok -> orderIdResult.value
        is FieldResult.Err -> return orderIdResult
    }

    try {
        val orderId = OrderId(orderIdStr)
        return FieldResult.Ok(orderId)
    } catch (e: IllegalArgumentException) {
        return FieldResult.Err(OrderError.InvalidValue("OrderId", e.message ?: "invalid"))
    }
}

fun parseCustomerId(raw: Any?): FieldResult<CustomerId> {
    val customerIdResult = parseRequiredString(raw, "CustomerId")
    val customerIdStr = when (customerIdResult) {
        is FieldResult.Ok -> customerIdResult.value
        is FieldResult.Err -> return customerIdResult
    }

    try {
        val customerId = CustomerId(customerIdStr)
        return FieldResult.Ok(customerId)
    } catch (e: IllegalArgumentException) {
        return FieldResult.Err(OrderError.InvalidValue("CustomerId", e.message ?: "invalid"))
    }
}

fun parseSku(raw: Any?): FieldResult<Sku> {
    val skuResult = parseRequiredString(raw, "SKU")
    val skuStr = when (skuResult) {
        is FieldResult.Ok -> skuResult.value
        is FieldResult.Err -> return skuResult
    }

    try {
        val sku = Sku(skuStr)
        return FieldResult.Ok(sku)
    } catch (e: IllegalArgumentException) {
        return FieldResult.Err(OrderError.InvalidValue("SKU", e.message ?: "invalid"))
    }
}

/**
 * TODO 4: quantity is the interesting one - the raw value might already be an Int (came from a
 * typed source) OR a String holding digits (came from a query param). Use a `when` with `is Int`
 * and `is String` smart-cast branches:
 *  - if it's an Int: must be > 0, else InvalidValue("must be positive")
 *  - if it's a String: parse with `toIntOrNull()`; null means InvalidType; otherwise same
 *    positivity check as above
 *  - null raw -> MissingField
 *  - anything else (e.g. a Double, a Boolean) -> InvalidType(field, expected = "Int")
 */
fun parseQuantity(raw: Any?): FieldResult<Int> {
    if (raw == null) {
        return FieldResult.Err(OrderError.MissingField("Quantity"))
    }

    when (raw) {
        is Int -> return quantityCheck(raw)
        is String -> {
            val rawInt = raw.toIntOrNull() ?: return quantityTypeError()
            return quantityCheck(rawInt)
        }
        else -> return quantityTypeError()
    }
}

private fun quantityTypeError(): FieldResult<Int> {
    return FieldResult.Err(OrderError.InvalidType("Quantity","Int"))
}

private fun quantityCheck(quantity: Int): FieldResult<Int> {
    if (quantity <= 0) {
        return FieldResult.Err(OrderError.InvalidValue("Quantity","must be positive"))
    }
    return FieldResult.Ok(quantity)
}

/**
 * TODO 5: notes is OPTIONAL - null is fine and should return FieldResult.Ok(null), not an error.
 * If present, it must be a String (use `as?`); if it's present but the wrong type, that's still
 * InvalidType. This is the one field where "missing" is not a failure - make sure your
 * implementation reflects that.
 */
fun parseNotes(raw: Any?): FieldResult<String?> {
    if (raw == null) {
        return FieldResult.Ok(raw)
    }
    return parseRequiredString(raw, "Notes")
}

/**
 * TODO 6: the orchestration function - this is where the whole module comes together.
 *  - call all five parse* functions
 *  - collect every FieldResult.Err into a list of OrderError (don't stop at the first failure -
 *    a real intake report should tell the caller about ALL the problems in one pass, not one at a
 *    time across five retries)
 *  - if the list of errors is empty, build a ValidatedOrder from the five FieldResult.Ok values
 *    and return OrderValidationResult.Valid(...)
 *  - otherwise return OrderValidationResult.Invalid(errors)
 */
fun validateOrder(raw: RawOrderInput): OrderValidationResult {
    val errors = mutableListOf<OrderError>()
    val orderId = parseOrderId(raw.orderId).valueOrCollect(errors)
    val customerId = parseCustomerId(raw.customerId).valueOrCollect(errors)
    val sku = parseSku(raw.sku).valueOrCollect(errors)
    val quantity = parseQuantity(raw.quantity).valueOrCollect(errors)
    val notes = parseNotes(raw.notes).valueOrCollect(errors)

    if (errors.isNotEmpty()) {
        return OrderValidationResult.Invalid(errors)
    }

    return OrderValidationResult.Valid(
        ValidatedOrder(
            orderId = orderId!!,
            customerId = customerId!!,
            sku = sku!!,
            quantity = quantity!!,
            notes = notes
        )
    )
}

private fun <T> FieldResult<T>.valueOrCollect(errors: MutableList<OrderError>): T? =
    when (this) {
        is FieldResult.Ok -> value
        is FieldResult.Err -> {
            errors.add(error)
            null
        }
    }