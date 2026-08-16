package adamski.domain.calculators;

import adamski.domain.models.BankedXpResult;
import adamski.domain.models.ItemQuantities;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeGroup;
import adamski.domain.models.RecipeRow;
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
 * Turns the runs into one result per planned row.
 * <p>
 * A run belongs to the row whose route contains its recipe. Routes never overlap, because two rows
 * only exist when one cannot reach the other's product.
 */
public final class RecipeGrouper {
    private RecipeGrouper() {
    }

    /**
     * @param byBankedItem one run list per item the player holds
     * @param rows         the planned rows, in the order they should be shown
     * @param owned        what the player holds, for the quantity entering each stage
     */
    public static List<RecipeGroup> group(Map<Integer, List<RecipeRun>> byBankedItem,
                                          List<RecipeRow> rows,
                                          ItemQuantities owned) {
        final Map<Integer, Integer> rowOfRecipe = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            for (Recipe recipe : rows.get(i).getRoute()) {
                rowOfRecipe.put(recipe.getId(), i);
            }
        }

        final Map<Integer, Map<Integer, List<RecipeRun>>> runsByRowByBankedItem = new HashMap<>();

        byBankedItem.forEach((bankedItemId, runs) -> {
            for (RecipeRun run : runs) {
                final Integer row = rowOfRecipe.get(run.getRecipe().getId());
                if (row == null) continue;

                runsByRowByBankedItem
                        .computeIfAbsent(row, k -> new HashMap<>())
                        .computeIfAbsent(bankedItemId, k -> new ArrayList<>())
                        .add(run);
            }
        });

        final List<RecipeGroup> groups = new ArrayList<>(rows.size());

        for (int i = 0; i < rows.size(); i++) {
            final Map<Integer, List<RecipeRun>> byBanked = runsByRowByBankedItem.get(i);
            if (byBanked == null) continue;

            groups.add(build(rows.get(i), byBanked, owned));
        }

        return groups;
    }

    private static RecipeGroup build(RecipeRow row,
                                     Map<Integer, List<RecipeRun>> byBankedItem,
                                     ItemQuantities owned) {
        final Map<Recipe, Integer> position = new HashMap<>();
        for (int i = 0; i < row.getRoute().size(); i++) {
            position.put(row.getRoute().get(i), i);
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
                row.getEntryItemId(),
                row.getProductItemId(),
                stages,
                steps(whole, position),
                produced(allRuns, row.getProductItemId()),
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
