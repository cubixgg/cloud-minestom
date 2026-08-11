package gg.cubix.cloudminestom.argument;

import java.time.Duration;
import java.util.UUID;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.arguments.number.ArgumentLong;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.standard.BooleanParser;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.DurationParser;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.LongParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.parser.standard.UUIDParser;

/**
 * The built-in {@link ArgumentMapper}s shipped by {@link ArgumentMapperRegistry#createDefault()}
 * (docs/spec.md §5.2's mapping table).
 */
final class StandardArgumentMappers {

    private StandardArgumentMappers() {
    }

    static void registerAll(final ArgumentMapperRegistry registry) {
        registry.register(StringParser.class, StandardArgumentMappers::mapString);
        registry.register(BooleanParser.class, StandardArgumentMappers::mapBoolean);
        registry.register(IntegerParser.class, StandardArgumentMappers::mapInteger);
        registry.register(LongParser.class, StandardArgumentMappers::mapLong);
        registry.register(FloatParser.class, StandardArgumentMappers::mapFloat);
        registry.register(DoubleParser.class, StandardArgumentMappers::mapDouble);
        registry.register(UUIDParser.class, StandardArgumentMappers::mapUuid);
        registry.register(EnumParser.class, StandardArgumentMappers::mapEnum);
        registry.register(DurationParser.class, StandardArgumentMappers::mapDuration);
    }

    private static Argument<?> mapString(final CommandComponent<?> component, final ArgumentParser<?, String> parser) {
        final StringParser<?> stringParser = (StringParser<?>) parser;
        return switch (stringParser.stringMode()) {
            case SINGLE -> ArgumentType.Word(component.name());
            case QUOTED -> ArgumentType.String(component.name());
            // GREEDY_FLAG_YIELDING has no dedicated row in spec §5.2's table; it consumes the rest of
            // the input just like GREEDY, so it gets the same native shape.
            case GREEDY, GREEDY_FLAG_YIELDING -> ArgumentType.StringArray(component.name());
        };
    }

    private static Argument<?> mapBoolean(final CommandComponent<?> component, final ArgumentParser<?, Boolean> parser) {
        return ArgumentType.Boolean(component.name());
    }

    private static Argument<?> mapInteger(final CommandComponent<?> component, final ArgumentParser<?, Integer> parser) {
        final IntegerParser<?> integerParser = (IntegerParser<?>) parser;
        final ArgumentInteger argument = ArgumentType.Integer(component.name());
        if (integerParser.hasMin()) {
            argument.min(integerParser.range().minInt());
        }
        if (integerParser.hasMax()) {
            argument.max(integerParser.range().maxInt());
        }
        return argument;
    }

    private static Argument<?> mapLong(final CommandComponent<?> component, final ArgumentParser<?, Long> parser) {
        final LongParser<?> longParser = (LongParser<?>) parser;
        final ArgumentLong argument = ArgumentType.Long(component.name());
        if (longParser.hasMin()) {
            argument.min(longParser.range().minLong());
        }
        if (longParser.hasMax()) {
            argument.max(longParser.range().maxLong());
        }
        return argument;
    }

    private static Argument<?> mapFloat(final CommandComponent<?> component, final ArgumentParser<?, Float> parser) {
        final FloatParser<?> floatParser = (FloatParser<?>) parser;
        final ArgumentFloat argument = ArgumentType.Float(component.name());
        if (floatParser.hasMin()) {
            argument.min(floatParser.range().minFloat());
        }
        if (floatParser.hasMax()) {
            argument.max(floatParser.range().maxFloat());
        }
        return argument;
    }

    private static Argument<?> mapDouble(final CommandComponent<?> component, final ArgumentParser<?, Double> parser) {
        final DoubleParser<?> doubleParser = (DoubleParser<?>) parser;
        final ArgumentDouble argument = ArgumentType.Double(component.name());
        if (doubleParser.hasMin()) {
            argument.min(doubleParser.range().minDouble());
        }
        if (doubleParser.hasMax()) {
            argument.max(doubleParser.range().maxDouble());
        }
        return argument;
    }

    private static Argument<?> mapUuid(final CommandComponent<?> component, final ArgumentParser<?, UUID> parser) {
        return ArgumentType.UUID(component.name());
    }

    private static Argument<?> mapEnum(final CommandComponent<?> component, final ArgumentParser<?, ?> parser) {
        // Minestom has no generic native enum node (spec §5.2/§15) - Word shape, with Cloud's own
        // suggestions (every mapped node gets those regardless, see CloudSuggestionCallback) standing
        // in for restricted-choice validation instead of a second, native source of truth.
        return ArgumentType.Word(component.name());
    }

    private static Argument<?> mapDuration(final CommandComponent<?> component, final ArgumentParser<?, Duration> parser) {
        // Documented mismatch (spec §5.2): Cloud's grammar accepts multiple combined units
        // (e.g. "2d15h7m12s"), Minestom's ArgumentTime accepts one number plus a single optional
        // unit suffix from {d, s, t} - no hours/minutes, but a tick unit Cloud has no equivalent for.
        // Native shape only; Cloud remains the actual parser (spec §5.4), so this is a client-side
        // coloring quirk, not a functional gap - not silently coerced into matching either grammar.
        return ArgumentType.Time(component.name());
    }
}
