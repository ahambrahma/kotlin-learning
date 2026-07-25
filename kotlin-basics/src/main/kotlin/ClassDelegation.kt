package org.example

class ClassDelegation {
    fun display() {
        val waiter = Waiter(Chef(), Bartender())
        val beverage = waiter.prepareBeverage("Soda")
        val entree = waiter.prepareEntree("Salmon on Rice")

        println()
        println()
        println(beverage)
        println(entree)
        waiter.receiveCompliment("Good beverage")
    }
}

enum class Entree { TOSSED_SALAD, SALMON_ON_RICE }
enum class Beverage { WATER, SODA, PEACH_ICED_TEA, TEA_LEMONADE }

interface KitchenStaff {
    fun prepareEntree(name: String): Entree?
    fun receiveCompliment(message: String)
}

interface BarStaff {
    fun prepareBeverage(name: String): Beverage?
    fun receiveCompliment(message: String)
}

class Bartender: BarStaff {
    override fun prepareBeverage(name: String): Beverage? = when (name) {
        "Water"        -> Beverage.WATER
        "Soda"         -> Beverage.SODA
        "Peach Tea"    -> Beverage.PEACH_ICED_TEA
        "Tea-Lemonade" -> Beverage.TEA_LEMONADE
        else           -> null
    }
    override fun receiveCompliment(message: String) =
        println("Bartender received a compliment: $message")
}



class Chef : KitchenStaff {
    override fun prepareEntree(name: String): Entree? = when (name) {
        "Tossed Salad"   -> Entree.TOSSED_SALAD
        "Salmon on Rice" -> Entree.SALMON_ON_RICE
        else             -> null
    }
    override fun receiveCompliment(message: String) =
        println("Chef received a compliment: $message")
}

// In case multiple interfaces have the same method, then a class implementing those interfaces
// has to override the same
class Waiter (
    private val chef: Chef,
    private val bartender: Bartender
): KitchenStaff by chef, BarStaff by bartender {
    // The waiter can prepare a beverage by himself...
    // prepareBeverage is being taken care of by the bartender
//    fun prepareBeverage(name: String): Beverage? = when (name) {
//        "Water" -> Beverage.WATER
//        "Soda"  -> Beverage.SODA
//        else    -> null
//    }

    // ... but needs the chef to prepare an entree
    // with by chef - this method gets delegated to the chef object
    // override fun prepareEntree(name: String): Entree? = chef.prepareEntree(name)

    fun acceptPayment(money: Int) = println("Thank you for paying for your meal")

    override fun receiveCompliment(message: String) = when {
        message.contains("entree")   -> chef.receiveCompliment(message)
        message.contains("beverage") -> bartender.receiveCompliment(message)
        else                         -> println("Waiter received compliment: $message")
    }
}

