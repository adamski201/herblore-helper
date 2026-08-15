package adamski.domain.calculators;

import adamski.domain.models.Ingredient;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeGroup;
import adamski.domain.models.RecipeRun;
import adamski.domain.models.RecipeStage;
import adamski.domain.models.RecipeStep;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Synthetic recipes - the real table encodes selection, which is a placeholder.
 * <p>
 * 1 -> 2 -> 3 -> 4 is one path ending at 4, and 4 -> 5 is a second hanging off its end.
 */
public class RecipeGrouperTest {
    private static final int PATH_A = 2;
    private static final int PATH_B = 5;

    private static final Recipe ONE_TO_TWO = recipe(10, 1, 2);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 3);
    private static final Recipe THREE_TO_FOUR = recipe(12, 3, 4);
    private static final Recipe FOUR_TO_FIVE = recipe(13, 4, 5);

    private static final List<Recipe> RECIPES =
            Arrays.asList(ONE_TO_TWO, TWO_TO_THREE, THREE_TO_FOUR, FOUR_TO_FIVE);

    private static final Map<Integer, Integer> PATHS = paths();
    private static final Map<Integer, Integer> TERMINALS = terminals();

    @Test
    public void emptyInputIsNoGroups() {
        assertTrue(RecipeGrouper.group(Collections.emptyMap(), PATHS, TERMINALS, RECIPES).isEmpty());
    }

    @Test
    public void twoBankedItemsOnOneChainCollapseIntoOneGroup() {
        final List<RecipeGroup> groups = group(
                banked(1, run(ONE_TO_TWO, 8), run(TWO_TO_THREE, 8), run(THREE_TO_FOUR, 8)),
                banked(3, run(THREE_TO_FOUR, 5)));

        final RecipeGroup a = find(groups, PATH_A);

        assertEquals(2, a.getStages().size());
        assertEquals(1, a.getStages().get(0).getEntryItemId());
        assertEquals(3, a.getStages().get(1).getEntryItemId());
    }

    @Test
    public void stagesComeBackLeastMatureFirst() {
        // Declared deepest-first to prove the order is computed, not incidental
        final List<RecipeGroup> groups = group(
                banked(3, run(THREE_TO_FOUR, 1)),
                banked(2, run(TWO_TO_THREE, 1), run(THREE_TO_FOUR, 1)),
                banked(1, run(ONE_TO_TWO, 1), run(TWO_TO_THREE, 1), run(THREE_TO_FOUR, 1)));

        assertEquals(Arrays.asList(1, 2, 3), find(groups, PATH_A).getStages().stream()
                .map(RecipeStage::getEntryItemId)
                .collect(Collectors.toList()));
    }

    @Test
    public void aRunLandsOnItsOutputsPathNotItsPrimarys() {
        final List<RecipeGroup> groups = group(banked(4, run(FOUR_TO_FIVE, 3)));

        assertEquals(1, groups.size());
        assertEquals(PATH_B, groups.get(0).getPathItemId());
    }

    @Test
    public void oneBankedItemCanFeedTwoGroups() {
        final List<RecipeGroup> groups = group(
                banked(1, run(ONE_TO_TWO, 2), run(TWO_TO_THREE, 2), run(THREE_TO_FOUR, 2), run(FOUR_TO_FIVE, 2)));

        assertEquals(2, groups.size());
        assertEquals(1, find(groups, PATH_A).getEntryItemId());
        assertEquals(4, find(groups, PATH_B).getEntryItemId());
    }

    @Test
    public void chainsCrossingIntoOnePathShareItsEntryStage() {
        final List<RecipeGroup> groups = group(
                banked(1, run(ONE_TO_TWO, 2), run(TWO_TO_THREE, 2), run(THREE_TO_FOUR, 2), run(FOUR_TO_FIVE, 2)),
                banked(3, run(THREE_TO_FOUR, 5), run(FOUR_TO_FIVE, 5)));

        final RecipeGroup b = find(groups, PATH_B);

        assertEquals(1, b.getStages().size());
        assertEquals(4, b.getStages().get(0).getEntryItemId());
        assertEquals(7.0, b.getStages().get(0).getQuantity(), 0.0001);
    }

    @Test
    public void aStageOnlyHoldsTheRunsBelongingToItsGroup() {
        final List<RecipeGroup> groups = group(
                banked(1, run(ONE_TO_TWO, 2), run(TWO_TO_THREE, 2), run(THREE_TO_FOUR, 2), run(FOUR_TO_FIVE, 2)));

        assertEquals(3, find(groups, PATH_A).getStages().get(0).getRuns().size());
        assertEquals(1, find(groups, PATH_B).getStages().get(0).getRuns().size());
    }

    @Test
    public void everyRunIsKeptExactlyOnce() {
        final List<RecipeGroup> groups = group(
                banked(1, run(ONE_TO_TWO, 2), run(TWO_TO_THREE, 2), run(THREE_TO_FOUR, 2), run(FOUR_TO_FIVE, 2)),
                banked(3, run(THREE_TO_FOUR, 9), run(FOUR_TO_FIVE, 9)));

        final long kept = groups.stream()
                .flatMap(g -> g.getStages().stream())
                .flatMap(s -> s.getRuns().stream())
                .count();

        assertEquals(6, kept);
    }

    @Test
    public void anOutputWithNoTerminalIsDropped() {
        final Recipe orphan = recipe(14, 5, 6);

        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        byBankedItem.put(5, Collections.singletonList(run(orphan, 1)));

        assertTrue(RecipeGrouper.group(byBankedItem, PATHS, TERMINALS,
                Collections.singletonList(orphan)).isEmpty());
    }

    @Test
    public void bankedItemsThatProduceNothingAreAbsent() {
        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        byBankedItem.put(99, Collections.emptyList());

        assertTrue(RecipeGrouper.group(byBankedItem, PATHS, TERMINALS, RECIPES).isEmpty());
    }

    @Test
    public void groupsComeBackInDependencyOrder() {
        final List<RecipeGroup> groups = group(
                banked(4, run(FOUR_TO_FIVE, 1)),
                banked(1, run(ONE_TO_TWO, 1)));

        assertEquals(Arrays.asList(PATH_A, PATH_B),
                groups.stream().map(RecipeGroup::getPathItemId).collect(Collectors.toList()));
    }

    @Test
    public void stageXpIsTheFoldOfItsOwnRuns() {
        // every synthetic recipe is worth 1xp a run
        final List<RecipeGroup> groups = group(
                banked(1, run(ONE_TO_TWO, 2), run(TWO_TO_THREE, 2), run(THREE_TO_FOUR, 2), run(FOUR_TO_FIVE, 4)));

        assertEquals(6.0, find(groups, PATH_A).getStages().get(0).getXp(), 0.0001);
        assertEquals(4.0, find(groups, PATH_B).getStages().get(0).getXp(), 0.0001);
    }

    @Test
    public void groupXpIsTheSumOfItsStages() {
        final List<RecipeGroup> groups = group(
                banked(1, run(ONE_TO_TWO, 8), run(TWO_TO_THREE, 8), run(THREE_TO_FOUR, 8)),
                banked(3, run(THREE_TO_FOUR, 5)));

        final RecipeGroup a = find(groups, PATH_A);

        assertEquals(29.0, a.getXp(), 0.0001);
        assertEquals(a.getStages().stream().mapToDouble(RecipeStage::getXp).sum(), a.getXp(), 0.0001);
    }

    @Test
    public void aStepSumsOneRecipeAcrossEveryItemThatReachesIt() {
        final List<RecipeGroup> groups = group(
                banked(1, run(ONE_TO_TWO, 8), run(TWO_TO_THREE, 8), run(THREE_TO_FOUR, 8)),
                banked(3, run(THREE_TO_FOUR, 5)));

        final Map<Integer, Double> xpByRecipe = find(groups, PATH_A).getSteps().stream()
                .collect(Collectors.toMap(step -> step.getRecipe().getId(), RecipeStep::getXp));

        assertEquals(8.0, xpByRecipe.get(10), 0.0001);
        assertEquals(8.0, xpByRecipe.get(11), 0.0001);
        assertEquals(13.0, xpByRecipe.get(12), 0.0001); // 8 from item 1, 5 from item 3
    }

    @Test
    public void stepsComeBackInDependencyOrder() {
        // Declared deepest-first to prove the order is computed, not incidental
        final List<RecipeGroup> groups = group(
                banked(3, run(THREE_TO_FOUR, 1)),
                banked(1, run(ONE_TO_TWO, 1), run(TWO_TO_THREE, 1)));

        assertEquals(Arrays.asList(10, 11, 12), find(groups, PATH_A).getSteps().stream()
                .map(step -> step.getRecipe().getId())
                .collect(Collectors.toList()));
    }

    @Test
    public void stepsAndStagesSumToTheSameGroupXp() {
        final List<RecipeGroup> groups = group(
                banked(1, run(ONE_TO_TWO, 8), run(TWO_TO_THREE, 8), run(THREE_TO_FOUR, 8)),
                banked(3, run(THREE_TO_FOUR, 5)));

        final RecipeGroup a = find(groups, PATH_A);

        assertEquals(a.getXp(), a.getSteps().stream().mapToDouble(RecipeStep::getXp).sum(), 0.0001);
        assertEquals(a.getXp(), a.getStages().stream().mapToDouble(RecipeStage::getXp).sum(), 0.0001);
    }

    @Test
    public void aStepWorthNoXpIsLeftOut() {
        final Recipe freeStep = new Recipe(15, new Ingredient(2, 1), new Ingredient[0],
                new Ingredient(3, 1), "test", 0f, 0);

        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        byBankedItem.put(2, Arrays.asList(run(freeStep, 4), run(THREE_TO_FOUR, 4)));

        final List<RecipeGroup> groups = RecipeGrouper.group(byBankedItem, PATHS, TERMINALS,
                Arrays.asList(freeStep, THREE_TO_FOUR));

        assertEquals(Collections.singletonList(12), find(groups, PATH_A).getSteps().stream()
                .map(step -> step.getRecipe().getId())
                .collect(Collectors.toList()));
    }

    @Test
    public void stageQuantityIsWhatEnteredThePath() {
        final List<RecipeGroup> groups = group(banked(1, run(ONE_TO_TWO, 6), run(TWO_TO_THREE, 6)));

        assertEquals(6.0, find(groups, PATH_A).getStages().get(0).getQuantity(), 0.0001);
    }

    @Test
    public void outputQuantityCountsOnlyTheTerminal() {
        final Recipe threeToFourPairs = new Recipe(16, new Ingredient(3, 1), new Ingredient[0],
                new Ingredient(4, 3), "test", 1f, 0);

        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        byBankedItem.put(3, Collections.singletonList(run(threeToFourPairs, 5)));

        final List<RecipeGroup> groups = RecipeGrouper.group(byBankedItem, PATHS, TERMINALS,
                Collections.singletonList(threeToFourPairs));

        assertEquals(15.0, find(groups, PATH_A).getOutputQuantity(), 0.0001);
    }

    @Test
    public void secondaryDemandCoversOnlyTheGroupsOwnRuns() {
        final Recipe needsTwoOfItem7 = new Recipe(17, new Ingredient(3, 1),
                new Ingredient[]{new Ingredient(7, 2)}, new Ingredient(4, 1), "test", 1f, 0);

        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        byBankedItem.put(3, Arrays.asList(run(needsTwoOfItem7, 4), run(FOUR_TO_FIVE, 4)));

        final List<RecipeGroup> groups = RecipeGrouper.group(byBankedItem, PATHS, TERMINALS,
                Arrays.asList(needsTwoOfItem7, FOUR_TO_FIVE));

        assertEquals(8.0, find(groups, PATH_A).getSecondaryDemand().get(7), 0.0001);
        assertTrue(find(groups, PATH_B).getSecondaryDemand().isEmpty());
    }

    @Test
    public void runsKeepTheirQuantities() {
        final List<RecipeGroup> groups = group(banked(1, run(ONE_TO_TWO, 7.5)));

        assertEquals(7.5, find(groups, PATH_A).getStages().get(0).getRuns().get(0).getRuns(), 0.0001);
    }

    @SafeVarargs
    private static List<RecipeGroup> group(Map.Entry<Integer, List<RecipeRun>>... bankedItems) {
        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        for (Map.Entry<Integer, List<RecipeRun>> entry : bankedItems) {
            byBankedItem.put(entry.getKey(), entry.getValue());
        }

        return RecipeGrouper.group(byBankedItem, PATHS, TERMINALS, RECIPES);
    }

    private static Map.Entry<Integer, List<RecipeRun>> banked(int itemId, RecipeRun... runs) {
        return new java.util.AbstractMap.SimpleEntry<>(itemId, Arrays.asList(runs));
    }

    private static RecipeGroup find(List<RecipeGroup> groups, int pathItemId) {
        return groups.stream()
                .filter(g -> g.getPathItemId() == pathItemId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no group for path " + pathItemId));
    }

    private static RecipeRun run(Recipe recipe, double runs) {
        return new RecipeRun(recipe, runs);
    }

    private static Recipe recipe(int id, int primary, int output) {
        return new Recipe(id, new Ingredient(primary, 1), new Ingredient[0],
                new Ingredient(output, 1), "test", 1f, 0);
    }

    private static Map<Integer, Integer> paths() {
        final Map<Integer, Integer> paths = new HashMap<>();
        paths.put(1, PATH_A);
        paths.put(2, PATH_A);
        paths.put(3, PATH_A);
        paths.put(4, PATH_A);
        paths.put(5, PATH_B);
        return paths;
    }

    private static Map<Integer, Integer> terminals() {
        final Map<Integer, Integer> terminals = new HashMap<>();
        terminals.put(1, 4);
        terminals.put(2, 4);
        terminals.put(3, 4);
        terminals.put(4, 4);
        terminals.put(5, 5);
        return terminals;
    }
}
