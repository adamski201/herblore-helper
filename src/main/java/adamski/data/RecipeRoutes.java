package adamski.data;

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
    private static final Map<Integer, List<Recipe>> OPTIONS = new LinkedHashMap<>();
    private static final Map<Integer, Set<Integer>> TERMINALS_BY_PATH = new LinkedHashMap<>();

    static {
        for (Recipe recipe : Recipes.all()) {
            OPTIONS.computeIfAbsent(recipe.getPrimary().getItemId(), k -> new ArrayList<>()).add(recipe);
        }

        RecipePaths.pathsByItem().forEach((itemId, path) -> {
            if (isTerminal(itemId, path)) {
                TERMINALS_BY_PATH.computeIfAbsent(path, k -> new LinkedHashSet<>()).add(itemId);
            }
        });
    }

    private RecipeRoutes() {
    }

    /**
     * @return what this item can be turned into, in table order
     */
    public static List<Recipe> optionsFor(int itemId) {
        return Collections.unmodifiableList(OPTIONS.getOrDefault(itemId, Collections.emptyList()));
    }

    /**
     * @return the products this path can end at, as item ids
     */
    public static Set<Integer> terminalsOf(int pathItemId) {
        return Collections.unmodifiableSet(TERMINALS_BY_PATH.getOrDefault(pathItemId, Collections.emptySet()));
    }

    /**
     * One recipe per primary item, in dependency order, each item taking the first option the table
     * offers it. Stands in for the config until it lands.
     */
    public static List<Recipe> defaultSelection() {
        final Map<Integer, Recipe> byPrimary = new HashMap<>();
        OPTIONS.forEach((itemId, options) -> byPrimary.put(itemId, options.get(0)));

        final List<Recipe> ordered = new ArrayList<>(byPrimary.size());
        for (Integer itemId : RecipeDependencyResolver.order()) {
            final Recipe recipe = byPrimary.get(itemId);
            if (recipe != null) ordered.add(recipe);
        }

        return Collections.unmodifiableList(ordered);
    }

    /**
     * @return the product each item ends at under this selection, keyed by item id
     */
    public static Map<Integer, Integer> terminalByItem(List<Recipe> selection) {
        final Map<Integer, Recipe> byPrimary = new HashMap<>();
        for (Recipe recipe : selection) {
            byPrimary.put(recipe.getPrimary().getItemId(), recipe);
        }

        final Map<Integer, Integer> terminals = new HashMap<>();
        RecipePaths.pathsByItem().forEach(
                (itemId, path) -> terminals.put(itemId, follow(itemId, path, byPrimary)));

        return Collections.unmodifiableMap(terminals);
    }

    private static boolean isTerminal(int itemId, int path) {
        if (RecipeDependencyResolver.producersOf(itemId).isEmpty()) return false;

        for (Recipe recipe : OPTIONS.getOrDefault(itemId, Collections.emptyList())) {
            if (RecipePaths.pathOf(recipe.getOutput().getItemId()) == path) return false;
        }

        return true;
    }

    private static int follow(int itemId, int path, Map<Integer, Recipe> byPrimary) {
        int item = itemId;
        final Set<Integer> walked = new HashSet<>();

        while (walked.add(item)) {
            final Recipe recipe = byPrimary.get(item);
            if (recipe == null) break;

            final int output = recipe.getOutput().getItemId();
            if (RecipePaths.pathOf(output) != path) break;

            item = output;
        }

        return item;
    }
}
