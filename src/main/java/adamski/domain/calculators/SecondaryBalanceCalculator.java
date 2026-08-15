package adamski.domain.calculators;

import adamski.domain.models.Ingredient;
import adamski.domain.models.RecipeRun;
import adamski.domain.models.SecondaryBalance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the planned runs will cost in secondaries, netted against what is held.
 */
public final class SecondaryBalanceCalculator {
    private SecondaryBalanceCalculator() {
    }

    /**
     * @param yields how many times each recipe runs
     * @param owned  quantities in 1-dose units, keyed by item id
     */
    public static SecondaryBalance calculate(List<RecipeRun> yields, Map<Integer, Integer> owned) {
        final Map<Integer, Double> demanded = demand(yields);
        final Map<Integer, Double> net = new HashMap<>();

        demanded.forEach((itemId, required) -> net.put(itemId, owned.getOrDefault(itemId, 0) - required));

        return new SecondaryBalance(demanded, net);
    }

    /**
     * @return how much of each secondary the runs consume, keyed by item id
     */
    public static Map<Integer, Double> demand(List<RecipeRun> yields) {
        final Map<Integer, Double> demanded = new HashMap<>();

        for (RecipeRun yield : yields) {
            for (Ingredient secondary : yield.getRecipe().getSecondaries()) {
                demanded.merge(secondary.getItemId(), yield.getRuns() * secondary.getQuantity(), Double::sum);
            }
        }

        return demanded;
    }
}
