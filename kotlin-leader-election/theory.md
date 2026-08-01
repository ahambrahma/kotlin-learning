# Leader Election — Theory

Read this once before touching the TODOs. This module simulates a small distributed system
inside a single process: each "node" is a coroutine, the "network" is a set of `Channel`s, and the
goal is to implement the Bully algorithm - the classic, simplest leader-election protocol - well
enough to survive a node crashing and a network partition, including the split-brain that
partition can cause.

## 1. Nodes as coroutines, channels as the network

Every node gets its own inbox `Channel<Message>`. "Sending a message to node 3" is nothing more
than `channel.send(message)` on node 3's inbox - the same primitive you used for producer/consumer
work in `kotlin-ingestion-pipeline`, just repurposed as a stand-in for a network link instead of a
work queue.

One deliberate choice: unlike `kotlin-shared-state`, this module runs entirely on
cooperative, single-threaded scheduling - plain `launch`/`runBlocking`, no `Dispatchers.Default`.
There's no real parallelism between nodes here, and that's intentional: every node's own state
(`currentLeaderId`, whether it's mid-election, etc.) is only ever touched by that node's own
coroutine, so there's no shared-mutable-state race to worry about, and nothing from that module's
`Mutex`/`Atomic` toolkit is needed here. The interesting correctness problems in this module are
entirely about *message timing and ordering*, not thread-safety - keeping the two concerns
separate is worth doing deliberately rather than accidentally mixing them.

## 2. The Bully algorithm

Every node has a unique integer id. The rule is simple to state: **whoever has the highest id
among the currently-alive nodes is the leader.** The mechanism that makes every node actually
agree on who that is, without any central coordinator, is what you're implementing.

**Starting an election.** A node with no known leader sends `Election(myId)` to every peer with a
*higher* id than its own - never to lower ids, since a lower-id peer could never legitimately
outrank you anyway. Then it waits (with a timeout) for `Alive` responses.

**Responding to an election.** When a node receives `Election(candidateId)` (necessarily from a
lower id, by construction), it does two things: replies `Alive(myId)` to the candidate - "I outrank
you, stand down" - and, if it isn't already running its own election, starts one. This second part
is what makes the algorithm *cascade upward*: node 2's election message reaches node 3, which
starts its own election reaching node 4, which starts its own reaching node 5 - and so on, until
whichever node has the true highest id among everyone currently alive gets no `Alive` replies at
all (because nothing outranks it) and wins by default.

**Winning.** If a node's election timeout expires with zero `Alive` responses received - or it had
no higher peers to even ask in the first place - it declares itself leader:
`currentLeaderId = myId`, and broadcasts `Coordinator(myId)` to everyone.

**Losing (temporarily).** A node that receives `Alive` from a higher peer abandons its own
election attempt without declaring anyone leader yet - it just waits, expecting a `Coordinator`
message to arrive once the actual winner finishes.

## 3. Detecting failure: heartbeats and timeouts

Whoever currently believes itself to be leader periodically broadcasts `Heartbeat(myId)` to every
peer - this is the "how does anyone find out the leader died" mechanism. Every non-leader node
tracks the last time it heard from its recorded leader (via `Heartbeat` or `Coordinator`). If that
gap ever exceeds a leader-timeout, the node assumes the leader is gone, clears its own
`currentLeaderId`, and starts a fresh election - exactly the same election machinery from section 2,
just triggered by "silence" instead of "just booted up." This is the same `withTimeoutOrNull`
pattern from `kotlin-health-checker`, just wrapping a channel receive instead of a network call:
`run()`'s main loop uses it to either handle a message that arrived, or - if nothing arrived within
one polling interval - run the periodic timeout/heartbeat checks instead.

## 4. Split-brain, and the rule that fixes it

Here's the scenario TODO 4's demo deliberately creates: partition the network into two groups that
can't reach each other. Each side independently notices "no heartbeat from the leader" (because the
real leader is now unreachable from one side) and elects its *own* leader from among whoever it can
still see. Two groups, two leaders, both convinced they're right - a genuine split-brain, the
classic failure mode of any leader-election scheme under partition.

The fix is a small, specific rule inside `handleMessage`: whenever a node receives a `Coordinator`
or `Heartbeat` claiming leadership, if the claimed id is `>=` whatever it currently has recorded as
leader, it adopts that id - **even if it currently believes itself to be the leader.** A higher id
always wins, unconditionally. Once the partition heals, the *true* highest-id leader's heartbeats
start reaching the mistaken lower-id "leader" again, and this rule makes it step down and adopt the
real leader instead of the two sides just disagreeing forever. Watch for this specifically in the
demo output: right after `healPartition()`, give it a beat, and the split-brain should resolve back
to a single agreed-upon leader.

Worth naming explicitly: this "highest id always wins" rule is a real simplification. Raft (a more
modern, far more common leader-election protocol in production - etcd, Consul, CockroachDB all use
it) replaces "static id" with a monotonically increasing **term number** that increments every
election, specifically because relying on a fixed id has sharp edges Raft is designed to avoid.
Bully is the right algorithm to *learn* the core cascade-and-timeout shape from; Raft is what you'd
actually reach for in a real system, and is worth reading about once this clicks.

## 5. Partition simulation: silent drops, not errors

`Network.send`'s partition-aware logic (TODO 1) must **silently drop** messages between nodes in
different reachability groups - no exception, no error return, just nothing happens. This matches
what a real network partition looks like from any single node's point of view: a message you sent
doesn't bounce back with "unreachable," it just... never gets a response, and you find out only
once your own timeout expires. Simulating an explicit error here would teach the wrong mental model
- real partitions are silent, and that silence is exactly what your timeout-based failure detection
(section 3) has to cope with.

## Java-8 / Go callouts

- Since you know Go: this maps almost directly onto Go's own idioms - a `Channel<Message>` here is
  a `chan Message` there, and `withTimeoutOrNull(pollIntervalMs) { inbox.receive() }` is the same
  shape as Go's `select { case msg := <-inbox: ... case <-time.After(pollInterval): ... }`. If
  you've written that `select` pattern in Go, you've already built the core of this module's main
  loop once before.
- Java 8 has no comparably lightweight equivalent - the closest analog would be an
  `ExecutorService` per node plus a `BlockingQueue` with `poll(timeout, unit)`, which gets verbose
  fast precisely because Java has no `select`-style "wait on this queue or a timer, whichever comes
  first" primitive built in the way Kotlin's coroutines (and Go's channels) do.
- Real systems using Bully-like or Raft-based election for real: ZooKeeper (a Bully-family
  algorithm), and Raft itself inside etcd, Consul, and CockroachDB.
