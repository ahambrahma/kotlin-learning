package org.example

/***
 * Pros:
 * 1. They create equals() and hashcode() functions for value equality
 * 2. toString() for output
 * 3. Copies
 * 4. Destructuring assignments.
 *
 * Cons:
 * 1. Cannot be extended by another class i.e no open/abstract modifiers possible. However, it can extend another class.
 * 2. The second disadvantage is that all of the constructor parameters in a data class must be property parameters.
 * In other words, each one must be declared with either val or var.
 * This means it’s not possible to add a constructor parameter that is only relayed to a superclass constructor.
 *
 * 3. A data class must have at least one parameter in its primary constructor.
 * 4. a data class may have properties that are not part of its constructor, they will not be regarded in any of the functions that are generated like equals(), hashcode)_ etc
 *
 *
 */

class DataClasses {
    fun display() {
        val bill1 = DollarBillDataClass(100)
        val bill2 = DollarBillDataClass(100)

        println()
        println()
        println(bill1 == bill2)                  // true
        println(mutableSetOf(bill1, bill2).size) // 1
        println(bill1)

        val book = BookDataClass("The Malt Shop Caper", 18, "Slim Chancery", 6, 9, "020516918K")
        val newBook = book.copy(price=20) // Only changes the price and keeps all other properties same
        println(newBook)

        // Example of destructuring a data class
        // Make sure ordering is same as defined in the primary constructor
        val (title, cost, author, widthInInches, heightInInches, isbn) = book
        val (title1, cost1) = book // This also works
        println(title1)
        println(cost1)
    }
}

data class DollarBillDataClass(val amount: Int)

data class BookDataClass(
    val title: String,
    val price: Int,
    val author: String,
    val width: Int,
    val height: Int,
    val isbn: String,
)