package gg.cubix.cloudminestom.parser;

import org.incendo.cloud.caption.Caption;

/**
 * Caption keys for {@code cloud-minestom}'s own parsers (docs/spec.md §9), registered with a default
 * English value in {@link gg.cubix.cloudminestom.MinestomCommandManager} the same way Cloud registers
 * {@link org.incendo.cloud.caption.StandardCaptionKeys} - there is no built-in caption for a
 * Minestom-specific failure like "no such player", so this library provides its own instead of
 * silently producing an unregistered-caption {@link IllegalArgumentException} at feedback time.
 */
public final class MinestomCaptionKeys {

    /**
     * Caption shown when {@link PlayerParser} cannot resolve its input to exactly one online player.
     */
    public static final Caption ARGUMENT_PARSE_FAILURE_PLAYER = Caption.of("argument.parse.failure.player");

    /**
     * Default English text for {@link #ARGUMENT_PARSE_FAILURE_PLAYER}.
     */
    public static final String ARGUMENT_PARSE_FAILURE_PLAYER_DEFAULT = "'<input>' is not an online player";

    private MinestomCaptionKeys() {
    }
}
