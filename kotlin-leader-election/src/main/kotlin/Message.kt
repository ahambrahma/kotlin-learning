package leader.election

/**
 * The four message types the Bully algorithm needs. See theory.md section 2 for what each one
 * means and when a node sends/receives it.
 */
sealed class Message {
    data class Election(val candidateId: Int) : Message()
    data class Alive(val fromId: Int) : Message()
    data class Coordinator(val leaderId: Int) : Message()
    data class Heartbeat(val fromId: Int) : Message()
}
