package org.example

data class Contact(val id: Int, var email: String) {
    var category: String = "work"

    override fun toString(): String {
        return "Context(id:$id, email:$email, category: $category)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Contact

        if (id != other.id) return false
        if (email != other.email) return false
        if (category != other.category) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + email.hashCode()
        result = 31 * result + category.hashCode()
        return result
    }

}