# 0005: AI-assisted development ("vibecoding") with Claude Code

Status: Accepted

## Context

A full Cloud v2 integration for Minestom needs a substantial amount of scaffolding before the
interesting design work pays off: a multi-module Gradle build, a native command-tree translation
layer, several built-in argument mappers, permission/exception/help wiring, custom parsers,
annotation-command support, two required testing layers, and full documentation including ADRs.
Building all of that by hand — dependency wiring, module boilerplate, test harness setup, docs
structure — takes real time that would otherwise come out of the time available for getting the
design itself right.

## Decision

This project is built primarily through AI-assisted development ("vibecoding") using
[Claude Code](https://claude.com/claude-code), as a deliberate experiment: whether an AI coding agent
can carry a real, fully-specified project — not a prototype — end to end, from concept through
implementation, tests and documentation, while held to the same bar a careful hand-written
implementation would be held to.

This is not a shortcut around design rigor. [`spec.md`](../spec.md) — an exact concept — was worked
out and reviewed *before* any implementation started. Load-bearing decisions are recorded as ADRs the
same way they would be for hand-written code (this document included). `roadmap.md` enforces full
implementation over partial (spec.md §1.2, Goals), and both a unit-testing and an `@EnvTest`
integration-testing layer are required for anything touching the library's core behavior — see
`CLAUDE.md`'s Testing section.

## Consequences

- Significantly faster to stand up the dependency wiring, multi-module layout, initial documentation
  set and boilerplate than doing it by hand, freeing time for the design itself.
- Forces the design to be written down precisely before implementation, since an agent needs an
  explicit concept to build against — a discipline that benefits the project regardless of who or what
  writes the code.
- Requires deliberate review at each step rather than trusting output wholesale; `CONTRIBUTING.md`'s
  one-item-per-commit/PR workflow exists partly to keep that review tractable, not just to keep history
  tidy.
- Documented here rather than left unstated, so anyone evaluating the codebase's origin has the actual
  reasoning, not just the result.

## References

- [`README.md`](../../README.md), "A note on how this project was built"
- [`spec.md`](../spec.md) §1.2 (Goals), §11 (Testing strategy)
- [`CONTRIBUTING.md`](../../CONTRIBUTING.md) (workflow enforcing incremental, reviewable steps)
