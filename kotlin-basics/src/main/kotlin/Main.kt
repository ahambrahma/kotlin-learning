package org.example

fun main() {
    /* Basic printing and for loop */
    val name = "Kotlin"
    println("Hello, " + name + "!")

    for (i in 1..5) {
        println("i = $i")
    }

    var customerName:String = "Shubham"
    println("Customer name: $customerName")

    /* List operations */

    val integerList:List<Int> = listOf(1, 2, 3, 4);
    println(integerList)

    val mutableIntegerList: MutableList<Int> = mutableListOf(1, 2, 3, 4);
    println("Current list: $mutableIntegerList")

    mutableIntegerList.add(5)
    println("Updated list: $mutableIntegerList")

    val firstItem = mutableIntegerList.first()
    val lastItem = mutableIntegerList.last()
    val count = mutableIntegerList.count()

    println("FirstItem: $firstItem, LastItem: $lastItem, count: $count")
    println(7 in mutableIntegerList)

    /**** Map operations */

    val juiceMenu: MutableMap<String, Int> = mutableMapOf();
    juiceMenu["Apple"] = 50
    juiceMenu["Banana"] = 60
    juiceMenu["Mango"] = 70

    println(juiceMenu.contains("Apricot"))
    println(juiceMenu.get("Apple"))

    println(juiceMenu)


    /**
     * If and when statements
     */
    var ok = true

    for (i in 1..2) {
        if (ok) {
            println("Hello")
        } else {
            println("world")
        }
        ok = !ok
    }

    // Ternary operator usage
    val a = 1
    val b = 2

    println(if (a > b) a else b) // Returns a value: 2

    // When is the equivalent of switch expression in Kotlin
    // Note that all branch conditions are checked sequentially until one of them is satisfied.
    // So only the first suitable branch is executed.
    for (obj in listOf("Hello", "World", "Shubham")) {
        when (obj) {
            // Checks whether obj equals to "1"
            "World" -> println("One")
            // Checks whether obj equals to "Hello"
            "Hello" -> println("Two")
            // Default statement
            else -> println("Three")
        }
    }

    val obj = "Hello"

    val result = when (obj) {
        "1" -> "One"
        "2" -> "Two"
        "3" -> "Three"
        else -> "Four"
    }

    println(result)

}