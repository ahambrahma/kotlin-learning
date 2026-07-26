# Channels & Backpressure — Theory

Read this once before touching the TODOs. This module builds directly on the fan-out/fan-in work
from `kotlin-health-checker`, but solves a different shaped problem: instead of "run N independent
checks with a concurrency cap," this is "a producer generates work faster than consumers can
handle it - what happens?" That question is what backpressure means, and `Channel` is Kotlin's
primitive for it.

## 1. What a `Channel` actually is

A `Channel<T>` is a queue with one crucial twist: instead of blocking a thread when it's full or
empty, `send()` and `receive()` **suspend**. Same relationship as `Thread.sleep` vs `delay` from
the last module - a `java.util.concurrent.BlockingQueue`'s `put()`/`take()` park a real OS thread
when the queue is full/empty; a `Channel`'s `send()`/`receive()` free the thread to go do other
work while waiting, and resume the coroutine once there's room or an element. Same mental model,
same reason it scales better, just applied to a producer/consumer relationship instead of a single
suspending call.

`Channel<T>` implements both `SendChannel<T>` (the `send()`/`close()`/`trySend()` side) and
`ReceiveChannel<T>` (the `receive()`/`trySend()`/iteration side) - which is why `produceLogEvents`
takes a `SendChannel<LogEvent>` and `consumeLogEvents` takes a `ReceiveChannel<LogEvent>`: each
function only needs (and should only be given) the half of the API it actually uses. This is the
same "expose the narrowest interface that does the job" instinct as accepting `List` instead of
`ArrayList` when you don't need mutation.

## 2. Capacity is where backpressure actually lives

This is the core idea of the whole module: **a channel's capacity determines whether the producer
gets throttled by the consumer's pace, or is free to race ahead of it.**

- **`Channel.RENDEZVOUS`** (capacity `0`, the default from `Channel<T>()`): `send()` suspends until
  a `receive()` is *already waiting* to take that exact element - a direct handoff, no buffering
  at all. This is the strongest form of backpressure: the producer can never get more than one
  element ahead of the slowest available consumer.
- **A specific buffered capacity** (e.g. `Channel<T>(capacity = 50)`): `send()` only suspends once
  50 unconsumed elements are already sitting in the channel. This is a tunable middle ground - some
  slack so the producer doesn't lock-step with consumers on every single element, but still a hard
  ceiling that eventually pushes back once consumers fall behind by too much.
- **`Channel.UNLIMITED`**: `send()` never suspends - the channel grows without bound to hold
  everything the producer throws at it. This is **backpressure fully removed.** It looks like a
  win (the producer finishes instantly!) but it's really just moving the problem into memory: if
  consumers are permanently slower than the producer, this is exactly how you get an
  out-of-memory crash in production instead of a controlled, visible slowdown. TODO 2 is designed
  to make this difference directly observable rather than theoretical.
