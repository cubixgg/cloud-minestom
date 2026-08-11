# Known limitations

## Flags degrade to a single greedy argument

Cloud flags (`--name value`, presence flags like `--verbose`, `-abc` short-flag aliasing) have no
Minestom-native argument node that matches their shape. As soon as a command's flag subtree is
reached, `cloud-minestom` stops mirroring individual native arguments there and falls back to a
single trailing greedy argument for the rest of the line — the same shape every argument used to
get before native argument-tree mirroring existed ([spec.md
§1.1](./spec.md#11-why-not-the-obvious-shortcut)).

Everything **before** the flags in a command still gets full native argument-tree mirroring —
correct per-argument tab completion and client-side red/green syntax coloring. Only the flag
portion itself, and anything after it, loses that: the client sees one grey "it's a string" field
covering `--verbose --count 5` rather than individually validated, individually suggested
sub-arguments.

This does **not** affect correctness. Cloud is the only parser that ever runs, for flagged commands
exactly as much as for any other ([spec.md §5.4](./spec.md#54-cloud-is-the-only-parser-that-matters)):
the fallback argument's raw text is re-joined with everything before it and re-dispatched through
Cloud's own command executor, which parses the flags itself — presence flags, value flags,
repeatable flags, aliasing, all of it — the same way it would on any other Cloud platform. What's
lost is purely native client-side shape for that portion of the command.

### Why this isn't going away

Minestom's argument tree has no flag-shaped node type — no `Argument` that represents "zero or
more `--name value` / `--flag` pairs in any order." Adding one would mean building flag-aware
argument-tree support into Minestom itself, which is out of scope for this library. If Minestom
ever ships a native flag argument type, this could be revisited; until then it's a deliberate,
documented boundary of the native-mirroring approach, not a gap later roadmap phases are expected
to close (spec.md §5.5).

See `CommandTreeTranslator` (`gg.cubix.cloudminestom.registration`) for where this fallback is
applied during translation.

## An argument named the same as its enclosing literal

Cloud component names become Minestom native argument IDs 1:1 (spec.md §5.1). If a required or
optional argument is given the *same* name as a literal earlier in the same command path — e.g. a
component named `"target"` directly under a `.literal("target")` — Minestom's own native command
parser throws `IllegalStateException: Duplicate key <name>` while collecting parsed arguments,
because it tracks matched nodes (literals included) by that same ID internally. This is a Minestom
native-parser quirk, not a `cloud-minestom` bug or a Cloud-level ambiguity: Cloud's own tree has no
trouble with a component sharing a name with a literal, since Cloud looks components up by tree
position, not by a single flat ID map.

Give the argument a different name than any literal in its own command path (e.g. `"player"` instead
of `"target"` for a `/demo target <player>`-style command) to avoid it — this is purely a naming
choice, not a shape or suggestion trade-off like the flag fallback above.
