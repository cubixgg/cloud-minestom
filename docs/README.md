# Documentation index

User-facing docs — how to use `cloud-minestom`, not how it's built internally. Written for someone
who already knows Cloud from another platform (`cloud-velocity`, `cloud-paper`, ...) and needs the
Minestom-specific delta.

- [`getting-started.md`](./getting-started.md) — install, your first command, where to go next
- [`command-manager.md`](./command-manager.md) — every `MinestomCommandManager.Builder` option, sender
  mapping
- [`argument-mapping.md`](./argument-mapping.md) — the built-in Cloud-parser → native-`Argument` table,
  the fallback rule, registering a custom `ArgumentMapper`
- [`permissions.md`](./permissions.md) — the permission function, and why the default never denies
- [`annotations.md`](./annotations.md) — `@Command`-annotated commands, optional arguments, custom
  named parsers
- [`threading.md`](./threading.md) — what thread handlers and suggestion providers run on, and what
  opting into async execution commits you to
- [`help-and-exceptions.md`](./help-and-exceptions.md) — `MinecraftHelp` wiring, default exception
  feedback, overriding it
- [`limitations.md`](./limitations.md) — deliberate, documented boundaries (flags, an
  argument/literal-naming trap, the `DurationParser`/`Time` mismatch)
- [`compatibility.md`](./compatibility.md) — Java/Minestom/Cloud version table, kept in sync with
  `gradle/libs.versions.toml`

## Architecture & process

Not user-facing docs, but the material behind them:

- [`spec.md`](./spec.md) — the full architecture and every design decision, with rationale
- [`roadmap.md`](./roadmap.md) — implementation checklist, phase by phase, the authoritative record of
  what's built and what corrections were made along the way
- [`decisions/`](./decisions) — ADRs for load-bearing, non-obvious decisions a future contributor
  could plausibly second-guess without the context
- [`../CLAUDE.md`](../CLAUDE.md) — code style and non-negotiable architectural principles
- [`../CONTRIBUTING.md`](../CONTRIBUTING.md) — workflow, commit/branch/PR conventions

## Reference example

`minestom-demo` (see its own [`README.md`](../minestom-demo/README.md)) is a runnable server
exercising every feature documented above — the acceptance test for "full implementation, not
partial."
