package gg.cubix.cloudminestom.suggestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gg.cubix.cloudminestom.MinestomCommandManager;
import java.util.List;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.incendo.cloud.execution.CommandExecutionHandler;
import org.junit.jupiter.api.Test;

class CloudSuggestionCallbackTest {

    @Test
    void suggestsForAPartiallyTypedArgument() {
        final CloudSuggestionCallback<CommandSender> callback = new CloudSuggestionCallback<>(testManager());
        final CommandContext context = new CommandContext("test f");
        final Suggestion suggestion = new Suggestion(context.getInput(), 0, 0);

        callback.apply(new ConsoleSender(), context, suggestion);

        assertEquals(List.of(new SuggestionEntry("foo")), suggestion.getEntries());
    }

    @Test
    void returnsEverythingAfterATrailingSpacePlaceholder() {
        final CloudSuggestionCallback<CommandSender> callback = new CloudSuggestionCallback<>(testManager());
        // '\0' is the placeholder net.minestom.server.listener.TabCompleteListener appends when the
        // player's input ends with a space, so the parser has a non-empty last token to work with.
        final CommandContext context = new CommandContext("test \0");
        final Suggestion suggestion = new Suggestion(context.getInput(), 0, 0);

        callback.apply(new ConsoleSender(), context, suggestion);

        assertEquals(
                List.of(new SuggestionEntry("bar"), new SuggestionEntry("foo")),
                suggestion.getEntries().stream().sorted((a, b) -> a.getEntry().compareTo(b.getEntry())).toList()
        );
    }

    @Test
    void asksCloudWithTheWholeLineNotJustTheTrailingArgumentText() {
        final CloudSuggestionCallback<CommandSender> callback = new CloudSuggestionCallback<>(testManager());
        final CommandContext context = new CommandContext("test f");
        // The fallback ArgumentStringArray's own raw text is just "f" - if the callback asked Cloud
        // with that instead of the full context input, "f" wouldn't match the "test" root literal and
        // no suggestions would come back.
        context.setArg("args", new String[]{"f"}, "f");
        final Suggestion suggestion = new Suggestion(context.getInput(), 0, 0);

        callback.apply(new ConsoleSender(), context, suggestion);

        assertEquals(List.of(new SuggestionEntry("foo")), suggestion.getEntries());
    }

    private static MinestomCommandManager<CommandSender> testManager() {
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .commandRegistrationCallback(command -> { })
                .build();
        manager.command(manager.commandBuilder("test").literal("foo").handler(noOpHandler()));
        manager.command(manager.commandBuilder("test").literal("bar").handler(noOpHandler()));
        return manager;
    }

    private static CommandExecutionHandler<CommandSender> noOpHandler() {
        return context -> { };
    }
}
