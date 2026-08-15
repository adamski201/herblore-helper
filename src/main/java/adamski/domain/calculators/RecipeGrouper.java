package adamski.domain.calculators;

import adamski.domain.models.BankedXpResult;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeGroup;
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
 * Sorts recipe runs into one group per product.
 * <p>
 * A run belongs to the path of the item it <em>produces</em>, so one banked item's chain can cross
 * paths - harralander seeds make stat restore on the harralander path, then guthix balance on its own.
 */
public final class RecipeGrouper {
    private RecipeGrouper() {
    }

    /**
     * @param byBankedItem   one run list per item the player holds
     * @param pathByItem     item id to the representative item of its path
     * @param terminalByItem item id to the product its path ends at
     * @param orderedRecipes in dependency order, so a recipe's index in it is its maturity
     * @return one group per product made, its stages least mature first and its steps in dependency order
     */
    public static List<RecipeGroup> group(Map<Integer, List<RecipeRun>> byBankedItem,
                                          Map<Integer, Integer> pathByItem,
                                          Map<Integer, Integer> terminalByItem,
                                          List<Recipe> orderedRecipes) {
        final Map<Integer, Integer> position = new HashMap<>();
        for (int i = 0; i < orderedRecipes.size(); i++) {
            position.put(orderedRecipes.get(i).getId(), i);
        }

        final Map<Integer, Map<Integer, Contribution>> byTerminal = new HashMap<>();

        byBankedItem.values().forEach(runs -> {
            Integer terminal = null;
            Contribution contribution = null;

            for (RecipeRun run : runs) {
                // Guarded by RecipePathsTest - a dropped run would leave the total above the groups
                final Integer produces = terminalByItem.get(run.getRecipe().getOutput().getItemId());
                if (produces == null) continue;

                if (!produces.equals(terminal)) {
                    terminal = produces;
                    contribution = byTerminal
                            .computeIfAbsent(terminal, k -> new HashMap<>())
                            .computeIfAbsent(run.getRecipe().getPrimary().getItemId(), k -> new Contribution());
                    contribution.enter(entering(run), positionOf(position, run));
                }

                contribution.add(run);
            }
        });

        final List<RecipeGroup> groups = new ArrayList<>(byTerminal.size());

        byTerminal.forEach((terminal, contributions) -> {
            final List<RecipeStage> stages = contributions.entrySet().stream()
                    .sorted(Comparator
                            .comparingInt((Map.Entry<Integer, Contribution> e) -> e.getValue().position)
                            .thenComparingInt(Map.Entry::getKey))
                    .map(e -> e.getValue().toStage(e.getKey()))
                    .collect(Collectors.toList());

            final List<RecipeRun> allRuns = stages.stream()
                    .flatMap(stage -> stage.getRuns().stream())
                    .collect(Collectors.toList());

            final BankedXpResult whole = BankedXpCalculator.calculate(allRuns);

            groups.add(new RecipeGroup(
                    pathByItem.get(terminal),
                    terminal,
                    stages,
                    stepsOf(whole, allRuns, position),
                    produced(allRuns, terminal),
                    SecondaryBalanceCalculator.demand(allRuns),
                    whole.getTotal()));
        });

        groups.sort(Comparator
                .comparingInt((RecipeGroup group) -> earliestRecipe(position, group))
                .thenComparingInt(RecipeGroup::getTerminalItemId));

        return groups;
    }

    private static List<RecipeStep> stepsOf(BankedXpResult whole, List<RecipeRun> runs,
                                            Map<Integer, Integer> position) {
        final Map<Integer, Recipe> recipeById = runs.stream()
                .collect(Collectors.toMap(run -> run.getRecipe().getId(), RecipeRun::getRecipe, (a, b) -> a));

        return whole.getXpPerRecipe().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> position.getOrDefault(e.getKey(), Integer.MAX_VALUE)))
                .map(e -> new RecipeStep(recipeById.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    private static double produced(List<RecipeRun> runs, int terminal) {
        double quantity = 0;

        for (RecipeRun run : runs) {
            if (run.getRecipe().getOutput().getItemId() == terminal) {
                quantity += run.getRuns() * run.getRecipe().getOutput().getQuantity();
            }
        }

        return quantity;
    }

    private static double entering(RecipeRun run) {
        return run.getRuns() * run.getRecipe().getPrimary().getQuantity();
    }

    private static int positionOf(Map<Integer, Integer> position, RecipeRun run) {
        return position.getOrDefault(run.getRecipe().getId(), Integer.MAX_VALUE);
    }

    private static int earliestRecipe(Map<Integer, Integer> position, RecipeGroup group) {
        return group.getStages().stream()
                .flatMap(stage -> stage.getRuns().stream())
                .mapToInt(run -> positionOf(position, run))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private static final class Contribution {
        private final List<RecipeRun> runs = new ArrayList<>();
        private double quantity;
        private int position = Integer.MAX_VALUE;

        private void enter(double quantity, int position) {
            this.quantity += quantity;
            this.position = Math.min(this.position, position);
        }

        private void add(RecipeRun run) {
            runs.add(run);
        }

        private RecipeStage toStage(int entryItemId) {
            return new RecipeStage(entryItemId, quantity, runs, BankedXpCalculator.calculate(runs).getTotal());
        }
    }
}
