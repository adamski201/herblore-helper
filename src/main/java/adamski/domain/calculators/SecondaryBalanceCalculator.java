package adamski.domain.calculators;

import adamski.domain.models.Ingredient;
import adamski.domain.models.RecipeRun;
import adamski.domain.models.SecondaryBalance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the planned runs will cost in secondaries, netted against what is held.
 * <p>
 * A fold over {@link RecipeYieldCalculator} output, like the XP calculator. Sharing the same runs
 * is what stops the shopping list and the XP total from being computed off different numbers.
 * <p>
 * Note that secondaries never constrain the cascade - it assumes they are purchasable. This is the
 * view that tells the player what purchasing that actually implies.
 */
public final class SecondaryBalanceCalculator {
    private SecondaryBalanceCalculator() {
    }

    /**
     * @param yields how many times each recipe runs, from {@link RecipeYieldCalculator}
     * @param owned  quantities in 1-dose units, the same map the cascade was given
     */
    public static SecondaryBalance calculate(List<RecipeRun> yields, Map<Integer, Integer> owned) {
        final Map<Integer, Double> demanded = new HashMap<>();

        for (RecipeRun yield : yields) {
            for (Ingredient secondary : yield.getRecipe().getSecondaries()) {
                demanded.merge(secondary.getItemId(), yield.getRuns() * secondary.getQuantity(), Double::sum);
            }
        }

        final Map<Integer, Double> net = new HashMap<>();
        demanded.forEach((itemId, required) -> net.put(itemId, owned.getOrDefault(itemId, 0) - required));

        return new SecondaryBalance(demanded, net);
    }
}
