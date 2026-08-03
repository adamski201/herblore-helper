package adamski.domain.calculators;

import adamski.domain.models.BankedXpResult;
import adamski.domain.models.RecipeRun;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How much Herblore XP a set of recipe runs is worth.
 * <p>
 * A fold over {@link RecipeYieldCalculator} output - all the ordering and propagation happens
 * there, so there is nothing subtle left here.
 */
public final class BankedXpCalculator {
    private BankedXpCalculator() {
    }

    /**
     * @param yields how many times each recipe runs, from {@link RecipeYieldCalculator}
     */
    public static BankedXpResult calculate(List<RecipeRun> yields) {
        final Map<Integer, Double> xpPerRecipe = new HashMap<>();
        double total = 0;

        for (RecipeRun yield : yields) {
            final double xp = yield.getRuns() * yield.getRecipe().getXp();
            if (xp == 0) continue;

            total += xp;
            xpPerRecipe.put(yield.getRecipe().getId(), xp);
        }

        return new BankedXpResult(total, xpPerRecipe);
    }
}
