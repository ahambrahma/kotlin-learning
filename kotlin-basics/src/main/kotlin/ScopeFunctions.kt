package org.example

/**
 * There are five functions in Kotlin’s standard library that are designated as scope functions.
 * Each of them is a higher-order function that you typically call with a lambda,
 * which introduces a new statement scope.
 * The point of a scope function is to take an existing object - called a context object 2
 * - and represent it in a particular way inside that new scope.
 *
 * Reference: https://typealias.com/start/kotlin-scopes-and-scope-functions/
 *
 */
class ScopeFunctions {
    public fun display() {

        println()
        println()

        // with() scope function
        // To avoid doing address.X = Y everytime, we can use with function instead.
        val address = Address("","","","","")

        with(address) { // Address acts as the receiver here
            street1 = "9801 Maple Ave"
            street2 = "Apartment 255"
            city = "Rocksteady"
            state = "IN"
            postalCode = "12345"
        }

        println(address)

        // Rest 4 are extension functions
        // run() works same as that of with() - only difference being the fact that run is an extension
        // function and with() isn't

        // Since it is an extension function - it can be used along with chain calls.
        address.run {
            street1 = "9822 Maple Ave"
            street2 = "Apartment 256"
            city = "Rocksteady 1"
            state = "IN 1"
            postalCode = "12346"
        }

        println(address)

        // Next one is let()
        // let() might be the most frequently-used scope function.
        // It’s very similar to run(), but instead of representing the context object as an implicit receiver,
        // it’s represented as the parameter of its lambda

        address.let {
            x -> x.street1 += ".1"
        }

        println(address)

        // also()
        // As with let(), the also() function represents the context object as the lambda parameter, too.
        // However, unlike let(), which returns the result of the lambda, the also() function returns the context object.
        // It can be used for operations which need to be done on the side like logging or printing
        val title = "The Robots from Planet X3"
        val newTitle = title
            .removePrefix("The ")
            .also { println(it) } // Robots from Planet X3
            .singleQuoted()
            .uppercase()


    }
}

class Address (var street1: String, var street2: String, var city: String, var state: String, var postalCode: String) {
    override fun toString(): String {
        return "Address(street1='$street1', street2='$street2', city='$city', state='$state', postalCode='$postalCode')"
    }
}