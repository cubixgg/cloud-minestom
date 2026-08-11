# cloud-minestom

[![Build](https://img.shields.io/github/actions/workflow/status/cubixgg/cloud-minestom/build.yml?branch=main)](https://github.com/cubixgg/cloud-minestom/actions)
[![Maven Central](https://img.shields.io/maven-central/v/gg.cubix.cloudminestom/cloud-minestom)](https://central.sonatype.com/artifact/gg.cubix.cloudminestom/cloud-minestom)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)

A full [Cloud v2](https://cloud.incendo.org) command framework integration for
[Minestom](https://minestom.net). Declare commands once with Cloud's parsers, permissions,
suggestions, flags and exception handling, and get fully native Minestom commands back: real
per-argument tab completion, real client-side syntax highlighting, real permission-gated branches —
not a single greedy string that swallows the whole line.

There is no official Cloud module for Minestom; this is a clean-room v2 implementation built the way
Cloud itself expects a new platform to be added. See [`spec.md`](./docs/spec.md) for the full design and
why it's built this way, in particular [§1.1](./docs/spec.md#11-why-not-the-obvious-shortcut) on the
tradeoff this project deliberately does not take.

## Status

Early development. [`roadmap.md`](./docs/roadmap.md) is the authoritative list of what's built and what's
next, broken into small, individually-shippable steps. Nothing here is published yet — the usage
example below is the target API, not something you can `implementation(...)` today.

## Modules

| Module | Published | Purpose |
|---|---|---|
| `cloud-minestom` | yes | the library |
| `cloud-minestom-bom` | yes | version-aligned BOM for `cloud-minestom` + the `cloud-*` versions it needs |
| `minestom-demo` | no | runnable example server exercising every feature the library ships |

## Usage

Once published (see [Status](#status)), add the BOM and the library:

```kotlin
dependencies {
    implementation(platform("gg.cubix.cloudminestom:cloud-minestom-bom:<version>"))
    implementation("gg.cubix.cloudminestom:cloud-minestom")

    // optional, for @Command-annotated commands (see spec.md §10)
    implementation("org.incendo:cloud-annotations")
}
```

```java
MinestomCommandManager<CommandSender> commands = MinestomCommandManager.create();

commands.command(
    commands.commandBuilder("hello")
        .handler(ctx -> ctx.sender().sendMessage("Hello from cloud-minestom!")));
```

`minestom-demo` (once it exists per `roadmap.md` P11) is the reference for everything else: bounded
arguments, optional arguments, flags, permissions, the built-in `PlayerParser`, exception feedback,
help, and the same commands declared again via annotations.

## Documentation

- [`spec.md`](./docs/spec.md) — the full architecture and every design decision, with rationale
- [`roadmap.md`](./docs/roadmap.md) — implementation checklist, phase by phase
- [`CLAUDE.md`](./CLAUDE.md) — code style and non-negotiable architectural principles
- [`docs/`](./docs) — user-facing guides (getting started, argument mapping, permissions, threading, ...)
- [`docs/decisions/`](./docs/decisions) — ADRs for load-bearing decisions
- [`CONTRIBUTING.md`](./CONTRIBUTING.md) — workflow, commit/branch/PR conventions

## Development

Requirements: JDK 25 (see `gradle/libs.versions.toml` for the exact pinned toolchain version). All
dependency versions — Minestom, Cloud, JUnit — are managed centrally through the
[Gradle version catalog](https://docs.gradle.org/current/userguide/platforms.html) at
`gradle/libs.versions.toml`; nothing is version-pinned inline in a module's `build.gradle.kts`.

```bash
./gradlew build          # build + test every module
./gradlew :minestom-demo:run   # run the example server
```

See [`CONTRIBUTING.md`](./CONTRIBUTING.md) for the full workflow (roadmap-driven, one item per
commit/PR) and [`CLAUDE.md`](./CLAUDE.md) for code style and architecture rules.

## Testing

Every capability that touches registration, parsing, suggestions, permissions or execution needs both:

- **Unit tests**, run via `./gradlew test`, against a fake registration sink instead of a real
  server — fast, no Minestom process involved.
- **`@EnvTest` integration tests** ([`net.minestom:testing`](https://minestom.net)), also run via
  `./gradlew test`, which boot a real headless Minestom server with virtual players to prove the whole
  stack (registration → native parse shape → Cloud re-parse → permission check → handler → feedback)
  actually works end to end.

Details and the reasoning behind this split are in `CLAUDE.md`'s Testing section.

## Deployment / publishing

- Every commit on `main` publishes a snapshot (`cloud-minestom` + `cloud-minestom-bom`) to a snapshot
  repository.
- Tagged releases publish to Maven Central.
- The compatibility table in [`docs/compatibility.md`](./docs/compatibility.md) tracks which
  Java/Minestom/Cloud versions a given release was built and tested against.

(Both publishing workflows are `roadmap.md` P14 items and not live yet.)

## Contributing

See [`CONTRIBUTING.md`](./CONTRIBUTING.md). Short version: pick the next unchecked box in
`roadmap.md`, one item per commit/PR, both testing layers required, Conventional Commits.

## A note on how this project was built

This project was built primarily through AI-assisted development ("vibecoding") with
[Claude Code](https://claude.com/claude-code), as a deliberate experiment to see how well an AI coding
agent could carry a real, fully-specified project end to end — not a toy script, a proper multi-module
library with tests, docs and a public API surface.

It turned out to be very productive. Standing up the dependency wiring, the Gradle multi-module
layout, the initial documentation set and the implementation itself would have taken meaningfully
longer to do by hand, and this let that time go into getting the design right instead.

That doesn't mean quality was skipped to get there. Every architectural decision is grounded in the
actual upstream Cloud and Minestom APIs and written down with its reasoning in
[`spec.md`](./docs/spec.md) and [`docs/decisions/`](./docs/decisions), an exact concept was worked out and
reviewed *before* any implementation started, and the same bar applies regardless of how the code got
written: full implementation over partial, both testing layers required, documentation required. See
[`roadmap.md`](./docs/roadmap.md) for the checklist that keeps that honest, item by item.

## License

[MIT](./LICENSE)
