package gg.cubix.cloudminestom.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gg.cubix.cloudminestom.argument.ArgumentMapperRegistry;
import java.util.List;
import java.util.Set;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandNode;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.junit.jupiter.api.Test;

class CommandTreeTranslatorTest {

    @Test
    void multiLiteralMultiArgumentTreeTranslatesToExpectedNestedShape() {
        final TestManager manager = new TestManager();
        manager.command(manager.commandBuilder("test")
                .literal("group")
                .required("name", StringParser.stringParser())
                .handler(context -> { }));

        final CommandTreeTranslator.Result result = translate(manager, "test");

        assertEquals(1, result.tree().size());
        final CommandTreeTranslator.TranslatedNode literalNode = result.tree().get(0);
        assertInstanceOf(ArgumentLiteral.class, literalNode.argument());
        assertEquals(1, literalNode.children().size());
        assertInstanceOf(ArgumentWord.class, literalNode.children().get(0).argument());

        assertEquals(1, result.syntaxes().size());
        final Argument<?>[] syntax = result.syntaxes().get(0);
        assertEquals(2, syntax.length);
        assertInstanceOf(ArgumentLiteral.class, syntax[0]);
        assertInstanceOf(ArgumentWord.class, syntax[1]);
    }

    @Test
    void literalAliasesProduceOneNativeLiteralPerName() {
        final TestManager manager = new TestManager();
        manager.command(manager.commandBuilder("test")
                .literal("group", "g")
                .handler(context -> { }));

        final CommandTreeTranslator.Result result = translate(manager, "test");

        assertEquals(2, result.tree().size());
        final Set<String> literalIds = Set.of(
                result.tree().get(0).argument().getId(),
                result.tree().get(1).argument().getId()
        );
        assertEquals(Set.of("group", "g"), literalIds);
        assertEquals(2, result.syntaxes().size());
    }

    @Test
    void componentWithoutRegisteredMapperFallsBackByPosition() {
        final TestManager manager = new TestManager();
        manager.command(manager.commandBuilder("test")
                .required("first", StringParser.stringParser())
                .required("last", StringParser.stringParser())
                .handler(context -> { }));

        final CommandTreeTranslator.Result result = translate(manager, "test", new ArgumentMapperRegistry());

        final CommandTreeTranslator.TranslatedNode first = result.tree().get(0);
        assertInstanceOf(ArgumentWord.class, first.argument());
        final CommandTreeTranslator.TranslatedNode last = first.children().get(0);
        assertInstanceOf(ArgumentStringArray.class, last.argument());
    }

    @Test
    void customArgumentMapperIsUsedAlongsideBuiltIns() {
        final ArgumentMapperRegistry registry = ArgumentMapperRegistry.createDefault();
        final Argument<?> customResult = new ArgumentWord("custom");
        registry.register(StringParser.class, (component, parser) -> customResult);

        final TestManager manager = new TestManager();
        manager.command(manager.commandBuilder("test")
                .required("name", StringParser.stringParser())
                .required("age", IntegerParser.integerParser())
                .handler(context -> { }));

        final CommandTreeTranslator.Result result = translate(manager, "test", registry);

        assertEquals(customResult, result.tree().get(0).argument());
        assertInstanceOf(ArgumentInteger.class, result.tree().get(0).children().get(0).argument());
    }

    @Test
    void bareRootCommandProducesAnEmptySyntax() {
        final TestManager manager = new TestManager();
        manager.command(manager.commandBuilder("test").handler(context -> { }));

        final CommandTreeTranslator.Result result = translate(manager, "test");

        assertEquals(List.of(), result.tree());
        assertEquals(1, result.syntaxes().size());
        assertEquals(0, result.syntaxes().get(0).length);
    }

    @Test
    void optionalTrailingComponentIsMarkedOptionalOnTheNativeArgument() {
        final TestManager manager = new TestManager();
        manager.command(manager.commandBuilder("test")
                .optional("name", StringParser.stringParser())
                .handler(context -> { }));

        final CommandTreeTranslator.Result result = translate(manager, "test");

        assertEquals(1, result.syntaxes().size());
        final Argument<?> nameArgument = result.syntaxes().get(0)[0];
        assertTrue(nameArgument.isOptional());
    }

    private static CommandTreeTranslator.Result translate(final TestManager manager, final String rootName) {
        return translate(manager, rootName, ArgumentMapperRegistry.createDefault());
    }

    private static CommandTreeTranslator.Result translate(
            final TestManager manager,
            final String rootName,
            final ArgumentMapperRegistry registry
    ) {
        final CommandNode<CommandSender> rootNode = manager.commandTree().getNamedNode(rootName);
        return CommandTreeTranslator.translate(rootNode, registry, (sender, context, suggestion) -> { });
    }

    /**
     * A bare Cloud {@code CommandManager} (no Minestom registration wiring) - enough to build a real
     * {@code CommandNode} tree via {@code commandTree()} without needing a native registration handler
     * or a running server.
     */
    private static final class TestManager extends org.incendo.cloud.CommandManager<CommandSender> {
        TestManager() {
            super(ExecutionCoordinator.simpleCoordinator(), org.incendo.cloud.internal.CommandRegistrationHandler.nullCommandRegistrationHandler());
        }

        @Override
        public boolean hasPermission(final CommandSender sender, final String permission) {
            return true;
        }
    }
}
