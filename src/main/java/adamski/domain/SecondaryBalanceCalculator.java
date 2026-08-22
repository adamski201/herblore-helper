package adamski.domain;


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
     * @param demanded what the runs consume, from {@link #sumDemand}
     * @param owned    what the player holds
     */
    public static SecondaryBalance netAgainstOwned(ItemQuantities demanded, ItemQuantities owned) {
        final Map<Integer, Double> net = new HashMap<>();

        demanded.forEach((itemId, required) -> net.put(itemId, owned.get(itemId) - required));

        return new SecondaryBalance(demanded, ItemQuantities.of(net));
    }

    /**
     * @return how much of each secondary the runs consume
     */
    public static ItemQuantities sumDemand(List<RecipeRun> yields) {
        final Map<Integer, Double> demanded = new HashMap<>();

        for (RecipeRun yield : yields) {
            for (Ingredient secondary : yield.getRecipe().getSecondaries()) {
                demanded.merge(secondary.getItemId(), yield.getRuns() * secondary.getQuantity(), Double::sum);
            }
        }

        return ItemQuantities.of(demanded);
    }
}
