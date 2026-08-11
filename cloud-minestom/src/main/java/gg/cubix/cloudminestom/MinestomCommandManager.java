package gg.cubix.cloudminestom;

import java.util.Objects;
import net.minestom.server.command.CommandSender;
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
 * <p>Registration is still {@link CommandRegistrationHandler#nullCommandRegistrationHandler()} at this
 * point - native command-tree translation lands in P2/P3. This class only carries the manager shape
 * and its construction paths.
 */
public final class MinestomCommandManager<C> extends CommandManager<C> {

    private final SenderMapper<CommandSender, C> senderMapper;

    private MinestomCommandManager(
            final SenderMapper<CommandSender, C> senderMapper,
            final ExecutionCoordinator<C> executionCoordinator
    ) {
        super(executionCoordinator, CommandRegistrationHandler.nullCommandRegistrationHandler());
        this.senderMapper = senderMapper;
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
         * Builds the manager.
         *
         * @return the built manager
         */
        public MinestomCommandManager<C> build() {
            return new MinestomCommandManager<>(senderMapper, executionCoordinator);
        }
    }
}
