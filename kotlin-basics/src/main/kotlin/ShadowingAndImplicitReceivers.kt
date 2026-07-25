package org.example

class ShadowingAndImplicitReceivers {
    public fun display() {

        val person = Person("Julia")
        val dog = Dog("Sparky")

        with(person) {
            with(dog) {
                println(name) // Prints Sparky from the dog object
                bark()        // Calls bark() on the dog object
                sayHello()    // Calls sayHello() on the person object
            }
        }

//        with(person) {
//            with(dog) {
//                println(name) // Prints Sparky from the dog object
//                bark()        // Calls bark() on the dog object
//                this.sayHello()    // Calls sayHello() on the person object /
//                // This will break as the innermost object is a dog and we don't have sayHello method
//                // defined on a dog
//            }
//        }
    }
}

class Person(val name: String) {
    fun sayHello() = println("Hello!")
}

class Dog(val name: String) {
    fun bark() = println("Ruff!")
}

