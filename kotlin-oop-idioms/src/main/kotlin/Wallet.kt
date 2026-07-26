package oop.idioms

import kotlin.properties.Delegates

/**
 * Property delegation - see theory.md section 4 before implementing.
 */
class Wallet(private val currency: Currency) {
    private val transactions = mutableListOf<Money>()

    /**
     * TODO 1: computed once, on first access, by summing `transactions` - implement with
     * `by lazy { ... }`.
     *
     * Once this compiles: call wallet.total once, THEN call wallet.add(...) with another
     * transaction, THEN read wallet.total again in Main.kt. Note what you actually see printed,
     * and be ready to explain why in review - this is a real `by lazy` gotcha (see theory.md),
     * not a bug in the exercise.
     */
    val total: Money by lazy {
        var amount = Money.zero(currency)
        for (transaction in transactions) {
            amount += transaction
        }
        amount
    }

    /**
     * TODO 2: print a line every time this is reassigned, showing the old and new value -
     * implement with `by Delegates.observable(...)`.
     */
    var lastTransaction: Money? by Delegates.observable(null) { _, old, new ->
        println("Value changed from old value: $old -> $new")
    }

    fun add(money: Money) {
        require(money.currency == currency) {
            "Wallet is $currency, got ${money.currency}"
        }
        transactions.add(money)
        lastTransaction = money
    }
}
