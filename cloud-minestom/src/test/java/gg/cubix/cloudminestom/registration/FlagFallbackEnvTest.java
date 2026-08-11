package gg.cubix.cloudminestom.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gg.cubix.cloudminestom.MinestomCommandManager;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.junit.jupiter.api.Test;

/**
 * @EnvTest for spec.md §5.5's documented flag fallback: a flagged command still executes and
 * parses correctly end to end, entirely through Cloud, even though the native shape degrades to a
 * single trailing greedy argument for the flag subtree (docs/roadmap.md P4).
 */
@EnvTest
class FlagFallbackEnvTest {

    @Test
    void presenceAndValueFlagsAreParsedCorrectlyThroughTheFallbackPath(final Env env) {
        final var instance = env.createFlatInstance();
        final Player player = env.createConnection().connect(instance, new Pos(0, 42, 0));

        final AtomicBoolean verbose = new AtomicBoolean(false);
        final AtomicInteger count = new AtomicInteger(-1);
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        manager.command(manager.commandBuilder("test")
                .required("name", StringParser.stringParser())
                .flag(CommandFlag.<CommandSender>builder("verbose").build())
                .flag(CommandFlag.<CommandSender>builder("count").withComponent(IntegerParser.integerParser()).build())
                .handler(context -> {
                    verbose.set(context.flags().isPresent("verbose"));
                    count.set(context.flags().<Integer>get("count"));
                }));

        env.process().command().execute(player, "test alice --verbose --count 5");

        assertTrue(verbose.get());
        assertEquals(5, count.get());
    }
}
