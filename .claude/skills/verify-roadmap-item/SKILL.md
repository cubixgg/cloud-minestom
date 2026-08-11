---
name: verify-roadmap-item
description: Verify a docs/roadmap.md item's command/feature actually round-trips through both the unit-test and @EnvTest layers before checking its box off. Use before checking a roadmap checkbox, before committing a feature that touches registration/parsing/suggestions/permissions/execution, or when asked "is this roadmap item actually done".
---

# Verify a roadmap item before checking it off

`CONTRIBUTING.md` is explicit: a roadmap item "isn't done because it compiles - it's done when both
required testing layers pass... and, where the item calls for it, `minestom-demo` actually
demonstrates the behavior." This skill is the checklist that keeps that honest.

## The two layers, and when each applies

Per `CLAUDE.md`'s Testing section, **anything that touches registration, parsing, suggestions,
permissions or execution** needs both layers — neither substitutes for the other:

1. **Unit test** — a fake registration sink (`Consumer<net.minestom.server.command.builder.Command>`,
   e.g. `list::add`) or hand-built Cloud objects (`CommandContext`, `CommandInput`, a fake
   `CommandSender`), no running server. Tests `CommandTreeTranslator`, `ArgumentMapperRegistry`,
   `CloudSuggestionCallback`, default permission/exception wiring, or a parser's own `parse`/suggestion
   logic in isolation.
2. **`@EnvTest`** — `net.minestom:testing`'s real (headless, virtual-player) server. Proves the whole
   stack: registration → native parse shape → Cloud re-parse → permission check → handler → feedback,
   actually reaching a connected `Player`.

A roadmap item phrased as `@EnvTest` in `roadmap.md` needs one, full stop, not just a unit test around
the pieces — that phrasing is itself a signal the item is testing an end-to-end path.

## Checklist

1. **Read the exact roadmap item wording.** It usually tells you which layer(s) it expects — an item
   phrased as "Unit test: ..." vs. "`@EnvTest`: ...". Don't downgrade an `@EnvTest` item to a unit test
   because it's faster to write.
2. **Run the actual layer(s), don't assume.** `./gradlew test --tests "..."` for the specific new
   test(s), then a full `./gradlew build` before considering the item done — a passing new test with a
   broken existing one isn't done.
3. **For an `@EnvTest`, check what actually reached the client**, not just that no exception was
   thrown — collect the relevant packet type (`SystemChatPacket` for messages,
   `TabCompletePacket`/`ClientTabCompletePacket` for suggestions) and assert on its real content. A
   test that only asserts "didn't crash" can pass while the feature is silently broken.
4. **Watch for `net.minestom.testing`'s `Collector#collect()` footgun**: calling `.collect()` (or
   `.assertCount(...)`/any other `Collector` method that calls it internally) more than once on the
   same tracker silently unsubscribes it from further packets after the first call. Debug prints that
   call `.collect()` mid-test will make later assertions see fewer packets than actually arrived, which
   looks exactly like a registration or exception-handling bug. If a test behaves strangely across
   multiple sequential `execute(...)` calls, check this before suspecting the library.
5. **If this is a `minestom-demo` command**, manually verify it too (`./gradlew :minestom-demo:run`,
   or a throwaway `@EnvTest` in a temporarily-added test source set — see any `minestom-demo` commit
   from `docs/roadmap.md` P11 for the pattern: add `net.minestom:testing` as a temporary test
   dependency, write the check, verify, then revert the temporary test scaffolding since
   `minestom-demo` has no permanent test suite by design, per `spec.md` §13).
6. **Only then** check the box in `docs/roadmap.md`, in the same commit as the implementation
   (`CONTRIBUTING.md`) — never ahead of actually running the verification above.

## Red flags that mean "not actually done yet"

- A unit test exists for something the roadmap phrased as `@EnvTest`.
- An `@EnvTest` exists but only checks `CommandResult.Type` or "no exception thrown," not the real
  packet content a client would see.
- A test was written but never actually run (no `./gradlew test`/`build` output to point to).
- The box is checked but `./gradlew build` hasn't been re-run since the last change.
