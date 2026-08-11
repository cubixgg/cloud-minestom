package gg.cubix.cloudminestom.parser;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.exception.parsing.ParserException;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

/**
 * Parses a single currently-online {@link Player}, by exact (case-insensitive) username or by a
 * single-player-and-only-players target selector (docs/spec.md §9).
 *
 * <p>Selector input (anything starting with {@code @}) is parsed by calling Minestom's own
 * {@link ArgumentEntity#parse(CommandSender, String)} directly, constrained to
 * {@code singleEntity(true).onlyPlayers(true)}, then resolved against the sender via
 * {@link EntityFinder#findFirstPlayer(CommandSender)} - a plain library call to reuse Minestom's own
 * selector grammar (relative-to-sender syntax with no Cloud-native equivalent to reimplement), made
 * from inside this Cloud parser's own {@link #parse} method. Spec §5.4/ADR-0001 is unaffected: this is
 * still the one and only parse Cloud performs for the argument, not a second, independently-parsed
 * value read back from the native argument tree - the mapped {@code Entity} node registered by
 * {@code ArgumentMapper} (see {@code StandardArgumentMappers}) remains shape only, exactly like every
 * other mapped node.
 *
 * @param <C> the command sender type
 */
public final class PlayerParser<C> implements ArgumentParser<C, Player>, BlockingSuggestionProvider.Strings<C> {

    private static final ArgumentEntity SELECTOR = new ArgumentEntity("selector")
            .singleEntity(true)
            .onlyPlayers(true);

    private final Function<C, CommandSender> senderMapper;
    private final Supplier<Collection<Player>> onlinePlayers;

    /**
     * Creates a new player parser.
     *
     * @param senderMapper  maps this parser's command sender type onto a Minestom {@link CommandSender},
     *                      used only to resolve selector input ({@code @s}/{@code @p}/{@code @a}/...)
     *                      relative to the sender
     * @param onlinePlayers supplies the currently-online players to match a raw username against and to
     *                      suggest; a {@link Supplier} (not a fixed {@link Collection}) so the default
     *                      registration in {@code MinestomCommandManager} can defer to
     *                      {@code MinecraftServer.getConnectionManager()} without resolving it before
     *                      the server is initialized, and so tests can supply a fixed list without a
     *                      running server
     */
    public PlayerParser(final Function<C, CommandSender> senderMapper, final Supplier<Collection<Player>> onlinePlayers) {
        this.senderMapper = Objects.requireNonNull(senderMapper, "senderMapper");
        this.onlinePlayers = Objects.requireNonNull(onlinePlayers, "onlinePlayers");
    }

    /**
     * Creates a new player parser descriptor.
     *
     * @param senderMapper  see {@link #PlayerParser(Function, Supplier)}
     * @param onlinePlayers see {@link #PlayerParser(Function, Supplier)}
     * @param <C>           the command sender type
     * @return the created parser descriptor
     */
    public static <C> ParserDescriptor<C, Player> playerParser(
            final Function<C, CommandSender> senderMapper,
            final Supplier<Collection<Player>> onlinePlayers
    ) {
        return ParserDescriptor.of(new PlayerParser<>(senderMapper, onlinePlayers), Player.class);
    }

    /**
     * Returns a {@link CommandComponent.Builder} using {@link #playerParser(Function, Supplier)} as the
     * parser.
     *
     * @param senderMapper  see {@link #PlayerParser(Function, Supplier)}
     * @param onlinePlayers see {@link #PlayerParser(Function, Supplier)}
     * @param <C>           the command sender type
     * @return the component builder
     */
    public static <C> CommandComponent.Builder<C, Player> playerComponent(
            final Function<C, CommandSender> senderMapper,
            final Supplier<Collection<Player>> onlinePlayers
    ) {
        return CommandComponent.<C, Player>builder().parser(playerParser(senderMapper, onlinePlayers));
    }

    @Override
    public ArgumentParseResult<Player> parse(final CommandContext<C> commandContext, final CommandInput commandInput) {
        final String input = commandInput.readString();

        if (input.startsWith("@")) {
            return parseSelector(commandContext, input);
        }

        for (final Player player : this.onlinePlayers.get()) {
            if (player.getUsername().equalsIgnoreCase(input)) {
                return ArgumentParseResult.success(player);
            }
        }
        return ArgumentParseResult.failure(new PlayerParseException(input, commandContext));
    }

    private ArgumentParseResult<Player> parseSelector(final CommandContext<C> commandContext, final String input) {
        final CommandSender sender = this.senderMapper.apply(commandContext.sender());

        final EntityFinder finder;
        try {
            finder = SELECTOR.parse(sender, input);
        } catch (final ArgumentSyntaxException e) {
            return ArgumentParseResult.failure(new PlayerParseException(input, commandContext));
        }

        final Player player = finder.findFirstPlayer(sender);
        if (player == null) {
            return ArgumentParseResult.failure(new PlayerParseException(input, commandContext));
        }
        return ArgumentParseResult.success(player);
    }

    @Override
    public Iterable<String> stringSuggestions(final CommandContext<C> commandContext, final CommandInput input) {
        return this.onlinePlayers.get().stream().map(Player::getUsername).toList();
    }

    /**
     * Thrown when {@link PlayerParser} cannot resolve its input to exactly one online player.
     */
    public static final class PlayerParseException extends ParserException {

        private final String input;

        /**
         * Constructs a new player parse exception.
         *
         * @param input   the supplied input
         * @param context the command context
         */
        public PlayerParseException(final String input, final CommandContext<?> context) {
            super(
                    PlayerParser.class,
                    context,
                    MinestomCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER,
                    CaptionVariable.of("input", input)
            );
            this.input = input;
        }

        /**
         * Returns the supplied input.
         *
         * @return the input
         */
        public String input() {
            return this.input;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final PlayerParseException that)) {
                return false;
            }
            return this.input.equals(that.input);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.input);
        }
    }
}
