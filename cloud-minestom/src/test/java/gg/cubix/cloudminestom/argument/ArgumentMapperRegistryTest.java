package gg.cubix.cloudminestom.argument;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.standard.BooleanParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.junit.jupiter.api.Test;

class ArgumentMapperRegistryTest {

    @Test
    void mapUsesTheMapperRegisteredForTheComponentsParserClass() {
        final ArgumentMapperRegistry registry = new ArgumentMapperRegistry();
        final Argument<?> fakeResult = ArgumentType.Word("fake");
        registry.register(BooleanParser.class, (component, parser) -> fakeResult);

        final CommandComponent<Object> component = CommandComponent.<Object, Boolean>builder()
                .name("flag")
                .parser(BooleanParser.booleanParser())
                .build();

        final Argument<?> mapped = registry.map(component).orElseThrow();

        assertSame(fakeResult, mapped);
    }

    @Test
    void mapIsEmptyWhenNoMapperIsRegisteredForTheParserClass() {
        final ArgumentMapperRegistry registry = new ArgumentMapperRegistry();

        final CommandComponent<Object> component = CommandComponent.<Object, String>builder()
                .name("word")
                .parser(StringParser.stringParser())
                .build();

        assertTrue(registry.map(component).isEmpty());
    }

    @Test
    void laterRegistrationForTheSameParserClassReplacesTheEarlierOne() {
        final ArgumentMapperRegistry registry = new ArgumentMapperRegistry();
        final Argument<?> first = ArgumentType.Word("first");
        final Argument<?> second = ArgumentType.Word("second");
        registry.register(BooleanParser.class, (component, parser) -> first);
        registry.register(BooleanParser.class, (component, parser) -> second);

        final CommandComponent<Object> component = CommandComponent.<Object, Boolean>builder()
                .name("flag")
                .parser(BooleanParser.booleanParser())
                .build();

        assertSame(second, registry.map(component).orElseThrow());
    }
}
