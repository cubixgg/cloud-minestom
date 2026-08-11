package gg.cubix.cloudminestom;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import net.minestom.testing.TestConnection;
import org.junit.jupiter.api.Test;

/**
 * @EnvTest for spec.md §7 (docs/roadmap.md P6): a command that throws mid-handler results in the
 * sender receiving default feedback, driven against a real embedded server.
 */
@EnvTest
class ExceptionFeedbackEnvTest {

    @Test
    void commandThatThrowsMidHandlerResultsInFeedbackToTheSender(final Env env) {
        final var instance = env.createFlatInstance();
        final TestConnection connection = env.createConnection();
        final Player player = connection.connect(instance, new Pos(0, 42, 0));

        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        manager.command(manager.commandBuilder("boom").handler(context -> {
            throw new IllegalStateException("kaboom");
        }));

        final var listener = connection.trackIncoming(SystemChatPacket.class);
        env.process().command().execute(player, "boom");

        listener.assertSingle(packet -> {
            final String feedback = PlainTextComponentSerializer.plainText().serialize(packet.message());
            assertTrue(feedback.contains("internal error"));
        });
    }
}
