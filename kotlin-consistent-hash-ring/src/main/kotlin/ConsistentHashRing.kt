package consistent.hashring

import java.security.MessageDigest
import java.util.TreeMap

/**
 * Given - a stable, deterministic hash function mapping any string onto a 32-bit space,
 * represented as a Long so we never have to deal with Kotlin's lack of unsigned Int literals. MD5
 * is used purely for its well-distributed output bits, not for any cryptographic property -
 * nothing here needs to be secure, just evenly spread out. See theory.md section 4 for why
 * `hashCode()` alone would have been a weaker choice for this specific use.
 */
private fun hash(input: String): Long {
    val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
    return ((digest[0].toLong() and 0xFF) shl 24) or
        ((digest[1].toLong() and 0xFF) shl 16) or
        ((digest[2].toLong() and 0xFF) shl 8) or
        (digest[3].toLong() and 0xFF)
}

/**
 * A consistent hash ring over string node names. See theory.md for the full mental model - in
 * short: both nodes and keys are hashed onto the same numeric space (a circle, conceptually), and
 * a key belongs to whichever node's hash is the next one clockwise from the key's own hash.
 */
class ConsistentHashRing(private val virtualNodesPerNode: Int = 100) {
    private val ring = TreeMap<Long, String>()
    private val nodes = mutableSetOf<String>()

    /**
     * TODO 1: add [node] to the ring. See theory.md section 3 for why a single physical node
     * needs multiple points on the ring, not just one. Requirements:
     *  - For `i in 0 until virtualNodesPerNode`: compute `hash("$node#$i")` and put that as a key
     *    into `ring`, mapped to `node` (the physical node name - every virtual point for this
     *    node maps back to the same physical node).
     *  - Also add `node` to the `nodes` set (used by TODO 2 to know what to remove later).
     */
    fun addNode(node: String) {
        for (i in 0 until virtualNodesPerNode) {
            ring[hash("$node#$i")] = node
        }
        nodes.add(node)
    }

    /**
     * TODO 2: remove [node] from the ring - the exact inverse of addNode. Recompute the same
     * `virtualNodesPerNode` keys (`hash("$node#$i")` for the same range of `i`) and remove each
     * one from `ring`, then remove `node` from `nodes`.
     */
    fun removeNode(node: String) {
        for (i in 0 until virtualNodesPerNode) {
            ring.remove(hash("$node#$i"))
        }
        nodes.remove(node)
    }

    /**
     * TODO 3: find which physical node owns [key]. See theory.md section 2 for the exact
     * algorithm. Requirements:
     *  - Compute `val keyHash = hash(key)`.
     *  - Find the smallest ring key that is `>= keyHash` - `ring.ceilingKey(keyHash)` gives you
     *    exactly that (returns null if no such key exists, i.e. `keyHash` is greater than every
     *    key currently on the ring).
     *  - If `ceilingKey` found something, look it up in `ring` to get the node name it maps to.
     *  - If `ceilingKey` returned null, wrap around to the SMALLEST key on the ring instead -
     *    `ring.firstEntry()` - and use that entry's node. This wraparound is the entire reason
     *    it's called a "ring": there's no true end, the highest hash value wraps back to the
     *    lowest.
     *  - Return null only if the ring itself is empty (no nodes have been added at all).
     */
    fun getNode(key: String): String? {
        if (ring.isEmpty()) {
            return null
        }

        val keyHash = hash(key)
        var nextNodePosition = ring.ceilingKey(keyHash)
        if (nextNodePosition == null) {
            nextNodePosition = ring.firstEntry().key
        }
        return ring[nextNodePosition]
    }

    fun nodeCount(): Int = nodes.size
}
