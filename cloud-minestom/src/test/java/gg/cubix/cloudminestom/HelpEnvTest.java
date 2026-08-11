package gg.cubix.cloudminestom;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import net.minestom.testing.TestConnection;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.junit.jupiter.api.Test;

/**
 * @EnvTest for spec.md §8 (docs/roadmap.md P7): MinecraftHelp renders and reaches a real virtual
 * player's client without a bridging adapter.
 */
@EnvTest
class HelpEnvTest {

    @Test
    void minecraftHelpRendersAndReachesThePlayer(final Env env) {
        final var instance = env.createFlatInstance();
        final TestConnection connection = env.createConnection();
        final Player player = connection.connect(instance, new Pos(0, 42, 0));

        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        manager.command(manager.commandBuilder("demo").literal("greet").handler(context -> { }));

        final MinecraftHelp<CommandSender> help = MinecraftHelp.create("/demo help", manager, sender -> sender);
        manager.command(manager.commandBuilder("demo")
                .literal("help")
                .optional("query", StringParser.greedyStringParser())
                .handler(context -> help.queryCommands(context.getOrDefault("query", ""), context.sender())));

        final var listener = connection.trackIncoming(SystemChatPacket.class);
        env.process().command().execute(player, "demo help");

        // MinecraftHelp renders a header/footer plus one line per command, each its own packet -
        // not one combined message - so this checks that at least one line names "demo".
        listener.assertAnyMatch(packet ->
                PlainTextComponentSerializer.plainText().serialize(packet.message()).contains("demo"));
    }
}
