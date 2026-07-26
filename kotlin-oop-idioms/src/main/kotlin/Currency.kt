package oop.idioms

/**
 * Enum with per-constant behavior - see theory.md section 1 before implementing.
 *
 * TODO 1: USD.format(cents) should render like "$12.34" - symbol BEFORE the amount, '.' as the
 * decimal separator.
 * TODO 2: EUR.format(cents) should render like "12,34€" - symbol AFTER the amount, ',' as the
 * decimal separator. Deliberately different logic per constant, not just a different symbol.
 *
 * Hint: cents / 100 gives whole units, cents % 100 gives the remainder - watch out for single
 * digit remainders (7 cents should show as "07", not "7").
 */
enum class Currency(val symbol: String) {
    USD("$") {
        override fun format(cents: Long): String {
            val sign = if (cents < 0) "-" else ""
            return "$sign${symbol}${getDollars(cents)}.${getCents(cents)}"
        }
    },
    EUR("€") {
        override fun format(cents: Long): String {
            val sign = if (cents < 0) "-" else ""
            return "$sign${getDollars(cents)},${getCents(cents)}${symbol}"
        }
    };

    abstract fun format(cents: Long): String

    fun getDollars(cents: Long): Long = kotlin.math.abs(cents) / 100
    fun getCents(cents: Long): String = (kotlin.math.abs(cents) % 100).toString().padStart(2, '0')
}
