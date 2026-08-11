# CLAUDE.md

Conventions for working in `cloud-minestom`. Read [`spec.md`](./docs/spec.md) for the architecture and
[`roadmap.md`](./docs/roadmap.md) for what's next before making changes — this file is *how* to build it,
those are *what* to build. [`CONTRIBUTING.md`](./CONTRIBUTING.md) owns the commit/branch/PR/workflow
rules; don't duplicate those here.

## Architecture principles (non-negotiable)

These come straight out of `spec.md`'s design decisions. Don't "improve" around them without updating
the spec and writing an ADR — they're the reason this is a full implementation and not another
greedy-string bridge.

- **Cloud is the only parser, always** (spec §5.4 / ADR-0001). A syntax executor never reads
  Minestom's own parsed argument values into the handler — it re-joins the raw input and re-dispatches
  through `commandExecutor().executeCommand(...)`. Native Minestom `Argument` nodes exist purely for
  client-side shape (coloring, tab-complete structure), never as a second opinion on what a command
  means. If you're tempted to read a value straight off a Minestom `Argument`, stop — that's the bug
  class this rule exists to prevent.
- **Native argument-tree mirroring is the default, not the exception.** Every Cloud component gets a
  real Minestom node via `ArgumentMapperRegistry` (spec §5.2); the flattened greedy-string fallback is
  only for parsers with no sane native equivalent and for flag subtrees (spec §5.5). If you're adding a
  new parser type, add a mapper — don't let it silently fall back.
- **`ExecutionCoordinator.simpleCoordinator()` is the default and stays the default** (spec §4.1). It's
  a correctness requirement (handlers touch thread-confined Minestom game state), not a performance
  knob. Never change the default to an async coordinator to "speed things up" — that's a footgun for
  every consumer who doesn't read the fine print.
- **No new module without a second real consumer on the other side of the split.** `cloud-annotations`
  and `cloud-minecraft-extras` are already platform-agnostic upstream, which is why there's no
  `cloud-minestom-annotations`/`cloud-minestom-extras` (spec §3 / ADR-0003). Don't add a module because
  it "feels cleaner" — add it when something else besides `cloud-minestom` would actually depend on it.
- **The library is generic over the sender type `C`** via `SenderMapper<CommandSender, C>` (spec §4).
  Never hardcode a specific project's player/permission model into `cloud-minestom` itself — the
  identity case (`C = CommandSender`) is the only sender type the library may assume.
- **The library never talks to an external system.** No bundled permission backend, no bundled auth
  service, no network calls. Permission resolution, sender mapping and exception formatting are all
  pluggable through the builder (spec §6, §7) with a sane Minestom-native default; the plug points are
  the product, not a workaround.

## Code style

- **Java 21**, matching Minestom's own floor (spec §2). Bump only when the pinned Minestom version in
  `gradle/libs.versions.toml` bumps.
- Package root: `gg.cubix.cloudminestom`, sub-packaged per spec §3.1's table
  (`registration`, `argument`, `suggestion`, `parser`, `exception`). A new top-level package needs a
  reason beyond "this file didn't fit elsewhere."
- `final` on parameters and locals wherever it doesn't hurt readability — makes the "this is never
  reassigned" invariant visible at the call site, not just true by convention.
- `Objects.requireNonNull(x, "x")` at the boundary of every public constructor/factory/builder setter.
  Internal, package-private code trusts its callers instead of re-validating what a public entry point
  already checked.
