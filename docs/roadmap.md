# cloud-minestom — Roadmap

This is the implementation checklist for [`spec.md`](./spec.md), broken into the smallest steps that
still make sense as one commit. See [`CONTRIBUTING.md`](./CONTRIBUTING.md) for the workflow this
drives: one item = one commit (no direct commits to `main`, ever), box checked in the same commit as
the implementation, PRs grouped per phase rather than per item, phase order is the recommended
build/test order but not a hard dependency. If an item turns out bigger than it looked, split it into
sub-items here instead of batching it with something else.

Every item that adds behavior needs the tests called out for it (or better, if not called out) —
"Unit Tests sind auch wichtig" is not optional per project scope. Items phrased as `@EnvTest` require
the `net.minestom:testing` integration harness from P2.

- [x] P0 — Repository & build scaffolding
- [x] P1 — Command manager core
- [x] P2 — Minimal end-to-end registration
- [x] P3 — Native argument-tree translation
- [x] P4 — Flags (documented fallback)
- [x] P5 — Permissions
- [x] P6 — Exception handling & feedback
- [x] P7 — Help
- [x] P8 — Custom parsers
- [ ] P9 — Annotation commands
- [ ] P10 — `cloud-minestom-bom`
- [ ] P11 — `minestom-demo`
- [ ] P12 — Documentation
- [ ] P13 — Claude Code tooling
- [ ] P14 — Release & CI hardening

---

## P0 — Repository & build scaffolding

Turns the current single-module scaffold into the multi-module layout from spec §3.

- [x] Add `gradle/libs.versions.toml` with pinned versions: `java` (25 — the pinned Minestom version
      below turned out to require it; see the correction noted under item 12), `minestom`
      (2026.08.07-26.2), `cloud` (2.1.0 — covers `cloud-core` **and** `cloud-annotations`, both
      published from the same `Incendo/cloud` monorepo tag), `cloud-minecraft` (2.0.0 —
      `cloud-minecraft-extras`, published separately from `Incendo/cloud-minecraft` and currently
      trailing `cloud`'s version), `adventure` (5.2.0, matching what the pinned Minestom version itself
      ships), `junit` (6.1.2), `slf4j` (2.0.18). Corrects a mislabeling in the original item text, which
      grouped `cloud-annotations` under the `cloud-minecraft` pin — verified against both repos' actual
      tags before writing the catalog.
- [x] Convert root `build.gradle.kts` into a parent build: remove the `java` plugin and dependency
      block, keep only `subprojects` shared config (group, repositories, Java toolchain via the
      version catalog). The toolchain config only fires for subprojects that apply a `java`-family
      plugin (`plugins.withType<JavaBasePlugin>`), so it safely skips `cloud-minestom-bom`'s
      `java-platform` plugin, which cannot coexist with `java`/`java-library`.
- [x] Delete the placeholder root `src/main/java`, `src/main/resources`, `src/test/java`,
      `src/test/resources` directories — already gone (they were always empty and Git never tracked
      them), nothing to delete
