package org.example

class ClassDelegationForGeneralTypes {
    fun display() {
        println()
        println()
        FancyCow().eat()     // Eating grass - munch, munch, munch!
        FancyChicken().eat() // Eating bugs - munch, munch, munch!
        FancyPig().eat()
    }
}

interface Eater {
    fun eat()
}

// The only thing changing in the eat implementations woild have been food
class Muncher(private val food: String) : Eater {
    override fun eat() = println("Eating $food - munch, munch, munch!")
}

class FancyCow : Eater by Muncher("grass")
class FancyChicken : Eater by Muncher("bugs")
class FancyPig : Eater by Muncher("corn")

