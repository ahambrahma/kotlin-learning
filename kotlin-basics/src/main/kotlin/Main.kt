package org.example

fun hello() {
    println()
    println("Hello world")
    println()
}

fun sum(x: Int, y: Int): Int {
    return x + y
}

// Default prefix is Hello
fun printMessageWithPrefix(message: String, prefix: String = "Hello") {
    println("$prefix $message")
}

fun strLengthWithNullable(nullable1: String?): Int {
    return (if (nullable1 == null) 0 else nullable1.length)
}

val lowerCaseString: (String) -> (String) = { text -> text.lowercase() }


// Usage of Safe call
fun lengthString(maybeString: String?): Int? = maybeString?.length

// Usage of elvis operator
fun lengthStringWithElvis(maybeString: String?): Int = maybeString?.length ?: 0

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

    /**
     * Functions
     */
    hello() // Using function to say hello
    println("Sum of 2 and 3 is ${sum(2,3)}")

    // Both should have same result
    printMessageWithPrefix("Shubham", "Welcome")
    printMessageWithPrefix(prefix = "Welcome", message = "Shubham")
    printMessageWithPrefix("Shubham")


    /***
     * Lambda expressions
     */
    println()
    val upperCaseString = { text: String -> text.uppercase() }
    println(upperCaseString("Hello world"))

    val numbers = listOf(1, 2, -1, -2, 4)
    // Usage of lambda function within filter
    val positives = numbers.filter({ x -> x > 0 })
    println(positives)

    val isNegative = {x:Int -> x < 0} // Wrap it will {} to mark it as a lambda function
    // Pass on a lambda function as a parameter
    val negatives = numbers.filter(isNegative)
    println(negatives)

    val doubles = numbers.map{ x -> x*2 }
    println(doubles)

    /***
     * Classes
     */

    println(lowerCaseString("Hello world"))

    val selfContact = Contact(1, "shubham@gmail.com")
    selfContact.category = "self"

    println(selfContact)

    val selfContact2 = Contact(1, "shubham@gmail.com")
    selfContact2.category = "self2"

    println(selfContact == selfContact2)

    val selfContact3 = Contact(1, "shubham@gmail.com")
    selfContact3.category = "self"

    println(selfContact == selfContact3)

    /***
     * Nullable types
     */
    var nullable: String? = "You can keep a null here"
    nullable = null

    println(strLengthWithNullable(nullable))


    val nullString: String? = null
    println(lengthString(nullString))

    println(lengthStringWithElvis(nullString))
}