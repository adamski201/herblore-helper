package adamski.data;

import adamski.domain.models.Ingredient;
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
 * Where each path can end, and which recipes get an item there.
 * <p>
 * A route stays on one path, so a chain crossing into another ends at the boundary - harralander
 * ends at stat restore, and guthix balance is the start of its own route.
 */
public final class RecipeRoutes {
    private static final Map<Integer, Set<Integer>> TERMINALS_BY_PATH = new LinkedHashMap<>();
    private static final Map<Integer, Map<Integer, Recipe>> NEXT_BY_TERMINAL = new LinkedHashMap<>();

    static {
        final Map<Integer, List<Recipe>> consumers = new HashMap<>();
        for (Recipe recipe : Recipes.all()) {
            consumers.computeIfAbsent(recipe.getPrimary().getItemId(), k -> new ArrayList<>()).add(recipe);
        }

        RecipePaths.pathsByItem().forEach((itemId, path) -> {
            if (isTerminal(itemId, path, consumers)) {
                TERMINALS_BY_PATH.computeIfAbsent(path, k -> new LinkedHashSet<>()).add(itemId);
            }
        });

        final List<Integer> outputsFirst = new ArrayList<>(RecipeDependencyResolver.order());
        Collections.reverse(outputsFirst);

        TERMINALS_BY_PATH.values().forEach(terminals -> terminals.forEach(
                terminal -> NEXT_BY_TERMINAL.put(terminal, routesTo(terminal, outputsFirst))));
    }

    private RecipeRoutes() {
    }

    /**
     * @return the products this path can end at, as item ids
     */
    public static Set<Integer> terminalsOf(int pathItemId) {
        return Collections.unmodifiableSet(TERMINALS_BY_PATH.getOrDefault(pathItemId, Collections.emptySet()));
    }

    /**
     * @return every item a route reaches this terminal from
     */
    public static Set<Integer> itemsReaching(int terminal) {
        return Collections.unmodifiableSet(NEXT_BY_TERMINAL.getOrDefault(terminal, Collections.emptyMap()).keySet());
    }

    /**
     * @return the recipe starting the best route from this item, or null if it cannot get there
     */
    public static Recipe nextTowards(int itemId, int terminal) {
        return NEXT_BY_TERMINAL.getOrDefault(terminal, Collections.emptyMap()).get(itemId);
    }

    /**
     * One recipe per primary, in dependency order. An item that cannot reach its path's chosen
     * terminal takes its own best route instead, which is what gives banked cadantine blood vials
     * somewhere to go when the path is set to super defence.
     *
     * @param terminalByPath the product chosen for each path
     */
    public static List<Recipe> select(Map<Integer, Integer> terminalByPath) {
        final Map<Integer, Recipe> byPrimary = new HashMap<>();

        terminalByPath.values().forEach(terminal -> takeRoutes(byPrimary, terminal));

        for (Integer itemId : RecipeDependencyResolver.order()) {
            if (byPrimary.containsKey(itemId)) continue;

            final Integer terminal = nearestTerminal(itemId);
            if (terminal != null) takeRoute(byPrimary, itemId, terminal);
        }

        final List<Recipe> ordered = new ArrayList<>(byPrimary.size());
        for (Integer itemId : RecipeDependencyResolver.order()) {
            final Recipe recipe = byPrimary.get(itemId);
            if (recipe != null) ordered.add(recipe);
        }

        return Collections.unmodifiableList(ordered);
    }

    /**
     * The product each path ends at when every primary takes the first recipe offered by the table.
     */
    public static Map<Integer, Integer> defaultTerminals() {
        final Map<Integer, Recipe> firstOffered = new HashMap<>();
        for (Recipe recipe : Recipes.all()) {
            firstOffered.putIfAbsent(recipe.getPrimary().getItemId(), recipe);
        }

        final Map<Integer, Integer> terminals = new LinkedHashMap<>();
        for (Integer path : RecipePaths.paths()) {
            terminals.put(path, follow(path, path, firstOffered));
        }

        return terminals;
    }

