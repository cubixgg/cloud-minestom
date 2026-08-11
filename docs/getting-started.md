# Getting started

This page assumes you already know [Cloud](https://cloud.incendo.org) from another platform
(`cloud-velocity`, `cloud-paper`, ...) and just need the Minestom-specific delta — not a
re-explanation of Cloud itself.

## Install

Not published yet (see the root [`README.md`](../README.md#status) — `roadmap.md` P14 covers
publishing). Once it is, add the BOM and the library:

```kotlin
dependencies {
    implementation(platform("gg.cubix.cloudminestom:cloud-minestom-bom:<version>"))
    implementation("gg.cubix.cloudminestom:cloud-minestom")

    // optional, only needed for @Command-annotated commands - see annotations.md
    implementation("org.incendo:cloud-annotations")
}
```

The BOM pins `cloud-minestom` against exactly the `cloud-core`/`cloud-annotations`/
`cloud-minecraft-extras` versions it was built and tested against — see
[`compatibility.md`](./compatibility.md).

## Your first command

```java
MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();

manager.command(manager.commandBuilder("hello")
        .handler(context -> context.sender().sendMessage(Component.text("Hello!"))));
```

`MinestomCommandManager.create()` is the zero-config entry point: it fixes the command sender type
`C` to Minestom's own `net.minestom.server.command.CommandSender`, uses every other builder default,
and registers commands against the real server (`MinecraftServer.getCommandManager()`) the moment you
call `manager.command(...)`. There's no separate "finalize registration" step — each `command(...)`
call is live immediately, exactly like calling `MinecraftServer.getCommandManager().register(...)`
yourself would be.

That single call already gets you, for free:

- A real native Minestom command (client-side tab completion, no greedy-string swallow-everything
  bridge) — see [`command-manager.md`](./command-manager.md) and [`argument-mapping.md`](./argument-mapping.md).
- Default exception feedback if the handler throws or an argument fails to parse — see
  [`help-and-exceptions.md`](./help-and-exceptions.md).
- A permission function that's honestly always-allowed by default (Minestom has no native
  permission-node system) — see [`permissions.md`](./permissions.md).

## A command with an argument

```java
manager.command(manager.commandBuilder("greet")
        .required("target", StringParser.stringParser())
        .handler(context -> context.sender().sendMessage(
                Component.text("Hello, " + context.<String>get("target") + "!"))));
```

`"target"` becomes a real native Minestom `Word` argument (client-side shape only), while Cloud
remains the only thing that actually parses and validates it — see
[`argument-mapping.md`](./argument-mapping.md) for the full built-in mapping table and
[`spec.md` §5.4](./spec.md#54-cloud-is-the-only-parser-that-matters) for why that split exists.

## Your own sender type

If your project already has its own player/sender abstraction, map onto it instead of using
Minestom's `CommandSender` directly:

```java
MinestomCommandManager<MyPlayer> manager = MinestomCommandManager.builder(
        SenderMapper.create(
                commandSender -> myPlayerLookup.wrap(commandSender),
                myPlayer -> myPlayer.commandSender()
        )
).build();
```

See [`command-manager.md`](./command-manager.md) for every other builder option (execution
coordinator, permission function, argument mappers, exception handling, command registration
callback).

## Where to go next

- [`command-manager.md`](./command-manager.md) — every `MinestomCommandManager.Builder` option
- [`argument-mapping.md`](./argument-mapping.md) — which Cloud parsers get which native shape
- [`permissions.md`](./permissions.md) — the permission function, and why the default never denies
- [`annotations.md`](./annotations.md) — `@Command`-annotated commands
- [`help-and-exceptions.md`](./help-and-exceptions.md) — `MinecraftHelp` and exception feedback
- [`threading.md`](./threading.md) — what thread your handlers and suggestion providers run on
- [`limitations.md`](./limitations.md) — known, deliberate gaps (flags, argument/literal naming)
- `minestom-demo` (see its own [`README.md`](../minestom-demo/README.md)) — every feature above,
  running in a real server you can connect a client to
