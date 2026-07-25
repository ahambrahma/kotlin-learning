package org.example

class AnyClassMethodOverriding {
    fun display() {
        val bill1 = DollarBill(5)
        val bill2 = DollarBill(5)

        println()
        println()
        println(bill1 == bill2) // Uses equals operation
        println(bill1 === bill2) // Used for checking whether the same object is being used or not - called as referential equality operator

        val denominations = mutableSetOf<DollarBill>()
        denominations.add(DollarBill(1))
        denominations.add(DollarBill(2))
        denominations.add(DollarBill(5))
        denominations.add(DollarBill(1))

        // Comes as 4 if we don't override the hashcode method
        // Prints 3 if we override hashcode
        println(denominations.size)
        // Prints DollarBill@1, DollarBill@2, DollarBill@5 if we don't override toString()
        println(denominations)
    }
}

class DollarBill(val amount: Int) {
    override fun equals(other: Any?): Boolean =
        if (other is DollarBill) amount.equals(other.amount) else false

    override fun hashCode() = amount.hashCode()

    override fun toString() = "DollarBill(amount=$amount)"
}