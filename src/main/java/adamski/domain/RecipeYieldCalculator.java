package adamski.domain;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What one recipe yields on what is available.
 */
final class RecipeYieldCalculator {
    private RecipeYieldCalculator() {
    }

    /**
     * The only place a recipe's yield is worked out, so anything that modifies it - an alchemist's
     * amulet adding a dose, a per-recipe setting - reaches the whole app by changing this.
     *
     * @param available how much of the recipe's primary there is, in 1-dose units
     * @return how many times it runs, fractionally, and what that makes
     */
    public static RecipeRun calculate(Recipe recipe, double available) {
        return new RecipeRun(recipe, available / recipe.getPrimary().getQuantity());
    }

    /**
     * Feeds one banked item through recipes in order, each one's output becoming what the next has
     * to work with.
     * <p>
     * Cascading each banked item separately rather than over the merged bank leaves every total
     * unchanged - this is linear in its input - and makes each run traceable to what was banked.
     *
     * @param itemId         the one item to start from, so every run returned is traceable to it
     * @param quantity       how much is held, in 1-dose units
     * @param orderedRecipes recipes in order of resolved dependencies
     * @return the recipes that actually run, in the order applied. A recipe with nothing available
     * is omitted rather than reported with zero runs.
     */
    public static List<RecipeRun> cascade(int itemId, double quantity, List<Recipe> orderedRecipes) {
        final Map<Integer, Double> available = new HashMap<>();
        available.put(itemId, quantity);

        final List<RecipeRun> yields = new ArrayList<>();

        for (Recipe recipe : orderedRecipes) {
            final double held = available.getOrDefault(recipe.getPrimary().getItemId(), 0d);
            if (held == 0) continue;

            final RecipeRun run = calculate(recipe, held);

            yields.add(run);

            available.merge(recipe.getOutput().getItemId(), run.getOutputQuantity(), Double::sum);
        }

        return yields;
    }
}
