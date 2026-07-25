# kotlin-learning
Learning kotlin, one step at a time

## kotlin-basics 

This module contains basics of Kotlin including types, loops, if/else, functions, classes etc 

## kotlin-concurrency 

This module contains concurrency paradigms of Kotlin. It follows this detailed learning path:
https://docs.google.com/document/d/17mvgnDj-Ha3gUoLNDJ6QmNK7N7Or4fBG9yMJSQjnSVQ/edit?tab=t.0

## kotlin-order-intake

Null safety (!! and its dangers, safe cast `as?`) + value classes (fixing primitive obsession),
via a small order-intake validator that parses loosely-typed input into a typed, validated domain
model or a list of typed errors. Skeleton with TODOs in `src/main/kotlin/Validation.kt` - run with
`./gradlew run` once implemented.

## kotlin-health-checker

Coroutines-fundamentals capstone: a concurrent endpoint health checker. Covers suspend functions,
launch/async, structured concurrency (coroutineScope vs supervisorScope), Dispatchers, cancellation,
timeouts (withTimeoutOrNull), and concurrency limiting (Semaphore). Skeleton with TODOs in
`src/main/kotlin/HealthChecker.kt` - run with `./gradlew run` once implemented.

---

**Convention going forward:** every module in this repo is its own standalone Gradle project
(own `build.gradle.kts`/`settings.gradle.kts`), never edited once handed over - each represents
one project from the learning roadmap. See the roadmap doc for the full module list and how
projects build on each other conceptually (without sharing code).
