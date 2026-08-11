package gg.cubix.cloudminestom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

@EnvTest
class MinestomCommandManagerEnvTest {

    @Test
    void registeredCommandSendsMessageToPlayer(final Env env) {
        final var instance = env.createFlatInstance();
        final var connection = env.createConnection();
        final var player = connection.connect(instance, new Pos(0, 42, 0));

        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        manager.command(manager.commandBuilder("greet")
                .handler(context -> context.sender().sendMessage(Component.text("hello")))
        );

        final var collector = connection.trackIncoming(SystemChatPacket.class);
        env.process().command().execute(player, "greet");

        collector.assertSingle(packet -> assertEquals(Component.text("hello"), packet.message()));
    }
}
