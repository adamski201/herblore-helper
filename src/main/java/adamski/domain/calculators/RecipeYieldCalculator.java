package adamski.domain.calculators;

import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeRun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecipeYieldCalculator {
    private RecipeYieldCalculator() {
    }

    /**
     * Calculating each item separately rather than over the merged bank leaves every total
     * unchanged - this is linear in its input - and makes each run traceable to what was banked.
     *
     * @param owned          quantities in 1-dose units, keyed by item id
     * @param orderedRecipes recipes in order of resolved dependencies
     * @return one run list per item, with items that produce nothing left out
     */
    public static Map<Integer, List<RecipeRun>> calculateByBankedItem(Map<Integer, Integer> owned,
                                                                      List<Recipe> orderedRecipes) {
        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();

        owned.forEach((itemId, quantity) -> {
            final List<RecipeRun> runs = calculate(itemId, quantity, orderedRecipes);

            if (!runs.isEmpty()) byBankedItem.put(itemId, runs);
        });

        return byBankedItem;
    }

    /**
     * @param itemId         the one item to start from, so every run returned is traceable to it
     * @param quantity       how many are held, in 1-dose units
     * @param orderedRecipes recipes in order of resolved dependencies
     * @return the recipes that actually run, in the order applied. A recipe with nothing available
     * is omitted rather than reported with zero runs.
     */
    public static List<RecipeRun> calculate(int itemId, int quantity, List<Recipe> orderedRecipes) {
        final Map<Integer, Double> available = new HashMap<>();
        available.put(itemId, (double) quantity);

        final List<RecipeRun> yields = new ArrayList<>();

        for (Recipe recipe : orderedRecipes) {
            final double qtyOwned = available.getOrDefault(recipe.getPrimary().getItemId(), 0d);
            if (qtyOwned == 0) continue;

            // Fractional!
            final double runs = qtyOwned / recipe.getPrimary().getQuantity();

            yields.add(new RecipeRun(recipe, runs));

            available.merge(recipe.getOutput().getItemId(),
                    runs * recipe.getOutput().getQuantity(), Double::sum);
        }

        return yields;
    }
}