- [x] Update `settings.gradle.kts` to include `cloud-minestom`, `cloud-minestom-bom`, `minestom-demo`
- [x] Create `cloud-minestom/build.gradle.kts`: `java-library` plugin, `api(libs.cloud.core)`,
      `api(libs.minestom)`, `api(libs.cloud.minecraft.extras)`, `compileOnly(libs.cloud.annotations)`
      (spec §10 — optional for consumers who don't use annotations), `implementation(libs.slf4j.api)`,
      JUnit test deps (also `testImplementation(libs.cloud.annotations)`, needed by later
      annotation-related tests in P9)
- [x] Create `cloud-minestom/src/main/java/gg/cubix/cloudminestom` package root with a placeholder
      compile-anchor class (mirrors the convention of documenting module scope in one landing class):
      `CloudMinestom`
- [x] Create `cloud-minestom-bom/build.gradle.kts`: `java-platform` plugin, empty constraints block
      (filled in P10)
- [x] Create `minestom-demo/build.gradle.kts`: `application` plugin,
      `implementation(project(":cloud-minestom"))`, `implementation(libs.cloud.annotations)`.
      `application.mainClass` deliberately left unset until P11 adds the `Main` class it needs to
      point at.
- [x] Pick and record a license (MIT, matching Cloud's own license and the prior community ports);
      add `LICENSE` at repo root — already present from the GitHub repo's own initial commit,
      merged into local history rather than recreated
- [x] Add root `README.md`: what the project is, links to `spec.md`/`roadmap.md`/`docs/`, build badge
      placeholder — landed with the rest of the project docs before this phase started
- [x] Add GitHub Actions workflow `.github/workflows/build.yml`: `./gradlew build` on push and PR,
      using the JDK pinned in `libs.versions.toml` (hardcoded in the workflow YAML with a keep-in-sync
      comment, since Actions' `setup-java` needs a literal value and can't read the Gradle catalog)
- [x] Verify `./gradlew build` succeeds on the empty multi-module skeleton before any real code lands.
      First attempt failed: `net.minestom:minestom:2026.08.07-26.2` is compiled for JVM 25, not 21 as
      `spec.md` §2 originally (incorrectly) claimed as Minestom's floor - a stale assumption, not
      something actually verified against the pinned artifact when spec.md was written. Corrected the
      `java` catalog entry to 25 and every doc that repeated the wrong number
      (`spec.md`, `CLAUDE.md`, `CONTRIBUTING.md`, `README.md`, `.github/workflows/build.yml`) in the
      same commit; build passes clean after the fix.

## P1 — Command manager core

Spec §4. No registration or tree translation yet — just the manager shape and its construction paths.

- [x] `MinestomCommandManager<C>` class: extends `CommandManager<C>`, constructor takes
      `SenderMapper<CommandSender, C>` and `ExecutionCoordinator<C>`, calls
      `super(executionCoordinator, CommandRegistrationHandler.nullCommandRegistrationHandler())` as a
      temporary stand-in (real handler lands in P2)
- [x] `MinestomCommandManager#senderMapper()` accessor
- [x] `MinestomCommandManager.Builder<C>`: required `senderMapper`, optional `executionCoordinator`
      (default `ExecutionCoordinator.simpleCoordinator()`, spec §4.1), `build()`
- [x] `MinestomCommandManager.builder()` static overload fixing `C = CommandSender` via
      `SenderMapper.identity()`
- [x] `MinestomCommandManager.create()` static convenience: `builder().build()` with `C = CommandSender`
- [x] `hasPermission(C, String)` override: temporary stub returning `true` unconditionally (real
      implementation is P5 — do not build a half version here, land the whole default in one piece)
- [x] Unit test: `builder().build()` returns a non-null manager with the identity sender mapper
- [x] Unit test: a custom `SenderMapper<CommandSender, MyType>` round-trips correctly through
      `senderMapper()`
- [x] Unit test: `create()` and `builder().build()` (identity) produce equivalent managers

## P2 — Minimal end-to-end registration

Gets one full round trip working — register, execute, suggest — before native argument mapping
(P3) adds complexity on top. Deliberately close in shape to the simplest possible bridge so the
"why not just this" contrast in spec §1.1 is visible in the repo's own history.

- [x] `net.minestom:testing` added as a test dependency to `cloud-minestom`
- [x] `MinestomCommandManager.Builder#commandRegistrationCallback(Consumer<Command>)`: how built
      native commands reach the server; default `MinecraftServer.getCommandManager()::register`
- [x] `MinestomCommandRegistrationHandler<C>` implementing Cloud's `CommandRegistrationHandler<C>`:
      for now, registers only the root literal (+ aliases) as a native `Command` with a default
      executor and a single trailing `ArgumentStringArray` syntax
- [x] Root/default executor and the fallback syntax both re-join their input and dispatch to
      `manager.commandExecutor().executeCommand(mappedSender, line)`
- [x] Wire `MinestomCommandRegistrationHandler` into `MinestomCommandManager`'s constructor, replacing
      the P1 null-handler stub
- [x] `CloudSuggestionCallback`: strips Minestom's trailing-space placeholder character, calls
      `manager.suggestionFactory().suggestImmediately(mappedSender, input)`, fills the Minestom
      `Suggestion` with the results
- [x] Attach `CloudSuggestionCallback` to the fallback `ArgumentStringArray`
- [x] Unit test: registering a Cloud command against a fake registration sink produces exactly one
      native `Command` with the expected name and aliases
- [x] Unit test: registering two syntaxes under the same root literal produces exactly one native
      `Command` (root de-duplication)
- [x] Unit test: suggestion callback matches a partially-typed argument
- [x] Unit test: suggestion callback returns everything after a trailing-space placeholder
- [x] Unit test: suggestion callback asks Cloud with the whole line including the root literal, not
      just the trailing text
- [x] First `@EnvTest`: register a Cloud command with a handler that sends a message, connect a
      virtual player, run the command, assert the player received the message

## P3 — Native argument-tree translation

Spec §5. The part that makes this a full implementation instead of the greedy-string shortcut from
P2/spec §1.1. Each row of spec §5.2's table is its own item.

- [x] `ArgumentMapper<T>` interface (spec §5.2)
- [x] `ArgumentMapperRegistry`: register/look up an `ArgumentMapper` by Cloud parser class; unit
      tested standalone with hand-built fake mappers before any real one exists
- [x] Built-in mapper: `StringParser` (single/greedy/quoted) → `Word`/`StringArray`/`String`
- [x] Built-in mapper: `BooleanParser` → `Boolean`
- [x] Built-in mapper: `IntegerParser` → `Integer`, honoring configured min/max bounds
- [x] Built-in mapper: `LongParser` → `Long`, honoring bounds
- [x] Built-in mapper: `FloatParser` → `Float`, honoring bounds
- [x] Built-in mapper: `DoubleParser` → `Double`, honoring bounds
- [x] Built-in mapper: `UUIDParser` → `UUID`
- [x] Built-in mapper: `EnumParser<E>` → `Word` + Cloud suggestions (no native Minestom enum node,
      spec §5.2)
- [x] Built-in mapper: `DurationParser` → `Time`, with the grammar mismatch from spec §5.2 covered by
      a unit test pinning the documented behavior, not silently coercing it
- [x] Fallback mapping: any parser without a registered mapper → `Word`/`String`/`StringArray` chosen
      by whether the component is greedy/last (spec §5.1, row 3)
- [x] `CommandTreeTranslator`: pure function from a Cloud `CommandNode<C>` (via
      `CommandManager#commandTree()`) to a nested `Argument<?>` graph plus a list of
      `(Argument<?>[] syntax, executor)` pairs, using the registry above — no Minestom server
      interaction, unit-testable standalone
- [x] Literal node translation: inner (non-root) literals via `ArgumentType.Literal`, including
      aliases
- [x] Optional-variable components: given a (never-read) default value via `Argument#setDefaultValue`
      - Minestom has no `setOptional()`; `isOptional()` is derived from having a default value
      supplier, verified against Cloud's own optional-component ordering rules
- [x] `CommandComponent` → `ArgumentMapper` dispatch inside the translator, falling back per the item
      above when no mapper is registered for the component's parser
- [x] Replace `MinestomCommandRegistrationHandler`'s P2 flat-fallback-only registration with the full
      `CommandTreeTranslator` output
- [x] Confirm (and pin with a test) that every generated syntax executor — mapped node or fallback —
      still re-joins the raw input and dispatches through `executeCommand(...)`; Minestom's own parsed
      argument values are never read into the handler (spec §5.4)
- [x] ~~Generalize~~ `CloudSuggestionCallback` needed no changes: Minecraft's tab-complete protocol
      always sends the whole currently-typed line (never a mid-line cursor position), so
      `context.getInput()` is already the correct input for Cloud's suggestion factory regardless of
      which node's callback fires - confirmed by the `@EnvTest` below, not assumed
- [x] Attach the (unchanged) suggestion callback to every mapped node, not only the fallback
- [x] `MinestomCommandManager.Builder#argumentMapper(...)` / `argumentMapperRegistry(...)`: lets
      consumers register or replace mappers, mirroring `CloudBrigadierManager`'s registration API
- [x] Unit test per built-in mapper (one test class, one method per parser from the list above):
      given a configured Cloud parser, assert the produced `Argument` type and its options
- [x] Unit test: a multi-literal, multi-argument command tree translates into the expected nested
      `Argument` graph shape
- [x] Unit test: an optional-argument command produces syntaxes for both the short and long form
- [x] Unit test: a consumer-registered custom `ArgumentMapper` is used instead of / alongside the
      built-in set
- [x] `@EnvTest`: a bounded `IntegerParser` argument accepts valid input and rejects out-of-range
      input with Cloud's own error, proving Cloud (not Minestom) is still the parser (spec §5.4)
- [x] `@EnvTest`: suggestions for a mapped node (not the old whole-line fallback) return Cloud's
      suggestions at the correct cursor position

## P4 — Flags (documented fallback)

Spec §5.5. A deliberate, honest limitation, not a gap to quietly leave open.

- [x] Detect a flag-containing subtree during translation and degrade that subtree to the P2-style
      trailing greedy fallback instead of attempting a partial native mapping
- [x] Unit test: a command with a flag component produces a trailing greedy fallback node for that
      subtree, and native nodes for everything before it
- [x] `@EnvTest`: a flagged command (presence flag and value flag) still executes and parses
      correctly end to end via the fallback path
- [x] `docs/limitations.md`: document the flag fallback behavior, referencing spec §5.5

## P5 — Permissions

Spec §6. **Corrected during implementation** (see spec.md §6's correction note): the pinned
Minestom version has no native permission-node system at all (`net.minestom.server.permission`,
`Player#hasPermission`, `PermissionHandler` - none of it exists), only a vanilla-style numeric
`Player#getPermissionLevel()` unrelated in shape to Cloud's String-keyed model. The items below
reflect the corrected design (default = always allowed, no player/non-player split), not the
original wording.

- [x] `BiPredicate<C, String>` `permissionFunction` field + `MinestomCommandManager.Builder#permissionFunction(...)`
- [x] Default permission function: always allowed, for every sender and every permission string -
      the honest default for a platform with no native permission system to check against, not a
      placeholder (spec §6)
- [x] Wire `hasPermission(C, String)` to the configured function, replacing the P1 stub
- [x] Unit test: default function allows an empty permission string
- [x] Unit test: default function allows a non-empty permission string too (there is no native
      concept to gate it against)
- [x] Unit test: a consumer-supplied custom permission function fully replaces the default
- [x] `@EnvTest`: a permission-gated command (via a consumer-supplied permission function, since the
      default never denies) is both hidden from a lacking player's tab-completion and rejected on
      execution, and works normally for a player holding the node

## P6 — Exception handling & feedback

Spec §7. **Corrected during implementation** (see spec.md §7's correction note):
`MinecraftExceptionHandler` has no built-in handler for `NoSuchCommandException` at all - its
`defaultHandlers()` covers `InvalidSyntaxException`, `InvalidCommandSenderException`,
`NoPermissionException`, `ArgumentParseException` and `CommandExecutionException` (not originally
listed, but skipping it would mean a handler bug fails silently). `NoSuchCommandException` gets a
manually registered handler alongside those, styled the same way.

- [x] Default `MinecraftExceptionHandler` registration during `MinestomCommandManager` construction
      (its own `defaultHandlers()`), plus a manually registered `NoSuchCommandException` handler
      styled the same way
- [x] `MinestomCommandManager.Builder#exceptionHandler(...)` override hook, documented as a thin
      wrapper over `exceptionController()` rather than a new concept
- [x] Unit test: default handlers produce feedback for `NoPermissionException`,
      `InvalidSyntaxException`, `ArgumentParseException`, `InvalidCommandSenderException` and
      `NoSuchCommandException`, driven directly against `exceptionController()` (no live server needed)
- [x] `@EnvTest`: a command that throws mid-handler results in the sender receiving the expected
      feedback component

## P7 — Help

Spec §8.

- [x] `docs/help-and-exceptions.md` snippet: wiring `MinecraftHelp<C>` against
      `MinestomCommandManager` (no bridging adapter needed — `CommandSender` is already an `Audience`)
- [x] `@EnvTest`: `MinecraftHelp` renders and reaches a real virtual player's client without a
      bridging adapter

## P8 — Custom parsers

Spec §9. `PlayerParser` only for v1.0 — anything else is future roadmap, not a blocker (spec §9).
**Extended during implementation**: spec §5.2's mapping table only ever listed the built-in Cloud
parsers that already existed when P3 landed - it never had a row for a parser that didn't exist yet.
`PlayerParser` has a sane, unambiguous native equivalent (`ArgumentEntity`, constrained to
`singleEntity(true).onlyPlayers(true)`), so per `CLAUDE.md`'s non-negotiable "add a mapper, don't let it
silently fall back" rule, an `ArgumentMapper` registration was added as its own item below and spec
§5.2's table gained a row for it, rather than leaving `PlayerParser` components on the `Word`/`String`
fallback shape.

- [x] `PlayerParser<C>` in `gg.cubix.cloudminestom.parser`: parses a currently-online player by exact
      name
- [x] `PlayerParser` suggestions: currently-online player names
- [x] `PlayerParser` selector support (`@s`/`@p`/`@a`/...) via Minestom's `ArgumentEntity` where the
      sender context allows resolution
- [x] Register `PlayerParser` with Cloud's `ParserRegistry` by default so a `Player`-typed
      `@Argument`-annotated parameter resolves it automatically (spec §10 parity)
- [x] Built-in mapper: `PlayerParser` → `Entity` (`singleEntity(true).onlyPlayers(true)`), plus the
      matching spec §5.2 table row (see the correction note above)
- [x] Unit test: `PlayerParser` parses an exact online name, rejects an unknown/offline name with the
      correct exception type
- [x] Unit test: `PlayerParser` suggestions list currently-online names only
- [x] Unit test: `PlayerParser` maps to a single-player-only `Entity` argument (added alongside the
      mapper item above, see the correction note)
- [x] `@EnvTest`: `PlayerParser` resolves and suggests correctly against real virtual players

## P9 — Annotation commands

Spec §10. No dedicated module — `cloud-annotations` used directly.

- [x] `docs/annotations.md` getting-started snippet: constructing `AnnotationParser<C>` against
      `MinestomCommandManager<C>`
- [x] `@EnvTest`: an `@Command`-annotated method registers and executes correctly through the same
      manager as a builder-declared command
- [ ] `@EnvTest`: `@Permission` on an annotated command is enforced through the P5 permission function
- [ ] `@EnvTest`: `@Suggestions`-provided suggestions surface correctly through the P3 suggestion
      bridge
- [ ] `@EnvTest`: an annotated command using an `@Argument Player` parameter resolves through the P8
      `PlayerParser`

## P10 — `cloud-minestom-bom`

Spec §3.2.

- [ ] `cloud-minestom-bom/build.gradle.kts`: constraints for `cloud-minestom`, `cloud-core`,
      `cloud-annotations`, `cloud-minecraft-extras` at the versions pinned in `libs.versions.toml`
- [ ] Manual verification (documented in the PR description, not a unit test): a throwaway consumer
      project importing only the BOM resolves all four artifacts without further version pins

## P11 — `minestom-demo`

Spec §13. One item per demo command so each is its own reviewable, checkoffable step.

- [ ] `minestom-demo` `Main` class: boots `MinecraftServer` on a flat/void instance, registers
      `MinestomCommandManager`
- [ ] Demo command: builder-declared literal subcommand tree with a bounded `IntegerParser` argument,
      an optional argument, and Cloud-suggested string choices
- [ ] Demo command: the same command shape re-declared via `@Command` annotations, to show parity
      between styles side by side
- [ ] Demo command: `PlayerParser`-based "target a player" command
- [ ] Demo command: a flagged command that demonstrates and comments on the P4 fallback behavior
- [ ] Demo command: an intentionally-throwing command demonstrating default exception feedback
- [ ] Demo command: `/demo help` wired through `MinecraftHelp`
- [ ] Demo command: a permission-gated command demonstrating the default permission function
- [ ] `minestom-demo/README.md`: how to run the demo server and what each command demonstrates

## P12 — Documentation

Spec §12. Some pages start as stubs earlier (P4, P7, P9) — this phase is where they're completed and
the rest are written, plus the index tying them together.

- [ ] `docs/getting-started.md`
- [ ] `docs/command-manager.md` (builder options, sender mapping, spec §4)
- [ ] `docs/argument-mapping.md` — the full mapping table from spec §5.2, written to be kept in sync
      with `ArgumentMapperRegistry`'s actual registered set
- [ ] `docs/permissions.md` (spec §6)
- [ ] `docs/annotations.md` — expand the P9 stub into a full page
- [ ] `docs/threading.md` (spec §4.1, §5.3)
- [ ] `docs/help-and-exceptions.md` — expand the P7 stub into a full page, add spec §7 content
- [ ] `docs/limitations.md` — expand the P4 stub if any further limitations surfaced during P3
- [ ] `docs/compatibility.md` — Java/Minestom/Cloud version table (spec §2), process note for keeping
      it updated on every version-catalog bump
- [ ] `docs/decisions/0001-cloud-is-the-single-source-of-truth-for-parsing.md` (spec §5.4)
- [ ] `docs/decisions/0002-simple-execution-coordinator-by-default.md` (spec §4.1)
- [ ] `docs/decisions/0003-no-separate-annotations-or-extras-module.md` (spec §3)
- [x] `docs/decisions/0004-gradle-version-catalog-for-dependency-management.md` (spec §2) — written
      ahead of the rest of this phase since the decision was already made and needed recording
- [x] `docs/decisions/0005-ai-assisted-development-with-claude-code.md` — same as above, project-process
      decision recorded as soon as it applied rather than deferred to this phase
- [ ] `docs/README.md` index page linking all of the above

## P13 — Claude Code tooling

Spec §14. Deferred until the library's real shape exists so the tooling is built against reality, not
a guess.

- [ ] `.claude/skills/` skill: scaffold a new `ArgumentMapper` (class + registry wiring + unit test
      stub) for a given Cloud parser type
- [ ] `.claude/skills/` skill: verify a roadmap item's command round-trips through both the unit and
      `@EnvTest` layers before it gets checked off
- [ ] `.claude/agents/` agent: review-oriented agent tuned to this repo's conventions from `CLAUDE.md`
      (spec adherence, ADR triggers, both testing layers present)
- [ ] Document the available skills/agents and when to use them in `CONTRIBUTING.md`

## P14 — Release & CI hardening

- [ ] Snapshot publishing workflow on every `main` commit (spec §2 distribution)
- [ ] Tagged-release publishing workflow for `cloud-minestom` + `cloud-minestom-bom` to Maven Central
- [ ] Dependency-update automation (Renovate or Dependabot) tracking Minestom/Cloud version bumps,
      with a reminder to update `docs/compatibility.md` in the same PR
- [ ] `CHANGELOG.md` seeded, Conventional-Commits-driven per `CONTRIBUTING.md`
- [ ] 1.0.0 release checklist: every phase above checked, `./gradlew build` green, `minestom-demo`
      manually run end to end
