package gg.cubix.cloudminestom.demo;

import gg.cubix.cloudminestom.MinestomCommandManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.incendo.cloud.annotations.AnnotationParser;

/**
 * Boots a flat-world {@link MinecraftServer} and registers a {@link MinestomCommandManager} against it
 * (docs/roadmap.md P11, spec.md §13) - the acceptance test for "full implementation, not partial": if
 * a feature can't be shown working here, it isn't done.
 */
public final class Main {

    private Main() {
    }

    /**
     * Starts the demo server on {@code 0.0.0.0:25565}.
     *
     * @param args unused
     */
    public static void main(final String[] args) {
        final MinecraftServer server = MinecraftServer.init();

        final Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 42, 0));
        });

        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        DemoCommands.registerRoll(manager);
        DemoCommands.registerTarget(manager);
        DemoCommands.registerAnnounce(manager);

        final AnnotationParser<CommandSender> annotations = new AnnotationParser<>(manager, CommandSender.class);
        annotations.parse(new AnnotatedRollCommand());
        // Further demo commands are registered here, one roadmap item (and commit) at a time - see
        // docs/roadmap.md P11.

        server.start("0.0.0.0", 25565);
    }
}
