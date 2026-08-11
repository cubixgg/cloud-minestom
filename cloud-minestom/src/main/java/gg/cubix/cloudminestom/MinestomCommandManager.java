package gg.cubix.cloudminestom;

import gg.cubix.cloudminestom.argument.ArgumentMapper;
import gg.cubix.cloudminestom.argument.ArgumentMapperRegistry;
import gg.cubix.cloudminestom.parser.MinestomCaptionKeys;
import gg.cubix.cloudminestom.parser.PlayerParser;
import gg.cubix.cloudminestom.registration.MinestomCommandRegistrationHandler;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.caption.CaptionProvider;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.caption.StandardCaptionKeys;
import org.incendo.cloud.exception.NoSuchCommandException;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.minecraft.extras.AudienceProvider;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.parser.ParserDescriptor;

/**
 * Cloud v2 command manager for Minestom (docs/spec.md §4).
 *
 * <p>Generic over the command sender type {@code C} via {@link #senderMapper()}, exactly like
 * {@code cloud-velocity}/{@code cloud-paper}: {@link #create()} fixes {@code C} to Minestom's own
 * {@link CommandSender} for projects happy to use it directly, {@link #builder(SenderMapper)} lets a
 * project map onto its own sender/player type instead.
 *
 * <p>Registration goes through {@link MinestomCommandRegistrationHandler}: every Cloud root command
 * becomes one native Minestom {@link Command} (docs/spec.md §1.1/§5 - full argument-tree mirroring
 * lands in P3, this is deliberately the flattened greedy-string bridge for now).
 */
public final class MinestomCommandManager<C> extends CommandManager<C> {

    private final SenderMapper<CommandSender, C> senderMapper;
    private final BiPredicate<C, String> permissionFunction;

    private MinestomCommandManager(
            final SenderMapper<CommandSender, C> senderMapper,
            final ExecutionCoordinator<C> executionCoordinator,
            final Consumer<Command> commandRegistrationCallback,
            final ArgumentMapperRegistry argumentMapperRegistry,
            final BiPredicate<C, String> permissionFunction,
            final Consumer<MinestomCommandManager<C>> exceptionHandlerRegistrar
    ) {
        super(executionCoordinator, CommandRegistrationHandler.nullCommandRegistrationHandler());
        this.senderMapper = senderMapper;
        this.permissionFunction = permissionFunction;
        // Can't hand `this` to the registration handler before `super(...)` returns, so it replaces
        // the temporary null handler above via CommandManager's protected setter instead of being
        // passed to the super() call directly.
        this.commandRegistrationHandler(
                new MinestomCommandRegistrationHandler<>(this, commandRegistrationCallback, argumentMapperRegistry)
        );
        exceptionHandlerRegistrar.accept(this);
        registerDefaultPlayerParser(this);
    }

    /**
     * The default {@link MinestomCommandManager.Builder#exceptionHandler(Consumer)}: wires
     * {@code cloud-minecraft-extras}' {@link MinecraftExceptionHandler} for
     * {@link org.incendo.cloud.exception.NoPermissionException NoPermissionException},
     * {@link org.incendo.cloud.exception.InvalidSyntaxException InvalidSyntaxException},
     * {@link org.incendo.cloud.exception.ArgumentParseException ArgumentParseException},
     * {@link org.incendo.cloud.exception.InvalidCommandSenderException InvalidCommandSenderException}
     * and {@link org.incendo.cloud.exception.CommandExecutionException CommandExecutionException}
     * (its own {@code defaultHandlers()}), plus a manually registered, identically styled handler for
     * {@link NoSuchCommandException} - {@code MinecraftExceptionHandler} has no default for that one
     * (spec.md §7's correction note). {@code CommandSender} already implements Adventure's
     * {@code Audience}, reached here via the reverse direction of {@link #senderMapper()}, so no
     * bridging adapter is needed for any sender type {@code C}.
     */
    private static <C> void registerDefaultExceptionHandlers(final MinestomCommandManager<C> manager) {
        final AudienceProvider<C> audienceProvider = sender -> manager.senderMapper().reverse(sender);
        MinecraftExceptionHandler.<C>create(audienceProvider)
                .defaultHandlers()
                .handler(NoSuchCommandException.class, (formatter, ctx) -> Component.text()
                        .color(NamedTextColor.RED)
                        .append(ctx.context().formatCaption(
                                formatter,
                                StandardCaptionKeys.EXCEPTION_NO_SUCH_COMMAND,
                                CaptionVariable.of("command", ctx.exception().suppliedCommand())
                        )))
                .registerTo(manager);
    }

