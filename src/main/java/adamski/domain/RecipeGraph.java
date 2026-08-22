package adamski.domain;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recipe table as a graph, with a recipe being one edge from its primary to its output.
 * Secondaries are not part of the graph.
 */
public final class RecipeGraph {
    private final Map<Integer, List<Recipe>> options = new LinkedHashMap<>();
    private final Map<Integer, Integer> maturity = new HashMap<>();

    public RecipeGraph(List<Recipe> recipes) {
        final Set<Integer> items = new LinkedHashSet<>();

        for (Recipe recipe : recipes) {
            options.computeIfAbsent(recipe.getPrimary().getItemId(), k -> new ArrayList<>()).add(recipe);

            items.add(recipe.getPrimary().getItemId());
            items.add(recipe.getOutput().getItemId());
        }

        final List<Integer> sorted = sortByDependency(items);
        for (int i = 0; i < sorted.size(); i++) {
            maturity.put(sorted.get(i), i);
        }
    }

    /**
     * @return what this item can be turned into in one step, in table order
     */
    public List<Recipe> recipeOptionsFor(int itemId) {
        return Collections.unmodifiableList(options.getOrDefault(itemId, Collections.emptyList()));
    }

    /**
     * Everything this item can eventually become, which is what the product picker offers. Intermediates
     * are included - stopping at an unfinished potion is a valid choice.
     */
    public Set<Integer> findItemsReachableFrom(int itemId) {
        final Set<Integer> reached = new LinkedHashSet<>();
        final Deque<Integer> pending = new ArrayDeque<>();
        pending.add(itemId);

        while (!pending.isEmpty()) {
            for (Recipe recipe : recipeOptionsFor(pending.poll())) {
                final int output = recipe.getOutput().getItemId();
                if (reached.add(output)) pending.add(output);
            }
        }

        return Collections.unmodifiableSet(reached);
    }

    /**
     * The recipes turning one item into another, fewest steps first. A longer route is expressed by
     * choosing an intermediate as a product in its own right, so the shortest is the one meant.
     *
     * @return the recipes to apply in order, empty if the product cannot be reached
     */
    public List<Recipe> findShortestRoute(int from, int product) {
        final Map<Integer, Recipe> arrivedBy = new HashMap<>();
        final Deque<Integer> pending = new ArrayDeque<>();
        pending.add(from);

        while (!pending.isEmpty()) {
            for (Recipe recipe : recipeOptionsFor(pending.poll())) {
                final int output = recipe.getOutput().getItemId();
                if (output == from || arrivedBy.containsKey(output)) continue;

                arrivedBy.put(output, recipe);
                if (output == product) return retrace(arrivedBy, from, product);

                pending.add(output);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Where this item ends up when every step takes the first option the table offers. Stands in for
     * the config until it lands.
     */
    public int findDefaultProduct(int itemId) {
        int item = itemId;
        final Set<Integer> walked = new HashSet<>();

        while (walked.add(item) && !recipeOptionsFor(item).isEmpty()) {
            item = recipeOptionsFor(item).get(0).getOutput().getItemId();
        }

        return item;
    }

    /**
     * How far along a chain an item sits. Lower comes first, and an item always ranks below anything
     * its production depends on, so sorting by this never puts a consumer before its producer.
     */
    public int maturityOf(int itemId) {
        return maturity.getOrDefault(itemId, Integer.MAX_VALUE);
    }

    private static List<Recipe> retrace(Map<Integer, Recipe> arrivedBy, int from, int product) {
        final List<Recipe> route = new ArrayList<>();

        int item = product;
        while (item != from) {
            final Recipe recipe = arrivedBy.get(item);
            route.add(recipe);
            item = recipe.getPrimary().getItemId();
        }

        Collections.reverse(route);

        return Collections.unmodifiableList(route);
    }

    private List<Integer> sortByDependency(Set<Integer> items) {
        final Map<Integer, Integer> remaining = new HashMap<>();
        for (Integer item : items) {
            for (Recipe recipe : recipeOptionsFor(item)) {
                remaining.merge(recipe.getOutput().getItemId(), 1, Integer::sum);
            }
        }

        final Deque<Integer> ready = new ArrayDeque<>();
        final List<Integer> sorted = new ArrayList<>(items.size());

        for (Integer item : items) {
            if (remaining.getOrDefault(item, 0) == 0) ready.add(item);
        }

        while (!ready.isEmpty()) {
            final Integer item = ready.poll();
            sorted.add(item);

            for (Recipe recipe : recipeOptionsFor(item)) {
                final int output = recipe.getOutput().getItemId();
                if (remaining.merge(output, -1, Integer::sum) == 0) ready.add(output);
            }
        }

        if (sorted.size() != items.size()) {
            final Set<Integer> cyclic = new LinkedHashSet<>(items);
            sorted.forEach(cyclic::remove);
            throw new IllegalStateException("recipe table has a dependency cycle among items: " + cyclic);
        }

        return sorted;
    }
}
