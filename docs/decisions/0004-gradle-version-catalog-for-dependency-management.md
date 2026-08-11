# 0004: Gradle version catalog for dependency management

Status: Accepted

## Context

`cloud-minestom` is a multi-module Gradle build (`cloud-minestom`, `cloud-minestom-bom`,
`minestom-demo`) with dependencies shared across every module: Minestom, `cloud-core`,
`cloud-annotations`, `cloud-minecraft-extras`, JUnit, SLF4J. Without a single place for versions, each
module's `build.gradle.kts` would pin its own version strings, and nothing would keep
`cloud-minestom-bom`'s constraints, [`docs/compatibility.md`](../compatibility.md), and what's
actually resolved at build time from quietly drifting apart.

## Decision

Every dependency version, in every module, is declared exactly once in `gradle/libs.versions.toml`
using [Gradle's built-in version catalog](https://docs.gradle.org/current/userguide/platforms.html).
No module's `build.gradle.kts` contains an inline version string. `cloud-minestom-bom`'s constraints
are read from the same catalog entries, not hand-duplicated.

## Consequences

- One place to bump a Minestom or Cloud version — a one-line diff instead of a grep-and-replace across
  every module's build file.
- Type-safe `libs.*` accessors catch a mistyped dependency coordinate at configuration time rather than
  at dependency resolution.
- The BOM and `docs/compatibility.md` can be treated as generated from the catalog rather than manually
  kept in sync — there's exactly one source of truth (spec.md §2).
- Slightly more ceremony to add a dependency used by only one module (it still needs a catalog entry).
  Accepted: the consistency this buys is worth the extra line for the rare single-use dependency.

## References

- [`spec.md`](../spec.md) §2 (Target audience & compatibility)
- [`roadmap.md`](../roadmap.md) P0
