package gg.cubix.cloudminestom;

import gg.cubix.cloudminestom.argument.ArgumentMapper;
import gg.cubix.cloudminestom.argument.ArgumentMapperRegistry;
import gg.cubix.cloudminestom.registration.MinestomCommandRegistrationHandler;
import java.util.Objects;
import java.util.function.Consumer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;

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

    private MinestomCommandManager(
            final SenderMapper<CommandSender, C> senderMapper,
            final ExecutionCoordinator<C> executionCoordinator,
            final Consumer<Command> commandRegistrationCallback,
            final ArgumentMapperRegistry argumentMapperRegistry
    ) {
        super(executionCoordinator, CommandRegistrationHandler.nullCommandRegistrationHandler());
        this.senderMapper = senderMapper;
        // Can't hand `this` to the registration handler before `super(...)` returns, so it replaces
        // the temporary null handler above via CommandManager's protected setter instead of being
        // passed to the super() call directly.
        this.commandRegistrationHandler(
                new MinestomCommandRegistrationHandler<>(this, commandRegistrationCallback, argumentMapperRegistry)
        );
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
        // Temporary stub. The real default - and the pluggable permission function it comes from -
        // lands whole in P5 (docs/spec.md §6); no half version here in the meantime.
        return true;
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
         * Builds the manager.
         *
         * @return the built manager
         */
        public MinestomCommandManager<C> build() {
            return new MinestomCommandManager<>(senderMapper, executionCoordinator, commandRegistrationCallback, argumentMapperRegistry);
        }
    }
}
