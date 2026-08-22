package adamski.domain;


import java.util.ArrayList;
import java.util.List;

/**
 * Turns each chain into one result, by running the items banked along it against its own recipes.
 */
public final class ChainResultCalculator {
    private ChainResultCalculator() {
    }

    /**
     * @param chains the planned chains
     * @param owned  what the player holds
     * @return one result per chain, with chains nothing is banked against left out
     */
    public static List<ChainResult> calculate(List<RecipeChain> chains, ItemQuantities owned) {
        final List<ChainResult> results = new ArrayList<>(chains.size());

        for (RecipeChain chain : chains) {
            final List<ChainItemXp> byItem = calculateItemContributions(chain, owned);

            if (!byItem.isEmpty()) results.add(build(chain, byItem));
        }

        return results;
    }

    private static List<ChainItemXp> calculateItemContributions(RecipeChain chain, ItemQuantities owned) {
        final List<ChainItemXp> contributions = new ArrayList<>();

        for (Integer itemId : findOwnedItemsInChain(chain, owned)) {
            final double quantity = owned.get(itemId);
            final List<RecipeRun> runs = RecipeYieldCalculator.cascade(itemId, quantity, chain.getRecipes());

            if (!runs.isEmpty()) {
                contributions.add(new ChainItemXp(itemId, quantity, runs,
                        BankedXpCalculator.calculate(runs).getTotal()));
            }
        }

        return contributions;
    }

    private static List<Integer> findOwnedItemsInChain(RecipeChain chain, ItemQuantities owned) {
        final List<Integer> onChain = new ArrayList<>();

        if (owned.get(chain.getRootItemId()) > 0) onChain.add(chain.getRootItemId());

        for (Recipe recipe : chain.getRecipes()) {
            final int output = recipe.getOutput().getItemId();
            if (owned.get(output) > 0) onChain.add(output);
        }

        return onChain;
    }

    private static ChainResult build(RecipeChain chain, List<ChainItemXp> byItem) {
        final List<RecipeRun> allRuns = new ArrayList<>();
        for (ChainItemXp contribution : byItem) {
            allRuns.addAll(contribution.getRuns());
        }

        final BankedXpResult whole = BankedXpCalculator.calculate(allRuns);

        return new ChainResult(
                chain.getRootItemId(),
                chain.getProductItemId(),
                byItem,
                calculateRecipeContributions(chain, whole),
                sumOutputQuantity(allRuns, chain.getProductItemId()),
                SecondaryBalanceCalculator.sumDemand(allRuns),
                whole.getTotal());
    }

    private static List<ChainRecipeXp> calculateRecipeContributions(RecipeChain chain, BankedXpResult whole) {
        final List<ChainRecipeXp> contributions = new ArrayList<>();

        for (Recipe recipe : chain.getRecipes()) {
            final Double xp = whole.getXpPerRecipeId().get(recipe.getId());

            if (xp != null) contributions.add(new ChainRecipeXp(recipe, xp));
        }

        return contributions;
    }

    private static double sumOutputQuantity(List<RecipeRun> runs, int product) {
        double quantity = 0;

        for (RecipeRun run : runs) {
            if (run.getRecipe().getOutput().getItemId() == product) quantity += run.getOutputQuantity();
        }

        return quantity;
    }
}
