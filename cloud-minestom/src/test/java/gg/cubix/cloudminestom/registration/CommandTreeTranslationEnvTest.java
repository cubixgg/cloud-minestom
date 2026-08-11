package gg.cubix.cloudminestom.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gg.cubix.cloudminestom.MinestomCommandManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.play.TabCompletePacket;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import net.minestom.testing.TestConnection;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.junit.jupiter.api.Test;

@EnvTest
class CommandTreeTranslationEnvTest {

    @Test
    void boundedIntegerArgumentIsValidatedByCloudNotMinestom(final Env env) {
        final var instance = env.createFlatInstance();
        final Player player = env.createConnection().connect(instance, new Pos(0, 42, 0));

        final AtomicInteger observed = new AtomicInteger(-1);
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        manager.command(manager.commandBuilder("bounded")
                .required("num", IntegerParser.integerParser(1, 10))
                .handler(context -> observed.set(context.get("num"))));

        env.process().command().execute(player, "bounded 5");
        assertEquals(5, observed.get());

        observed.set(-1);
        env.process().command().execute(player, "bounded 999");
        // Cloud's own IntegerParser rejects it - the handler never runs (spec.md §5.4: Cloud is the
        // only parser that matters, native shape is never a second source of truth).
        assertEquals(-1, observed.get());
    }

    @Test
    void suggestionsForAMappedNodeReturnCloudsSuggestions(final Env env) {
        final var instance = env.createFlatInstance();
        final TestConnection connection = env.createConnection();
        final Player player = connection.connect(instance, new Pos(0, 42, 0));

        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        manager.command(manager.commandBuilder("greet")
                .required("name", StringParser.stringParser(), SuggestionProvider.suggestingStrings("alice", "alex", "bob"))
                .handler(context -> { }));

        final var listener = connection.trackIncoming(TabCompletePacket.class);
        player.addPacketToQueue(new ClientTabCompletePacket(1, "greet al"));
        player.interpretPacketQueue();

        listener.assertSingle(packet -> {
            final List<String> suggestions = packet.matches().stream().map(TabCompletePacket.Match::match).toList();
            assertTrue(suggestions.containsAll(List.of("alice", "alex")));
            assertTrue(suggestions.stream().noneMatch("bob"::equals));
        });
    }
}
