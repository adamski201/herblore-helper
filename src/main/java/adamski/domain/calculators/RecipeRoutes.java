package adamski.domain.calculators;

import adamski.domain.models.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where each path can end, and which recipe each item takes.
 * <p>
 * A route stays on one path, so a chain crossing into another ends at the boundary - harralander
 * ends at stat restore, and guthix balance is the start of its own.
 */
public final class RecipeRoutes {
    private final Map<Integer, Integer> pathByItem;
    private final RecipeDependencyResolver dependencies;

    private final Map<Integer, List<Recipe>> options = new LinkedHashMap<>();
    private final Map<Integer, Set<Integer>> terminalsByPath = new LinkedHashMap<>();

    /**
     * @param recipes    every recipe the table offers, not a selection
     * @param pathByItem item id to the representative item of its path
     */
    public RecipeRoutes(List<Recipe> recipes, Map<Integer, Integer> pathByItem) {
        this.pathByItem = Map.copyOf(pathByItem);
        this.dependencies = new RecipeDependencyResolver(recipes);

        for (Recipe recipe : recipes) {
            options.computeIfAbsent(recipe.getPrimary().getItemId(), k -> new ArrayList<>()).add(recipe);
        }

        pathByItem.forEach((itemId, path) -> {
            if (isTerminal(itemId, path)) {
                terminalsByPath.computeIfAbsent(path, k -> new LinkedHashSet<>()).add(itemId);
            }
        });
    }

    /**
     * @return what this item can be turned into, in table order
     */
    public List<Recipe> optionsFor(int itemId) {
        return Collections.unmodifiableList(options.getOrDefault(itemId, Collections.emptyList()));
    }

    /**
     * @return the products this path can end at, as item ids
     */
    public Set<Integer> terminalsOf(int pathItemId) {
        return Collections.unmodifiableSet(terminalsByPath.getOrDefault(pathItemId, Collections.emptySet()));
    }

    /**
     * One recipe per primary item, in dependency order, each item taking the first option the table
     * offers it. Stands in for the config until it lands.
     */
    public List<Recipe> defaultSelection() {
        final Map<Integer, Recipe> byPrimary = new HashMap<>();
        options.forEach((itemId, offered) -> byPrimary.put(itemId, offered.get(0)));

        final List<Recipe> ordered = new ArrayList<>(byPrimary.size());
        for (Integer itemId : dependencies.order()) {
            final Recipe recipe = byPrimary.get(itemId);
            if (recipe != null) ordered.add(recipe);
        }

        return Collections.unmodifiableList(ordered);
    }

    /**
     * @return the product each item ends at under this selection, keyed by item id
     */
    public Map<Integer, Integer> terminalByItem(List<Recipe> selection) {
        final Map<Integer, Recipe> byPrimary = new HashMap<>();
        for (Recipe recipe : selection) {
            byPrimary.put(recipe.getPrimary().getItemId(), recipe);
        }

        final Map<Integer, Integer> terminals = new HashMap<>();
        pathByItem.forEach((itemId, path) -> terminals.put(itemId, follow(itemId, path, byPrimary)));

        return Collections.unmodifiableMap(terminals);
    }

    private boolean isTerminal(int itemId, int path) {
        if (dependencies.producersOf(itemId).isEmpty()) return false;

        for (Recipe recipe : options.getOrDefault(itemId, Collections.emptyList())) {
            final Integer output = pathByItem.get(recipe.getOutput().getItemId());
            if (output != null && output == path) return false;
        }

        return true;
    }

    private int follow(int itemId, int path, Map<Integer, Recipe> byPrimary) {
        int item = itemId;
        final Set<Integer> walked = new HashSet<>();

        while (walked.add(item)) {
            final Recipe recipe = byPrimary.get(item);
            if (recipe == null) break;

            final int output = recipe.getOutput().getItemId();
            final Integer outputPath = pathByItem.get(output);
            if (outputPath == null || outputPath != path) break;

            item = output;
        }

        return item;
    }
}