- **`Channel.CONFLATED`**: capacity of effectively 1, but with different semantics than "buffer of
  1" - a new `send()` **overwrites** whatever unconsumed element is currently sitting there rather
  than suspending. Nothing is ever queued; only the *latest* value survives if the consumer hasn't
  kept up. This is backpressure of a totally different flavor: not "slow down," but "stale data is
  worthless, only ever give me the newest thing" - the right tool for something like a live price
  ticker or a sensor reading, and the *wrong* tool for anything like our log events, where every
  single item matters and none should be silently dropped. TODO 3 is designed to make this loss
  directly visible (the processed count won't add up to what was sent) so it stops being an
  abstract warning and becomes something you've actually seen happen.

The general lesson, independent of this specific module: **backpressure is a design decision, not
a default you can ignore.** An unbounded queue anywhere in a real pipeline (a `Channel`, a message
broker topic with no consumer lag alerting, a thread pool backed by an unbounded work queue) is a
memory leak waiting for the day your downstream gets slow - which it eventually will.

## 3. Closing a channel, and why `for (x in channel)` is the idiom

`channel.close()` marks "no more elements will ever be sent." It does **not** discard anything
already sitting in the channel - existing buffered elements are still delivered; only future
`send()` calls fail. `for (event in channel) { ... }` is the standard way to consume: it keeps
looping, receiving elements as they arrive, and exits cleanly exactly once the channel is both
closed and drained - no manual sentinel/"poison pill" value needed the way you might hand-roll with
a raw `BlockingQueue` in Java. This is the direct Kotlin-channel equivalent of Go's `for v := range
ch` after `close(ch)` - same idiom, same guarantee.

## 4. Fan-out: multiple consumers, one channel, no duplication

If several coroutines all call `receive()` (or run a `for` loop) against the *same* `Channel`,
each element is delivered to exactly one of them - the channel divides work across whichever
consumer happens to be ready next, it does not broadcast the same element to everyone. This is
easy to mix up with "multiple listeners on an event," which is a different concept entirely
(that's what `SharedFlow`/`StateFlow` are for, further down the roadmap) - a plain `Channel` is a
work-distribution queue, not a broadcast/pub-sub mechanism. `consumeLogEvents`'s doc comment asks
you to confirm this for yourself once four workers are pulling from one channel: the 200 events
should end up split across all four `WorkerStats`, not duplicated four times over.

## 5. `produce { }` - the idiomatic shorthand (for later, not required here)

Once the raw `Channel` + manual `close()` pattern feels natural, real code usually reaches for the
`produce { }` builder instead: `fun CoroutineScope.produceLogEvents(count: Int): ReceiveChannel<LogEvent>
= produce { repeat(count) { send(...) } }` bundles "create a channel, launch a coroutine that
fills it, close it automatically when that coroutine finishes or throws" into one call. This
module deliberately has you write the manual version first - seeing exactly where the channel is
created and exactly when `close()` fires is what makes `produce { }` legible later as "the same
thing, just packaged," rather than a shortcut you're trusting blindly.

## 6. How this differs from the `Semaphore` you just used

Easy to conflate these since both "limit how much happens at once," but they solve different
shaped problems. `Semaphore` (previous module) bounds **how many independent, unrelated tasks run
concurrently** - N health checks that don't feed into each other. A `Channel`'s capacity bounds
**how far ahead a producer can get from its consumers in a pipeline relationship** - the tasks
here aren't independent, they're a single stream of work items flowing from one stage to the next.
If you only need "don't run more than N of these at once," reach for `Semaphore`. If you have a
genuine producer/consumer relationship where the producer might outpace the consumer, that's a
`Channel`.

## Java-8 / Go callouts

- The direct Java-8 analogue is `java.util.concurrent.BlockingQueue` (`ArrayBlockingQueue` for a
  bounded, backpressured queue; `LinkedBlockingQueue` unbounded by default - the same
  memory-blowup risk as `Channel.UNLIMITED`, just less obvious because nobody had to type
  "UNLIMITED" to get it). The difference is purely blocking-a-thread vs suspending, same theme as
  every module so far.
- Since you know Go: `Channel<T>()` ≈ an unbuffered `chan T` (synchronous handoff);
  `Channel<T>(capacity = n)` ≈ `make(chan T, n)`; `close(channel)` behaves the same way in both
  languages, including that sending on a closed channel is an error in both, and that a closed,
  drained channel's receive/range loop ends cleanly rather than hanging. Kotlin's `Channel.CONFLATED`
  has no single-line Go equivalent - the closest hand-rolled pattern in Go is a buffered
  `chan T` of size 1 combined with a non-blocking `select` that drains-then-sends to overwrite,
  which Kotlin gives you for free as a capacity option instead of boilerplate.
- Kotlin also has a `select { }` expression for waiting on whichever of several channels produces
  something first (direct parallel to Go's `select` statement) - not needed for this module's
  TODOs, but worth knowing it exists once you're combining multiple channels.
