package gg.cubix.cloudminestom.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gg.cubix.cloudminestom.MinestomCommandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import org.incendo.cloud.execution.CommandExecutionHandler;
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

    private static CommandExecutionHandler<CommandSender> noOpHandler() {
        return context -> { };
    }
}
