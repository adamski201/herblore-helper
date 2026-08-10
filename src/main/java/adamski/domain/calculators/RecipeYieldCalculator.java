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
     * @param items          quantities in 1-dose units, keyed by item id
     * @param orderedRecipes recipes in order of resolved dependencies
     * @return the recipes that actually run, in the order applied. A recipe with nothing available
     * is omitted rather than reported with zero runs.
     */
    public static List<RecipeRun> calculate(Map<Integer, Integer> items, List<Recipe> orderedRecipes) {
        final Map<Integer, Double> available = new HashMap<>();
        items.forEach((itemId, quantity) -> available.put(itemId, (double) quantity));

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
