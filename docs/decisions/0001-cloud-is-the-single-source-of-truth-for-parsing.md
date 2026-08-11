# 0001: Cloud is the single source of truth for parsing

Status: Accepted

## Context

`cloud-minestom` mirrors Cloud's whole command tree into Minestom's native argument tree, node for
node (spec.md §5, §1.1) — every mapped node gets a real native `Argument<T>` with the correct
client-side shape, including bounds where Minestom has a native concept of them (`IntegerParser` →
`ArgumentInteger.min()/max()`, for example).

That native shape means Minestom's own client-server protocol *could* produce a parsed value for a
typed argument — the temptation is to read that value straight into the Cloud `CommandContext` handed
to the handler, skipping a second parse. Doing so would mean two parsers exist for the same input:
Minestom's native one (shape-only, driven by whatever the `ArgumentMapper` chose) and Cloud's own
(the actual parser, with its own validation, suggestion providers and permission-gated branches). They
can disagree — a value Minestom's native `Integer` argument accepts but Cloud's ranged `IntegerParser`
rejects, a context-dependent Cloud suggestion provider Minestom's native completion has no way to
consult, a permission-gated branch Minestom's tree doesn't know exists. When two parsers can disagree,
the bug shows up as "works sometimes," which is worse than a parser that's simply wrong every time.

## Decision

Every syntax executor — mapped node or fallback alike — re-joins the raw command line and
re-dispatches it through `manager.commandExecutor().executeCommand(mappedSender, line)`. Minestom's
own parse of the typed arguments is **never** read into the `CommandContext` handed to a Cloud
handler. The native argument tree exists for client-side shape and completion structure only; Cloud
remains the only parser that ever decides what a command means, on every execution, no exceptions.

If a piece of code inside this library is tempted to read a value off a native Minestom `Argument`
directly, that's the signal this decision is being violated — see `CLAUDE.md`'s architecture
principles for the same rule stated as a standing project constraint, not just a one-time decision.

## Consequences

- Correctness has exactly one source of truth: whatever Cloud's parsers, suggestion providers and
  permission checks say, independent of what native shape a component happens to have.
- Re-parsing the whole line through Cloud on every execution has a real cost, but it's cheap relative
  to a command dispatch (not a hot loop) — paying it buys away an entire class of "two parsers
  disagreed" bugs.
- Client-side shape (coloring, tab-complete structure) can still be wrong or absent for a parser
  without a built-in `ArgumentMapper` (falls back to `Word`/`String`/`StringArray`, spec.md §5.1) or
  for a documented native/Cloud grammar mismatch (`DurationParser`/`Time`, spec.md §5.2) — that's an
  acceptable, honestly-documented cosmetic gap, never a correctness gap, because Cloud still re-parses
  and validates regardless.
- Any future built-in parser that reads a native value for actual value resolution (rather than
  client-side shape) — the way `PlayerParser` calls Minestom's own `ArgumentEntity.parse(...)` as a
  library routine for selector grammar — must still be the *one* parse Cloud performs, not a second,
  independently-parsed value read back from the tree. `PlayerParser`'s own Javadoc documents exactly
  why that specific case doesn't violate this ADR.

## References

- [`spec.md`](../spec.md) §5.4 (Cloud is the only parser that matters), §1.1 (Why not the obvious
  shortcut)
- [`CLAUDE.md`](../../CLAUDE.md), "Cloud is the only parser, always"
- `PlayerParser` (`gg.cubix.cloudminestom.parser`) — the one deliberate, documented exception, and why
  it doesn't actually violate this decision
