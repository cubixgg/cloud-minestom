package gg.cubix.cloudminestom.demo;

import gg.cubix.cloudminestom.MinestomCommandManager;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

/**
 * Registers every demo command against a shared manager, one roadmap item at a time
 * (docs/roadmap.md P11, spec.md §13).
 */
final class DemoCommands {

    private DemoCommands() {
    }

    /**
     * {@code /demo roll <sides> [modifier]} - a builder-declared literal subcommand tree with a
     * bounded {@link IntegerParser} argument, an optional argument, and Cloud-suggested string
     * choices, to show native node shape and Cloud suggestions together.
     *
     * @param manager the manager to register against
     */
    static void registerRoll(final MinestomCommandManager<CommandSender> manager) {
        manager.command(manager.commandBuilder("demo")
                .literal("roll")
                .required("sides", IntegerParser.integerParser(2, 100))
                .optional(
                        "modifier",
                        StringParser.stringParser(),
                        SuggestionProvider.suggestingStrings("normal", "advantage", "disadvantage")
                )
                .handler(context -> context.sender().sendMessage(
                        rollMessage(context.get("sides"), context.getOrDefault("modifier", "normal")))));
    }

    /**
     * Shared by both the builder-declared {@link #registerRoll} and the annotation-declared
     * {@code AnnotatedRollCommand}, so the two commands demonstrate parity of shape/suggestions
     * without duplicating the actual roll logic between them.
     *
     * @param sides    the die's side count
     * @param modifier {@code "normal"}, {@code "advantage"} (best of two) or {@code "disadvantage"}
     *                 (worst of two)
     * @return the feedback message
     */
    static Component rollMessage(final int sides, final String modifier) {
        final int first = ThreadLocalRandom.current().nextInt(sides) + 1;
        final int result = switch (modifier) {
            case "advantage" -> Math.max(first, ThreadLocalRandom.current().nextInt(sides) + 1);
            case "disadvantage" -> Math.min(first, ThreadLocalRandom.current().nextInt(sides) + 1);
            default -> first;
        };
        return Component.text("Rolled a d" + sides + " (" + modifier + "): " + result);
    }
}
