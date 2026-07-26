package oop.idioms

import kotlin.math.roundToLong

/**
 * Money as whole cents (Long) + a Currency - never use Double for money, rounding errors compound
 * across enough operations to actually matter.
 *
 * See theory.md section 3 (operator overloading) before implementing the operators, and section 5
 * (sealed class vs interface) before implementing MoneyOperationResult below.
 */
data class Money(val amountCents: Long, val currency: Currency) {

    /**
     * TODO 1: add two Money values. Throw IllegalStateException if currencies differ - operators
     * are expected to either succeed or throw, they can't return a sealed Result and still work as
     * `a + b`. Compare this to tryAdd() below, which models the same operation without throwing.
     */
    operator fun plus(other: Money): Money {
        if (currency != other.currency) {
            throw IllegalStateException("Source currency: $currency and target currency: ${other.currency} are not the same")
        }

        return Money(amountCents + other.amountCents, currency)
    }

    /** TODO 2: scale by an integer factor (e.g. unit price * quantity). */
    operator fun times(factor: Int): Money {
        return Money(amountCents * factor, currency)
    }

    /** TODO 3: negate - enables `-money`. */
    operator fun unaryMinus(): Money {
        return Money(-amountCents, currency)
    }

    /**
     * TODO 4: enables <, >, <=, >= directly on Money. Must return negative/zero/positive per the
     * Comparable contract. Throw IllegalStateException on currency mismatch, same as plus.
     */
    operator fun compareTo(other: Money): Int {
        if (currency != other.currency) {
            throw IllegalStateException("Source currency: $currency and target currency: ${other.currency} are not the same")
        }

        return amountCents.compareTo(other.amountCents)
    }

    override fun toString(): String = currency.format(amountCents)

    /**
     * TODO 5: companion object as a factory - see theory.md section 2 for how this differs from
     * the `object` declaration in ExchangeRates.kt.
     *  - zero(currency): a Money of 0 cents in that currency
     *  - of(amount, currency): convert dollars-and-cents (e.g. 12.34) into whole cents (Long) -
     *    use (amount * 100).roundToLong(), not a plain Int cast, to avoid floating-point
     *    truncation bugs (e.g. 12.30 * 100 can come out as 1229.999... as a raw Double)
     */
    companion object {
        fun zero(currency: Currency): Money {
            return Money(0, currency)
        }

        fun of(amount: Double, currency: Currency): Money {
            return Money((amount*100).roundToLong(), currency)
        }
    }
}

/**
 * TODO 6: sealed INTERFACE, not sealed class - there's no shared state between the two outcomes,
 * so there's nothing to justify a class here (see theory.md section 5).
 */
sealed interface MoneyOperationResult {
    data class Success(val result: Money) : MoneyOperationResult
    data class CurrencyMismatch(val left: Currency, val right: Currency) : MoneyOperationResult
}

/**
 * TODO 7: the safe sibling of operator fun plus above - same underlying logic, but returns
 * MoneyOperationResult instead of throwing.
 */
fun tryAdd(a: Money, b: Money): MoneyOperationResult {
    if (a.currency != b.currency) {
        return MoneyOperationResult.CurrencyMismatch(a.currency, b.currency)
    }
    return MoneyOperationResult.Success(a+b)
}
