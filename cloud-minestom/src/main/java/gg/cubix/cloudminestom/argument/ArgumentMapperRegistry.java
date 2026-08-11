package gg.cubix.cloudminestom.argument;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minestom.server.command.builder.arguments.Argument;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.parser.ArgumentParser;

/**
 * Registers and looks up {@link ArgumentMapper}s by Cloud parser class (docs/spec.md §5.2), mirroring
 * how {@code CloudBrigadierManager} lets consumers register Cloud-parser → Brigadier-{@code ArgumentType}
 * mappings.
 */
public final class ArgumentMapperRegistry {

    private final Map<Class<?>, ArgumentMapper<?>> mappers = new HashMap<>();

    /**
     * Creates an empty registry with no mappers.
     */
    public ArgumentMapperRegistry() {
    }

    /**
     * Creates a registry pre-populated with the built-in mappers (spec.md §5.2's mapping table).
     *
     * @return a new registry with the default mappers registered
     */
    public static ArgumentMapperRegistry createDefault() {
        final ArgumentMapperRegistry registry = new ArgumentMapperRegistry();
        StandardArgumentMappers.registerAll(registry);
        return registry;
    }

    /**
     * Registers a mapper for the given Cloud parser class, replacing any mapper already registered
     * for it.
     *
     * <p>{@code parserType} is a raw {@link Class}, not generically tied to {@code <T>}: a
     * {@code .class} literal on a generic type (e.g. {@code IntegerParser.class}) always erases to a
     * raw {@code Class}, so there is no way to express that link statically here. {@link #map} is
     * where the corresponding unchecked cast happens, once, in one place.
     *
     * @param parserType the concrete {@link ArgumentParser} class to map, e.g. {@code IntegerParser.class}
     * @param mapper     the mapper to use for that parser type
     * @param <T>        the type of value the parser produces
     */
    public <T> void register(final Class<?> parserType, final ArgumentMapper<T> mapper) {
        Objects.requireNonNull(parserType, "parserType");
        Objects.requireNonNull(mapper, "mapper");
        this.mappers.put(parserType, mapper);
    }

    /**
     * Maps the given component using the mapper registered for its parser's class, if any.
     *
     * @param component the component to map
     * @return the mapped argument, or empty if no mapper is registered for the component's parser
     */
    public Optional<Argument<?>> map(final CommandComponent<?> component) {
        Objects.requireNonNull(component, "component");
        final ArgumentMapper<?> mapper = this.mappers.get(component.parser().getClass());
        if (mapper == null) {
            return Optional.empty();
        }
        return Optional.of(dispatch(component, mapper));
    }

    @SuppressWarnings("unchecked")
    private static <T> Argument<?> dispatch(final CommandComponent<?> component, final ArgumentMapper<T> mapper) {
        return mapper.map(component, (ArgumentParser<?, T>) component.parser());
    }
}
