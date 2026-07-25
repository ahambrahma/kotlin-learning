package org.example

import kotlin.reflect.typeOf

class MapSamples {
    fun display() {
        // One simple way to associate two values is to use a class called Pair
        val association = Pair("Nail", "Hammer")
        println()
        println()
        println(association)

        // Another way of writing the same thing
        val associationViaInfixFunction: Pair<String, String> = "Nail" to "Hammer"
        println()
        println()
        println(associationViaInfixFunction)

        val toolbox = mapOf(
            "Nail" to "Hammer",
            "Hex Nut" to "Wrench",
            "Hex Bolt" to "Wrench",
            "Slotted Screw" to "Slotted Screwdriver",
            "Phillips Screw" to "Phillips Screwdriver",
        )

        println()
        println()
        println(toolbox)
        println(toolbox["Hello"]) // Prints null
        try {
            println(toolbox.getValue("Hello")) // Gives an exception - this is a non-nullable tyope
        } catch (e: NoSuchElementException) {
            println(e::class.simpleName)
        }

        println(toolbox.getOrDefault("Hanger Bolt", "Hand"))

        /***
         * Modifiable maps
         */
        val toolboxMutable = mutableMapOf(
            "Nail" to "Hammer",
            "Hex Nut" to "Wrench",
            "Hex Bolt" to "Wrench",
            "Slotted Screw" to "Slotted Screwdriver",
            "Phillips Screw" to "Phillips Screwdriver"
        )

        println()
        println()
        toolboxMutable.forEach { entry ->
            println("Use a ${entry.value} on a ${entry.key}")
        }

        val screwDrivers = toolbox.filter { entry ->
            entry.value.contains("Screwdriver")
        }
        println(screwDrivers)

        val newToolbox = toolbox
            .mapKeys { entry -> entry.key.replace("Hex", "Flange") }
            .mapValues { entry -> entry.value.replace("Wrench", "Ratchet") }

        println(newToolbox)
    }
}