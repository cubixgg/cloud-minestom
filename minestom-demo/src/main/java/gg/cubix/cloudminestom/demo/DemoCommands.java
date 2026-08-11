package gg.cubix.cloudminestom.demo;

import gg.cubix.cloudminestom.MinestomCommandManager;
import gg.cubix.cloudminestom.parser.PlayerParser;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.parser.flag.CommandFlag;
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
     * {@code /demo target <player>} - a {@link PlayerParser}-based "target a player" command
     * (spec.md §9/§13): resolves a currently-online player by name or selector, messaging both the
     * sender and the resolved target. The argument is named {@code "player"}, not {@code "target"} -
     * naming it the same as the enclosing literal collides at Minestom's native argument-id level.
     *
     * @param manager the manager to register against
     */
    static void registerTarget(final MinestomCommandManager<CommandSender> manager) {
        manager.command(manager.commandBuilder("demo")
                .literal("target")
                .required("player", PlayerParser.playerParser(
                        sender -> sender,
                        () -> MinecraftServer.getConnectionManager().getOnlinePlayers()
                ))
                .handler(context -> {
                    final Player target = context.get("player");
                    context.sender().sendMessage(Component.text("Targeted " + target.getUsername()));
                    target.sendMessage(Component.text("You were targeted by " + senderName(context.sender())));
                }));
    }

    private static String senderName(final CommandSender sender) {
        return sender instanceof Player player ? player.getUsername() : "the console";
    }

    /**
     * {@code /demo announce <message> [--loud] [--repeat <count>]} - a flagged command (spec.md
     * §5.5/§13). Everything before the flag subtree ({@code message}) still gets full native
     * argument-tree mirroring; the flag subtree itself degrades to the documented trailing-greedy
     * fallback (docs/roadmap.md P4) - the client sees one grey "it's a string" field covering
     * {@code --loud --repeat 3} rather than individually validated sub-arguments, but Cloud still
     * parses the flags correctly, exactly as it would on any other Cloud platform
     * (docs/limitations.md).
     *
     * @param manager the manager to register against
     */
    static void registerAnnounce(final MinestomCommandManager<CommandSender> manager) {
        manager.command(manager.commandBuilder("demo")
                .literal("announce")
                .required("message", StringParser.stringParser())
                .flag(CommandFlag.<CommandSender>builder("loud").build())
                .flag(CommandFlag.<CommandSender>builder("repeat").withComponent(IntegerParser.integerParser(1, 5)).build())
                .handler(context -> {
                    final String message = context.<String>get("message");
                    final boolean loud = context.flags().isPresent("loud");
                    final Integer repeatFlag = context.flags().get("repeat");
                    final int repeat = repeatFlag == null ? 1 : repeatFlag;

                    final Component announcement = Component.text(loud ? message.toUpperCase(Locale.ROOT) + "!" : message);
                    for (int i = 0; i < repeat; i++) {
                        context.sender().sendMessage(announcement);
                    }
                }));
    }

    /**
     * {@code /demo boom} - an intentionally-throwing command (spec.md §7/§13), demonstrating the
     * default {@code cloud-minecraft-extras} exception feedback wired automatically by
     * {@link MinestomCommandManager} (docs/roadmap.md P6) with no builder configuration needed.
     *
     * @param manager the manager to register against
     */
    static void registerBoom(final MinestomCommandManager<CommandSender> manager) {
        manager.command(manager.commandBuilder("demo")
                .literal("boom")
                .handler(context -> {
                    throw new IllegalStateException("kaboom");
                }));
    }

    /**
     * {@code /demo help [query]} - wired through {@link MinecraftHelp} (spec.md §8/§13), listing
     * every command registered against {@code manager} so far - no bridging adapter needed, since
     * Minestom's {@link CommandSender} already implements Adventure's {@code Audience}
     * (docs/help-and-exceptions.md).
     *
     * @param manager the manager to register against
     */
    static void registerHelp(final MinestomCommandManager<CommandSender> manager) {
        final MinecraftHelp<CommandSender> help = MinecraftHelp.create("/demo help", manager, sender -> sender);
        manager.command(manager.commandBuilder("demo")
                .literal("help")
                .optional("query", StringParser.greedyStringParser())
                .handler(context -> help.queryCommands(context.getOrDefault("query", ""), context.sender())));
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
