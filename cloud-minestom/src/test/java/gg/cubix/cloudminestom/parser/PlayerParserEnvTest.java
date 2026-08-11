package gg.cubix.cloudminestom.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gg.cubix.cloudminestom.MinestomCommandManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.server.network.packet.server.play.TabCompletePacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

/**
 * @EnvTest for docs/roadmap.md P8: {@link PlayerParser} resolves and suggests correctly against real
 * virtual players (exact name, selector, unknown name, and suggestions), proving the whole stack -
 * registration, native {@code ArgumentEntity} shape, Cloud re-parse, handler - not just the pure-parser
 * logic already covered by {@code PlayerParserTest}.
 */
@EnvTest
class PlayerParserEnvTest {

    @Test
    void resolvesByExactNameAndBySelfSelector(final Env env) {
        final var instance = env.createFlatInstance();
        final Player steve = env.createConnection(new GameProfile(UUID.randomUUID(), "Steve"))
                .connect(instance, new Pos(0, 42, 0));

        final AtomicReference<Player> resolved = new AtomicReference<>();
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        manager.command(manager.commandBuilder("target")
                .required("target", PlayerParser.playerParser(sender -> sender, MinecraftServer.getConnectionManager()::getOnlinePlayers))
                .handler(context -> resolved.set(context.get("target"))));

        env.process().command().execute(steve, "target Steve");
        assertEquals(steve, resolved.get());

        resolved.set(null);
        env.process().command().execute(steve, "target @s");
        assertEquals(steve, resolved.get());
    }

    @Test
    void rejectsAnUnknownNameWithFeedbackAndSuggestsOnlyOnlinePlayers(final Env env) {
        final var instance = env.createFlatInstance();
        final var connection = env.createConnection(new GameProfile(UUID.randomUUID(), "Steve"));
        final Player steve = connection.connect(instance, new Pos(0, 42, 0));

        final AtomicReference<Player> resolved = new AtomicReference<>();
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        manager.command(manager.commandBuilder("target")
                .required("target", PlayerParser.playerParser(sender -> sender, MinecraftServer.getConnectionManager()::getOnlinePlayers))
                .handler(context -> resolved.set(context.get("target"))));

        final var feedbackListener = connection.trackIncoming(SystemChatPacket.class);
        env.process().command().execute(steve, "target Ghost");
        assertNull(resolved.get());
        feedbackListener.assertAnyMatch(packet ->
                PlainTextComponentSerializer.plainText().serialize(packet.message()).contains("Ghost"));

        final var suggestionListener = connection.trackIncoming(TabCompletePacket.class);
        steve.addPacketToQueue(new ClientTabCompletePacket(1, "target "));
        steve.interpretPacketQueue();
        suggestionListener.assertSingle(packet -> {
            final List<String> suggestions = packet.matches().stream().map(TabCompletePacket.Match::match).toList();
            assertTrue(suggestions.contains("Steve"));
            assertEquals(1, suggestions.size());
        });
    }
}
