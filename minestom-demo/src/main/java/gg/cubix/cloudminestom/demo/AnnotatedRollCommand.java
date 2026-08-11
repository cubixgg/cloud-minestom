package gg.cubix.cloudminestom.demo;

import java.util.List;
import net.minestom.server.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;

/**
 * {@code /demo roll-annotated <sides> [modifier]} - the same shape as {@link DemoCommands#registerRoll}
 * (bounded numeric argument, optional argument, Cloud-suggested string choices), re-declared via
 * {@code @Command} annotations to show parity between the two styles side by side (docs/roadmap.md
 * P11, spec.md §10/§13). A distinct literal from the builder-declared version - two commands can't
 * both claim the same {@code demo roll} syntax on one manager.
 */
public final class AnnotatedRollCommand {

    @Command("demo roll-annotated <sides> [modifier]")
    public void roll(
            final CommandSender sender,
            final @Argument(value = "sides", parserName = "bounded-die-sides") int sides,
            final @Argument(value = "modifier", suggestions = "roll-modifier") @Default("normal") String modifier
    ) {
        sender.sendMessage(DemoCommands.rollMessage(sides, modifier));
    }

    /**
     * Cloud-annotations has no terse numeric-range specifier annotation in this version (unlike the
     * builder's {@code IntegerParser.integerParser(2, 100)}), so the bound is expressed as a named
     * {@code @Parser} method instead - itself another annotations-specific feature worth
     * demonstrating, not a workaround.
     */
    @Parser(name = "bounded-die-sides")
    public int diesSidesParser(final CommandInput input) {
        final int value = Integer.parseInt(input.readString());
        if (value < 2 || value > 100) {
            throw new IllegalArgumentException("Must be between 2 and 100 (was " + value + ")");
        }
        return value;
    }

    @Suggestions("roll-modifier")
    public List<String> modifierSuggestions(final CommandContext<CommandSender> context) {
        return List.of("normal", "advantage", "disadvantage");
    }
}
