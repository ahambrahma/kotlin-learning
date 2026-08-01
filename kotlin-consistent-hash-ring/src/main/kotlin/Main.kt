package consistent.hashring

/**
 * TODO 4: prove consistent hashing's actual value with real numbers - see theory.md section 1 for
 * why this comparison matters. Requirements:
 *  - Generate 1000 sample keys, e.g. `(0 until 1000).map { "key-$it" }`.
 *  - Build a `ConsistentHashRing`, then `addNode("server-1")`, `addNode("server-2")`,
 *    `addNode("server-3")`.
 *  - Record `getNode(key)` for every sample key into a `Map<String, String?>` (key -> owning
 *    node) - this is your "before" snapshot.
 *  - Call `addNode("server-4")` on the SAME ring.
 *  - Record `getNode(key)` again for every sample key - your "after" snapshot.
 *  - Count how many keys have a DIFFERENT owning node between before/after, and print that as a
 *    percentage of 1000. Expect roughly 20-25% (moving from 3 nodes to 4 means only the keys that
 *    now "belong" to the new node's ring segment should move).
 *  - For contrast, compute the same before/after comparison using NAIVE modulo hashing instead:
 *    `nodeNames[key.hashCode().mod(nodeNames.size)]`, where `nodeNames` is
 *    `listOf("server-1", "server-2", "server-3")` before and that list plus `"server-4"` after.
 *    Use `.mod(...)`, not `%` - `String.hashCode()` can be negative, and `%` in Kotlin can return
 *    a negative result for a negative left-hand side, which would crash indexing into
 *    `nodeNames`; `.mod(...)` always returns a non-negative result. Print that percentage too -
 *    expect it to be dramatically higher (most keys remap), demonstrating exactly what consistent
 *    hashing avoids.
 */
fun main() {
    val consistentHashRing = ConsistentHashRing()
    val keys = (0 until 1000).map {
        "key-$it"
    }
    consistentHashRing.addNode("server-1")
    consistentHashRing.addNode("server-2")
    consistentHashRing.addNode("server-3")

    val keyToNodeMap1 = mutableMapOf<String, String?>()
    keys.forEach { key -> keyToNodeMap1[key] = consistentHashRing.getNode(key) }
    println(keyToNodeMap1)

    consistentHashRing.addNode("server-4")
    val keyToNodeMap2 = mutableMapOf<String, String?>()
    keys.forEach { key -> keyToNodeMap2[key] = consistentHashRing.getNode(key) }
    println(keyToNodeMap2)

    var count = 0
    keyToNodeMap2.forEach { (key1, value1) ->
        val value2 = keyToNodeMap1[key1]
        if (value2 != value1) {
            count++
        }
    }


    val nodeNamesBefore = listOf("server-1", "server-2", "server-3")
    val nodeNamesAfter = nodeNamesBefore + "server-4"

    var naiveCount = 0
    keys.forEach { key ->
        val before = nodeNamesBefore[key.hashCode().mod(nodeNamesBefore.size)]
        val after = nodeNamesAfter[key.hashCode().mod(nodeNamesAfter.size)]
        if (before != after) naiveCount++
    }

    println("Consistent hashing remap: ${count * 100.0 / keys.size}%")
    println("Naive modulo remap: ${naiveCount * 100.0 / keys.size}%")
}
