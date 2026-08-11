package gg.cubix.cloudminestom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.play.TabCompletePacket;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import net.minestom.testing.TestConnection;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.junit.jupiter.api.Test;

/**
 * @EnvTest for docs/roadmap.md P9 (spec.md §10): {@code @Suggestions}-provided suggestions surface
 * correctly through the same {@code CloudSuggestionCallback} bridge every mapped argument uses
 * (spec.md §5.3) - nothing about the suggestion path changes because the command was
 * annotation-declared.
 */
@EnvTest
class AnnotationSuggestionsEnvTest {

    @Test
    void suggestionsAnnotatedMethodSurfacesThroughTheSuggestionBridge(final Env env) {
        final var instance = env.createFlatInstance();
        final TestConnection connection = env.createConnection();
        final Player player = connection.connect(instance, new Pos(0, 42, 0));

        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        final AnnotationParser<CommandSender> annotations = new AnnotationParser<>(manager, CommandSender.class);
        annotations.parse(new GreetCommand());

        final var listener = connection.trackIncoming(TabCompletePacket.class);
        player.addPacketToQueue(new ClientTabCompletePacket(1, "greet "));
        player.interpretPacketQueue();

        listener.assertSingle(packet -> {
            final List<String> suggestions = packet.matches().stream().map(TabCompletePacket.Match::match).toList();
            assertEquals(List.of("hello", "hey", "hi"), suggestions.stream().sorted().toList());
        });
    }

    public static final class GreetCommand {

        @Command("greet <greeting>")
        public void greet(final CommandSender sender, final @Argument(value = "greeting", suggestions = "greeting") String greeting) {
        }

        @Suggestions("greeting")
        public List<String> greetingSuggestions(final CommandContext<CommandSender> context) {
            return List.of("hi", "hello", "hey");
        }
    }
}
