package gg.cubix.cloudminestom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.junit.jupiter.api.Test;

/**
 * @EnvTest for docs/roadmap.md P9 (spec.md §10): {@code @Permission} on an annotated command is
 * enforced through the same permission function configured on the manager (spec.md §6, P5) - there is
 * no separate annotation-driven permission system.
 */
@EnvTest
class AnnotationPermissionEnvTest {

    @Test
    void permissionAnnotationIsEnforcedThroughTheConfiguredPermissionFunction(final Env env) {
        final var instance = env.createFlatInstance();
        final Player allowedPlayer = env.createConnection(new GameProfile(UUID.randomUUID(), "Allowed"))
                .connect(instance, new Pos(0, 42, 0));
        final Player lackingPlayer = env.createConnection(new GameProfile(UUID.randomUUID(), "Lacking"))
                .connect(instance, new Pos(0, 42, 0));

        final AtomicBoolean executed = new AtomicBoolean(false);
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .permissionFunction((sender, permission) ->
                        permission.isEmpty() || (sender instanceof Player player && player.getUsername().equals("Allowed")))
                .build();

        final AnnotationParser<CommandSender> annotations = new AnnotationParser<>(manager, CommandSender.class);
        annotations.parse(new AdminCommand(executed));

        env.process().command().execute(lackingPlayer, "admin-only");
        assertFalse(executed.get());

        env.process().command().execute(allowedPlayer, "admin-only");
        assertTrue(executed.get());
    }

    public static final class AdminCommand {

        private final AtomicBoolean executed;

        public AdminCommand(final AtomicBoolean executed) {
            this.executed = executed;
        }

        @Command("admin-only")
        @Permission("admin")
        public void run(final CommandSender sender) {
            this.executed.set(true);
        }
    }
}
