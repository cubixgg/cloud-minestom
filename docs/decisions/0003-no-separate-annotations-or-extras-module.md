# 0003: No separate `cloud-minestom-annotations` or `cloud-minestom-extras` module

Status: Accepted

## Context

Several Cloud platform integrations ship a companion annotations module (bridging `cloud-annotations`
onto platform-specific injection/sender concerns) and a companion "extras" module (help menus,
exception formatting glue) alongside their core platform module. The pattern exists because those
platforms need real platform-specific glue for `cloud-annotations` or `cloud-*-extras` to work at
all: an adapter from the platform's sender type to whatever `cloud-*-extras` expects, or
platform-specific parameter injection for annotated commands.

`cloud-minestom` doesn't have that problem. `cloud-annotations` is platform-agnostic upstream —
`AnnotationParser<C>` only needs a `CommandManager<C>` instance and the sender type, both of which
`MinestomCommandManager<C>` already provides with zero adaptation (spec.md §10). `cloud-minecraft-extras`
works directly against Minestom's own `CommandSender` for the same reason: `CommandSender` already
implements Adventure's `Audience`, which is all `MinecraftHelp`/`MinecraftExceptionHandler` need
(spec.md §7, §8).

## Decision

No `cloud-minestom-annotations` or `cloud-minestom-extras` module exists. `cloud-annotations` and
`cloud-minecraft-extras` are used directly against `MinestomCommandManager`, documented in
[`annotations.md`](../annotations.md) and [`help-and-exceptions.md`](../help-and-exceptions.md)
respectively, with no bridging code of `cloud-minestom`'s own in between. `cloud-minestom` (the
library module) only ever gets `cloud-annotations` as a `compileOnly` dependency (optional for
consumers who don't use annotation-declared commands) and `cloud-minecraft-extras` as `api` (its
`MinecraftExceptionHandler` is wired by default, spec.md §7).

## Consequences

- Three modules total (`cloud-minestom`, `cloud-minestom-bom`, `minestom-demo`), not five — no module
  exists as an abstraction with nothing on the other side of it.
- If a real second axis of variation shows up later — a Brigadier-literal-sharing concern the way
  Paper/Velocity/Sponge share `cloud-brigadier`, for example — split then, with a concrete second
  consumer driving the split, not speculatively now.
- A consumer wanting annotation-declared commands still needs `cloud-annotations` on their own
  classpath (it's `compileOnly`, not `api`, in `cloud-minestom`) — one extra dependency line, not
  friction beyond that, and documented as such in [`getting-started.md`](../getting-started.md).

## References

- [`spec.md`](../spec.md) §3 (Module architecture), §10 (Annotation commands)
- [`annotations.md`](../annotations.md), [`help-and-exceptions.md`](../help-and-exceptions.md)
- `CLAUDE.md`, "No new module without a second real consumer on the other side of the split"
