package gg.cubix.cloudminestom.argument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentBoolean;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentTime;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentUUID;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.arguments.number.ArgumentLong;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.standard.BooleanParser;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.DurationParser;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.LongParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.parser.standard.UUIDParser;
import org.junit.jupiter.api.Test;

class StandardArgumentMappersTest {

    private final ArgumentMapperRegistry registry = ArgumentMapperRegistry.createDefault();

    @Test
    void singleModeStringParserMapsToWord() {
        final Argument<?> mapped = map("word", StringParser.stringParser());
        assertInstanceOf(ArgumentWord.class, mapped);
    }

    @Test
    void quotedModeStringParserMapsToString() {
        final Argument<?> mapped = map("quoted", StringParser.quotedStringParser());
        assertInstanceOf(ArgumentString.class, mapped);
    }

    @Test
    void greedyModeStringParserMapsToStringArray() {
        final Argument<?> mapped = map("greedy", StringParser.greedyStringParser());
        assertInstanceOf(ArgumentStringArray.class, mapped);
    }

    @Test
    void booleanParserMapsToBoolean() {
        final Argument<?> mapped = map("flag", BooleanParser.booleanParser());
        assertInstanceOf(ArgumentBoolean.class, mapped);
    }

    @Test
    void integerParserMapsToIntegerHonoringBounds() {
        final Argument<?> mapped = map("num", IntegerParser.integerParser(1, 10));
        final ArgumentInteger argument = assertInstanceOf(ArgumentInteger.class, mapped);
        assertTrue(argument.hasMin());
        assertEquals(1, argument.getMin());
        assertTrue(argument.hasMax());
        assertEquals(10, argument.getMax());
    }

    @Test
    void integerParserWithoutBoundsProducesUnboundedArgument() {
        final Argument<?> mapped = map("num", IntegerParser.integerParser());
        final ArgumentInteger argument = assertInstanceOf(ArgumentInteger.class, mapped);
        assertFalse(argument.hasMin());
        assertFalse(argument.hasMax());
    }

    @Test
    void longParserMapsToLongHonoringBounds() {
        final Argument<?> mapped = map("num", LongParser.longParser(1L, 10L));
        final ArgumentLong argument = assertInstanceOf(ArgumentLong.class, mapped);
        assertTrue(argument.hasMin());
        assertEquals(1L, argument.getMin());
        assertTrue(argument.hasMax());
        assertEquals(10L, argument.getMax());
    }

    @Test
    void floatParserMapsToFloatHonoringBounds() {
        final Argument<?> mapped = map("num", FloatParser.floatParser(1.5f, 10.5f));
        final ArgumentFloat argument = assertInstanceOf(ArgumentFloat.class, mapped);
        assertTrue(argument.hasMin());
        assertEquals(1.5f, argument.getMin());
        assertTrue(argument.hasMax());
        assertEquals(10.5f, argument.getMax());
    }

    @Test
    void doubleParserMapsToDoubleHonoringBounds() {
        final Argument<?> mapped = map("num", DoubleParser.doubleParser(1.5d, 10.5d));
        final ArgumentDouble argument = assertInstanceOf(ArgumentDouble.class, mapped);
        assertTrue(argument.hasMin());
        assertEquals(1.5d, argument.getMin());
        assertTrue(argument.hasMax());
        assertEquals(10.5d, argument.getMax());
    }

    @Test
    void uuidParserMapsToUuid() {
        final Argument<?> mapped = map("id", UUIDParser.uuidParser());
        assertInstanceOf(ArgumentUUID.class, mapped);
    }

    @Test
    void enumParserMapsToWordSinceMinestomHasNoNativeEnumNode() {
        final Argument<?> mapped = map("choice", EnumParser.enumParser(Fruit.class));
        assertInstanceOf(ArgumentWord.class, mapped);
    }

    @Test
    void durationParserMapsToTimeDespiteTheGrammarMismatch() {
        // Documented mismatch (spec §5.2): Cloud's "2d15h7m12s" grammar vs. Minestom's Time argument
        // (one number + a single d/s/t suffix). Native shape only - Cloud remains the real parser.
        final Argument<?> mapped = map("duration", DurationParser.durationParser());
        assertInstanceOf(ArgumentTime.class, mapped);
    }

    private <T> Argument<?> map(final String name, final ParserDescriptor<Object, T> parser) {
        final CommandComponent<Object> component = CommandComponent.<Object, T>builder()
                .name(name)
                .parser(parser)
                .build();
        return this.registry.map(component).orElseThrow();
    }

    private enum Fruit {
        APPLE, BANANA
    }
}
