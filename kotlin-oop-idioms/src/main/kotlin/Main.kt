package oop.idioms

fun main() {

    println(Currency.USD.format(12345))
    println(Currency.EUR.format(12345))

    val wallet = Wallet(Currency.USD)
    wallet.add(Money.of(12.50, Currency.USD))
    wallet.add(Money.of(7.25, Currency.USD))

    println("Total: ${wallet.total}")

    wallet.add(Money.of(10.25, Currency.USD))
    println("Total: ${wallet.total}") // Total still remains the same as it was cached the first time it was read

    // TODO 1: the `by lazy` gotcha - add one more transaction to the wallet here, then print
    // wallet.total again. Compare it to the first println above and explain in a comment why
    // it did (or didn't) change.

    val price = Money.of(9.99, Currency.USD)
    println("3x price: ${price * 3}")
    println("Negated: ${-price}")
    println("price < wallet.total: ${price < wallet.total}")

    // TODO 2: call tryAdd() with two Money values in DIFFERENT currencies, and use a `when` over
    // the returned MoneyOperationResult (Success / CurrencyMismatch) to print an appropriate
    // message for each case.

    println(tryAdd(Money.of(12.50, Currency.USD), Money.of(12.50, Currency.EUR)))
    println(tryAdd(Money.of(12.50, Currency.USD), Money.of(12.50, Currency.USD)))

    // TODO 3: call the operator `+` (plus) directly with two Money values in different currencies
    // instead, inside a try/catch, and print the exception's message - this is the "throws
    // instead of returning a Result" sibling behavior tryAdd() avoids.

    try {
        Money.of(12.50, Currency.USD) + Money.of(12.50, Currency.EUR)
    } catch (e: IllegalStateException) {
        println(e.message)
    }

    // TODO 4: use ExchangeRates.convert to convert `price` to EUR and print the result.
    println(ExchangeRates.convert(price, Currency.EUR))

    // TODO 5: build a List<Money> with a few different amounts (same currency), and sort it using
    // an object expression implementing Comparator<Money> - e.g. sortedWith(object : Comparator<Money> { ... }) -
    // sort it DESCENDING (largest first) to make sure you're not just relying on the natural
    // compareTo ordering from TODO 4 in Money.kt.

    val moneyList = listOf(
        Money.of(2.5, Currency.USD),
        Money.of(1.0, Currency.USD),
        Money.of(12.0, Currency.USD),
        Money.of(8.0, Currency.USD),
    )

    val sortedList = moneyList.sortedWith(object : Comparator<Money> {
        override fun compare(o1: Money, o2: Money): Int {
            return o2.compareTo(o1)
        }
    })
    println(sortedList)
}
