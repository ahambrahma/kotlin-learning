# Shared Mutable State & Race Conditions — Theory

Read this once before touching the TODOs. Every module so far has been about coordinating
independent work (health checks, a producer/consumer pipeline) - nothing so far required two
coroutines to fight over the *same* mutable variable. This module is specifically about that: what
happens when they do, why it breaks in a very particular way, and the two different tools
(`Mutex`, `AtomicInteger`) for fixing it - plus where each one's guarantee actually ends.

## 1. What a race condition actually is, traced with real numbers

`count++` reads as one operation but is really three: **read** the current value, **compute**
value + 1, **write** the result back. Each of those is a separate step at the bytecode level.
A race condition happens when two threads interleave those steps instead of running them fully
before/after each other. Concretely, with `count` starting at 5:

```
Thread A: read count -> 5
Thread B: read count -> 5      (before A has written anything back)
Thread A: compute 5 + 1 -> 6
Thread B: compute 5 + 1 -> 6
Thread A: write 6
Thread B: write 6
```

Two increments happened, but `count` went from 5 to 6, not 7. B's increment is silently lost - not
an exception, not a crash, just a quietly wrong number. This is what `UnsafeCounter` in
`Counters.kt` does, and why TODO 4 asks you to run it against a real expected total rather than
just eyeball the code: the bug is invisible in the source, only visible in the outcome.

## 2. Why this module needs *real* threads, not just coroutines

Here's the detail that ties directly back into everything you've built so far: **on a
single-threaded dispatcher, this specific race is impossible.** Kotlin coroutines are cooperative -
one coroutine runs until it hits an actual suspension point, and only then does another get a
turn. `count++` has no suspension point in it at all, so on one thread, it always runs start-to-
finish uninterrupted - there's no window for another coroutine to sneak in between the read and
the write. Every previous module in this roadmap either ran on a single thread by default
(`runBlocking`'s own dispatcher) or coordinated through a `Channel` (which serializes access by
design). This is the first module where you'll explicitly use `Dispatchers.Default` - a real,
multi-threaded thread pool - because genuine *parallelism* (multiple threads actually executing
at the same literal instant) is what's required to make this race reliably observable. Without it,
TODO 4 would just print the correct total every time and the whole lesson would be invisible.

## 3. Coroutine lifecycle: continuations, dispatchers, and thread-hopping

Worth understanding precisely, since it's the reason section 4's warning is true at all, not just
an assertion to memorize.

**A coroutine is not a thread - it's a compiler transformation.** When Kotlin compiles a
`suspend fun`, it rewrites it into a state machine: each suspension point becomes a numbered state,
and your local variables get lifted into fields on an object (a `Continuation`) instead of living
on a call stack. A suspended coroutine is nothing more than one of these objects sitting on the
heap, holding "resume here, with this data." That's the entire reason coroutines are so cheap
compared to threads - no OS involvement is needed to create, suspend, or resume one. A real OS
thread costs roughly half a megabyte of stack plus kernel scheduling overhead, capping you in the
thousands; coroutines are ordinary heap objects, and you can have millions.

**The dispatcher decides which thread actually runs the code, moment to moment.**
`Dispatchers.Default` is a shared pool sized to your CPU core count, for CPU-bound work.
`Dispatchers.IO` is a larger, elastic pool for blocking calls. `runBlocking` with no dispatcher argument sets up its own
single-thread event loop bound to the calling thread - which is exactly why no earlier module in
this roadmap ran into any of this: everything ran on one thread, cooperatively, by default.

**The lifecycle, concretely.** Launch a coroutine on `Dispatchers.Default` and the dispatcher hands
it to some free thread - say Thread-3. It runs completely normally, like any function call, until
it hits a genuine suspension point (`delay()`, a `Mutex` wait, a `channel.receive()` with nothing
available). Right there, the machinery packages up "resume here, with this state" into the
`Continuation`, and control returns up through the call stack - Thread-3 is now entirely free, and
goes back to the pool to pick up other work. The suspended coroutine isn't "on" any thread at all
at this point; it's just an inert object waiting for something to happen. Later, when that
something happens (the timer fires, the lock frees, an element arrives), whatever triggered it
calls `continuation.resumeWith(...)`. That resumption request goes back through the *same
dispatcher*, which assigns it to whatever thread happens to be free **at that moment** - maybe
Thread-3 again, by coincidence, maybe Thread-7. There's no preference for the original thread
unless the dispatcher is inherently single-threaded. Execution then continues exactly where it left
off - seamless from the code's perspective, but potentially on a completely different physical
thread than where it started.

This is safe for almost everything, because the coroutine machinery guarantees correct memory
visibility across that handoff, and most code simply doesn't care which literal thread is running
it. It only becomes a problem when something ties correctness to *thread identity itself* - which
is exactly what's next.

## 4. `Mutex` - a coroutine-aware lock

`Mutex` is coroutines' mutual-exclusion primitive: `mutex.withLock { ... }` guarantees only one
coroutine executes that block at a time. Critically, waiting for the lock **suspends**, it doesn't
block a thread - same theme as every suspending API in this roadmap. This is exactly why `withLock`
requires a `suspend fun` to call it from.

