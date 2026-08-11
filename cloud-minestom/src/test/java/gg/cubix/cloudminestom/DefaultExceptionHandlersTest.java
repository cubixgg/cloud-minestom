package gg.cubix.cloudminestom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.tag.TagHandler;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.exception.NoSuchCommandException;
import org.incendo.cloud.permission.Permission;
import org.incendo.cloud.permission.PermissionResult;
import org.junit.jupiter.api.Test;

/**
 * Drives the default exception handlers directly against {@code exceptionController()} - no live
 * server needed (docs/roadmap.md P6, spec.md §7).
 */
class DefaultExceptionHandlersTest {

    @Test
    void noPermissionExceptionProducesFeedback() throws Throwable {
        final String feedback = handle(new NoPermissionException(
                PermissionResult.denied(Permission.of("some.permission")), FAKE_SENDER, List.of()
        ));
        // Cloud's own default caption for this exception doesn't interpolate the permission node
        // into the message text (unlike e.g. invalid-syntax/argument-parse) - matching Cloud's own
        // default English wording here, not a placeholder cloud-minestom introduces.
        assertTrue(feedback.contains("do not have permission"));
    }

    @Test
    void invalidSyntaxExceptionProducesFeedbackNamingTheCorrectSyntax() throws Throwable {
        final String feedback = handle(new InvalidSyntaxException("test <name>", FAKE_SENDER, List.of()));
        assertTrue(feedback.contains("test <name>"));
    }

    @Test
    void argumentParseExceptionProducesFeedbackNamingTheCause() throws Throwable {
        final String feedback = handle(new ArgumentParseException(
                new NumberFormatException("not a number"), FAKE_SENDER, List.of()
        ));
        assertTrue(feedback.contains("not a number"));
    }

    @Test
    void invalidCommandSenderExceptionProducesFeedback() throws Throwable {
        final String feedback = handle(new InvalidCommandSenderException(
                FAKE_SENDER, CommandSender.class, List.of(), null
        ));
        assertFalse(feedback.isBlank());
    }

    @Test
    void noSuchCommandExceptionProducesFeedback() throws Throwable {
        final String feedback = handle(new NoSuchCommandException(FAKE_SENDER, List.of(), "bogus"));
        // MinecraftExceptionHandler has no built-in handler for this one (spec.md §7's correction
        // note) - registered manually, using Cloud's own default caption, which doesn't interpolate
        // the supplied command into the message text.
        assertTrue(feedback.contains("Unknown command"));
    }

    private static final FakeSender FAKE_SENDER = new FakeSender();

    private static String handle(final Throwable exception) throws Throwable {
        FAKE_SENDER.received.clear();
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        final CommandContext<CommandSender> context = new CommandContext<>(FAKE_SENDER, manager);

        manager.exceptionController().handleException(context, exception);

        assertFalse(FAKE_SENDER.received.isEmpty(), "expected a message to have been sent");
        return PlainTextComponentSerializer.plainText().serialize(FAKE_SENDER.received.get(0));
    }

    private static final class FakeSender implements CommandSender {

        private final List<Component> received = new ArrayList<>();
        private final TagHandler tagHandler = TagHandler.newHandler();

        @Override
        public void sendMessage(final Component message) {
            this.received.add(message);
        }

        @Override
        public TagHandler tagHandler() {
            return this.tagHandler;
        }

        @Override
        public Identity identity() {
            return Identity.nil();
        }

        @Override
        public Pointers pointers() {
            return Pointers.empty();
        }
    }
}
