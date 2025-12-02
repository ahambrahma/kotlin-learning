package org.example

class Circle (var radius: Double = 2.0) {
    private val pi: Double = 3.14
    @Override
    override fun toString(): String {
        return circumference().toString()
    }

    fun circumference() = 2 * radius * pi
    fun area() = pi * radius * radius
    fun diameter() = 2 * radius
}