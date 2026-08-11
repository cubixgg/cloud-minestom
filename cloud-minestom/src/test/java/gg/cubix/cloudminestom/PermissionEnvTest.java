package gg.cubix.cloudminestom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.play.TabCompletePacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

/**
 * @EnvTest for spec.md §6 (as corrected in docs/roadmap.md P5): the default permission function
 * never denies (the pinned Minestom version has no native permission-node system), but a
 * consumer-supplied one is fully wired through to both suggestions and execution.
 */
@EnvTest
class PermissionEnvTest {

    @Test
    void permissionGatedSubcommandIsHiddenFromSuggestionsAndRejectedOnExecutionForALackingPlayer(final Env env) {
        final var instance = env.createFlatInstance();
        final Player allowedPlayer = env.createConnection(new GameProfile(UUID.randomUUID(), "Allowed"))
                .connect(instance, new Pos(0, 42, 0));
        final var lackingConnection = env.createConnection(new GameProfile(UUID.randomUUID(), "Lacking"));
        final Player lackingPlayer = lackingConnection.connect(instance, new Pos(0, 42, 0));

        final AtomicBoolean executed = new AtomicBoolean(false);
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .permissionFunction((sender, permission) ->
                        permission.isEmpty() || (sender instanceof Player player && player.getUsername().equals("Allowed")))
                .build();
        manager.command(manager.commandBuilder("test").literal("public").handler(context -> { }));
        manager.command(manager.commandBuilder("test")
                .literal("secret")
                .permission("admin")
                .handler(context -> executed.set(true)));

        final var listener = lackingConnection.trackIncoming(TabCompletePacket.class);
        lackingPlayer.addPacketToQueue(new ClientTabCompletePacket(1, "test "));
        lackingPlayer.interpretPacketQueue();
        listener.assertSingle(packet -> {
            final List<String> suggestions = packet.matches().stream().map(TabCompletePacket.Match::match).toList();
            assertTrue(suggestions.contains("public"));
            assertFalse(suggestions.contains("secret"));
        });

        env.process().command().execute(lackingPlayer, "test secret");
        assertFalse(executed.get());

        env.process().command().execute(allowedPlayer, "test secret");
        assertTrue(executed.get());
    }
}
