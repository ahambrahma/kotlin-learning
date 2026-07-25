package org.example

fun speed(distance: Double = 42.123, time: Double): Double{
  return distance / time
}

const val taxMultiplier = 1.09

// In case of a function which doesn't return anything, behind the scenes, Kotlin returns a type called
// Unit - equivalent of void
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

    val temperature = 48.5
    val reaction = when {
        temperature > 55 -> "It's too hot"
        temperature < 40 -> "It's too cold"
        else -> "It feels right"
    }

    // Multiple values evaluating to same result case
    val quantity = 4

    val pricePerBook = when (quantity) {
        1 -> 19.99
        2 -> 18.99
        3,4 -> 16.99
        else -> 14.99
    }

    println("Price per book: ${pricePerBook}")

    println("Reaction based on temperature: ${reaction}")


    /**
     * Functions
     */
    hello() // Using function to say hello
    println("Sum of 2 and 3 is ${sum(2,3)}")

    // Both should have same result
    printMessageWithPrefix("Shubham", "Welcome")
    printMessageWithPrefix(prefix = "Welcome", message = "Shubham")
    printMessageWithPrefix("Shubham")


    // Edge case related to default arguments - when default argument comes first
    println("Speed in case of edge case: ${String.format("%.3f", speed(time=8.27))}")

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

    val smallCircle = Circle(4.2)
    println("Circumference of small circle: ${smallCircle.circumference()}")
    println("Area of small circle: ${smallCircle.area()}")
    println("Diameter of small circle: ${smallCircle.diameter()}")

    println()

    val defaultCircle = Circle()
    println("Circumference of default circle: ${defaultCircle.circumference()}")
    println("Area of default circle: ${defaultCircle.area()}")
    println("Diameter of small circle: ${defaultCircle.diameter()}")


    val button = Button("Sample button")
    val defaultButton = DefaultButton()

    val abstractButtonImpl = ButtonImpl("Abstract button impl")
    abstractButtonImpl.printDescription()

    val simpleButton = SimpleButton(10, 20,  "Simple button")
    println("Area: ${simpleButton.area()}")
    println("Label: ${simpleButton.description()}")


    // Enum examples
    println(SchnauzerBreed.STANDARD.height)
    println(SchnauzerBreed.MINIATURE.ordinal)
    println(SchnauzerBreed.GIANT.isShorterThan(20))


    // Null safety
    val payment: Payment? = getPayment() // type of payment is Payment?
    if (payment != null) {
        val coffee = orderCoffee(payment) // type of payment is Payment here using smart casting
    } else {
        println("I can't order coffee today!")
    }

    val coffee = when (payment) {
        null -> println("I can't order coffee today!")
        else -> orderCoffee(payment)
    }
    println(coffee)

    // Functions being passed as references
    val withFiveDollarsOff = calculateTotal(20.0, discountForCouponCode("FIVE_BUCKS")) // $16.35
    println("Price with 5 dollars off: ${withFiveDollarsOff}")
    val withTenPercentOff  = calculateTotal(20.0, discountForCouponCode("TAKE_10"))  // $19.62
    println("Price with 10% off: ${withTenPercentOff}")
    val fullPrice          = calculateTotal(20.0, discountForCouponCode("NONE"))
    println("Full price: ${fullPrice}")


    // Usage of lambdas

    // These are examples of trailing lambdas
    val withFiveDollarsOff2 = calculateTotal(20.0) {price -> price - 5.0} // $16.35
    val withTenPercentOff2  = calculateTotal(20.0) {price -> price * 0.9}  // $19.62
    val fullPrice2          = calculateTotal(20.0) {price -> price }

    println("Price with 5 dollars off: ${withFiveDollarsOff2}")
    println("Price with 10 percent off: ${withTenPercentOff2}")
    println("Price: ${fullPrice2}")

    val withFiveDollarsOffComplexLambda = calculateTotal(20.0) { price ->
        val result = price - 5.0
        println("Initial price: $price")
        println("Discounted price: $result")
        result
    }
    println("Price with 5 dollars off: ${withFiveDollarsOffComplexLambda}")


    val listSamples = ListSamples()
    listSamples.display()

    val setSamples = SetExamples()
    setSamples.display()

    val mapSamples = MapSamples()
    mapSamples.display()

    val mapListCombination = MapListCombination()
    mapListCombination.display()

    val helloStr = "hello"
    println(helloStr.singleQuoted())

    val helloStrNullable: String? = null
    println(helloStrNullable.singleQuoted2())

    val scopeFunctions = ScopeFunctions()
    scopeFunctions.display()

    val shadowingAndImplicitReceivers = ShadowingAndImplicitReceivers()
    shadowingAndImplicitReceivers.display()

    val interfaces = InterfaceExamples()
    interfaces.display()

    val classDelegation = ClassDelegation()
    classDelegation.display()

    val classDelegationForGeneralTypes = ClassDelegationForGeneralTypes()
    classDelegationForGeneralTypes.display()

    val abstractAndOpenClasses = AbstractAndOpenClasses()
    abstractAndOpenClasses.display()

    val anyClassMethodOverriding = AnyClassMethodOverriding()
    anyClassMethodOverriding.display()

    val dataClass = DataClasses()
    dataClass.display()

    val destructuringNormalClasses = DestructuringNormalClasses()
    destructuringNormalClasses.display()

    val sealedTypes = SealedTypes()
    sealedTypes.display()

    val runtimeExceptions = RuntimeExceptions()
    runtimeExceptions.display()
}

// Extension function type
fun String.singleQuoted() = "'$this'"
// Nullable receiver types
fun String?.singleQuoted2() = "'$this'"

fun discountForCouponCodeLambda(couponCode: String): (Double) -> Double = when (couponCode) {
    "FIVE_BUCKS" -> { price -> price - 5.0 }
    "TAKE_10"    -> { price -> price * 0.9 }
    else         -> { price -> price }
}

fun discountForCouponCode(couponCode: String): (Double) -> Double = when (couponCode) {
    "FIVE_BUCKS" -> ::discountFiveDollars
    "TAKE_10"    -> ::discountTenPercent
    else         -> ::noDiscount
}

fun discountFiveDollars(price: Double): Double = price - 5.0
fun discountTenPercent(price: Double): Double = price * 0.9
fun noDiscount(price: Double): Double = price

fun calculateTotal(
    initialPrice: Double,
    applyDiscount: (Double) -> Double
): Double {
    // Apply coupon discount
    val priceAfterDiscount = applyDiscount(initialPrice)
    // Apply tax
    val total = priceAfterDiscount * taxMultiplier

    return total
}

fun getPayment(): Payment? {
    return Payment(10)
}
