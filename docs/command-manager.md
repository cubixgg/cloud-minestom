# The command manager

`MinestomCommandManager<C>` (`gg.cubix.cloudminestom`) is Cloud's `CommandManager<C>` subclass for
Minestom — the object you build once and register every command against (spec.md §4).

## Constructing one

```java
// C = net.minestom.server.command.CommandSender, every builder default
MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
```

```java
// C = your own sender type
MinestomCommandManager<MyPlayer> manager = MinestomCommandManager.builder(
        SenderMapper.create(commandSender -> wrap(commandSender), myPlayer -> myPlayer.commandSender())
).build();
```

```java
// C = CommandSender, but with builder options set
MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
        .permissionFunction((sender, permission) -> hasNode(sender, permission))
        .build();
```

`create()` and the no-arg `builder()` both fix `C` to Minestom's own `CommandSender` via
`SenderMapper.identity()`; `builder(SenderMapper<CommandSender, C>)` is the one to reach for when a
project already has its own player/sender abstraction. `senderMapper()` is the only thing the
constructor *requires* — every other option below has a sane default.

## Builder options

| Option | Default | Purpose |
|---|---|---|
| `senderMapper` | required (identity when using `create()`/`builder()`) | maps Minestom's `CommandSender` to/from `C` |
| `executionCoordinator` | `ExecutionCoordinator.simpleCoordinator()` | which thread handlers run on — see [`threading.md`](./threading.md); **do not** change this to chase performance, see spec.md §4.1 and [ADR-0002](./decisions/0002-simple-execution-coordinator-by-default.md) |
| `commandRegistrationCallback` | `MinecraftServer.getCommandManager()::register` | how a built native `Command` reaches the server; override with a plain sink (e.g. `list::add`) to make registration testable without a running server |
| `argumentMapperRegistry` / `argumentMapper(Class, ArgumentMapper)` | `ArgumentMapperRegistry.createDefault()` | which Cloud parsers get which native `Argument` shape — see [`argument-mapping.md`](./argument-mapping.md) |
| `permissionFunction` | always allowed | backs `hasPermission(C, String)` — see [`permissions.md`](./permissions.md) |
| `exceptionHandler` | registers `MinecraftExceptionHandler`'s defaults + a `NoSuchCommandException` handler | see [`help-and-exceptions.md`](./help-and-exceptions.md) |

Every option is independent — set only the ones you need. `commandRegistrationCallback` is deferred
(a lambda, not a resolved reference) specifically so that building a manager before
`MinecraftServer.init()` has run, or with a fake sink in a test, doesn't force early initialization of
the real server's command manager.

## What happens when you call `manager.command(...)`

Cloud's own `commandTree()` gets the new command added to it as normal, then
`MinestomCommandRegistrationHandler` (`gg.cubix.cloudminestom.registration`) walks the whole tree
under that root through `CommandTreeTranslator` and hands the resulting native `Command` to
`commandRegistrationCallback` — one native `Command` per Cloud root literal, no matter how many
`manager.command(...)` calls share that root (see [`argument-mapping.md`](./argument-mapping.md) for
what the resulting tree looks like). This happens synchronously and immediately; there's no separate
"finalize" step to remember.

## Accessors

- `manager.senderMapper()` — the configured `SenderMapper<CommandSender, C>`, also usable directly
  when you need to go from `C` back to a real `CommandSender`/`Audience` outside a command handler
  (e.g. wiring `MinecraftHelp`, see [`help-and-exceptions.md`](./help-and-exceptions.md)).
- Everything `CommandManager<C>` itself exposes (`commandTree()`, `parserRegistry()`,
  `suggestionFactory()`, `exceptionController()`, `commandExecutor()`, ...) is inherited as normal —
  `MinestomCommandManager` doesn't hide or wrap Cloud's own API surface.