This matters more than it sounds like: **never guard suspending code with Java's `synchronized`
block or a plain `ReentrantLock`.** The underlying reason is the thread-hopping from section 3: a
suspended coroutine is not guaranteed to resume on the same OS thread it suspended on. JVM
monitor-based locks tie correctness to thread identity, which is exactly what a coroutine can't
promise you.

Concretely, for `ReentrantLock`: `.lock()` and `.unlock()` are ordinary function calls, so this
compiles without complaint -

```kotlin
lock.lock()
delay(100)     // suspends here - may resume on a different thread entirely
lock.unlock()  // called from whatever thread resumed the coroutine, not necessarily the one that locked it
```

`ReentrantLock` tracks ownership per-thread, so `unlock()` from a thread that never called `lock()`
throws `IllegalMonitorStateException` at best, or corrupts the lock's guarantees at worst - two
coroutines can end up believing they're both inside the critical section at once, the exact race
the lock was supposed to prevent.

`synchronized { }` is partly self-defending against this specific mistake - its block parameter is
a plain, non-suspend lambda, so the compiler outright rejects calling a suspend function directly
inside one. The danger with `synchronized` is different but still real: a contended `synchronized`
block **blocks** the waiting thread rather than suspending it. On a size-limited dispatcher like
`Dispatchers.Default` (roughly one thread per CPU core), enough coroutines piling up waiting on a
contended `synchronized` block can exhaust the entire pool, starving every other coroutine that
needs that same dispatcher to run at all - and a JVM monitor wait doesn't respond to coroutine
cancellation the way waiting on a `Mutex` does.

`Mutex` avoids both failure modes because it doesn't track ownership by thread identity at all - it
tracks it at the coroutine level, so it's correct no matter which physical thread happens to be
executing at lock time versus unlock time. If you only remember one hard rule from this section,
make it this one: reach for `Mutex`, not `synchronized`/`ReentrantLock`, anywhere a suspension
point might occur between acquiring and releasing.

## 5. `AtomicInteger`/`AtomicLong` - lock-free, and *why* they're lock-free

`java.util.concurrent.atomic.AtomicInteger` gives you `incrementAndGet()`, `get()`, `compareAndSet()`
etc. without ever suspending or blocking. Under the hood it uses a CPU-level compare-and-swap
instruction: "set this value to X, but only if it's still currently Y" - if another thread changed
it in between, the operation fails and silently retries with the new current value, instead of
your thread ever having to wait. No lock, no suspension, no thread parked - just a tight retry
loop at the hardware level. That's why atomics are cheaper than a `Mutex` for the case they're
built for: **protecting a single value.**

That phrase - *a single value* - is the entire boundary of what an atomic gives you, and it's the
point of `LatencyStats.kt`. `count` and `sum` are two separate pieces of state that need to stay
consistent *with each other*: an `averageMs()` call must never see one already updated and the
other not. Making `count` an `AtomicInteger` and `sum` an `AtomicLong` independently would make
each individual field correct in isolation, but it does **not** make the pair correct together - a
reader could still land exactly between one atomic's update and the other's. Atomicity doesn't
compose across variables just because each variable is individually atomic. This is precisely the
gap `Mutex` closes: a lock protects an *invariant spanning multiple fields*, not just one field's
own internal consistency. That's the real dividing line between when to reach for `Atomic*` versus
`Mutex` - not "which one is faster" (though `Atomic*` usually is, for what it covers), but "is this
one independent value, or several values that must be read/written as a unit."

## 6. The better fix, when you can get it: don't share the mutable state at all

Worth naming explicitly, because it connects straight back to `kotlin-ingestion-pipeline`: the
*best* fix for a shared-mutable-state race is often to not have shared mutable state in the first
place. Instead of N coroutines all mutating one `count` directly, you can have them each `send()`
their updates through a `Channel` to a single dedicated coroutine that owns the state and is the
only thing that ever touches it - the "actor" pattern. Since only one coroutine ever reads or
writes the state, there's no race to have, by construction, and you already have every tool you'd
need for this (`Channel`, `for (x in channel)`) from the last module. `Mutex`/`Atomic*` are the
right call when restructuring around a single owner isn't practical; funnelling through a channel
is often the more idiomatic Kotlin-coroutines answer when it is.

## Java-8 / Go callouts

- `synchronized`, `ReentrantLock`, and `java.util.concurrent.atomic.*` all already existed in
  Java 8 and haven't changed - what's new here is specifically the interaction with suspension
  (section 4's warning) that doesn't exist in plain Java thread code, because plain Java threads
  never "give back" a thread mid-lock the way a suspended coroutine can.
- Since you know Go: `Mutex` ≈ `sync.Mutex` - same "only one goroutine/coroutine in the critical
  section at a time" contract, `withLock { }` ≈ `mu.Lock(); defer mu.Unlock()`. Go's atomics
  (`sync/atomic`) map directly to `AtomicInteger`/`AtomicLong` too - same CAS-based mechanism,
  same "single value only" boundary. And section 6's actor pattern is exactly Go's own proverb
  put into practice: "don't communicate by sharing memory; share memory by communicating" - a
  channel-owned piece of state instead of a mutex-guarded one.
