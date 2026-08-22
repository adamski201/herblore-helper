package adamski.domain;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How much Herblore XP a set of recipe runs is worth.
 */
final class BankedXpCalculator {
    private BankedXpCalculator() {
    }

    /**
     * @param yields how many times each recipe runs, from {@link RecipeYieldCalculator}.
     */
    public static BankedXpResult calculate(List<RecipeRun> yields) {
        final Map<Integer, Double> xpPerRecipeId = new HashMap<>();
        double total = 0;

        for (RecipeRun yield : yields) {
            final double xp = yield.getRuns() * yield.getRecipe().getXp();
            if (xp == 0) continue;

            total += xp;
            xpPerRecipeId.merge(yield.getRecipe().getId(), xp, Double::sum);
        }

        return new BankedXpResult(total, xpPerRecipeId);
    }
}
