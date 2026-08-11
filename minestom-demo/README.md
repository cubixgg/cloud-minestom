# minestom-demo

A runnable Minestom server exercising every feature `cloud-minestom` ships (spec.md §13) - not a
snippet collection, a live server you can connect a client to. If a feature can't be shown working
here, it isn't done.

## Running it

```
./gradlew :minestom-demo:run
```

Boots a flat, grass-covered void world on `0.0.0.0:25565`. Connect with a vanilla client pointed at
`localhost:25565` (offline-mode auth). Every command below lives under the shared `/demo` root.

## Commands

| Command | Demonstrates |
|---|---|
| `/demo roll <sides> [modifier]` | Builder-declared literal subcommand tree: a bounded `IntegerParser` argument (2-100), an optional argument, and Cloud-suggested string choices (`normal`/`advantage`/`disadvantage`) - native argument shape and Cloud suggestions together (spec.md §5). |
| `/demo roll-annotated <sides> [modifier]` | The same shape as `/demo roll`, re-declared via `@Command` annotations (`@Argument`, `@Suggestions`, a named `@Parser` for the bound) - parity between builder and annotation styles against the same manager (spec.md §10). |
| `/demo target <player>` | A `PlayerParser`-based "target a player" command: resolves a currently-online player by exact name or selector (`@s`/`@p`/`@a`/...), messaging both sender and target (spec.md §9). |
| `/demo announce <message> [--loud] [--repeat <count>]` | A flagged command: `message` still gets full native argument-tree mirroring, the flag subtree degrades to the documented trailing-greedy fallback (`docs/limitations.md`) - Cloud still parses the flags correctly either way (spec.md §5.5). |
| `/demo boom` | Throws mid-handler to show the default `cloud-minecraft-extras` exception feedback, wired automatically with no builder configuration (spec.md §7). |
| `/demo help [query]` | Wired through `MinecraftHelp`, listing every command above - no bridging adapter needed since `CommandSender` already implements Adventure's `Audience` (spec.md §8). |
| `/demo admin` | A `.permission(...)`-gated command against a default-built manager, honestly demonstrating that the default permission function never denies rather than pretending it's a real gate (spec.md §6). |

See `DemoCommands` (builder-declared commands) and `AnnotatedRollCommand` (the annotation-declared
one) in `src/main/java/gg/cubix/cloudminestom/demo/` - each command is a single, independently
reviewable method, one per roadmap item (`docs/roadmap.md` P11).
