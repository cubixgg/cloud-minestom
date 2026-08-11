package gg.cubix.cloudminestom.suggestion;

import gg.cubix.cloudminestom.MinestomCommandManager;
import java.util.Objects;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionCallback;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.incendo.cloud.suggestion.Suggestions;

/**
 * Bridges a Minestom {@link SuggestionCallback} onto Cloud's own suggestion factory (docs/spec.md
 * §5.3), shared by every mapped node and the fallback node alike.
 *
 * @param <C> the command sender type
 */
public final class CloudSuggestionCallback<C> implements SuggestionCallback {

    // Minestom appends this to the input when it ends with a space, so its parser has a non-empty
    // last token to find a suggestion for (net.minestom.server.listener.TabCompleteListener). Cloud
    // has no notion of this placeholder and expects the trailing space to stand on its own.
    private static final char TRAILING_SPACE_PLACEHOLDER = '\0';

    private final MinestomCommandManager<C> manager;

    /**
     * Creates a new suggestion callback bound to the given manager.
     *
     * @param manager the manager whose sender mapper and suggestion factory back this callback
     */
    public CloudSuggestionCallback(final MinestomCommandManager<C> manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    @Override
    public void apply(final CommandSender sender, final CommandContext context, final Suggestion suggestion) {
        final String input = stripTrailingPlaceholder(context.getInput());
        final C mappedSender = this.manager.senderMapper().map(sender);

        final Suggestions<C, ? extends org.incendo.cloud.suggestion.Suggestion> suggestions =
                this.manager.suggestionFactory().suggestImmediately(mappedSender, input);

        for (final org.incendo.cloud.suggestion.Suggestion cloudSuggestion : suggestions.list()) {
            suggestion.addEntry(new SuggestionEntry(cloudSuggestion.suggestion()));
        }
    }

    private static String stripTrailingPlaceholder(final String input) {
        if (!input.isEmpty() && input.charAt(input.length() - 1) == TRAILING_SPACE_PLACEHOLDER) {
            return input.substring(0, input.length() - 1);
        }
        return input;
    }
}
