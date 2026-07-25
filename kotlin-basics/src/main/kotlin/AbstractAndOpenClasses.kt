package org.example

class AbstractAndOpenClasses {
    fun display() {
        println()
        println()
        val car = Clunker(0.25)
        car.accelerate()

        val openCar = OpenCar() // Can be instantiated directly
        openCar.accelerate()
    }
}

// There’s a catch, though - while an open class can have functions and properties
// that are either open or final, it cannot contain any that are abstract
open class OpenCar(private val acceleration: Double = 1.0) {
    private var speed = 0.0
    protected open fun makeEngineSound() = println("Vrrrrrr...")

    fun accelerate() {
        speed += acceleration
        makeEngineSound()
    }
}

abstract class AbstractCar(private val acceleration: Double = 1.0) {
    private var speed = 0.0
    protected open fun makeEngineSound() = println("Vrrrrrr...")

    fun accelerate() {
        speed += acceleration
        makeEngineSound()
    }
}

class Clunker(acceleration: Double): AbstractCar(acceleration) {
    override fun makeEngineSound() = println("putt-putt-putt")
}