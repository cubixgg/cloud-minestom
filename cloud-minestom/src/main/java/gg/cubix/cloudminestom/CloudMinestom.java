package gg.cubix.cloudminestom;

/**
 * Compile anchor for the {@code cloud-minestom} module.
 *
 * <p>Full Cloud v2 command framework integration for Minestom: a Minestom-generic
 * {@code MinestomCommandManager}, native command-tree translation, built-in argument mappers,
 * permission and exception wiring, and Minestom-specific parsers. See {@code docs/spec.md} for the
 * full design.
 *
 * <p>Carries nothing but the module id for now; real content starts landing in P1.
 */
public final class CloudMinestom {

    /** Gradle artifact id of this module. */
    public static final String MODULE_ID = "cloud-minestom";

    private CloudMinestom() {
    }
}
