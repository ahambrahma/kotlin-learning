package leader.election

import kotlinx.coroutines.*

/**
 * Demo driver - not a TODO, this is given so you can watch your Node/Network implementation
 * survive a crash and a network partition. Run it once TODOs 1-4 are done and read the output
 * against theory.md sections 3-4 to confirm: initial election converges on node 5 (highest id);
 * killing node 5 reconverges on node 4; partitioning {1,2} vs {3,4} produces genuine split-brain
 * (watch for two different leader beliefs); healing the partition reconverges back onto a single
 * leader.
 */
fun main() = runBlocking {
    val ids = listOf(1, 2, 3, 4, 5)
    val network = Network(ids)
    val nodes = ids.associateWith { id -> Node(id, ids.filter { it != id }, network) }
    val jobs = ids.associateWith { id -> launch { nodes.getValue(id).run() } }
    val aliveIds = ids.toMutableSet()

    fun printLeaders(label: String) {
        println("--- $label ---")
        aliveIds.sorted().forEach { id ->
            println("node $id believes leader is ${nodes.getValue(id).currentLeaderId}")
        }
        println()
    }

    delay(1000)
    printLeaders("initial election")

    println("killing node 5 (the current leader)...")
    jobs.getValue(5).cancel()
    aliveIds.remove(5)
    delay(1500)
    printLeaders("after node 5 crashes")

    println("partitioning the network into {1,2} and {3,4}...")
    network.partition(setOf(1, 2), setOf(3, 4))
    delay(1500)
    printLeaders("during partition (watch for split-brain: two different leaders)")

    println("healing the partition...")
    network.healPartition()
    delay(1500)
    printLeaders("after healing (should reconverge on a single leader)")

    jobs.values.forEach { it.cancel() }
}
