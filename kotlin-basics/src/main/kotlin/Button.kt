package org.example

class Image (val description: String)

// Makes this as extendable i.e abstract class
open class Button(val label: String) {
    open fun printDescription() = println(label)
}

class ImageButton(label: String, val image: Image): Button(label) {
    override fun printDescription() {
        println("$label with image ${image.description}")
    }
}

class DefaultButton : Button("Click")