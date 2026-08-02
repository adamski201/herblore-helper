package adamski.data;

import adamski.domain.models.Recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sorts recipes in order of increasing dependency - irit seed produces grimy irit, which produces irit, which produces
 * irit (unf), etc. {@link #order()}
 * <p>
 * Secondaries are assumed to be infinite and not taken into account.
 */
public final class RecipeDependencyResolver {
    private static final Map<Integer, List<Recipe>> PRODUCERS = new HashMap<>();
    private static final List<Integer> ORDER;

    static {
        // Nodes are every id appearing as a primary or an output. Secondaries are not nodes.
        final Set<Integer> nodes = new LinkedHashSet<>();
        final Map<Integer, Set<Integer>> successors = new HashMap<>();
        final Map<Integer, Integer> inDegree = new HashMap<>();

        for (Recipe recipe : HerbloreRecipes.all()) {
            final int primary = recipe.getPrimary().getItemId();
            final int output = recipe.getOutput().getItemId();

            nodes.add(primary);
            nodes.add(output);

            PRODUCERS.computeIfAbsent(output, k -> new ArrayList<>()).add(recipe);

            // Deduplicate recipes that share primary/output pair
            if (successors.computeIfAbsent(primary, k -> new HashSet<>()).add(output)) {
                inDegree.merge(output, 1, Integer::sum);
            }
        }

        ORDER = kahn(nodes, successors, inDegree);
    }

    private RecipeDependencyResolver() {
    }

    /**
     * @return item ids in dependency order - an item never precedes one its production depends on
     */
    public static List<Integer> order() {
        return Collections.unmodifiableList(ORDER);
    }

    /**
     * @return the recipes whose output is this item, empty for a terminal item nothing produces
     */
    public static List<Recipe> producersOf(int itemId) {
        return Collections.unmodifiableList(PRODUCERS.getOrDefault(itemId, Collections.emptyList()));
    }

    private static List<Integer> kahn(Set<Integer> nodes,
                                      Map<Integer, Set<Integer>> successors,
                                      Map<Integer, Integer> inDegree) {
        final Map<Integer, Integer> remaining = new HashMap<>(inDegree);
        final Deque<Integer> ready = new ArrayDeque<>();
        final List<Integer> sorted = new ArrayList<>(nodes.size());

        for (Integer node : nodes) {
            if (remaining.getOrDefault(node, 0) == 0) ready.add(node);
        }

        while (!ready.isEmpty()) {
            final Integer node = ready.poll();
            sorted.add(node);

            for (Integer successor : successors.getOrDefault(node, Collections.emptySet())) {
                if (remaining.merge(successor, -1, Integer::sum) == 0) ready.add(successor);
            }
        }

        if (sorted.size() != nodes.size()) {
            final Set<Integer> cyclic = new LinkedHashSet<>(nodes);
            sorted.forEach(cyclic::remove);
            throw new IllegalStateException("recipe table has a dependency cycle among items: " + cyclic);
        }

        return sorted;
    }
}
