package gg.cubix.cloudminestom.registration;

import gg.cubix.cloudminestom.MinestomCommandManager;
import gg.cubix.cloudminestom.suggestion.CloudSuggestionCallback;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.internal.CommandRegistrationHandler;

/**
 * Bridges Cloud's registration lifecycle onto Minestom's native {@link Command} (docs/spec.md §1.1).
 *
 * <p>For now every Cloud root command becomes exactly one native {@link Command}: a default executor
 * plus a single trailing {@link net.minestom.server.command.builder.arguments.ArgumentStringArray}
 * syntax, both of which re-join the raw input and re-dispatch it through Cloud
 * ({@link org.incendo.cloud.execution.CommandExecutor#executeCommand}) - the flattened bridge
 * described in spec §1.1, not yet the full argument-tree mirroring that lands in P3. The fallback
 * syntax's suggestions come from Cloud too, via {@link CloudSuggestionCallback} (spec §5.3).
 *
 * @param <C> the command sender type
 */
public final class MinestomCommandRegistrationHandler<C> implements CommandRegistrationHandler<C> {

    private static final String FALLBACK_ARGUMENT_NAME = "args";

    private final Map<String, Command> registeredRoots = new ConcurrentHashMap<>();
    private final MinestomCommandManager<C> manager;
    private final Consumer<Command> commandRegistrationCallback;
    private final CloudSuggestionCallback<C> suggestionCallback;

    /**
     * Creates a new registration handler.
     *
     * @param manager                     the owning manager, used to reach the sender mapper and
     *                                    executor when a native syntax is triggered
     * @param commandRegistrationCallback how a built native {@link Command} reaches the server
     */
    public MinestomCommandRegistrationHandler(
            final MinestomCommandManager<C> manager,
            final Consumer<Command> commandRegistrationCallback
    ) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.commandRegistrationCallback = Objects.requireNonNull(commandRegistrationCallback, "commandRegistrationCallback");
        this.suggestionCallback = new CloudSuggestionCallback<>(manager);
    }

    @Override
    public boolean registerCommand(final org.incendo.cloud.Command<C> command) {
        final CommandComponent<C> rootComponent = command.rootComponent();
        this.registeredRoots.computeIfAbsent(rootComponent.name(), name -> this.createNativeCommand(name, rootComponent));
        return true;
    }

    private Command createNativeCommand(final String name, final CommandComponent<C> rootComponent) {
        final String[] aliases = rootComponent.alternativeAliases().toArray(new String[0]);
        final Command nativeCommand = new Command(name, aliases);

        final CommandExecutor executor = (sender, context) -> this.dispatch(sender, context.getInput());
        nativeCommand.setDefaultExecutor(executor);

        final Argument<String[]> fallback = ArgumentType.StringArray(FALLBACK_ARGUMENT_NAME);
        fallback.setSuggestionCallback(this.suggestionCallback);
        nativeCommand.addSyntax(executor, fallback);

        this.commandRegistrationCallback.accept(nativeCommand);
        return nativeCommand;
    }

    private void dispatch(final CommandSender sender, final String input) {
        final C mappedSender = this.manager.senderMapper().map(sender);
        this.manager.commandExecutor().executeCommand(mappedSender, input);
    }
}
