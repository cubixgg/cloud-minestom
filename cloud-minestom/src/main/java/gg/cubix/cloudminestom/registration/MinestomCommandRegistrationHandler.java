package gg.cubix.cloudminestom.registration;

import gg.cubix.cloudminestom.MinestomCommandManager;
import gg.cubix.cloudminestom.argument.ArgumentMapperRegistry;
import gg.cubix.cloudminestom.suggestion.CloudSuggestionCallback;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.Argument;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.internal.CommandNode;
import org.incendo.cloud.internal.CommandRegistrationHandler;

/**
 * Bridges Cloud's registration lifecycle onto Minestom's native {@link Command} (docs/spec.md §1.1).
 *
 * <p>Every Cloud root command becomes exactly one native {@link Command}, built by walking
 * {@code manager.commandTree()}'s node for that root through {@link CommandTreeTranslator} - the full
 * argument-tree mirroring described in spec §5, replacing the P2 flattened greedy-string bridge.
 * Every generated syntax executor, mapped node or fallback alike, still re-joins the raw input and
 * re-dispatches through {@link org.incendo.cloud.execution.CommandExecutor#executeCommand} (spec
 * §5.4/ADR-0001) - Minestom's own parsed argument values are never read.
 *
 * @param <C> the command sender type
 */
public final class MinestomCommandRegistrationHandler<C> implements CommandRegistrationHandler<C> {

    private final Map<String, Command> registeredRoots = new ConcurrentHashMap<>();
    private final MinestomCommandManager<C> manager;
    private final Consumer<Command> commandRegistrationCallback;
    private final ArgumentMapperRegistry argumentMapperRegistry;
    private final CloudSuggestionCallback<C> suggestionCallback;

    /**
     * Creates a new registration handler.
     *
     * @param manager                     the owning manager, used to reach the sender mapper, command
     *                                    tree and executor
     * @param commandRegistrationCallback how a built native {@link Command} reaches the server
     * @param argumentMapperRegistry      the mappers used to translate Cloud components into native
     *                                    arguments
     */
    public MinestomCommandRegistrationHandler(
            final MinestomCommandManager<C> manager,
            final Consumer<Command> commandRegistrationCallback,
            final ArgumentMapperRegistry argumentMapperRegistry
    ) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.commandRegistrationCallback = Objects.requireNonNull(commandRegistrationCallback, "commandRegistrationCallback");
        this.argumentMapperRegistry = Objects.requireNonNull(argumentMapperRegistry, "argumentMapperRegistry");
        this.suggestionCallback = new CloudSuggestionCallback<>(manager);
    }

    @Override
    public boolean registerCommand(final org.incendo.cloud.Command<C> command) {
        final CommandComponent<C> rootComponent = command.rootComponent();
        final String rootName = rootComponent.name();

        final CommandNode<C> rootNode = this.manager.commandTree().getNamedNode(rootName);
        final CommandTreeTranslator.Result translation =
                CommandTreeTranslator.translate(rootNode, this.argumentMapperRegistry, this.suggestionCallback);

        final Command existing = this.registeredRoots.get(rootName);
        if (existing == null) {
            this.registeredRoots.put(rootName, this.createNativeCommand(rootComponent, translation.syntaxes()));
        } else {
            // The root was already handed off (e.g. to MinecraftServer.getCommandManager(), which
            // throws if register() is called twice for the same name), so a newly discovered leaf
            // under it can't be registered as a second Command. Command#getSyntaxes() leaks its
            // backing mutable list, so the already-registered instance is refreshed in place instead.
            //
            // This relies on something invalidating CommandManager's cached parse graph afterward -
            // true for the default callback (MinecraftServer.getCommandManager()) the moment any
            // other command is (un)registered, which is virtually always true in practice since
            // command registration happens in a burst at startup, before players can connect. A
            // server that registers exactly one root command, incrementally, entirely on its own,
            // while already accepting connections, is the one scenario this doesn't cover.
            existing.getSyntaxes().clear();
            applySyntaxes(existing, existing.getDefaultExecutor(), translation.syntaxes());
        }
        return true;
    }

    private Command createNativeCommand(final CommandComponent<C> rootComponent, final List<Argument<?>[]> syntaxes) {
        final String[] aliases = rootComponent.alternativeAliases().toArray(new String[0]);
        final Command nativeCommand = new Command(rootComponent.name(), aliases);

        final CommandExecutor executor = (sender, context) -> this.dispatch(sender, context.getInput());
        nativeCommand.setDefaultExecutor(executor);
        applySyntaxes(nativeCommand, executor, syntaxes);

        this.commandRegistrationCallback.accept(nativeCommand);
        return nativeCommand;
    }

    private static void applySyntaxes(final Command nativeCommand, final CommandExecutor executor, final List<Argument<?>[]> syntaxes) {
        for (final Argument<?>[] syntax : syntaxes) {
            // A zero-length syntax means the root itself is directly executable; the default
            // executor above already covers that case.
            if (syntax.length > 0) {
                nativeCommand.addSyntax(executor, syntax);
            }
        }
    }

    private void dispatch(final CommandSender sender, final String input) {
        final C mappedSender = this.manager.senderMapper().map(sender);
        this.manager.commandExecutor().executeCommand(mappedSender, input);
    }
}
