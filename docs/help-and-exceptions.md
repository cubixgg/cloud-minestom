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

### What's covered by default

`cloud-minecraft-extras`' `MinecraftExceptionHandler` is wired straight to the sender — no bridging
adapter, since `CommandSender` already implements Adventure's `Audience` (reached via the reverse
direction of `senderMapper()` for any sender type `C`). Its own `defaultHandlers()` covers:

- `InvalidSyntaxException` — wrong shape (missing/extra arguments)
- `InvalidCommandSenderException` — command restricted to a sender type the caller isn't
- `NoPermissionException` — see [`permissions.md`](./permissions.md)
- `ArgumentParseException` — a specific argument's parser rejected the input (bounds, format, ...)
- `CommandExecutionException` — an uncaught exception thrown *inside* a command handler

Plus one manually registered on top: `NoSuchCommandException` (unknown command). `MinecraftExceptionHandler`
has no built-in handler for that one at all — cloud-core's own `CommandManager` has a separate,
plain-caption (non-Adventure) fallback for it, but `MinestomCommandManager` doesn't use that path (it
would mean two different exception-handling styles running side by side for one manager). Instead, a
`NoSuchCommandException` handler is registered manually, styled the same red Adventure-`Component` way
as everything else, using Cloud's own `StandardCaptionKeys.EXCEPTION_NO_SUCH_COMMAND` caption
(`"Unknown command."` by default — note this one, unlike e.g. invalid-syntax's `<syntax>` placeholder,
doesn't interpolate the supplied command name into the message).

Every one of these fires exactly as it would on any other Cloud platform — the native argument-tree
mirroring (spec.md §5) never changes what gets validated or when; it only changes what the client's
own syntax highlighting shows before Cloud gets a chance to reject something.

### Overriding

```java
MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
        .exceptionHandler(m -> m.exceptionController()
                .registerHandler(MyCustomException.class, ctx -> {
                    ctx.context().sender().sendMessage(Component.text("Custom: " + ctx.exception().getMessage()));
                }))
        .build();
```

The replacement callback receives the manager itself, so it can register on
`manager.exceptionController()` directly (Cloud's own extension point) or call
`MinecraftExceptionHandler.create(...)` again with different styling — whatever fits. Nothing about
exception handling is `cloud-minestom`-specific past wiring the sensible defaults above; every
technique documented for `MinecraftExceptionHandler`/`ExceptionController` on any other Cloud platform
applies unchanged here.
