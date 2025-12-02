package org.example

abstract class AbstractButton(val label: String) {
    abstract fun printDescription()
}

class ButtonImpl(label: String) : AbstractButton(label) {
    override fun printDescription() {
        println(label)
    }
}