# Annotation commands

`cloud-annotations` is platform-agnostic upstream (spec.md §10) — there's no
`cloud-minestom-annotations` module and nothing Minestom-specific to learn beyond what's already true
of any other Cloud platform. `AnnotationParser<C>` just needs your `MinestomCommandManager<C>` and the
sender type:

```java
MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();

AnnotationParser<CommandSender> annotations =
        new AnnotationParser<>(manager, CommandSender.class);
annotations.parse(new MyCommands());
```

`annotations.parse(...)` scans the given instance(s) for `@Command`-annotated methods and registers
each one against `manager` directly — no separate `manager.command(...)` call needed, and the result
is a normal Cloud command indistinguishable from a builder-declared one: same native argument-tree
mirroring (spec.md §5), same permission function (spec.md §6), same default exception feedback
(spec.md §7).

```java
public final class MyCommands {

    @Command("greet <target>")
    @Permission("my.plugin.greet")
    public void greet(
            final CommandSender sender,
            final @Argument("target") Player target
    ) {
        target.sendMessage(Component.text("Hello from " + sender));
    }

    @Suggestions("greeting")
    public List<String> greetingSuggestions(final CommandContext<CommandSender> context) {
        return List.of("hi", "hello", "hey");
    }
}
```

A few things carry over from the rest of this library without any extra glue:

- An `@Argument Player` parameter resolves through `PlayerParser` (spec.md §9) exactly like a
  builder-declared `.required("target", PlayerParser.playerParser(...))` would, since `PlayerParser`
  is registered as `Player`'s default parser on every `MinestomCommandManager` (see
  `docs/argument-mapping.md`, once P12 lands it).
- `@Permission` is enforced through the same `permissionFunction` configured on the manager builder
  (spec.md §6) — there's no separate annotation-driven permission system to configure.
- `@Suggestions`-provided suggestion methods surface through the same `CloudSuggestionCallback` bridge
  every other mapped argument uses (spec.md §5.3); nothing about the suggestion path changes because
  the command was annotation-declared.

Builder-declared and annotation-declared commands can be freely mixed against the same manager
instance, including within the same class hierarchy — there's no "pick one style" restriction.
