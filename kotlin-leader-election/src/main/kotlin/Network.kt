package leader.election

import kotlinx.coroutines.channels.Channel

/**
 * Stands in for the network. Each node id gets its own unbounded inbox `Channel<Message>`;
 * "sending a message" is just `send`-ing onto the recipient's channel. See theory.md section 5
 * for why partitioned sends must drop silently instead of erroring.
 */
class Network(nodeIds: List<Int>) {
    private val inboxes: Map<Int, Channel<Message>> =
        nodeIds.associateWith { Channel(capacity = Channel.UNLIMITED) }

    /** null = no partition, everyone can reach everyone. Non-null = only nodes within the same
     * group can reach each other. */
    private var reachabilityGroups: List<Set<Int>>? = null

    fun inboxOf(nodeId: Int): Channel<Message> = inboxes.getValue(nodeId)

    /** Split the network into isolated groups. Nodes in different groups can no longer reach
     * each other until [healPartition] is called. */
    fun partition(vararg groups: Set<Int>) {
        reachabilityGroups = groups.toList()
    }

    /** Restore full connectivity. */
    fun healPartition() {
        reachabilityGroups = null
    }

    /**
     * TODO 1: implement partition-aware delivery.
     *  - If there is no active partition (`reachabilityGroups == null`), or [from] and [to] are
     *    in the same reachability group, deliver the message: `inboxOf(to).send(message)`.
     *  - Otherwise (different groups), drop the message silently - do nothing, no exception.
     *    Real network partitions don't throw; the sender just never hears back, and it's the
     *    receiver's own timeout logic (Node.kt TODO 4) that has to notice. See theory.md section 5.
     *  - Hint: `reachabilityGroups?.find { from in it }` gets you the group containing [from];
     *    check whether [to] is also in that group.
     */
    suspend fun send(from: Int, to: Int, message: Message) {
        val groups = reachabilityGroups // Using this because reachabilityGroups is marked var and is on heap and not method stack - so it could change
        if (groups == null) {
            inboxOf(to).send(message)
            return
        }
        val fromReachabilityGroup = groups.find { from in it }
        if (fromReachabilityGroup?.contains(to) == true) {
            inboxOf(to).send(message)
        }
    }
}
