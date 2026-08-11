package gg.cubix.cloudminestom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import org.incendo.cloud.SenderMapper;
import org.junit.jupiter.api.Test;

class MinestomCommandManagerTest {

    @Test
    void builderBuildReturnsManagerWithIdentitySenderMapper() {
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder().build();
        assertNotNull(manager);

        final CommandSender sender = new ConsoleSender();
        assertSame(sender, manager.senderMapper().map(sender));
        assertSame(sender, manager.senderMapper().reverse(sender));
    }

    @Test
    void customSenderMapperRoundTripsThroughSenderMapper() {
        final CommandSender console = new ConsoleSender();
        final FakePlayer expected = new FakePlayer("mapped");
        final SenderMapper<CommandSender, FakePlayer> mapper =
                SenderMapper.create(sender -> expected, mapped -> console);

        final MinestomCommandManager<FakePlayer> manager = MinestomCommandManager.builder(mapper).build();

        assertSame(expected, manager.senderMapper().map(console));
        assertSame(console, manager.senderMapper().reverse(expected));
    }

    @Test
    void createAndBuilderBuildIdentityProduceEquivalentManagers() {
        final MinestomCommandManager<CommandSender> created = MinestomCommandManager.create();
        final MinestomCommandManager<CommandSender> built = MinestomCommandManager.builder().build();

        final CommandSender sender = new ConsoleSender();
        assertSame(built.senderMapper().map(sender), created.senderMapper().map(sender));
        assertEquals(built.hasPermission(sender, "any"), created.hasPermission(sender, "any"));
    }

    @Test
    void defaultPermissionFunctionAllowsAnEmptyPermissionString() {
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        assertTrue(manager.hasPermission(new ConsoleSender(), ""));
    }

    @Test
    void defaultPermissionFunctionAllowsANonEmptyPermissionStringToo() {
        // The pinned Minestom version has no native permission-node system to check a non-empty
        // permission string against (spec.md §6's correction note), so the default allows it too -
        // this isn't a placeholder, it's the honest default for a platform with nothing to gate on.
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.create();
        assertTrue(manager.hasPermission(new ConsoleSender(), "some.permission.node"));
    }

    @Test
    void customPermissionFunctionFullyReplacesTheDefault() {
        final MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
                .permissionFunction((sender, permission) -> permission.equals("allowed"))
                .build();

        final CommandSender sender = new ConsoleSender();
        assertTrue(manager.hasPermission(sender, "allowed"));
        assertFalse(manager.hasPermission(sender, "denied"));
    }

    private record FakePlayer(String name) {
    }
}
