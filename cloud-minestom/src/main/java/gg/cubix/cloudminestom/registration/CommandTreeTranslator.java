package gg.cubix.cloudminestom.registration;

import gg.cubix.cloudminestom.argument.ArgumentMapperRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionCallback;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.internal.CommandNode;

/**
 * Pure function from a Cloud {@link CommandNode} (as returned by
 * {@code CommandManager#commandTree()}) to a nested native argument graph plus the flat list of
 * per-leaf syntaxes it corresponds to (docs/spec.md §5.1). No Minestom server interaction, so it is
 * unit-testable standalone against a hand-built {@link CommandNode} tree.
 *
 * <p>{@code rootNode}'s own component is never part of the output - it is the Minestom
 * {@code Command}'s own name/aliases, not an argument in any syntax.
 */
final class CommandTreeTranslator {

    private CommandTreeTranslator() {
    }

    /**
     * @param tree     the direct children of {@code rootNode}, translated recursively
     * @param syntaxes one entry per leaf found in the subtree (a leaf being any node - internal or
     *                 terminal - that owns a Cloud {@code Command}), in the order encountered
     */
    record Result(List<TranslatedNode> tree, List<Argument<?>[]> syntaxes) {
    }

    /**
     * One native argument plus the (already-translated) subtree beneath it.
     */
    record TranslatedNode(Argument<?> argument, List<TranslatedNode> children) {
    }

    static Result translate(
            final CommandNode<?> rootNode,
            final ArgumentMapperRegistry registry,
            final SuggestionCallback suggestionCallback
    ) {
        final List<TranslatedNode> tree = new ArrayList<>();
        final List<Argument<?>[]> syntaxes = new ArrayList<>();
        if (rootNode.command() != null) {
            syntaxes.add(new Argument<?>[0]);
        }
        for (final CommandNode<?> child : rootNode.children()) {
            walk(child, List.of(), tree, syntaxes, registry, suggestionCallback);
        }
        return new Result(tree, syntaxes);
    }

    private static void walk(
            final CommandNode<?> node,
            final List<Argument<?>> pathSoFar,
            final List<TranslatedNode> siblingsOut,
            final List<Argument<?>[]> syntaxesOut,
            final ArgumentMapperRegistry registry,
            final SuggestionCallback suggestionCallback
    ) {
        final boolean isLastInChain = node.children().isEmpty();
        for (final Argument<?> mapped : mapComponent(node.component(), isLastInChain, registry)) {
            mapped.setSuggestionCallback(suggestionCallback);

            final List<Argument<?>> path = new ArrayList<>(pathSoFar);
            path.add(mapped);

            final TranslatedNode translatedNode = new TranslatedNode(mapped, new ArrayList<>());
            siblingsOut.add(translatedNode);

            if (node.command() != null) {
                syntaxesOut.add(path.toArray(new Argument<?>[0]));
            }
            for (final CommandNode<?> child : node.children()) {
                walk(child, path, translatedNode.children(), syntaxesOut, registry, suggestionCallback);
            }
        }
    }

    /**
     * Returns one native argument per name a literal component accepts (canonical name plus every
     * alias), since {@link net.minestom.server.command.builder.arguments.ArgumentLiteral} - unlike
     * the root {@code Command} itself - has no native alias mechanism (spec.md §5.1); a single
     * variable component always maps to exactly one native argument.
     */
    private static List<Argument<?>> mapComponent(
            final CommandComponent<?> component,
            final boolean isLastInChain,
            final ArgumentMapperRegistry registry
    ) {
        if (component.type() == CommandComponent.ComponentType.FLAG) {
            // Flags (--name value, presence flags, -abc aliasing) have no Minestom-native node type
            // (spec.md §5.5, known limitation): the whole flag subtree degrades to a single trailing
            // greedy argument, the same shape P2 used for an entire command before native mapping
            // existed. Flags are always the tail of a command (Cloud collects them into one
            // component, appended after every other argument), so there is nothing to recurse into.
            return List.of(ArgumentType.StringArray(component.name()));
        }

        final List<Argument<?>> variants = new ArrayList<>();
        if (component.type() == CommandComponent.ComponentType.LITERAL) {
            for (final String name : component.aliases()) {
                variants.add(ArgumentType.Literal(name));
            }
        } else {
            variants.add(registry.map(component).orElseGet(() -> fallbackMap(component, isLastInChain)));
        }
        if (!component.required()) {
            variants.forEach(CommandTreeTranslator::markOptional);
        }
        return variants;
    }

    /**
     * Any parser without a registered {@link gg.cubix.cloudminestom.argument.ArgumentMapper} falls
     * back to a plain {@code Word} (single token) or {@code StringArray} (rest of the line) shape,
     * chosen by whether the component is the last one in its syntax (spec.md §5.1, row 3).
     */
    private static Argument<?> fallbackMap(final CommandComponent<?> component, final boolean isLastInChain) {
        return isLastInChain
                ? ArgumentType.StringArray(component.name())
                : ArgumentType.Word(component.name());
    }

    /**
     * Minestom has no {@code setOptional()} - {@link Argument#isOptional()} is derived from having a
     * non-null default value supplier ({@link Argument#setDefaultValue}), which is what makes
     * {@code Command#addSyntax} generate both the short and long form for a trailing optional
     * argument. The supplied value itself is never read: every syntax executor re-joins the raw
     * input and re-dispatches through Cloud (spec.md §5.4) rather than reading Minestom's own parsed
     * or default argument values, so {@code null} is exactly as good as a type-correct placeholder.
     */
    @SuppressWarnings("unchecked")
    private static void markOptional(final Argument<?> argument) {
        ((Argument<Object>) argument).setDefaultValue((Object) null);
    }
}
