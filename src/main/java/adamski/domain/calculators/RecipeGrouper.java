package adamski.domain.calculators;

import adamski.domain.models.BankedXpResult;
import adamski.domain.models.ItemQuantities;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeGroup;
import adamski.domain.models.RecipeChain;
import adamski.domain.models.RecipeRun;
import adamski.domain.models.RecipeStage;
import adamski.domain.models.RecipeStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns the runs into one result per planned chain.
 * <p>
 * A run belongs to the chain that contains its recipe. Chains never overlap, because a second chain
 * only exists when its root cannot reach the first one's product.
 */
public final class RecipeGrouper {
    private RecipeGrouper() {
    }

    /**
     * @param byBankedItem one run list per item the player holds
     * @param chains       the planned chains, in the order they should be shown
     * @param owned        what the player holds, for the quantity entering each stage
     */
    public static List<RecipeGroup> group(Map<Integer, List<RecipeRun>> byBankedItem,
                                          List<RecipeChain> chains,
                                          ItemQuantities owned) {
        final Map<Integer, Integer> chainOfRecipe = new HashMap<>();
        for (int i = 0; i < chains.size(); i++) {
            for (Recipe recipe : chains.get(i).getRecipes()) {
                chainOfRecipe.put(recipe.getId(), i);
            }
        }

        final Map<Integer, Map<Integer, List<RecipeRun>>> runsByChainByBankedItem = new HashMap<>();

        byBankedItem.forEach((bankedItemId, runs) -> {
            for (RecipeRun run : runs) {
                final Integer chain = chainOfRecipe.get(run.getRecipe().getId());
                if (chain == null) continue;

                runsByChainByBankedItem
                        .computeIfAbsent(chain, k -> new HashMap<>())
                        .computeIfAbsent(bankedItemId, k -> new ArrayList<>())
                        .add(run);
            }
        });

        final List<RecipeGroup> groups = new ArrayList<>(chains.size());

        for (int i = 0; i < chains.size(); i++) {
            final Map<Integer, List<RecipeRun>> byBanked = runsByChainByBankedItem.get(i);
            if (byBanked == null) continue;

            groups.add(build(chains.get(i), byBanked, owned));
        }

        return groups;
    }

    private static RecipeGroup build(RecipeChain chain,
                                     Map<Integer, List<RecipeRun>> byBankedItem,
                                     ItemQuantities owned) {
        final Map<Recipe, Integer> position = new HashMap<>();
        for (int i = 0; i < chain.getRecipes().size(); i++) {
            position.put(chain.getRecipes().get(i), i);
        }

        final List<RecipeStage> stages = byBankedItem.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<Integer, List<RecipeRun>> e) -> maturity(e.getValue(), position))
                        .thenComparingInt(Map.Entry::getKey))
                .map(e -> new RecipeStage(e.getKey(), owned.get(e.getKey()), e.getValue(),
                        BankedXpCalculator.calculate(e.getValue()).getTotal()))
                .collect(Collectors.toList());

        final List<RecipeRun> allRuns = stages.stream()
                .flatMap(stage -> stage.getRuns().stream())
                .collect(Collectors.toList());

        final BankedXpResult whole = BankedXpCalculator.calculate(allRuns);

        return new RecipeGroup(
                chain.getRootItemId(),
                chain.getProductItemId(),
                stages,
                steps(whole, position),
                produced(allRuns, chain.getProductItemId()),
                SecondaryBalanceCalculator.demand(allRuns),
                whole.getTotal());
    }

    private static int maturity(List<RecipeRun> runs, Map<Recipe, Integer> position) {
        return runs.stream()
                .mapToInt(run -> position.getOrDefault(run.getRecipe(), Integer.MAX_VALUE))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private static List<RecipeStep> steps(BankedXpResult whole, Map<Recipe, Integer> position) {
        return whole.getXpPerRecipe().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> position.getOrDefault(e.getKey(), Integer.MAX_VALUE)))
                .map(e -> new RecipeStep(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private static double produced(List<RecipeRun> runs, int product) {
        double quantity = 0;

        for (RecipeRun run : runs) {
            if (run.getRecipe().getOutput().getItemId() == product) {
                quantity += run.getRuns() * run.getRecipe().getOutput().getQuantity();
            }
        }

        return quantity;
    }
}
