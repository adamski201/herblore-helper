package adamski.domain.result;

import adamski.domain.Ingredient;
import adamski.domain.ItemQuantities;
import adamski.domain.Recipe;
import adamski.domain.route.RecipeChain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs banked items against the recipes in each chain, calculating an XP & quantity result sliced by recipe and item.
 */
public final class ChainResultCalculator {
    private ChainResultCalculator() {
    }

    /**
     * @param chains recipe chains
     * @param owned  items the player owns
     * @return one result per chain, in the order the chains were given, with chains nothing is
     * banked against left out
     */
    public static List<ChainResult> calculate(List<RecipeChain> chains, ItemQuantities owned) {
        final List<ChainResult> results = new ArrayList<>(chains.size());

        for (RecipeChain chain : chains) {
            List<Integer> ownedItemsInChain = findOwnedItemsInChain(chain, owned);

            final List<ChainItemXp> itemContributions = new ArrayList<>();
            final Map<Integer, Double> xpByRecipeId = new HashMap<>();
            double totalXp = 0.0;
            final var allRuns = new ArrayList<RecipeRun>();

            for (Integer itemId : ownedItemsInChain) {
                final double quantity = owned.get(itemId);
                final List<RecipeRun> runs = cascade(itemId, quantity, chain);

                // A banked product makes nothing further, so it is left out. A root that makes
                // nothing has been told to stop, and is kept so its chain still has a row.
                if (runs.isEmpty() && itemId != chain.getRootItemId()) continue;

                double itemXp = 0;
                for (RecipeRun run : runs) {
                    final double xp = run.getRuns() * run.getRecipe().getXp();
                    itemXp += xp;
                    if (xp != 0) xpByRecipeId.merge(run.getRecipe().getId(), xp, Double::sum);
                }

                totalXp += itemXp;
                allRuns.addAll(runs);
                itemContributions.add(new ChainItemXp(itemId, quantity, runs, itemXp));
            }

            if (itemContributions.isEmpty()) continue;

            final List<ChainRecipeXp> recipeContributions = new ArrayList<>();

            for (Recipe recipe : chain.getRecipes()) {
                final Double xp = xpByRecipeId.get(recipe.getId());
                if (xp != null) recipeContributions.add(new ChainRecipeXp(recipe, xp));
            }

            results.add(new ChainResult(
                    chain.getRootItemId(),
                    chain.getProductItemId(),
                    itemContributions,
                    recipeContributions,
                    sumOutputQuantity(allRuns, chain.getProductItemId()),
                    sumSecondaryDemand(allRuns),
                    totalXp));
        }

        return results;
    }

    /**
     * Returns how many times each recipe runs when one banked item is fed along the chain from
     * where it sits, each output feeding the next.
     */
    static List<RecipeRun> cascade(int itemId, double quantity, RecipeChain chain) {
        final List<RecipeRun> runs = new ArrayList<>();

        if (quantity == 0) return runs;

        int currentItem = itemId;
        double held = quantity;

        for (Recipe recipe : chain.getRecipes()) {
            if (recipe.getPrimary().getItemId() != currentItem) continue;

            final RecipeRun run = new RecipeRun(recipe, held / recipe.getPrimary().getQuantity());
            runs.add(run);

            currentItem = recipe.getOutput().getItemId();
            held = run.getOutputQuantity();
        }

        return runs;
    }

    /**
     * @return how much of each secondary the given runs consume
     */
    static ItemQuantities sumSecondaryDemand(List<RecipeRun> runs) {
        final Map<Integer, Double> demanded = new HashMap<>();

        for (RecipeRun run : runs) {
            for (Ingredient secondary : run.getRecipe().getSecondaries()) {
                demanded.merge(secondary.getItemId(), run.getRuns() * secondary.getQuantity(), Double::sum);
            }
        }

        return ItemQuantities.of(demanded);
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

    private static double sumOutputQuantity(List<RecipeRun> runs, int product) {
        double quantity = 0;

        for (RecipeRun run : runs) {
            if (run.getRecipe().getOutput().getItemId() == product) quantity += run.getOutputQuantity();
        }

        return quantity;
    }
}
