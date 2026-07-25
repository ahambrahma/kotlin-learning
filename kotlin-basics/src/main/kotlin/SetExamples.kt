package org.example

class SetExamples {
    // Note that a set does not guarantee the order of its elements when you print them out or use a collection operation on it.
    // It’s possible that the elements will be in the same order that you added them, but don’t depend on it!
    fun display() {
        val booksBySlim: MutableSet<String> = mutableSetOf(
            "The Malt Shop Caper",
            "Who is Mrs. W?",
            "At Midnight or Later",
        )
        booksBySlim.add("The Malt Shop Caper")
        println()
        println()
        println(booksBySlim)
    }
}