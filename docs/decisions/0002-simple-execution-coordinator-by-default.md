# 0002: `simpleCoordinator()` as the default execution coordinator

Status: Accepted

## Context

Minestom dispatches a registered command's syntax executor on the thread that received the packet —
for a player already in the world, that's the owning instance's tick thread. Minestom's own game
state (entities, instances, inventories, ...) is thread-confined: touching it from the wrong thread is
a correctness bug, not just a performance concern, the same way it would be for a hand-written native
Minestom command that hopped threads without care.

Cloud's `ExecutionCoordinator` is pluggable specifically so a platform can choose how much
parsing/execution gets scheduled asynchronously. A naive choice — defaulting to an async coordinator
"for responsiveness," the way a web-request-shaped platform reasonably might — would silently
reschedule every command handler off the thread Minestom handed it to, breaking any handler that
touches thread-confined state without every caller realizing they need to hop back manually.

## Decision

`MinestomCommandManager`'s default `executionCoordinator` is `ExecutionCoordinator.simpleCoordinator()`
— same-thread execution, matching the thread Minestom itself dispatches the syntax executor on. This
is a correctness requirement, not a performance default, and must not be "optimized" to an async
coordinator without the caller explicitly opting in through the builder (spec.md §4.1).

A project that wants async parsing/execution can still supply its own coordinator via
`MinestomCommandManager.Builder#executionCoordinator(...)`; doing so is an explicit, visible choice
that comes with an explicit responsibility documented in [`threading.md`](../threading.md): any
handler touching instance/entity state must hop back onto the owning thread itself, exactly as it
would have to for a raw Minestom command callback.

## Consequences

- Every command handler written against `cloud-minestom` behaves the way a hand-written native
  Minestom command would with respect to threading — no silent surprise reschedule to learn about
  after a `NullPointerException` or thread-safety bug in production.
- Suggestion providers inherit the same same-thread expectation independently (spec.md §5.3,
  [`threading.md`](../threading.md)) — `CloudSuggestionCallback` uses
  `suggestionFactory().suggestImmediately(...)`, not the async variant, for the same reason.
- A project that genuinely wants async execution has to opt in explicitly and accept the
  thread-hop-back responsibility that comes with it — a small amount of friction traded for making the
  default safe for everyone who doesn't need async.

## References

- [`spec.md`](../spec.md) §4.1 (Threading)
- [`threading.md`](../threading.md)
- `CLAUDE.md`, "`ExecutionCoordinator.simpleCoordinator()` is the default and stays the default"
