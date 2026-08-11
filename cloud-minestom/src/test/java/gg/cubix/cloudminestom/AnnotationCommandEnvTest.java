package gg.cubix.cloudminestom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.annotations.Command;
import org.junit.jupiter.api.Test;

/**
 * @EnvTest for docs/roadmap.md P9 (spec.md §10): an {@code @Command}-annotated method registers and
 * executes correctly through the same manager as a builder-declared command - proving annotation- and
 * builder-declared commands are indistinguishable once registered, not just that each style works in
 * isolation.
 */
@EnvTest
class AnnotationCommandEnvTest {

    @Test
    void annotatedCommandRegistersAndExecutesAlongsideABuilderDeclaredOne(final Env env) {
        final var instance = env.createFlatInstance();
        final var connection = env.createConnection();
        final var player = connection.connect(instance, new Pos(0, 42, 0));

        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        manager.command(manager.commandBuilder("builder-greet")
                .handler(context -> context.sender().sendMessage(Component.text("hello from builder"))));

        final AnnotationParser<CommandSender> annotations = new AnnotationParser<>(manager, CommandSender.class);
        annotations.parse(new GreetCommand());

        final var listener = connection.trackIncoming(SystemChatPacket.class);

        env.process().command().execute(player, "builder-greet");
        env.process().command().execute(player, "annotation-greet World");

        listener.assertCount(2);
        final var messages = listener.collect().stream().map(SystemChatPacket::message).toList();
        assertEquals(Component.text("hello from builder"), messages.get(0));
        assertEquals(Component.text("hello, World"), messages.get(1));
    }

    public static final class GreetCommand {

        @Command("annotation-greet <target>")
        public void greet(final CommandSender sender, final @Argument("target") String target) {
            sender.sendMessage(Component.text("hello, " + target));
        }
    }
}
