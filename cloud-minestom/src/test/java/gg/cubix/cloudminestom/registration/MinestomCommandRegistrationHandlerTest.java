package gg.cubix.cloudminestom.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gg.cubix.cloudminestom.MinestomCommandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandSyntax;
import org.incendo.cloud.execution.CommandExecutionHandler;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.junit.jupiter.api.Test;

class MinestomCommandRegistrationHandlerTest {

    @Test
    void registeringCommandProducesOneNativeCommandWithNameAndAliases() {
        final List<Command> sink = new ArrayList<>();
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .commandRegistrationCallback(sink::add)
                .build();

        manager.command(manager.commandBuilder("test", "t", "te").handler(noOpHandler()));

        assertEquals(1, sink.size());
        final Command nativeCommand = sink.get(0);
        assertEquals("test", nativeCommand.getName());
        // LiteralParser#alternativeAliases() is backed by a HashSet, so alias order is unspecified.
        assertEquals(Set.of("t", "te"), Set.of(nativeCommand.getAliases()));
    }

    @Test
    void twoSyntaxesUnderSameRootProduceExactlyOneNativeCommand() {
        final List<Command> sink = new ArrayList<>();
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .commandRegistrationCallback(sink::add)
                .build();

        manager.command(manager.commandBuilder("test").literal("foo").handler(noOpHandler()));
        manager.command(manager.commandBuilder("test").literal("bar").handler(noOpHandler()));

        assertEquals(1, sink.size());
        assertEquals("test", sink.get(0).getName());
    }

    @Test
    void aLeafAddedAfterTheRootWasAlreadyRegisteredStillReachesTheNativeCommand() {
        final List<Command> sink = new ArrayList<>();
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .commandRegistrationCallback(sink::add)
                .build();

        manager.command(manager.commandBuilder("test").literal("foo").handler(noOpHandler()));
        final Command afterFirst = sink.get(0);
        assertEquals(1, afterFirst.getSyntaxes().size());

        manager.command(manager.commandBuilder("test").literal("bar").handler(noOpHandler()));

        // Still exactly one registration callback invocation (MinecraftServer's own CommandManager
        // throws if register() is called twice for the same name) - the already-registered native
        // Command's syntax list is refreshed in place instead, see MinestomCommandRegistrationHandler.
        assertEquals(1, sink.size());
        assertEquals(2, afterFirst.getSyntaxes().size());
    }

    @Test
    void optionalTrailingArgumentProducesBothTheShortAndLongFormNativeSyntax() {
        final List<Command> sink = new ArrayList<>();
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .commandRegistrationCallback(sink::add)
                .build();

        manager.command(manager.commandBuilder("test")
                .optional("name", StringParser.stringParser())
                .handler(noOpHandler()));

        assertEquals(2, sink.get(0).getSyntaxes().size());
    }

    @Test
    void mappedSyntaxExecutorReJoinsRawInputAndDispatchesThroughCloud() {
        final List<Command> sink = new ArrayList<>();
        final AtomicInteger observed = new AtomicInteger(-1);
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .commandRegistrationCallback(sink::add)
                .build();

        manager.command(manager.commandBuilder("test")
                .required("num", IntegerParser.integerParser())
                .handler(context -> observed.set(context.get("num"))));

        final CommandSyntax syntax = sink.get(0).getSyntaxes().iterator().next();
        // Deliberately an empty context - Minestom's own parsed argument values are never populated
        // nor read; only getInput() (re-joined and re-parsed by Cloud) drives the handler.
        final CommandContext minestomContext = new CommandContext("test 42");

        syntax.getExecutor().apply(new ConsoleSender(), minestomContext);

        assertEquals(42, observed.get());
    }

    private static CommandExecutionHandler<CommandSender> noOpHandler() {
        return context -> { };
    }
}
