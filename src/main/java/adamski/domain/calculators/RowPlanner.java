package adamski.domain.calculators;

import adamski.domain.models.ItemQuantities;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out which rows the bank produces, before anything is calculated.
 * <p>
 * The least mature banked item claims a row and everything along its route joins it. Anything that
 * cannot reach that row's product starts a row of its own - which is what separates banked cadantine
 * blood vials from the cadantine going to super defence.
 */
public final class RowPlanner {
    private RowPlanner() {
    }

    /**
     * @param owned         what the player holds
     * @param productByItem the chosen product per row, keyed by the row's entry item
     */
    public static List<RecipeRow> plan(ItemQuantities owned, Map<Integer, Integer> productByItem, RecipeGraph graph) {
        final List<Integer> banked = new ArrayList<>(owned.itemIds());
        banked.sort(Comparator.comparingInt(graph::maturityOf));

        final Set<Integer> claimed = new HashSet<>();
        final List<RecipeRow> rows = new ArrayList<>();

        for (Integer itemId : banked) {
            if (claimed.contains(itemId)) continue;

            final int product = productByItem.getOrDefault(itemId, graph.findDefaultProduct(itemId));
            final List<Recipe> route = graph.findShortestRoute(itemId, product);
            if (route.isEmpty()) continue;

            rows.add(new RecipeRow(itemId, product, route));

            claimed.add(itemId);
            for (Recipe recipe : route) {
                claimed.add(recipe.getOutput().getItemId());
            }
        }

        return Collections.unmodifiableList(rows);
    }

    /**
     * Every row's route together, one recipe per primary, least mature first so the cascade always
     * produces an item before something consumes it.
     */
    public static List<Recipe> select(List<RecipeRow> rows, RecipeGraph graph) {
        final Map<Integer, Recipe> byPrimary = new HashMap<>();

        for (RecipeRow row : rows) {
            for (Recipe recipe : row.getRoute()) {
                final Recipe taken = byPrimary.putIfAbsent(recipe.getPrimary().getItemId(), recipe);

                if (taken != null && !taken.equals(recipe)) {
                    throw new IllegalStateException("rows disagree on what item " + recipe.getPrimary().getItemId() + " becomes: r" + taken.getId() + " and r" + recipe.getId());
                }
            }
        }

        final List<Recipe> ordered = new ArrayList<>(byPrimary.values());
        ordered.sort(Comparator.comparingInt(recipe -> graph.maturityOf(recipe.getPrimary().getItemId())));

        return Collections.unmodifiableList(ordered);
    }
}
