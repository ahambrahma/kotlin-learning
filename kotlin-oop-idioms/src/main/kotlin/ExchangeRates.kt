package oop.idioms

/**
 * `object` declaration - see theory.md section 2. A standalone singleton, not attached to any one
 * class the way Money's companion object is attached to Money - this represents a shared service
 * (a rate lookup) rather than a factory for a specific type.
 *
 * TODO 1: store a small fixed lookup of conversion rates - at minimum USD->EUR and EUR->USD - and
 * implement convert() using it. Throw IllegalArgumentException for any currency pair you haven't
 * defined a rate for (same-currency "conversion" should just return the input unchanged, no rate
 * lookup needed).
 */
object ExchangeRates {

    val conversionRatesMap: Map<Currency, Map<Currency, Double>> = mapOf(
        Currency.USD to mapOf(Currency.EUR to 0.92),
        Currency.EUR to mapOf(Currency.USD to 1.09),
    )

    fun convert(money: Money, targetCurrency: Currency): Money {
        val sourceCurrency = money.currency
        val amountInCents = money.amountCents

        if (sourceCurrency == targetCurrency) {
            return money
        }

        val conversionRatesMapForSourceCurrency = conversionRatesMap[sourceCurrency] ?: throw IllegalArgumentException("Currency $sourceCurrency doesn't exist in supported forex rates")
        val forexRate = conversionRatesMapForSourceCurrency[targetCurrency] ?: throw IllegalArgumentException("Currency $sourceCurrency to $targetCurrency conversion doesn't exist in supported forex rates")

        val amountInTargetCurrency = (amountInCents*forexRate)/100 // Should not be in cents
        return Money.of(amountInTargetCurrency, targetCurrency)
    }
}
