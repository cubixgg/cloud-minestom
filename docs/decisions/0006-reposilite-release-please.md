# 0006: Reposilite + release-please for Maven publishing

Status: Accepted

## Context

`cloud-minestom` (the library) and `cloud-minestom-bom` (its version-aligned BOM) are the two
published artifacts of this repo (spec.md §3); `minestom-demo` is a runnable application, not a
consumable dependency, and stays unpublished. Spec.md §2 originally specified Maven Central as the
distribution target with per-commit snapshot publishing - written before an actual publishing target
existed to build against. Neither of those turned out to be the real plan: this project instead
publishes to a self-hosted [Reposilite](https://reposilite.com/) instance at
`https://maven.cubix.gg/public-releases`, the same pattern already proven out in the `minecraft-platform`
repo (its own `docs/decisions/0006-reposilite-release-please.md`), minus that repo's Docker/Harbor
image-retagging concerns - `cloud-minestom` has no container images to publish.

Versioning needed a single source of truth that couldn't drift from what's actually tagged and
published. [`release-please`](https://github.com/googleapis/release-please) computes the next semver
from Conventional Commits (already this repo's required commit format, `CONTRIBUTING.md`) and opens a
release PR with the version bump and generated `CHANGELOG.md` entry; merging that PR is what
triggers an actual release.

## Decision

- **Single repo-wide version**, not per-module: root `build.gradle.kts`'s `subprojects { version = "1.0.0" // x-release-please-version }`
  line is release-please's `extra-files` generic target (its own updater does a plain find-and-replace
  on that annotated line, which requires it to already hold a real `X.Y.Z` value - this is why the
  original `1.0-SNAPSHOT` placeholder became `1.0.0`). `cloud-minestom-bom` already derives
  `cloud-minestom`'s coordinate via `project(":cloud-minestom")` rather than a hardcoded version
  string, so it never needed a second place to update.
- **Only `cloud-minestom` and `cloud-minestom-bom` publish** (root `build.gradle.kts`'s
  `publishedModules` set) - `minestom-demo` isn't a consumable dependency.
- **Release-triggered only, no per-commit snapshot publishing.** `.github/workflows/release-please.yml`
  opens/updates a release PR against `main` on every push, and only runs `./gradlew publish` when
  merging that PR actually creates a release (`release_created` output) - not on every commit.
  Snapshot publishing was explicitly deferred, not forgotten: Reposilite has no built-in artifact
  retention/eviction (only a storage-quota rejection), so per-commit snapshots would need a retention
  policy sorted out first, and there's no consumer needing pre-release artifacts yet.
- **`REPOSILITE_USERNAME`/`REPOSILITE_PASSWORD` repo secrets**, read via `System.getenv(...)` in the
  `maven-publish` repository credentials block - not committed, not defaulted; the `publish` job simply
  fails without them.
- PRs into `main` must stay squash-merged (already this repo's only merge style, `CONTRIBUTING.md`) -
  a regular merge commit's auto-generated message repeats the PR title on its second line, which
  release-please's Conventional Commits parser would read as a second, duplicate changelog entry for
  the same change.

## Why not what spec.md originally said

Maven Central requires a registered Sonatype namespace and GPG-signed artifacts - real infrastructure
this project doesn't have set up, and no consumer outside this org needs Central specifically today.
Reposilite is already the org's standing choice for exactly this kind of internal library artifact
(the same instance `minecraft-platform`'s own published modules use), so reusing it instead of standing
up a second publishing target was the pragmatic call. If a genuine public/Central-distribution need
shows up later, that's a new decision to make then, not a default to guess at now.

## Consequences

- `docs/spec.md` §2's "Distribution" bullet is corrected in the same commit as this ADR to describe
  Reposilite instead of Maven Central, and release-triggered-only instead of per-commit snapshots -
  matching `CONTRIBUTING.md`'s rule that a spec.md gap found during implementation gets fixed in the
  same commit, not left to drift.
- `CHANGELOG.md` is not hand-seeded: release-please generates it itself from Conventional Commit
  history the first time it opens a release PR, the same way `minecraft-platform`'s was.
- A snapshot-publishing pipeline, if ever added, needs a Reposilite retention policy decided first -
  this is the same conclusion `minecraft-platform`'s own ADR reached, not a new judgment call.

## References

- [`spec.md`](../spec.md) §2 (Target audience & compatibility), §3 (Module architecture)
- [`CONTRIBUTING.md`](../../CONTRIBUTING.md) (Conventional Commits, squash-merge-only workflow)
- `minecraft-platform`'s own `docs/decisions/0006-reposilite-release-please.md` - the proven-out
  pattern this decision reuses, minus its Docker/Harbor-specific parts
