package gg.cubix.cloudminestom.argument;

import net.minestom.server.command.builder.arguments.Argument;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.ArgumentParser;

/**
 * Maps a Cloud {@link CommandComponent} onto a native Minestom {@link Argument} (docs/spec.md §5.2).
 *
 * <p>The produced {@link Argument} is shape only - client-side coloring and tab-complete structure -
 * never a second source of truth for what the component means. Every mapped node still gets its
 * suggestions and validation from Cloud (spec §5.4/ADR-0001); implementations must not read the
 * native argument's own parsed value anywhere in this library.
 *
 * @param <T> the type of value the Cloud parser produces
 */
@FunctionalInterface
public interface ArgumentMapper<T> {

    /**
     * Maps the given component onto a native Minestom argument.
     *
     * @param component the component being mapped
     * @param parser    {@code component.parser()}, typed to the parser this mapper was registered for
     * @return the native argument
     */
    Argument<?> map(CommandComponent<?> component, ArgumentParser<?, T> parser);
}
