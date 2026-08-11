# Contributing

Thanks for looking at contributing to `cloud-minestom`. Before making changes, skim:

- [`spec.md`](./docs/spec.md) – full architecture and design decisions
- [`roadmap.md`](./docs/roadmap.md) – implementation status and what to work on next
- [`CLAUDE.md`](./CLAUDE.md) – code style and non-negotiable architectural principles
- [`docs/`](./docs) – user-facing documentation, [`docs/decisions/`](./docs/decisions) – ADRs for
  load-bearing decisions

## Setup

- JDK 25 (see `gradle/libs.versions.toml` for the exact pinned version)
- `./gradlew build` builds and tests every module (`cloud-minestom`, `cloud-minestom-bom`,
  `minestom-demo`)
- `./gradlew :minestom-demo:run` starts the example server from [§13 of `spec.md`](./docs/spec.md#13-minestom-demo)

See [`README.md`](./README.md) for more on local development.

## Workflow

- **Every change is a commit, every commit goes through a pull request.** There is no direct commit to
  `main` — not for a one-line fix, not for a docs typo, not for a version bump. Work happens on a
  branch and lands through a PR, every time, no exceptions carved out for "it's trivial."
- Pick the next unchecked item in `roadmap.md`. Phase order is the recommended build/test order, not
  a strict dependency.
- **One item = one commit.** Don't fold multiple checkboxes into one commit, even if both are small and
  related - each commit should be reviewable and revertable on its own. If an item turns out to be too
  big for one commit, split it into further sub-items in the roadmap instead.
- **PRs are grouped per roadmap phase (P0, P1, ...), not per item.** A phase's commits land on one
  branch and go out as one PR once the phase (or a sensible chunk of it) is done - not a PR per
  checkbox. Use judgment on where a phase's PR boundary actually falls; a phase that's naturally two
  reviewable halves can be two PRs, but don't default to the smallest possible PR.
- Check the box in `roadmap.md` in the same commit as the implementation - never leave it out of sync,
  even briefly.
- If implementing an item reveals a real gap or correction in `spec.md`, fix `spec.md` in the same
  commit and say so in the commit message.
- This is meant to be a full implementation, not a partial one (see `spec.md` §1.1/§1.2). A roadmap
  item isn't done because it compiles - it's done when both required testing layers pass (see Testing
  below) and, where the item calls for it, `minestom-demo` actually demonstrates the behavior.

## Commit messages

Commits follow [Conventional Commits](https://www.conventionalcommits.org/):
`<type>(<scope>): <description>`. This is what will eventually drive automated releases/changelogs, so
it's not just style.

- Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`.
- Scope = the module touched where useful, e.g. `feat(cloud-minestom): ...`,
  `docs(minestom-demo): ...`. Omit it for repo-wide changes (e.g. version catalog bumps).
- Breaking change: `!` after the type/scope (`feat(cloud-minestom)!: ...`) or a `BREAKING CHANGE:`
  footer - never just a description that happens to mention it.
- Each commit's type reflects what *that* commit does, not the theme of the overall PR: adding a
  builder option is `feat`, fixing a stale Javadoc is `fix` or `docs`, adding a missing test for
  already-merged behavior is `test`.

## Branch naming

`<username>/<type>/<short-description>`, e.g. `marlon/feature/argument-mapper-registry`. `<type>` is
the Conventional Commits type spelled out: `feature`, `fix`, `chore`, `docs`, `refactor`, `test`,
`build`, `ci`, `perf`.

## Pull requests

- Base branch: `main`. `main` only ever receives merged PRs, never a direct push.
- One PR per roadmap phase (or sensible chunk of one), not per item (see Workflow above).
- Make sure `./gradlew build` passes before opening.

## Testing

Both layers described in `CLAUDE.md`'s Testing section are required for anything touching
registration, argument mapping, suggestions, permissions or execution - not just whichever is
convenient to write:

- **Unit tests** against a fake registration sink
  (`Consumer<net.minestom.server.command.builder.Command>`) and hand-built Cloud/Minestom objects, for
  anything that can be tested as a pure function (`CommandTreeTranslator`, `ArgumentMapperRegistry`,
  `CloudSuggestionCallback`, default permission/exception wiring).
- **`@EnvTest` integration tests** (`net.minestom:testing`) for anything that needs to prove the whole
  stack works against a real, headless, virtual-player server.
- Don't mock Cloud's own types (`CommandManager`, `ExceptionController`, `SuggestionFactory`). Fake
  only at the Minestom boundary - see `CLAUDE.md` for why.

## Key architectural rules

Full detail and rationale in `CLAUDE.md` and `spec.md`; the short version:

- Cloud is the only parser, ever - native Minestom argument nodes are shape only, never a second
  source of truth for what a command means (`spec.md` §5.4, ADR-0001).
- Native argument-tree mirroring is the default; the greedy-string fallback is only for parsers
  without a native equivalent and for flag subtrees (`spec.md` §5.2/§5.5) - it is not a shortcut to
  reach for elsewhere.
- `ExecutionCoordinator.simpleCoordinator()` stays the default (`spec.md` §4.1, ADR-0002) - don't
  change it to chase performance.
- No new Gradle module without a second real consumer on the other side of the split (`spec.md` §3,
  ADR-0003).
- The library is generic over the sender type and never talks to an external system itself
  (`spec.md` §4, §6, §7) - every integration point is a builder option with a Minestom-native default.

## Documentation

- User-facing docs go in `docs/`, written for someone who already knows Cloud from another platform
  and needs the Minestom-specific delta - not a re-explanation of Cloud itself.
- Architecture Decision Records go in `docs/decisions/`, one file per decision, numbered sequentially.
  Write one only for a decision a future contributor could plausibly second-guess without the context
  - not for every roadmap item.
- If a roadmap item changes what an existing `docs/` page says, update that page in the same commit.

## Claude Code tooling

`.claude/skills/` and `.claude/agents/` exist to make working in this repo faster and more consistent
than re-deriving the same steps by hand every time. Use them where they apply:

- **`scaffold-argument-mapper` skill** — reach for this when adding a new row to the argument-mapping
  table (`docs/spec.md` §5.2): a new built-in `ArgumentMapper` method in `StandardArgumentMappers`, its
  registration, a unit test stub, and the `docs/argument-mapping.md`/`docs/spec.md` §5.2 updates that
  need to land alongside it.
- **`verify-roadmap-item` skill** — run through this before checking a `docs/roadmap.md` box: which
  testing layer(s) the item actually needs, what an `@EnvTest` should assert on (real packet content,
  not just "didn't throw"), and the `net.minestom.testing` `Collector#collect()` footgun that can make
  a working feature look broken in a hand-rolled verification test.
- **`cloud-minestom-reviewer` agent** — a review pass tuned to this repo's own written-down rules
  (`CLAUDE.md`'s architecture principles, ADR triggers, both testing layers present, spec/roadmap
  consistency, commit/PR granularity) rather than a generic code review. Run it after implementing a
  roadmap item and before opening a PR.
