package gg.cubix.cloudminestom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.junit.jupiter.api.Test;

/**
 * @EnvTest for docs/roadmap.md P9 (spec.md §10): an annotated command using an {@code @Argument Player}
 * parameter resolves through the P8 {@link gg.cubix.cloudminestom.parser.PlayerParser}, since
 * {@code Player} is registered as its default Cloud parser on every {@code MinestomCommandManager}.
 */
@EnvTest
class AnnotationPlayerParserEnvTest {

    @Test
    void argumentPlayerParameterResolvesThroughPlayerParser(final Env env) {
        final var instance = env.createFlatInstance();
        final Player sender = env.createConnection(new GameProfile(UUID.randomUUID(), "Sender"))
                .connect(instance, new Pos(0, 42, 0));
        final Player target = env.createConnection(new GameProfile(UUID.randomUUID(), "Target"))
                .connect(instance, new Pos(0, 42, 0));

        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        final AnnotationParser<CommandSender> annotations = new AnnotationParser<>(manager, CommandSender.class);
        final AtomicReference<Player> resolved = new AtomicReference<>();
        annotations.parse(new TargetCommand(resolved));

        env.process().command().execute(sender, "target Target");

        assertEquals(target, resolved.get());
    }

    public static final class TargetCommand {

        private final AtomicReference<Player> resolved;

        public TargetCommand(final AtomicReference<Player> resolved) {
            this.resolved = resolved;
        }

        @Command("target <target>")
        public void target(final CommandSender sender, final @Argument("target") Player target) {
            this.resolved.set(target);
        }
    }
}
