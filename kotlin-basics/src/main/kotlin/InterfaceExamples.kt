package org.example

class InterfaceExamples {
    fun display() {
        println()
        println()

        val sue = Farmer("Sue")
        val henrietta = Chicken("Henrietta", 2)
        val hamlet = Pig("Hamlet", 2)
        val dairyGodmother = Cow("Dairy Godmother")

        sue.greet(henrietta)
        sue.greet(hamlet)
        sue.greet(dairyGodmother)
    }
}

interface FarmAnimal {
    val name: String
    fun speak()
}

class Cow(override val name: String): FarmAnimal {
    override fun speak() {
        println("Moo!")
    }
}

class Chicken(override val name: String, val numberOfEggs: Int): FarmAnimal {
    override fun speak() {
        println("Cluck!")
    }
}

class Pig(override val name: String, val excitementLevel: Int): FarmAnimal {
    override fun speak() {
        repeat(excitementLevel) {
            println("Oink!")
        }

    }
}

class Farmer(val name: String) {
    fun greet(animal: FarmAnimal) {
        println("Good morning, ${animal.name}!")

//        if (animal is Chicken) { // Smart cast
//            println("I see you have ${animal.numberOfEggs} eggs today!")
//        }

        // Explicit cast - this might break if the type is not chicken
//        val chicken: Chicken = animal as Chicken
//        println("I see you have ${chicken.numberOfEggs} eggs today!")

        val chicken: Chicken? = animal as? Chicken
        chicken?.let { println("I see you have ${it.numberOfEggs} eggs today!") }

        animal.speak()
    }
}