- Nullability: match whatever annotation Minestom itself uses at its public API boundaries (check the
  pinned Minestom version's own dependencies/annotations) for anything that crosses into or out of a
  Minestom type. Don't introduce a second nullability annotation convention alongside it.
- Classes are package-private unless they're part of the public API surface described in spec §3.1's
  package table. If it's not in that table, default to not exporting it.
- Prefer immutable value types and builders for anything configuration-shaped
  (`MinestomCommandManager.Builder`, `ArgumentMapperRegistry` entries) over mutable setters on a live
  object.
- **No comments that restate what the code does.** A comment earns its place only by explaining a
  *why* that isn't obvious from the code — a Minestom quirk (e.g. the suggestion placeholder
  character), a spec cross-reference for a non-obvious tradeoff, a documented parser mismatch (spec
  §5.2's `DurationParser`/`Time` case). If deleting the comment wouldn't confuse the next reader,
  delete it.
- Every public class and public method gets Javadoc. Internal/package-private code gets Javadoc only
  where the *why* isn't obvious from the signature and name.

## Testing

Two layers are required for anything that touches registration, parsing, suggestions or execution —
neither one substitutes for the other (spec §11, roadmap items call both out explicitly where they
apply):

1. **Unit tests, no running server.** `CommandTreeTranslator`, `ArgumentMapperRegistry`,
   `CloudSuggestionCallback` and the default permission/exception wiring are all pure enough to test
   against a fake registration sink (`Consumer<net.minestom.server.command.builder.Command>` instead of
   `MinecraftServer.getCommandManager()::register`) and hand-built `CommandContext`/`Suggestion`
   objects. Keep that seam — don't reach for a real `MinecraftServer` where a fake sink would do; it's
   slower and tests more than the unit under test.
2. **`@EnvTest` integration tests** (`net.minestom:testing`) for anything that needs to prove the whole
   stack actually works against a real (headless, virtual-player) server: registration → native parse
   shape → Cloud re-parse → permission check → handler → feedback. If a roadmap item's checkbox
   description says "end to end," it needs one of these, not just a unit test around the pieces.

Don't mock Cloud's own types (`CommandManager`, `ExceptionController`, `SuggestionFactory`). Drive the
real ones; fake only at the Minestom boundary (registration sink, sender mapper, virtual player). A
test that mocks Cloud can pass while the real integration is broken — that defeats the point.

## Documentation

- `docs/` is user-facing: how to use the library, not how it's built internally. Write it for someone
  who already knows Cloud from `cloud-velocity`/`cloud-paper` and needs the Minestom-specific delta,
  not a re-explanation of Cloud itself.
- `docs/decisions/` is for ADRs on load-bearing, non-obvious decisions — the kind a future contributor
  could plausibly second-guess without the context (spec §12). Not every roadmap item needs one. If
  you're about to override or work around one of the architecture principles above, that's an ADR
  first, code second.
- Keep `docs/argument-mapping.md` and `docs/compatibility.md` in sync with the actual registered
  mappers / pinned versions — a docs page that lies about the mapping table is worse than none.

## Gradle & dependencies

- All version numbers live in `gradle/libs.versions.toml`. No inline version strings in a module's
  `build.gradle.kts`.
- `cloud-minestom-bom` must stay in sync with what `cloud-minestom` actually requires — update both in
  the same commit when a pinned Cloud/Minestom version changes.
- New dependencies in `cloud-minestom` (the published library) need a reason beyond convenience — every
  `api`/`implementation` dependency there is a transitive dependency for every consumer.

## Anti-patterns to avoid

- Adding a module, SPI, or extension point "just in case" — every abstraction in this repo should be
  traceable to a roadmap item or an ADR, not to a hypothetical future need.
- Silently coercing a parser mismatch instead of documenting it (see the `DurationParser`/`Time`
  precedent in spec §5.2) — a wrong answer that looks right is worse than a documented gap.
- Defensive null-checks or fallback branches for states that can't occur inside the library's own
  boundary (e.g. a `CommandComponent` the translator itself never produces). Validate at the public
  API edge (see `Objects.requireNonNull` above); trust internal invariants past that.
- Batching multiple `roadmap.md` checkboxes into one commit/PR because they're "related" — see
  `CONTRIBUTING.md`'s workflow section for why that's off-limits here.
