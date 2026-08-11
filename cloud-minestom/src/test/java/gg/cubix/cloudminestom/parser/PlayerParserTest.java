package gg.cubix.cloudminestom.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import gg.cubix.cloudminestom.MinestomCommandManager;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.tag.TagHandler;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlayerParser}, driven against hand-built {@link Player}s (no
 * {@code net.minestom:testing} env/tick loop - docs/roadmap.md P8). Constructing a real {@link Player}
 * still requires {@link MinecraftServer#init()} once, since its registries are looked up from a static
 * initializer, but that alone doesn't start a running server - no socket, no tick loop, no connection -
 * so this stays a plain unit test rather than an {@code @EnvTest}; {@code @EnvTest} re-initializes the
 * process before every test method regardless (see {@code net.minestom.testing.EnvTestExt}), so
 * leftover state from here can't leak into one.
 */
class PlayerParserTest {

    static {
        // Must run before the STEVE/ALEX field initializers below: Player's own static initializer
        // reads MinecraftServer's dimension-type registry, which only exists once the process is
        // initialized - a plain @BeforeAll would run too late, after these fields are already set.
        MinecraftServer.init();
    }

    private static final Player STEVE = fakePlayer("Steve");
    private static final Player ALEX = fakePlayer("Alex");

    private final PlayerParser<CommandSender> parser =
            new PlayerParser<>(sender -> sender, () -> List.of(STEVE, ALEX));

    @Test
    void parsesAnExactOnlineNameCaseInsensitively() {
        final var result = this.parser.parse(context(), CommandInput.of("steve"));

        assertEquals(STEVE, result.parsedValue().orElseThrow());
    }

    @Test
    void rejectsAnUnknownNameWithAPlayerParseException() {
        final var result = this.parser.parse(context(), CommandInput.of("Ghost"));

        final Throwable failure = result.failure().orElseThrow();
        final PlayerParser.PlayerParseException exception =
                assertInstanceOf(PlayerParser.PlayerParseException.class, failure);
        assertEquals("Ghost", exception.input());
    }

    @Test
    void suggestionsListOnlyCurrentlyOnlinePlayerNames() {
        final Iterable<String> suggestions = this.parser.stringSuggestions(context(), CommandInput.empty());

        assertEquals(Set.of("Steve", "Alex"), StreamSupport.stream(suggestions.spliterator(), false).collect(Collectors.toSet()));
    }

    private static CommandContext<CommandSender> context() {
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .commandRegistrationCallback(command -> { })
                .build();
        return new CommandContext<>(new FakeSender(), manager);
    }

    private static Player fakePlayer(final String name) {
        return new Player(new FakeConnection(), new GameProfile(UUID.randomUUID(), name));
    }

    private static final class FakeConnection extends PlayerConnection {
        @Override
        public void sendPacket(final SendablePacket packet) {
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }
    }

    private static final class FakeSender implements CommandSender {

        private final TagHandler tagHandler = TagHandler.newHandler();

        @Override
        public void sendMessage(final Component message) {
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
