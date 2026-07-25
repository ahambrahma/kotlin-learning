package org.example

class CoffeeReview (
    val name: String,
    val comment: String,
    val stars: Int?
)

class Payment(val money: Int)
class Coffee

fun orderCoffee(payment: Payment): Coffee {
    return Coffee()
}