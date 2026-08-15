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

        final Map<Integer, Map<Integer, List<Segment>>> byTerminal = new HashMap<>();

        byBankedItem.values().stream()
                .flatMap(runs -> segments(runs, terminalByItem).stream())
                .forEach(segment -> byTerminal
                        .computeIfAbsent(segment.terminal, k -> new HashMap<>())
                        .computeIfAbsent(segment.entryItemId, k -> new ArrayList<>())
                        .add(segment));

        final List<RecipeGroup> groups = new ArrayList<>(byTerminal.size());

        byTerminal.forEach((terminal, byEntryItem) -> {
            final List<RecipeStage> stages = byEntryItem.entrySet().stream()
                    .sorted(Comparator
                            .comparingInt((Map.Entry<Integer, List<Segment>> e) -> maturity(e.getValue(), position))
                            .thenComparingInt(Map.Entry::getKey))
                    .map(e -> stage(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            final List<RecipeRun> allRuns = stages.stream()
                    .flatMap(stage -> stage.getRuns().stream())
                    .collect(Collectors.toList());

            final BankedXpResult whole = BankedXpCalculator.calculate(allRuns);

            groups.add(new RecipeGroup(
                    pathByItem.get(terminal),
                    terminal,
                    stages,
                    steps(whole, allRuns, position),
                    produced(allRuns, terminal),
                    SecondaryBalanceCalculator.demand(allRuns),
                    whole.getTotal()));
        });

        groups.sort(Comparator
                .comparingInt((RecipeGroup group) -> earliestRecipe(position, group))
                .thenComparingInt(RecipeGroup::getTerminalItemId));

        return groups;
    }

    /**
     * Cuts one banked item's chain where it crosses from one product to the next.
     */
    private static List<Segment> segments(List<RecipeRun> runs, Map<Integer, Integer> terminalByItem) {
        final List<Segment> segments = new ArrayList<>();
        Segment current = null;

        for (RecipeRun run : runs) {
            // Guarded by RecipePathsTest - a dropped run would leave the total above the groups
            final Integer produces = terminalByItem.get(run.getRecipe().getOutput().getItemId());
            if (produces == null) continue;

            if (current == null || current.terminal != produces) {
                current = new Segment(produces, run);
                segments.add(current);
            }

            current.runs.add(run);
        }

        return segments;
    }

    private static RecipeStage stage(int entryItemId, List<Segment> segments) {
        final List<RecipeRun> runs = segments.stream()
                .flatMap(segment -> segment.runs.stream())
                .collect(Collectors.toList());

        final double quantity = segments.stream().mapToDouble(segment -> segment.quantity).sum();

        return new RecipeStage(entryItemId, quantity, runs, BankedXpCalculator.calculate(runs).getTotal());
    }

    private static int maturity(List<Segment> segments, Map<Integer, Integer> position) {
        return segments.stream()
                .mapToInt(segment -> positionOf(position, segment.runs.get(0)))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private static List<RecipeStep> steps(BankedXpResult whole, List<RecipeRun> runs,
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

    private static final class Segment {
        private final int terminal;
        private final int entryItemId;
        private final double quantity;
        private final List<RecipeRun> runs = new ArrayList<>();

        private Segment(int terminal, RecipeRun first) {
            this.terminal = terminal;
            this.entryItemId = first.getRecipe().getPrimary().getItemId();
            this.quantity = first.getRuns() * first.getRecipe().getPrimary().getQuantity();
        }
    }
}
