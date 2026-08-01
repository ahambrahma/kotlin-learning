# Consistent Hashing — Theory

Read this once before touching the TODOs. This module is a deliberate change of pace from the
last few - no coroutines, no HTTP, no Ktor. It's pure data structures and algorithms: build the
mechanism that lets systems like DynamoDB, Cassandra, and memcached (via the "Ketama" scheme)
spread keys across a set of nodes in a way that barely reshuffles anything when a node is added or
removed. Nothing here is concurrent, so nothing from `kotlin-shared-state` applies - this module's
difficulty is entirely in getting the ring algorithm itself right.

## 1. The problem: naive modulo hashing reshuffles almost everything

The obvious way to spread N keys across M nodes is `nodes[key.hashCode() % M]`. This works fine
right up until M changes. Say you have 3 nodes and route `key.hashCode() % 3`. Add a 4th node, and
now you're computing `key.hashCode() % 4` - a completely different formula. For nearly every key,
`hash % 3` and `hash % 4` land on different results, which means nearly every key now maps to a
different node than it used to. Concretely: a key with `hashCode() = 100` maps to node
`100 % 3 = 1` with 3 nodes, but `100 % 4 = 0` with 4 nodes - moved. A key with `hashCode() = 102`
maps to `102 % 3 = 0` before, `102 % 4 = 2` after - also moved. Across a large key set, roughly
`(M-1)/M` of all keys move to a new node purely because the modulus changed, even though only
1 node out of 4 is actually new. In a real system, that means adding one cache server to relieve
load triggers a near-total cache invalidation across every server - the exact opposite of what you
wanted. TODO 4 asks you to measure this gap directly rather than take it on faith.

## 2. The ring: the actual algorithm

Consistent hashing fixes this by hashing nodes and keys into the *same* numeric space - imagine a
circle of hash values from `0` up to some maximum, wrapping back around to `0`. Every node gets
placed at one or more points on this circle (by hashing its name). To find which node owns a given
key, hash the key into the same space, then walk clockwise around the circle until you hit the
first node - that node owns the key.

Concretely, with nodes at ring positions `10`, `50`, `90` (out of a much larger space) and a key
whose hash is `30`: walking clockwise from `30`, the first node position you reach is `50` - so
that node owns this key. A key with hash `95` walks clockwise past `90`... and there's nothing
after it, so it **wraps around** back to the smallest position on the ring, `10`. That wraparound
is the entire reason it's called a ring rather than a line - the highest possible hash value is
adjacent to the lowest one.

This is exactly why `getNode` (TODO 3) is built on a sorted map: `TreeMap.ceilingKey(keyHash)`
gives you "the smallest key present that is `>= keyHash`" in `O(log n)` - precisely "the first
node clockwise from here." When `ceilingKey` returns null, it means `keyHash` was greater than
every node position on the ring, which is exactly the wraparound case - the answer is the
*smallest* key on the ring (`ring.firstEntry()`), not "no owner."

Now the payoff: when you add a 4th node to a ring that already had 3, that new node lands at one
new point on the circle. Only the keys that fall between the new node's position and the previous
node clockwise from it move - every other key's "first node clockwise" answer is completely
unaffected, because nothing about their position or the other nodes' positions changed. Only a
fraction of the ring's circumference "belongs" to the new node, so only that fraction of keys move.

## 3. Virtual nodes: why one point per node isn't enough

If each physical node gets exactly one point on the ring, load balancing gets unlucky easily -
with only 3-4 random points on a circle, it's entirely plausible for one node's arc (the stretch
of the ring "before" it, going counter-clockwise to the previous node) to be 3-4x larger than
another's purely by chance, meaning that node ends up owning 3-4x the keys for no principled
reason. The fix used by every real implementation is **virtual nodes**: instead of hashing a
node's name once, hash it multiple times with a distinguishing suffix -
`hash("server-1#0")`, `hash("server-1#1")`, ..., `hash("server-1#99")` - and place all 100 (or
however many) results on the ring, all pointing back to the same physical node. With enough
virtual points spread pseudo-randomly around the circle, each physical node ends up owning many
small, scattered arcs instead of one large contiguous one, and the law of large numbers takes over
- the total arc length (and therefore key share) each physical node ends up with converges toward
an even split. This is also why removing a node (TODO 2) has to remove *all* of its virtual
points, not just one - forgetting even a handful would leave "ghost" ownership scattered around
the ring for a node that's supposed to be gone.

## 4. Why MD5 instead of `hashCode()`, and why a sorted map instead of a list

The hash function's *distribution quality* matters here in a way it usually doesn't. `String`'s
built-in `hashCode()` is a fine general-purpose hash, but it's not designed for spreading
structurally similar inputs apart - and virtual node names like `"server-1#0"`, `"server-1#1"`,
`"server-1#2"` are about as structurally similar as strings get (they differ by one trailing
character). A hash with weak avalanche behavior (small input changes producing only small, closely
related output changes) could cluster those virtual points close together on the ring instead of
spreading them out, quietly defeating the whole point of using them. MD5 (used here purely as a
convenient, dependency-free source of well-distributed bits - nothing here needs MD5's actual
cryptographic properties) avalanches hard: flipping one character produces a effectively unrelated
128-bit output, so even near-identical virtual node names land in unrelated ring positions.

The ring itself is a `TreeMap<Long, String>` rather than, say, a sorted `List<Pair<Long, String>>`
searched with `binarySearch`, mostly because `TreeMap` already gives you the exact operation this
algorithm needs as a named method - `ceilingKey` - instead of you having to hand-translate a binary
search's result index into "the next entry, or wrap around if none." Both are `O(log n)`; `TreeMap`
just names the operation you actually want.

## Java-8 / Go callouts

- `java.util.TreeMap` already existed in Java 8, unchanged - Kotlin's `ConsistentHashRing` here is
  just wrapping the same JDK class you'd use from Java directly.
- Since you know Go: Go's standard library has no built-in sorted map at all - the idiomatic
  approach there is a sorted `[]struct{ hash uint32; node string }` slice plus `sort.Search` (Go's
  binary search over a sorted slice, taking a predicate rather than a key) to find the same
  "first entry `>=` target" answer `TreeMap.ceilingKey` gives you for free. If you've ever reached
  for `sort.Search` in Go, you've already implemented the core of what `ceilingKey` does here.
- Consistent hashing is genuinely load-bearing production technology, not an academic exercise -
  Amazon's Dynamo paper (and DynamoDB after it), Cassandra's partitioner, and memcached's "Ketama"
  client-side sharding scheme are all built on close variants of exactly this algorithm.
