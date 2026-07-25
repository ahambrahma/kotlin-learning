package org.example

class MapListCombination {
    public fun display() {
        val tools = listOf(
            Tool("Hammer", 14, "Nail"),
            Tool("Wrench", 8, "Hex Nut"),
            Tool("Wrench", 8, "Hex Bolt"),
            Tool("Slotted Screwdriver", 5, "Slotted Screw"),
            Tool("Phillips Screwdriver", 5, "Phillips Screw"),
        )

        val toolbox = tools.associate { tool ->
            tool.name to tool.correspondingHardware // Since Wrench comes twice - overrides the value
        }

        println()
        println()
        println(toolbox)

        // Creates a map where keys are tool names and values are the tool objects
        val toolsByName = tools.associateBy {
            tool -> tool.name
        }

        println(toolsByName)

        // Inversely, if you want to create a map where the keys are the Tool object
        // and the value is specified in the lambda, you can use the associateWith() function.
        // The lambda of this function returns the value, and the original list element will be the key.
        val toolWeightInPounds = tools.associateWith { tool ->
            tool.weightInOunces * 0.0625
        }
        println(toolWeightInPounds)

        // Split the tools list on the basis of the weight in ounces
        val toolsByWeight = tools.groupBy { tool ->
            tool.weightInOunces
        }

        println(toolsByWeight)

        // In case you want something other than the original list element in the resulting lists, you can also call this function with a second argument
        val toolNamesByWeight = tools.groupBy(
            { tool -> tool.weightInOunces },
            { tool -> tool.name }
        )
        println(toolNamesByWeight)
    }
}

class Tool (
    val name: String,
    val weightInOunces: Int,
    val correspondingHardware: String
) {
    override fun toString(): String {
        return "Tool(name='$name', weightInOunces=$weightInOunces, correspondingHardware='$correspondingHardware')"
    }
}


