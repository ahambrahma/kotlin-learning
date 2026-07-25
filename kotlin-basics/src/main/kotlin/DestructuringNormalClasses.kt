package org.example

class DestructuringNormalClasses {

    /***
     * Destructuring doesn't just apply to data classes.
     * It also applies to normal classes as well.
     * For allowing destructuring on any class, implement component1() .... componentN() functions.
     */

    fun display() {
        val children = listOf(
            Child("Fiona", 5),
            Child("Jack", 7)
        )

        children.forEach { (name, age) ->
            println("$name is $age years old.")
        }

        children.forEach { (_, age) -> println("$age") }
    }
}

class Child(val name: String, val age: Int) {
    /***
     * When a function includes the operator modifier, it can still be called like any other function,
     * but it also serves some special purpose - and the particular purpose that it serves depends upon the name of the function.
     * When a function is named as they are in componentN(), that special purpose is that the function will be used when the object is destructured.
     */
    operator fun component1() = name
    operator fun component2() = age
}