# Help and exceptions

`cloud-minecraft-extras` ships both of these already, platform-agnostic. `cloud-minestom` doesn't
wrap them in anything Minestom-specific beyond what's needed to reach the sender — no bridging
adapter, because Minestom's `CommandSender` already implements Adventure's `Audience`.

## Help

`MinecraftHelp<C>` is usable as-is. `cloud-minestom` does not auto-register a help command for you
— what your help command is called, and whether you have one at all, is a decision that belongs to
your project, not the library (spec.md §8).

```java
MinecraftHelp<CommandSender> help = MinecraftHelp.create(
        "/demo help",
        manager,
        sender -> sender // CommandSender is already an Audience; for a custom C, use
                          // manager.senderMapper()::reverse instead
);

manager.command(manager.commandBuilder("demo")
        .literal("help")
        .optional("query", StringParser.greedyStringParser())
        .handler(context -> help.queryCommands(context.getOrDefault("query", ""), context.sender())));
```

If your manager uses a custom sender type `C` (via `MinestomCommandManager.builder(senderMapper)`
rather than the identity `create()`), pass `manager.senderMapper()::reverse` as the
`AudienceProvider<C>` instead of the identity function above — `SenderMapper`'s reverse direction
is exactly "get back to something Minestom (and therefore Adventure) understands," the same seam
the default exception handler uses (see below).

## Exceptions

The default exception feedback ([spec.md §7](./spec.md#7-exception-handling--feedback)) is wired
automatically when you build a `MinestomCommandManager` — there's nothing to set up. See
`MinestomCommandManager.Builder#exceptionHandler(...)` if you want to replace it; it's a thin
wrapper over `manager.exceptionController()`, not a new concept to learn.
