---
name: scaffold-argument-mapper
description: Scaffold a new built-in ArgumentMapper for a Cloud parser type in cloud-minestom - the mapping method, its registration, and a unit test stub. Use when adding a new row to the argument-mapping table (docs/spec.md §5.2), e.g. "add a mapper for X" or "map Y to a native argument".
---

# Scaffold an `ArgumentMapper`

Adds a new built-in Cloud-parser → native-Minestom-`Argument` mapping, following the exact pattern
every existing mapper in `StandardArgumentMappers` already uses (see `mapUuid`/`mapEnum`/`mapPlayer`
for reference before writing the new one - don't invent a different shape).

## Before you start

1. Confirm there's a real, unambiguous native Minestom `Argument` type for this parser. If there
   isn't a sane one, the parser should stay on the `Word`/`String`/`StringArray` fallback
   (`CommandTreeTranslator`'s own logic already does this automatically for anything with no
   registered mapper) - do **not** force a mapping that doesn't actually correspond to the parser's
   semantics. `CLAUDE.md`'s anti-patterns section calls this out explicitly: a mapping that looks
   right but silently mismatches the parser's actual grammar is worse than the honest fallback.
2. If the native shape only approximates the parser's grammar (the way `DurationParser`/`Time`
   does), that's fine — but it must be called out in a comment on the mapper method and in
   `docs/argument-mapping.md`'s "Known mismatches" section, not silently coerced.

## Steps

1. **The mapper method** — in `cloud-minestom/src/main/java/gg/cubix/cloudminestom/argument/StandardArgumentMappers.java`:
   - Add a `private static Argument<?> mapXxx(final CommandComponent<?> component, final ArgumentParser<?, T> parser)`
     method, `T` being the parser's actual value type.
   - Cast `parser` to the concrete parser class if you need its configuration (bounds, mode, ...) —
     every existing mapper does this (see `mapInteger`'s `(IntegerParser<?>) parser` cast).
   - Return the native `Argument<?>`, built via `ArgumentType.Xxx(component.name())`.
   - Add a one-line `//` comment only if there's a non-obvious *why* (a grammar mismatch, a
     Minestom-native quirk) — not a comment that just restates what the code does (`CLAUDE.md`).
2. **Registration** — add `registry.register(XxxParser.class, StandardArgumentMappers::mapXxx);` to
   `registerAll(...)`, in the same class.
3. **Unit test stub** — in `cloud-minestom/src/test/java/gg/cubix/cloudminestom/argument/StandardArgumentMappersTest.java`:
   - Add a `@Test` method asserting the produced `Argument` is the expected type (and, if the parser
     has options like bounds, that they made it onto the native argument) — follow the existing
     `integerParserMapsToIntegerHonoringBounds`-style pattern.
4. **Update `docs/argument-mapping.md`'s table** in the same commit — it's meant to be kept in sync
   with `StandardArgumentMappers`'s actual registered set, not left to drift.
5. **Update `docs/spec.md` §5.2's table too**, with a short note in `docs/roadmap.md` if this reveals
   a gap the spec didn't originally anticipate (see the `PlayerParser` mapper's own roadmap entry,
   P8, for the precedent on how that correction was written up).

## After scaffolding

This only covers the **unit** testing layer. If the new mapper is for a parser real projects will
actually use (not just an internal detail), also add an `@EnvTest` proving the native shape reaches a
real client correctly — see the `verify-roadmap-item` skill for the full round-trip checklist before
anything gets checked off in `docs/roadmap.md`.

## Don't

- Don't add a mapper "just in case" a parser might need one later — every mapper should trace back to
  a roadmap item or a real consumer need (`CLAUDE.md`'s anti-patterns section).
- Don't read the native argument's parsed value anywhere, including in the mapper itself — mappers
  only ever produce shape (`docs/spec.md` §5.4, [ADR-0001](../../docs/decisions/0001-cloud-is-the-single-source-of-truth-for-parsing.md)).
