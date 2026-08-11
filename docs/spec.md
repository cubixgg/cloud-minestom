# cloud-minestom — Specification

Status: draft
Companion docs: [`roadmap.md`](./roadmap.md) (build order), [`CLAUDE.md`](../CLAUDE.md) (conventions), [`decisions/`](./decisions) (ADRs)

## 1. Purpose

`cloud-minestom` is a full [Cloud v2](https://cloud.incendo.org) command framework integration for
[Minestom](https://minestom.net). It lets a Minestom server or library declare commands once, with
Cloud's parsers, permissions, suggestions, flags and exception handling, and have them show up as
fully native Minestom commands: correct per-argument tab completion, correct client-side red/green
syntax validation, real permission-gated branches — the same experience a hand-written Minestom
`Command` gives, minus writing the tree by hand.

There is no official Cloud module for Minestom. The existing community ports
([`OpenMinigameServer/cloud-minestom`](https://github.com/OpenMinigameServer/cloud-minestom),
[`MelonHell/CloudMinestom`](https://github.com/MelonHell/CloudMinestom)) target the abandoned Cloud
v1 API and are unmaintained. This project is a clean-room v2 implementation, built the way Cloud
itself expects a new platform to be added: cloud-core is platform-free, and a platform contributes a
`CommandManager` subclass, a `SenderMapper`, a `CommandRegistrationHandler` and an
`ExecutionCoordinator` choice.

### 1.1 Why not the obvious shortcut

The cheapest way to bridge Cloud onto Minestom is one native node per Cloud root command — a literal
plus a single greedy `StringArray` argument that swallows the rest of the line and hands it to Cloud
verbatim. That gets tab completion and error messages "for free" from Cloud's suggestion factory, at
the cost that Minestom's client-side syntax highlighting only ever marks the one greedy argument,
never the individual sub-arguments — a partial implementation of what Minestom's command tree can
actually do.

`cloud-minestom` does not take that shortcut. It mirrors Cloud's whole command tree into Minestom's
native argument tree, node for node, the way
[`cloud-brigadier`](https://github.com/Incendo/cloud-minecraft) mirrors Cloud's tree into Brigadier
for Paper/Velocity/Sponge/Fabric. The library has no assumption about what a "player" or "permission"
looks like beyond what Minestom already provides — any consumer, with any sender/player abstraction of
its own, can adopt it through the `SenderMapper` seam described in [§4](#4-command-manager).

### 1.2 Goals

- Full, general-purpose Cloud v2 platform module for Minestom, usable by any Minestom project.
- Native argument-tree mirroring: every Cloud parser that has a sane Minestom equivalent gets mapped
  to that equivalent's `Argument<T>` type, not flattened into a string.
- Cloud remains the single source of truth for parsing, validation and suggestions at all times (see
  [§5.4](#54-cloud-is-the-only-parser-that-matters), [ADR-0001](./decisions/0001-cloud-is-the-single-source-of-truth-for-parsing.md)).
- Builder-, annotation- (`cloud-annotations`) and Minestom-native argument styles all work, including
  mixed in the same project.
- Ships the extras a real project needs out of the box: permission bridging, exception feedback via
  `cloud-minecraft-extras`, a `PlayerParser`, help menu wiring — not just the bare minimum to compile.
- Unit-testable without a running server, plus real integration tests against an embedded Minestom
  instance (`net.minestom:testing`, `@EnvTest`).
- Documented well enough that someone who has used `cloud-velocity` or `cloud-paper` before can be
  productive in `cloud-minestom` without reading its source.

### 1.3 Non-goals

- Not a fork or continuation of the v1 community ports; no v1 compatibility.
- Not a general Minestom command *library* beyond what's needed to host Cloud (e.g. no new
  permission system — Minestom's own `Permission`/`PermissionHandler` is used as-is).
- Not tied to any specific Minecraft version pin beyond what the Minestom version in use supports.
- Not a proxy-side module. Minestom has no proxy; velocity/bungee users stay on `cloud-velocity`.
- No signed-argument (chat-report) support in v1.0 — Minestom's signed command argument support is
  still evolving; tracked as a future roadmap phase, not blocking the initial release.

## 2. Target audience & compatibility

- **Java:** 21 minimum (Minestom's own floor). Built against the same JDK Minestom itself targets;
  bumped when Minestom bumps.
- **Minestom:** tracks current Minestom releases. No LTS promise — Minestom itself doesn't have one.
  A compatibility table lives in `docs/compatibility.md` and is updated every time the pinned Minestom
  version in `gradle/libs.versions.toml` changes.
- **Cloud:** `org.incendo:cloud-core` 2.x, `org.incendo:cloud-annotations` 2.x,
  `org.incendo:cloud-minecraft-extras` 2.x. Starting pins: `cloud` 2.1.0, `cloud-minecraft-extras`
  2.0.0 — a combination already confirmed to work together against a recent Minestom build, not a
  guess.
- **Distribution:** Maven Central under group `gg.cubix.cloudminestom`, artifact `cloud-minestom`
  (plus `cloud-minestom-bom`). Snapshots to a snapshot repository for every commit on `main`.
- **Dependency management:** every dependency version, across every module, is declared exactly once
  in `gradle/libs.versions.toml` using [Gradle's built-in version
  catalog](https://docs.gradle.org/current/userguide/platforms.html) — no module's `build.gradle.kts`
  pins a version inline. This is not a style preference: it's what keeps the compatibility table above,
  `cloud-minestom-bom` ([§3.2](#32-cloud-minestom-bom)), and the ADR-tracked version pins from
  quietly drifting out of sync with what's actually built.

## 3. Module architecture

```
cloud-minestom/                  (root Gradle build — no sources of its own)
├── cloud-minestom/               published: the library
├── cloud-minestom-bom/           published: BOM pinning cloud-minestom + the cloud-* versions it needs
└── minestom-demo/                not published: runnable example server
```

Three modules, not more. `cloud-annotations` and `cloud-minecraft-extras` are already
platform-agnostic upstream (the latter works directly against Minestom's `CommandSender` because it
already implements Adventure's `Audience` — no bridging module needed), so there is no
`cloud-minestom-annotations` or `cloud-minestom-extras` split: that would be an abstraction with
nothing on the other side of it. If a real second axis of variation shows up later (e.g. a
Brigadier-literal-sharing concern the way Paper/Velocity/Sponge share `cloud-brigadier`), split then,
not now.

### 3.1 `cloud-minestom`

The library. Depends on `cloud-core`, `minestom`, and exposes `cloud-minecraft-extras` and
`cloud-annotations` as `api` (implementation) / documented optional (annotations) dependencies — see
[§10](#10-annotation-commands).

Package root: `gg.cubix.cloudminestom`.

| Package | Contents |
|---|---|
| `gg.cubix.cloudminestom` | `MinestomCommandManager`, `MinestomCommandManager.Builder` |
| `gg.cubix.cloudminestom.registration` | `CommandRegistrationHandler` impl, tree translator |
| `gg.cubix.cloudminestom.argument` | `ArgumentMapper<T>` SPI + built-in mappings registry |
| `gg.cubix.cloudminestom.suggestion` | suggestion bridge shared by every mapped node |
| `gg.cubix.cloudminestom.parser` | Minestom-specific Cloud parsers (`PlayerParser`, ...) |
| `gg.cubix.cloudminestom.exception` | default `cloud-minecraft-extras` exception wiring |

### 3.2 `cloud-minestom-bom`

Standard `java-platform` BOM, mirrors the convention `cloud-minecraft-bom` already sets upstream.
Lets a consumer write one version property and pull `cloud-minestom` plus compatible `cloud-core` /
`cloud-annotations` / `cloud-minecraft-extras` versions without pinning each by hand.

### 3.3 `minestom-demo`

A runnable Minestom server (`application` plugin, `main` starts `MinecraftServer` on a flat/void
instance) that exists to *prove* the library end to end, not just to compile against it. It is the
acceptance test for "full implementation, not partial": if something can't be shown working here, it
isn't done. Required content — see [§13](#13-minestom-demo) for the full command list.

## 4. Command manager

```java
public final class MinestomCommandManager<C> extends CommandManager<C> {
    public static Builder<CommandSender> builder();
    public static <C> Builder<C> builder(SenderMapper<CommandSender, C> senderMapper);
    public static MinestomCommandManager<CommandSender> create(); // sender type = Minestom's own CommandSender, zero config

    public SenderMapper<CommandSender, C> senderMapper();
}
```

- **Generic over the sender type `C`**, exactly like `cloud-velocity`/`cloud-paper`. The zero-config
  `create()` fixes `C = net.minestom.server.command.CommandSender` for projects happy to use
  Minestom's own sender type directly. Projects with their own player wrapper provide a
  `SenderMapper<CommandSender, C>` through the builder instead — the library never assumes what a
  "player" is beyond the Minestom `CommandSender` it maps *from*.
- **Builder options:** `senderMapper`, `executionCoordinator` (default
  `ExecutionCoordinator.simpleCoordinator()`, see [§4.1](#41-threading)), `permissionFunction`
  (default described in [§6](#6-permissions)), `argumentMapperRegistry` (default = the built-in
  registry from [§5](#5-command-tree-translation), extendable/replaceable),
  `commandRegistrationCallback` (how built `net.minestom.server.command.builder.Command`s reach the
  server — default `MinecraftServer.getCommandManager()::register`, overridable for tests via a plain
  `Consumer<net.minestom.server.command.builder.Command>` sink, so registration logic is testable
  without a running server, see [§11](#11-testing-strategy)).
- `CommandManager#hasPermission` delegates to the configured permission function.

### 4.1 Threading

Minestom dispatches a registered command's syntax executor on the thread that received the packet —
in practice the owning instance's tick thread for players already in the world. Command handlers are
therefore expected to run **on the calling thread by default**: `ExecutionCoordinator.simpleCoordinator()`
is the default so that nothing is silently rescheduled out from under a handler that touches
Minestom's (thread-confined) game state. This is a correctness requirement, not a performance
default, and it must not be "optimized" to an async coordinator without the caller opting in.

Projects that want async parsing/execution can still supply
`ExecutionCoordinator.builder()...build()` through the manager builder; if they do, any handler that
touches instance/entity state must hop back onto the owning thread itself
(`instance.scheduler()`/`MinecraftServer.getSchedulerManager()`), the same way it would have to if it
received a Minestom command callback directly. Document this tradeoff in `docs/threading.md`, do not
paper over it.

## 5. Command tree translation

This is the part that makes this a full implementation rather than the greedy-string bridge described
in [§1.1](#11-why-not-the-obvious-shortcut). Every `Command<C>` Cloud registers is
walked component by component and mirrored into a tree of native Minestom `Argument<?>` nodes before
it is handed to `net.minestom.server.command.builder.Command#addSyntax`.

### 5.1 Node mapping

| Cloud component | Minestom node |
|---|---|
| Root/inner literal, incl. aliases | `ArgumentType.Literal(name)`, aliases via Minestom's literal alternative support |
| Required/optional variable, parser has a built-in mapping | the mapped `Argument<T>` (see [§5.2](#52-built-in-argument-mappings)), wrapped `.setOptional()` when the Cloud component is optional |
| Required/optional variable, no built-in mapping | fallback `Argument<String>` (`Word` if the parser consumes one token, `String`/`StringArray` if it can contain spaces or is the last component), suggestions from Cloud (see [§5.3](#53-suggestions)) |
| Flags (`--name value`, presence flags) | fallback trailing `ArgumentStringArray`, entire flag subtree handed to Cloud verbatim from that point on (see [§5.5](#55-known-limitation-flags)) |

Building the tree is the job of `gg.cubix.cloudminestom.registration.CommandTreeTranslator`, driven
off `CommandManager#commandTree()`. It is a pure function of a Cloud `CommandNode<C>` to a Minestom
argument graph plus a list of `(Argument<?>[] syntax, executor)` pairs — no Minestom server
interaction — so it is unit-testable on its own (see [§11](#11-testing-strategy)).

### 5.2 Built-in argument mappings

`gg.cubix.cloudminestom.argument.ArgumentMapper<T>` is the extension point:

```java
public interface ArgumentMapper<T> {
    Argument<?> map(CommandComponent<?> component, ParserDescriptor<?, T> parser);
}
```

Registered per Cloud parser type in `ArgumentMapperRegistry`, keyed the same way Cloud's own
`ParserRegistry` keys parsers (by `TypeToken`/parser class), mirroring how `CloudBrigadierManager`
lets you register Cloud-parser → Brigadier-`ArgumentType` mappings. Ships with mappings for:

| Cloud parser | Minestom `Argument` |
|---|---|
| `StringParser` (single/greedy/quoted modes) | `Word` / `StringArray` / `String` respectively |
| `BooleanParser` | `Boolean` |
| `IntegerParser` (incl. min/max) | `Integer`, ranged via the argument's own bounds when set |
| `LongParser` | `Long` |
| `FloatParser` | `Float`, ranged |
| `DoubleParser` | `Double`, ranged |
| `UUIDParser` | `UUID` |
| `EnumParser<E>` | `Word` restricted via suggestions (Minestom has no generic native enum node; see below) |
| `DurationParser` | `Time` (best effort — Cloud's duration grammar and Minestom's tick-based `Time` argument don't fully agree; documented mismatch, not silently coerced) |

Every mapped node still gets its suggestions from Cloud (`setSuggestionCallback`, [§5.3](#53-suggestions)),
**not** from Minestom's native validation, even when a native type exists — this is deliberate, see
[§5.4](#54-cloud-is-the-only-parser-that-matters). The native type is there for shape and client-side
coloring, never as a second, competing source of truth. This mirrors `cloud-brigadier`'s own
documented default (native *shape*, Cloud *suggestions*, opt-in native suggestions per parser) rather
than inventing a new policy.

Consumers can register their own `ArgumentMapper` for a custom parser type through the manager
builder, exactly as they would register a Cloud-parser → Brigadier mapping on `CloudBrigadierManager`.

### 5.3 Suggestions

One suggestion bridge (`gg.cubix.cloudminestom.suggestion.CloudSuggestionCallback`), attached to every
mapped node and to the fallback node alike. It reconstructs the full input line up to and including
the node's own position, strips Minestom's trailing-space placeholder character, and calls
`manager.suggestionFactory().suggestImmediately(mappedSender, input)`. `suggestImmediately` (not the
async variant) is used deliberately: Minestom calls the suggestion callback while the client is
blocked waiting for the completion packet, so suggestion providers must be non-blocking pure lookups;
anything needing IO has to pre-compute (documented in `docs/threading.md` alongside [§4.1](#41-threading)).

### 5.4 Cloud is the only parser that matters

**Every syntax executor, mapped node or fallback alike, re-joins the raw command line and re-dispatches
it to `manager.commandExecutor().executeCommand(mappedSender, line)`.** Minestom's own parse of the
typed arguments is never used to build the `CommandContext` handed to the Cloud handler. This is the
single most important design decision in this library and has its own ADR
([`docs/decisions/0001-cloud-is-the-single-source-of-truth-for-parsing.md`](./decisions/0001-cloud-is-the-single-source-of-truth-for-parsing.md)):
mirroring the tree buys client-side shape and completion, but if Minestom's parse result were also
fed into the handler, two parsers could disagree (a value Minestom's native `Integer` argument accepts
but Cloud's ranged `IntegerParser` rejects, permission-gated branches, context-dependent Cloud
suggestion providers) and the bug would show up as "works sometimes." Re-parsing the whole line
through Cloud on every execution is cheap (it's a command, not a hot loop) and guarantees there is
never a second opinion about what a command means.

### 5.5 Known limitation: flags

Cloud flags (`--name value`, presence flags, `-abc` aliasing) have no Minestom-native equivalent node
type, so a command subtree starting at a flag falls back to a single trailing greedy argument, the
same as the rest of the fallback path — client-side coloring degrades to "it's a string" for the
flagged tail. This is called out explicitly in `docs/limitations.md`; it is not something later phases
are expected to fix (Minestom itself would need a flag-like node type first) — it is not a partial
implementation of the described feature.

## 6. Permissions

`MinestomCommandManager#hasPermission(C sender, String permission)` delegates to a
`BiPredicate<C, String>` supplied through the builder. Default:

- Empty permission string → always allowed (Cloud's own convention).
- Sender maps (via the reverse direction of `SenderMapper`) to a Minestom `CommandSender` that is a
  `Player` → `player.hasPermission(new Permission(permission))` (Minestom's own permission node
  check, wildcard matching included, since Minestom resolves `admin.*`-style nodes itself).
  Overrides go through the sender's own `PermissionHandler`.
- Sender maps to any other `CommandSender` (console, command blocks, function context) → always
  allowed, matching vanilla/Bukkit/Velocity convention that non-player senders bypass permission
  checks.

This default is a `BiPredicate` so it composes with `SenderMapper` regardless of what `C` is —
projects that resolve permissions externally (an auth service, a proxy-sent cache, a LuckPerms
integration) replace it wholesale through the builder; the library never talks to an external
permissions system itself.

## 7. Exception handling & feedback

`cloud-minecraft-extras`' `MinecraftExceptionHandler` is wired by default for the standard Cloud
exceptions (`NoPermissionException`, `InvalidSyntaxException`, `ArgumentParseException`,
`NoSuchCommandException`, `InvalidCommandSenderException`) because `CommandSender` already implements
Adventure's `Audience` — no adapter needed, `MinecraftExceptionHandler` sends straight to the sender.
Default component styling matches `cloud-minecraft-extras`' own defaults (red error text,
MiniMessage-based). Fully overridable via `MinestomCommandManager.Builder#exceptionHandler(...)`,
which is a thin convenience over Cloud's own `exceptionController()` — no new exception-handling
concept is invented here.

## 8. Help

`cloud-minecraft-extras`' `MinecraftHelp<C>` is usable as-is (it only needs an `Audience`-producing
sender and the manager). `minestom-demo` wires a `/demo help` command through it as the reference
example; the library itself does not auto-register a help command for any project using it (opinions
like "what is your help command called" belong to the consumer, not the library).

## 9. Custom parsers shipped with the library

`gg.cubix.cloudminestom.parser`:

- **`PlayerParser<C>`** — parses/suggests currently-online players by name (and `@s`/`@p`/`@a`/...
  selectors via Minestom's own `ArgumentEntity` under the hood where the sender context allows it),
  the Minestom equivalent of `cloud-bukkit`'s `PlayerParser`. Ships because "target a player" is the
  single most common real-world command argument and every consumer would otherwise reimplement it.
- Additional parsers (world/instance, item, block) are roadmap items, not v1.0 blockers — see
  `roadmap.md`. The library must not ship a parser it cannot suggest correctly; a stub that "sort of"
  parses items is worse than no item parser.

## 10. Annotation commands

`cloud-annotations` is platform-agnostic upstream — `AnnotationParser<C>` just needs the
`MinestomCommandManager<C>` instance and, for injected values, the sender type. No dedicated
`cloud-minestom-annotations` module exists ([§3](#3-module-architecture)); `docs/annotations.md`
documents the setup:

```java
AnnotationParser<CommandSender> annotations =
    new AnnotationParser<>(manager, CommandSender.class);
annotations.parse(new MyCommands());
```

`minestom-demo` must contain at least one annotation-declared command alongside the builder-declared
ones (input.md requirement — "commands inkl. Annotation Commands"), exercising `@Command`,
`@Argument`, `@Permission`, `@Default`/`@Suggestions`, and injection of the mapped sender type, so the
demo is proof both styles work against the same manager, including mixed in one class hierarchy.

## 11. Testing strategy

Two layers, both required — "full implementation" includes both, not just whichever is convenient:

1. **Unit tests, no running server.** `CommandTreeTranslator` is a pure function
   ([§5.1](#51-node-mapping)); it is tested by building Cloud commands against a manager constructed
   with a fake registration sink (`Consumer<net.minestom.server.command.builder.Command>` instead of
   `MinecraftServer.getCommandManager()::register`) and asserting on the resulting `Argument` tree's
   shape/types, and on suggestion callback behaviour (placeholder stripping, partial-line suggestion
   correctness) with hand-built `CommandContext`/`Suggestion` objects.
2. **Integration tests against a real embedded server.** `net.minestom:testing`'s `@EnvTest`/`Env`
   boots an actual (headless) Minestom server per test; a virtual player is connected, a real command
   line is sent through the real packet path, and the resulting feedback message / world side effect
   is asserted. This is what proves the whole stack — registration, native parsing shape, fallback to
   Cloud, permission check, handler execution, exception feedback — actually works end to end, not
   just that the pieces compile against each other.

No test relies on a real network connection or wall-clock sleeps; `@EnvTest` gives a synchronous tick
loop.

## 12. Documentation

- `docs/` — user-facing documentation: getting started, command-manager setup, argument mapping
  table, permissions, annotations, threading, help/exceptions, limitations, compatibility table.
  Written for someone who knows Cloud from another platform and needs the Minestom-specific 20%, not
  a re-explanation of Cloud itself (that's cloud.incendo.org's job).
- `docs/decisions/` — ADRs for load-bearing, non-obvious decisions:
  - `0001-cloud-is-the-single-source-of-truth-for-parsing.md` ([§5.4](#54-cloud-is-the-only-parser-that-matters))
  - `0002-simple-execution-coordinator-by-default.md` ([§4.1](#41-threading))
  - `0003-no-separate-annotations-or-extras-module.md` ([§3](#3-module-architecture))
  - [`0004-gradle-version-catalog-for-dependency-management.md`](./decisions/0004-gradle-version-catalog-for-dependency-management.md)
    (the "Dependency management" bullet in [§2](#2-target-audience--compatibility))
  - [`0005-ai-assisted-development-with-claude-code.md`](./decisions/0005-ai-assisted-development-with-claude-code.md)
    (project-process decision, not architectural, but load-bearing enough to record the same way)
  - Further ADRs as later decisions warrant one — not every roadmap item needs one, only decisions a
    future contributor could plausibly second-guess without the context.
- Every public class/method gets Javadoc; non-obvious *why* (not *what*) gets a comment in code —
  a hidden constraint, an invariant, a spec reference — never a restatement of what the code already
  says.

## 13. `minestom-demo`

A runnable server, not a snippet collection. Minimum command set to prove the spec:

- A builder-declared command with a literal subcommand tree, a required mapped argument (e.g.
  `IntegerParser` with bounds), an optional argument, and Cloud-suggested string choices — to show
  native node shape + Cloud suggestions together.
- The same shape again declared via `@Command` annotations, to show parity between styles
  ([§10](#10-annotation-commands)).
- A command using `PlayerParser` ([§9](#9-custom-parsers-shipped-with-the-library)).
- A command using a flag, to show and document the fallback behaviour honestly
  ([§5.5](#55-known-limitation-flags)) rather than hiding it.
- A command that intentionally throws to demonstrate default exception feedback
  ([§7](#7-exception-handling-feedback)).
- A `/demo help` command wired through `MinecraftHelp` ([§8](#8-help)).
- A permission-gated command demonstrating the default permission function against a `Player` sender
  ([§6](#6-permissions)).

## 14. Claude Code tooling (skills & agents)

input.md asks for project-specific Skills and Agents. These live in `.claude/skills/` and
`.claude/agents/` at the repo root and are specified in detail in `roadmap.md` once the core library
exists to build tooling around (a skill for scaffolding a new `ArgumentMapper`, an agent/skill for
verifying a roadmap item's command actually round-trips through both the unit and `@EnvTest` layers
before it's checked off, etc.). Not designed further here because designing them before the library's
real shape exists would mean designing against a guess.

## 15. Open questions

None blocking `roadmap.md` — the design above is implementable end to end. Items to revisit once
implementation surfaces real friction, tracked here rather than guessed at:

- Whether `EnumParser` should get a real Minestom-native mapping once/if Minestom ships a generic
  restricted-choice argument type, instead of `Word` + suggestions.
- Whether signed-argument support becomes necessary once Minestom's own support for it stabilizes.
- Whether a second sender-mapper convenience (beyond the identity `create()`) is worth shipping once
  real consumers building on this library show a common pattern.
