package org.example

enum class Size { CUP, BUCKET, BAG }

// When we define this to be a sealed interface, then if when condition on an object of a class which implemented this interface
// we'll have to ensure all sub types are handled.

// If we use a sealed modifier to an interface in a library, it prevents others from being able to add another subtype of the interface
// when they use the library.

// Restrictions of using sealed classes:
// 1. All subtypes must be declared in the same code base.
// 2. All subtypes must be defined in the same package

sealed interface Request {
    val id: Int
}


// An alternative to using the interface. Sealed classes are abstract by default - so we can't instantiate them as well.
sealed class RequestClass {
    val id: Int = kotlin.random.Random.nextInt()
}

class OrderRequest(override val id: Int, val size: Size) : Request
class RefundRequest(override val id: Int, val size: Size, val reason: String) : Request
class SupportRequest(override val id: Int, val text: String) : Request

object FrontDesk {
    fun receive(request: Request) {
        println("Handling request #${request.id}")
        when (request) {
            is OrderRequest  -> IceCubeFactory.fulfillOrder(request)
            is RefundRequest -> IceCubeFactory.fulfillRefund(request)
            is SupportRequest -> HelpDesk.handle(request)
        }
    }
}

object IceCubeFactory {
    fun fulfillOrder(order: OrderRequest) = println("Fulfilling order #${order.id}")
    fun fulfillRefund(refund: RefundRequest) = println("Fulfilling refund #${refund.id}")
}

object HelpDesk {
    fun handle(request: SupportRequest) = println("Help desk is handling ${request.id}")
}

class SealedTypes {
    fun display() {
        // Without sealed types
        println()
        println()
        val order = OrderRequest(123, Size.CUP)
        FrontDesk.receive(order)

        val refund = RefundRequest(456, Size.CUP, "Accidentally ordered too much")
        FrontDesk.receive(refund)

        val request = SupportRequest(789, "I can't open the bag of ice!")
        FrontDesk.receive(request)

        // If we forget to add a branch to a when in FrontDesk, then the support request won't be filled
        // In order for us to handle this at compile time, we have sealed types.


    }
}