# Permissions

`MinestomCommandManager#hasPermission(C sender, String permission)` delegates to a
`BiPredicate<C, String>` — the `permissionFunction` builder option (spec.md §6).

## The default: always allowed

```java
MinestomCommandManager.create().hasPermission(anySender, "any.permission.string"); // true, always
```

This isn't a placeholder waiting to be finished — it's the honest default for a platform with no
native permission system to check against. The pinned Minestom version has no
`net.minestom.server.permission` package, no `Player#hasPermission`, no `PermissionHandler` — none of
the Bukkit-style permission-node machinery a first read of "permissions" might suggest exists here.
The only permission-adjacent thing Minestom itself ships is `Player#getPermissionLevel()`, a
vanilla-style numeric operator level (0-4), which is unrelated in shape to Cloud's arbitrary,
namespaced, String-keyed permission model (`"myplugin.command.foo"` vs. `"myplugin.command.bar"`).
Mapping one onto the other would invent a `cloud-minestom`-specific convention Minestom doesn't have,
giving a false impression of fine-grained control that's actually a single binary gate — exactly the
kind of "wrong answer that looks right" this library avoids rather than a documented gap it's still
working toward.

## Supplying your own

```java
MinestomCommandManager<CommandSender> manager = MinestomCommandManager.builder()
        .permissionFunction((sender, permission) ->
                permission.isEmpty() || luckPerms.getUserManager()
                        .getUser(((Player) sender).getUuid())
                        .getCachedData().getPermissionData().checkPermission(permission).asBoolean())
        .build();
```

The `BiPredicate<C, String>` shape composes with whatever sender type `C` you're using — a project
with its own player wrapper checks permissions against that wrapper directly, no extra adapter needed.
Whatever real permission system a project has — an auth service call, a proxy-sent permission cache, a
LuckPerms integration, or a hand-rolled check against `Player#getPermissionLevel()` — plugs in here as
a wholesale replacement of the default, not an extension of it.

## Where this gets used

- `.permission("...")` on a `Command.Builder` — the gated branch is both hidden from a lacking
  sender's tab-completion and rejected on execution.
- `@Permission(...)` on an `@Command`-annotated method — see [`annotations.md`](./annotations.md);
  enforced through this exact same function, no separate annotation-driven permission system.
- Manual checks: `manager.hasPermission(sender, "some.permission")` anywhere you need one outside a
  command context.
