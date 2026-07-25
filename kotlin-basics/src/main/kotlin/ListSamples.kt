package org.example

class ListSamples {
    fun display() {
        val booksToRead: List<String> = listOf("Tea with Agatha",
            "Mystery on First Avenue",
            "The Ravine of Sorrows",
            "Among the Aliens",
            "The Kingsford Manor Mystery")

        println()
        println()

        println(booksToRead)
        println()
        println()

        var newBooksToRead = booksToRead + "Beyond the expanse"
        newBooksToRead = newBooksToRead + "Mission Impossible" // Possible only because newBooksToRead is defined as a var
        println(newBooksToRead)

        println()
        println()

        val modifiableBooksToRead: MutableList<String> = mutableListOf(
            "Tea with Agatha",
            "Mystery on First Avenue",
            "The Ravine of Sorrows",
            "Among the Aliens",
            "The Kingsford Manor Mystery"
        )
        modifiableBooksToRead.add("Mission Impossible")
        modifiableBooksToRead.remove("Tea with Agatha")

        // NOTE: Even if we use + / - operations on a mutable list, still it will result in a immutable list
        println(modifiableBooksToRead)

        modifiableBooksToRead.forEach { element ->
            println(element)
        }

        println()
        println()

        // Another way of writing the same thing
        modifiableBooksToRead.forEach { println(it) }

        println()
        println()

        val sortableTitlesList = modifiableBooksToRead
            .map { title -> title.removePrefix("The ") }
            .sorted()

        sortableTitlesList.forEach { println(it) }

        println()
        println()

        val mysteryNovels = sortableTitlesList
            .filter { title -> title.contains("Mystery") } // Always returns a boolean

        // // Will be more performant than mysteryLevels
        val booksForNolan = booksToRead
            .filter { title -> title.contains("Mystery") } // Filters out the only 2 eligible items
            .map { title -> title.removePrefix("The ") } // Maps them
            .sorted()

        println(mysteryNovels)
        println(booksForNolan)
    }
}