    /**
     * Registers {@link PlayerParser} as the default Cloud parser for {@link Player}-typed values
     * (spec.md §9/§10 parity: a {@code @Argument Player} parameter on an annotated command resolves
     * automatically, the same as a builder-declared {@code .required("target", playerParser(...))}
     * would). The online-players lookup defers to {@code MinecraftServer.getConnectionManager()} at
     * call time, not at registration time, for the same reason
     * {@link Builder#commandRegistrationCallback} defers its own {@code MinecraftServer} lookup.
     * {@link MinestomCaptionKeys#ARGUMENT_PARSE_FAILURE_PLAYER} has no built-in Cloud caption text, so
     * a default English value is registered alongside the parser instead of leaving it to throw an
     * unregistered-caption error at feedback time.
     */
    private static <C> void registerDefaultPlayerParser(final MinestomCommandManager<C> manager) {
        final PlayerParser<C> parser = new PlayerParser<>(
                sender -> manager.senderMapper().reverse(sender),
                () -> MinecraftServer.getConnectionManager().getOnlinePlayers()
        );
        manager.parserRegistry().registerParser(ParserDescriptor.of(parser, Player.class));
        manager.captionRegistry().registerProvider(CaptionProvider.<C>constantProvider(
                MinestomCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER,
                MinestomCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER_DEFAULT
        ));
    }

    /**
     * Creates a new builder using Minestom's own {@link CommandSender} as the command sender type.
     *
     * @return a new builder
     */
    public static Builder<CommandSender> builder() {
        return builder(SenderMapper.identity());
    }

    /**
     * Creates a new builder that maps Minestom's {@link CommandSender} onto a project-supplied sender
     * type {@code C}.
     *
     * @param senderMapper mapping between Minestom's sender type and {@code C}
     * @param <C>          the command sender type
     * @return a new builder
     */
    public static <C> Builder<C> builder(final SenderMapper<CommandSender, C> senderMapper) {
        return new Builder<>(senderMapper);
    }

    /**
     * Creates a manager using Minestom's own {@link CommandSender} as the command sender type and
     * every other builder default.
     *
     * @return a new manager
     */
    public static MinestomCommandManager<CommandSender> create() {
        return builder().build();
    }

    /**
     * The mapping between Minestom's {@link CommandSender} and this manager's command sender type.
     *
     * @return the sender mapper
     */
    public SenderMapper<CommandSender, C> senderMapper() {
        return senderMapper;
    }

    @Override
    public boolean hasPermission(final C sender, final String permission) {
        return this.permissionFunction.test(sender, permission);
    }

    /**
     * Builder for {@link MinestomCommandManager}.
     *
     * @param <C> the command sender type
     */
    public static final class Builder<C> {

        private final SenderMapper<CommandSender, C> senderMapper;
        private ExecutionCoordinator<C> executionCoordinator = ExecutionCoordinator.simpleCoordinator();
        // Not a `MinecraftServer.getCommandManager()::register` method reference: that would resolve
        // `getCommandManager()` eagerly at builder-construction time, forcing MinecraftServer to
        // already be initialized even for consumers who override this callback (or construct the
        // manager before MinecraftServer.init()). The lambda defers the lookup to first registration.
        private Consumer<Command> commandRegistrationCallback = command -> MinecraftServer.getCommandManager().register(command);
        private ArgumentMapperRegistry argumentMapperRegistry = ArgumentMapperRegistry.createDefault();
        // Always allowed: the pinned Minestom version has no native permission-node system to check
        // against (no Permission/PermissionHandler/Player#hasPermission - see spec.md §6's
        // correction note), only a numeric Player#getPermissionLevel() unrelated in shape to Cloud's
        // arbitrary String-keyed model. Projects with a real permission system (LuckPerms, an auth
        // service, a permissionLevel-based check) replace this wholesale.
        private BiPredicate<C, String> permissionFunction = (sender, permission) -> true;
        private Consumer<MinestomCommandManager<C>> exceptionHandler = MinestomCommandManager::registerDefaultExceptionHandlers;

