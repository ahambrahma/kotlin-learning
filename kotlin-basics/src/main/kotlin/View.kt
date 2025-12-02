package org.example

interface View {
    val width: Int
    val height: Int

    fun area(): Int {
        return width * height
    }
}

interface IButton {
    val label: String

    fun description(): String {
        return label
    }
}

class SimpleButton(
    override val width: Int,
    override val height: Int,
    override val label: String
) : View, IButton

