# Compatibility

`cloud-minestom` tracks current Minestom releases — there's no LTS promise, because Minestom itself
doesn't make one (spec.md §2). This table reflects the versions pinned in
[`gradle/libs.versions.toml`](../gradle/libs.versions.toml) right now; it is **not** a historical
release matrix.

| Dependency | Pinned version | Notes |
|---|---|---|
| Java | 25 | Minestom's own floor. Bumped only when the pinned Minestom version below requires a newer one. |
| Minestom | `2026.08.07-26.2` | Minestom's tag format is `YYYY.MM.DD-<minecraft-version>` — this build targets Minecraft `26.2`. |
| `org.incendo:cloud-core` | `2.1.0` | |
| `org.incendo:cloud-annotations` | `2.1.0` | Published from the same `Incendo/cloud` monorepo tag as `cloud-core` — the two move together. |
| `org.incendo:cloud-minecraft-extras` | `2.0.0` | Published from the separate `Incendo/cloud-minecraft` monorepo, which currently trails `cloud-core`'s own version (`2.0.0` vs. `2.1.0`). Do not assume these two move in lockstep when bumping either one. |
| Adventure (`net.kyori:adventure-api`) | `5.2.0` | Pinned to whatever the Minestom version above itself ships, not chosen independently. |

## How this stays accurate

Every version above lives in exactly one place — `gradle/libs.versions.toml`
([ADR-0004](./decisions/0004-gradle-version-catalog-for-dependency-management.md)) — so this table,
`cloud-minestom-bom`'s constraints, and what's actually resolved at build time can't quietly drift out
of sync with each other. When the catalog changes:

1. Update the relevant `[versions]` entry in `gradle/libs.versions.toml`.
2. Update this table in the **same commit** — `gradle/libs.versions.toml`'s own comments call out
   "keep docs/compatibility.md in sync on every bump" for exactly this reason.
3. `cloud-minestom-bom`'s constraints (`cloud-minestom-bom/build.gradle.kts`) read from the same
   catalog entries automatically; no separate edit needed there.
4. Re-run `./gradlew build` — a version bump that breaks compilation or tests surfaces immediately,
   before the bump lands.

Minestom and Cloud version bumps are each their own roadmap/PR-sized change, not something to bundle
with unrelated work — see `CONTRIBUTING.md`'s workflow section.
