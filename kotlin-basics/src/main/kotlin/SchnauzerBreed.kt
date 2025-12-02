package org.example

enum class SchnauzerBreed(val height: Int) {
    MINIATURE(33),
    STANDARD(47),
    GIANT(65);

    val family: String = "Schauzer"

    fun isShorterThan(centimeters: Int): Boolean = height < centimeters
}