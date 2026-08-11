# Threading

## Command handlers

Minestom dispatches a registered command's syntax executor on the thread that received the packet —
in practice, for a player already in the world, that's the owning instance's tick thread. Command
handlers therefore run **on the calling thread by default**: `ExecutionCoordinator.simpleCoordinator()`
is `MinestomCommandManager`'s default execution coordinator (spec.md §4.1,
[ADR-0002](./decisions/0002-simple-execution-coordinator-by-default.md)) precisely so that nothing
gets silently rescheduled out from under a handler touching Minestom's thread-confined game state
(entities, instances, inventories, ...).

This is a correctness requirement, not a performance default — see ADR-0002 for why it must not be
"optimized" to an async coordinator without the caller explicitly opting in.

### Opting into async execution

A project that wants async parsing/execution can still supply its own coordinator:

```java
MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
        .executionCoordinator(ExecutionCoordinator.<CommandSender>builder()
                .executor(myExecutorService)
                .build())
        .build();
```

If you do this, any handler that touches instance/entity state must hop back onto the owning thread
itself — `instance.scheduler()` or `MinecraftServer.getSchedulerManager()` — exactly the same way it
would have to if it received a raw Minestom command callback directly instead of going through Cloud.
`cloud-minestom` doesn't paper over that tradeoff; picking an async coordinator means picking up that
responsibility.

## Suggestion providers

`CloudSuggestionCallback` (`gg.cubix.cloudminestom.suggestion`) calls
`manager.suggestionFactory().suggestImmediately(...)` — the **synchronous**, not the async, suggestion
API — deliberately (spec.md §5.3). Minestom calls a suggestion callback while the client connection is
blocked waiting for the completion packet, so every suggestion provider registered against a
`MinestomCommandManager` must be a non-blocking, synchronous, pure lookup:

- Fine: filtering an in-memory `List<String>`, checking an already-populated cache, reading fields off
  the sender/context.
- Not fine: a database query, a network call, anything that would need to await a future — this will
  stall the connection thread for every keystroke the client sends while tab-completing.

If a suggestion source genuinely needs IO, pre-compute and cache the result somewhere a synchronous
lookup can read from (a periodically-refreshed in-memory snapshot, for example) rather than doing the
IO inside the suggestion provider itself.

## Registration

`manager.command(...)` and everything it triggers — `CommandTreeTranslator`, building the native
`Command`, handing it to `commandRegistrationCallback` — run synchronously on whatever thread calls
`manager.command(...)`. There's no background registration queue to be aware of.