        private Builder(final SenderMapper<CommandSender, C> senderMapper) {
            this.senderMapper = Objects.requireNonNull(senderMapper, "senderMapper");
        }

        /**
         * Sets the execution coordinator. Defaults to {@link ExecutionCoordinator#simpleCoordinator()};
         * see docs/spec.md §4.1 before changing this - it's a correctness requirement for Minestom's
         * thread-confined game state, not a performance knob.
         *
         * @param executionCoordinator the execution coordinator
         * @return this builder
         */
        public Builder<C> executionCoordinator(final ExecutionCoordinator<C> executionCoordinator) {
            this.executionCoordinator = Objects.requireNonNull(executionCoordinator, "executionCoordinator");
            return this;
        }

        /**
         * Sets how built native {@link Command}s reach the server. Defaults to
         * {@code MinecraftServer.getCommandManager()::register}; override with a plain sink (e.g. a
         * {@code List::add}) to make registration testable without a running server.
         *
         * @param commandRegistrationCallback the registration callback
         * @return this builder
         */
        public Builder<C> commandRegistrationCallback(final Consumer<Command> commandRegistrationCallback) {
            this.commandRegistrationCallback = Objects.requireNonNull(commandRegistrationCallback, "commandRegistrationCallback");
            return this;
        }

        /**
         * Replaces the registry of {@link ArgumentMapper}s used to translate Cloud components into
         * native Minestom arguments. Defaults to {@link ArgumentMapperRegistry#createDefault()}.
         * Calls to {@link #argumentMapper(Class, ArgumentMapper)} made before this replace the
         * registry are lost - set this first if both are used together.
         *
         * @param argumentMapperRegistry the replacement registry
         * @return this builder
         */
        public Builder<C> argumentMapperRegistry(final ArgumentMapperRegistry argumentMapperRegistry) {
            this.argumentMapperRegistry = Objects.requireNonNull(argumentMapperRegistry, "argumentMapperRegistry");
            return this;
        }

        /**
         * Registers or replaces a single {@link ArgumentMapper} on this builder's
         * {@link ArgumentMapperRegistry}, mirroring {@code CloudBrigadierManager}'s own
         * Cloud-parser-to-native-type registration API.
         *
         * @param parserType the concrete Cloud parser class to map, e.g. {@code IntegerParser.class}
         * @param mapper     the mapper to use for that parser type
         * @param <T>        the type of value the parser produces
         * @return this builder
         */
        public <T> Builder<C> argumentMapper(final Class<?> parserType, final ArgumentMapper<T> mapper) {
            this.argumentMapperRegistry.register(parserType, mapper);
            return this;
        }

        /**
         * Replaces the permission function backing {@link MinestomCommandManager#hasPermission}.
         * Defaults to always-allowed for every sender and permission string - see docs/spec.md §6's
         * correction note for why that, not a Minestom-native permission-node check, is the sane
         * default on this platform.
         *
         * @param permissionFunction the replacement permission function
         * @return this builder
         */
        public Builder<C> permissionFunction(final BiPredicate<C, String> permissionFunction) {
            this.permissionFunction = Objects.requireNonNull(permissionFunction, "permissionFunction");
            return this;
        }

        /**
         * Replaces exception-handler registration. A thin wrapper over
         * {@code manager.exceptionController()} rather than a new concept (docs/spec.md §7): the
         * default calls {@link MinestomCommandManager#registerDefaultExceptionHandlers}; supply a
         * different callback to register your own handlers there instead, or in addition, by calling
         * {@code manager.exceptionController().registerHandler(...)} directly.
         *
         * @param exceptionHandler the replacement registration callback
         * @return this builder
         */
        public Builder<C> exceptionHandler(final Consumer<MinestomCommandManager<C>> exceptionHandler) {
            this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
            return this;
        }

        /**
         * Builds the manager.
         *
         * @return the built manager
         */
        public MinestomCommandManager<C> build() {
            return new MinestomCommandManager<>(
                    senderMapper, executionCoordinator, commandRegistrationCallback, argumentMapperRegistry,
                    permissionFunction, exceptionHandler
            );
        }
    }
}
