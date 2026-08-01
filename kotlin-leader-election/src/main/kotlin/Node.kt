package leader.election

import kotlinx.coroutines.withTimeoutOrNull

/**
 * One participant in the Bully election. All state below is only ever touched by this node's own
 * coroutine (see theory.md section 1 for why that means no Mutex/Atomic is needed here).
 */
class Node(
    val id: Int,
    private val peerIds: List<Int>,
    private val network: Network,
    private val pollIntervalMs: Long = 100,
    private val electionTimeoutMs: Long = 300,
    private val leaderTimeoutMs: Long = 600,
    private val heartbeatIntervalMs: Long = 200
) {
    /** Who this node currently believes is the leader (null = no known leader). */
    var currentLeaderId: Int? = null
        private set

    private var electionInProgress = false
    private var electionStartedAtMs: Long = 0
    private var lastHeardFromLeaderMs: Long = System.currentTimeMillis()
    private var lastHeartbeatSentMs: Long = 0

    private val inbox get() = network.inboxOf(id)

    /**
     * Main loop: every poll interval, either handle a message that arrived, or - if nothing
     * arrived in time - run the periodic checks in [tick]. Runs forever until the coroutine is
     * cancelled. Kicks off with an initial election since no leader is known yet at startup.
     */
    suspend fun run() {
        startElection()
        while (true) {
            val message = withTimeoutOrNull(pollIntervalMs) { inbox.receive() }
            if (message != null) {
                handleMessage(message)
            } else {
                tick()
            }
        }
    }

    /**
     * TODO 2: handle one incoming message. See theory.md sections 2-4.
     *  - `Election(candidateId)`: this always arrives from a lower id (nodes only send Election
     *    to higher peers). Reply with `Alive(id)` sent back to `candidateId`, then - if this node
     *    isn't already running its own election - call `startElection()` too. This is the
     *    upward-cascade step.
     *  - `Alive(fromId)`: a higher peer just told you to stand down. Set `electionInProgress =
     *    false` and do NOT set `currentLeaderId` - just wait for the eventual `Coordinator`
     *    message from whoever actually wins.
     *  - `Coordinator(leaderId)`: someone is announcing themselves as leader. Adopt it if
     *    `leaderId >= (currentLeaderId ?: -1)` (the split-brain-resolving rule from theory.md
     *    section 4 - a higher id always wins, even over a leader id you currently hold yourself).
     *    On adoption: set `currentLeaderId = leaderId`, `electionInProgress = false`, and update
     *    `lastHeardFromLeaderMs = System.currentTimeMillis()`.
     *  - `Heartbeat(fromId)`: same adoption rule and same three updates as `Coordinator` - a
     *    heartbeat from a peer claiming leadership carries the same information.
     */
    private suspend fun handleMessage(message: Message) {
        when (message) {
            is Message.Alive -> electionInProgress = false
            is Message.Election -> {
                network.send(id, message.candidateId, Message.Alive(id))
                if (!electionInProgress) {
                    startElection()
                }
            }
            is Message.Coordinator -> handlePotentialLeaderMessage(message.leaderId)
            is Message.Heartbeat -> handlePotentialLeaderMessage(message.fromId)
        }
    }

    private fun handlePotentialLeaderMessage(potentialLeaderId: Int) {
        if (potentialLeaderId >= (currentLeaderId ?: -1)) {
            currentLeaderId = potentialLeaderId
            electionInProgress = false
            lastHeardFromLeaderMs = System.currentTimeMillis()
        }
    }

    /**
     * TODO 3: start an election. See theory.md section 2.
     *  - Mark `electionInProgress = true` and record `electionStartedAtMs = System.currentTimeMillis()`.
     *  - Find peers with a higher id than this node's (`peerIds.filter { it > id }`).
     *  - If there are none, this node can't be beaten - call `becomeLeader()` immediately.
     *  - Otherwise, send `Election(id)` to each higher peer via `network.send(id, peerId, ...)`
     *    and wait for `Alive` replies (handled by the main loop / handleMessage + tick, not here -
     *    this function's job is just to kick the election off).
     */
    private suspend fun startElection() {
        electionInProgress = true
        electionStartedAtMs = System.currentTimeMillis()
        val peersWithHigherIds = peerIds.filter { it > id }
        if (peersWithHigherIds.isEmpty()) {
            becomeLeader()
        } else {
            peersWithHigherIds.forEach { peerId -> network.send(id, peerId, Message.Election(id)) }
        }
    }

    /**
     * TODO 3 (cont.): declare this node the leader.
     *  - Set `currentLeaderId = id`, `electionInProgress = false`, `lastHeardFromLeaderMs =
     *    System.currentTimeMillis()`.
     *  - Broadcast `Coordinator(id)` to every peer in `peerIds` via `network.send`.
     */
    private suspend fun becomeLeader() {
        currentLeaderId = id
        electionInProgress = false
        val currentTimeMs = System.currentTimeMillis()
        lastHeardFromLeaderMs = currentTimeMs
        lastHeartbeatSentMs = currentTimeMs // Treating this similar to heartbeat as we did above
        peerIds.forEach { peerId -> network.send(id, peerId, Message.Coordinator(id)) }
    }

    /**
     * TODO 4: periodic checks, run once per poll interval when no message arrived. See theory.md
     * sections 3-4.
     *  - Election timeout: if `electionInProgress` and `System.currentTimeMillis() -
     *    electionStartedAtMs >= electionTimeoutMs`, this node never got an `Alive` reply from any
     *    higher peer within the timeout - it wins. Call `becomeLeader()`.
     *  - Leader timeout: if there IS a `currentLeaderId` that isn't this node's own id, and
     *    `System.currentTimeMillis() - lastHeardFromLeaderMs >= leaderTimeoutMs`, the leader has
     *    gone silent - assume it's dead. Clear `currentLeaderId = null` and call `startElection()`.
     *  - Heartbeat broadcast: if `currentLeaderId == id` (this node IS the leader) and
     *    `System.currentTimeMillis() - lastHeartbeatSentMs >= heartbeatIntervalMs`, broadcast
     *    `Heartbeat(id)` to every peer and update `lastHeartbeatSentMs`.
     */
    private suspend fun tick() {
        val currentTimeMs = System.currentTimeMillis()
        if (electionInProgress && currentTimeMs - electionStartedAtMs >= electionTimeoutMs) { // Election timeout
            // No message received from any peer
            becomeLeader()
        } else if (!electionInProgress && currentLeaderId != id && ((currentTimeMs - lastHeardFromLeaderMs) >= leaderTimeoutMs)) { // Leader timeout
            currentLeaderId = null
            startElection()
        } else if (currentLeaderId == id && ((currentTimeMs - lastHeartbeatSentMs) >= heartbeatIntervalMs)) { // Heartbeat timeout
            peerIds.forEach { peerId -> network.send(id, peerId, Message.Heartbeat(id)) }
            lastHeartbeatSentMs = currentTimeMs
        }
    }
}
