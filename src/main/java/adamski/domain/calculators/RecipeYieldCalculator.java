package adamski.domain.calculators;

import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeRun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How many times each recipe can be run, given what the player owns.
 * <p>
 * The shared front half of every calculation. Producing an item feeds the recipe that consumes it,
 * so a single pass in dependency order resolves the whole chain: 8 grimy ranarr become 8 clean,
 * which become 8 unf, which become 24 doses of potion.
 * <p>
 * Everything downstream is a fold over the result - XP is runs times recipe xp, the secondaries
 * shopping list is runs times secondary quantity. Keeping this in one place is what stops those two
 * from being computed off different run counts.
 * <p>
 * Secondaries do not constrain anything: herbs are the bottleneck, secondaries are assumed
 * purchasable.
 */
public final class RecipeYieldCalculator {
    private RecipeYieldCalculator() {
    }

    /**
     * @param items          quantities in 1-dose units, keyed by item id, already merged across
     *                       whichever containers the caller counts
     * @param orderedRecipes one recipe per primary item, ordered so that every recipe producing an
     *                       item comes before the recipe consuming it. That ordering is what makes
     *                       a single pass correct: an item's quantity is final by the time the
     *                       recipe consuming it is reached. An item no recipe consumes is simply
     *                       absent, which is what makes it terminal.
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

            // Fractional - flooring at every step compounds badly through a six-deep chain, and
            // the figure is an estimate either way.
            final double runs = qtyOwned / recipe.getPrimary().getQuantity();

            yields.add(new RecipeRun(recipe, runs));

            available.merge(recipe.getOutput().getItemId(),
                    runs * recipe.getOutput().getQuantity(), Double::sum);
        }

        return yields;
    }
}