    /**
     * @return the terminal each item ends at under this selection, keyed by item id
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

    private static boolean isTerminal(int itemId, int path, Map<Integer, List<Recipe>> consumers) {
        if (RecipeDependencyResolver.producersOf(itemId).isEmpty()) return false;

        for (Recipe recipe : consumers.getOrDefault(itemId, Collections.emptyList())) {
            if (RecipePaths.pathOf(recipe.getOutput().getItemId()) == path) return false;
        }

        return true;
    }

    private static Map<Integer, Recipe> routesTo(int terminal, List<Integer> outputsFirst) {
        final int path = RecipePaths.pathOf(terminal);
        final Map<Integer, long[]> cost = new HashMap<>();
        final Map<Integer, Recipe> next = new HashMap<>();

        cost.put(terminal, new long[]{0, 0});

        for (Integer itemId : outputsFirst) {
            final long[] here = cost.get(itemId);
            if (here == null || RecipePaths.pathOf(itemId) != path) continue;

            for (Recipe recipe : RecipeDependencyResolver.producersOf(itemId)) {
                final int primary = recipe.getPrimary().getItemId();
                final long[] candidate = {here[0] + 1, here[1] + secondaryQuantity(recipe)};

                if (isBetter(candidate, recipe, cost.get(primary), next.get(primary))) {
                    cost.put(primary, candidate);
                    next.put(primary, recipe);
                }
            }
        }

        next.remove(terminal);

        return next;
    }

    private static boolean isBetter(long[] candidate, Recipe recipe, long[] best, Recipe bestRecipe) {
        if (best == null) return true;
        if (candidate[0] != best[0]) return candidate[0] < best[0];
        if (candidate[1] != best[1]) return candidate[1] < best[1];

        return recipe.getId() < bestRecipe.getId();
    }

    private static long secondaryQuantity(Recipe recipe) {
        long total = 0;
        for (Ingredient secondary : recipe.getSecondaries()) {
            total += secondary.getQuantity();
        }

        return total;
    }

    /**
     * Super energy can become stamina or divine super energy, on two different paths, so the lower
     * recipe id wins rather than whichever path is resolved first.
     */
    private static void takeRoutes(Map<Integer, Recipe> byPrimary, int terminal) {
        NEXT_BY_TERMINAL.getOrDefault(terminal, Collections.emptyMap()).forEach(
                (itemId, recipe) -> byPrimary.merge(itemId, recipe, (a, b) -> a.getId() <= b.getId() ? a : b));
    }

    private static void takeRoute(Map<Integer, Recipe> byPrimary, int itemId, int terminal) {
        final Map<Integer, Recipe> next = NEXT_BY_TERMINAL.get(terminal);

        int item = itemId;
        final Set<Integer> walked = new HashSet<>();

        while (walked.add(item) && next.containsKey(item)) {
            final Recipe recipe = next.get(item);
            byPrimary.putIfAbsent(item, recipe);
            item = recipe.getOutput().getItemId();
        }
    }

    private static Integer nearestTerminal(int itemId) {
        Integer nearest = null;
        int fewestSteps = Integer.MAX_VALUE;
        int lowestRecipe = Integer.MAX_VALUE;

        for (Map.Entry<Integer, Map<Integer, Recipe>> entry : NEXT_BY_TERMINAL.entrySet()) {
            final Recipe first = entry.getValue().get(itemId);
            if (first == null) continue;

            final int steps = stepsTo(itemId, entry.getValue());
            if (steps < fewestSteps || (steps == fewestSteps && first.getId() < lowestRecipe)) {
                nearest = entry.getKey();
                fewestSteps = steps;
                lowestRecipe = first.getId();
            }
        }

        return nearest;
    }

    private static int stepsTo(int itemId, Map<Integer, Recipe> next) {
        int steps = 0;
        int item = itemId;
        final Set<Integer> walked = new HashSet<>();

        while (walked.add(item) && next.containsKey(item)) {
            item = next.get(item).getOutput().getItemId();
            steps++;
        }

        return steps;
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
