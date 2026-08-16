package adamski.domain.calculators;

import adamski.domain.models.Ingredient;
import adamski.domain.models.ItemQuantities;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeGroup;
import adamski.domain.models.RecipeRow;
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
 * Synthetic recipes: 1 -> 2 -> 3 -> 4 is one row, and a stranded 6 -> 7 is another.
 */
public class RecipeGrouperTest {
    private static final Recipe ONE_TO_TWO = recipe(10, 1, 2);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 3);
    private static final Recipe THREE_TO_FOUR = recipe(12, 3, 4);
    private static final Recipe SIX_TO_SEVEN = recipe(14, 6, 7);

    private static final RecipeRow ROW_A =
            new RecipeRow(1, 4, Arrays.asList(ONE_TO_TWO, TWO_TO_THREE, THREE_TO_FOUR));
    private static final RecipeRow ROW_B =
            new RecipeRow(6, 7, Collections.singletonList(SIX_TO_SEVEN));

    @Test
    public void nothingBankedIsNoGroups() {
        assertTrue(RecipeGrouper.group(Collections.emptyMap(), List.of(ROW_A), ItemQuantities.EMPTY).isEmpty());
    }

    @Test
    public void twoBankedItemsOnOneRouteCollapseIntoOneGroup() {
        final List<RecipeGroup> groups = group(owned(1, 8, 3, 5),
                banked(1, run(ONE_TO_TWO, 8), run(TWO_TO_THREE, 8), run(THREE_TO_FOUR, 8)),
                banked(3, run(THREE_TO_FOUR, 5)));

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getStages().size());
        assertEquals(1, groups.get(0).getEntryItemId());
        assertEquals(4, groups.get(0).getProductItemId());
    }

    @Test
    public void stagesComeBackLeastMatureFirst() {
        final List<RecipeGroup> groups = group(owned(3, 1, 2, 1, 1, 1),
                banked(3, run(THREE_TO_FOUR, 1)),
                banked(2, run(TWO_TO_THREE, 1), run(THREE_TO_FOUR, 1)),
                banked(1, run(ONE_TO_TWO, 1), run(TWO_TO_THREE, 1), run(THREE_TO_FOUR, 1)));

        assertEquals(List.of(1, 2, 3), groups.get(0).getStages().stream()
                .map(RecipeStage::getEntryItemId)
                .collect(Collectors.toList()));
    }

    @Test
    public void aStrandedItemIsItsOwnGroup() {
        final List<RecipeGroup> groups = group(owned(1, 2, 6, 3),
                banked(1, run(ONE_TO_TWO, 2), run(TWO_TO_THREE, 2), run(THREE_TO_FOUR, 2)),
                banked(6, run(SIX_TO_SEVEN, 3)));

        assertEquals(2, groups.size());
        assertEquals(1, groups.get(0).getEntryItemId());
        assertEquals(6, groups.get(1).getEntryItemId());
    }

    @Test
    public void aRunBelongingToNoRowIsDropped() {
        final Recipe orphan = recipe(99, 8, 9);

        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        byBankedItem.put(8, Collections.singletonList(run(orphan, 1)));

        assertTrue(RecipeGrouper.group(byBankedItem, List.of(ROW_A, ROW_B), owned(8, 1)).isEmpty());
    }

    @Test
    public void stageQuantityIsWhatIsHeld() {
        final List<RecipeGroup> groups = group(owned(1, 6),
                banked(1, run(ONE_TO_TWO, 6), run(TWO_TO_THREE, 6)));

        assertEquals(6.0, groups.get(0).getStages().get(0).getQuantity(), 0.0001);
    }

    @Test
    public void stageXpIsTheFoldOfItsOwnRuns() {
        // every synthetic recipe is worth 1xp a run
        final List<RecipeGroup> groups = group(owned(1, 8, 3, 5),
                banked(1, run(ONE_TO_TWO, 8), run(TWO_TO_THREE, 8), run(THREE_TO_FOUR, 8)),
                banked(3, run(THREE_TO_FOUR, 5)));

        assertEquals(24.0, groups.get(0).getStages().get(0).getXp(), 0.0001);
        assertEquals(5.0, groups.get(0).getStages().get(1).getXp(), 0.0001);
        assertEquals(29.0, groups.get(0).getXp(), 0.0001);
    }

    @Test
    public void aStepSumsOneRecipeAcrossEveryItemThatReachesIt() {
        final List<RecipeGroup> groups = group(owned(1, 8, 3, 5),
                banked(1, run(ONE_TO_TWO, 8), run(TWO_TO_THREE, 8), run(THREE_TO_FOUR, 8)),
                banked(3, run(THREE_TO_FOUR, 5)));

        final Map<Integer, Double> xpByRecipe = groups.get(0).getSteps().stream()
                .collect(Collectors.toMap(step -> step.getRecipe().getId(), RecipeStep::getXp));

        assertEquals(8.0, xpByRecipe.get(10), 0.0001);
        assertEquals(13.0, xpByRecipe.get(12), 0.0001); // 8 from item 1, 5 from item 3
    }

    @Test
    public void stepsComeBackInRouteOrder() {
        final List<RecipeGroup> groups = group(owned(3, 1, 1, 1),
                banked(3, run(THREE_TO_FOUR, 1)),
                banked(1, run(ONE_TO_TWO, 1), run(TWO_TO_THREE, 1)));

        assertEquals(List.of(10, 11, 12), groups.get(0).getSteps().stream()
                .map(step -> step.getRecipe().getId())
                .collect(Collectors.toList()));
    }

    @Test
    public void aStepWorthNoXpIsLeftOut() {
        final Recipe freeStep = new Recipe(11, new Ingredient(2, 1), new Ingredient[0],
                new Ingredient(3, 1), "test", 0f, 0);

        final RecipeRow row = new RecipeRow(2, 4, Arrays.asList(freeStep, THREE_TO_FOUR));

        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        byBankedItem.put(2, Arrays.asList(run(freeStep, 4), run(THREE_TO_FOUR, 4)));

        final List<RecipeGroup> groups = RecipeGrouper.group(byBankedItem, List.of(row), owned(2, 4));

        assertEquals(List.of(12), groups.get(0).getSteps().stream()
                .map(step -> step.getRecipe().getId())
                .collect(Collectors.toList()));
    }

    @Test
    public void outputQuantityCountsOnlyTheProduct() {
        final Recipe threeToFourTriples = new Recipe(12, new Ingredient(3, 1), new Ingredient[0],
                new Ingredient(4, 3), "test", 1f, 0);

        final RecipeRow row = new RecipeRow(3, 4, Collections.singletonList(threeToFourTriples));

        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        byBankedItem.put(3, Collections.singletonList(run(threeToFourTriples, 5)));

        final List<RecipeGroup> groups = RecipeGrouper.group(byBankedItem, List.of(row), owned(3, 5));

        assertEquals(15.0, groups.get(0).getOutputQuantity(), 0.0001);
    }

    @Test
    public void secondaryDemandCoversOnlyTheGroupsOwnRuns() {
        final Recipe needsTwoOfItem50 = new Recipe(12, new Ingredient(3, 1),
                new Ingredient[]{new Ingredient(50, 2)}, new Ingredient(4, 1), "test", 1f, 0);

        final RecipeRow row = new RecipeRow(3, 4, Collections.singletonList(needsTwoOfItem50));

        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        byBankedItem.put(3, Arrays.asList(run(needsTwoOfItem50, 4), run(SIX_TO_SEVEN, 4)));

        final List<RecipeGroup> groups = RecipeGrouper.group(byBankedItem, List.of(row), owned(3, 4));

        assertEquals(1, groups.size());
        assertEquals(8.0, groups.get(0).getSecondaryDemand().get(50), 0.0001);
    }

    @SafeVarargs
    private static List<RecipeGroup> group(ItemQuantities owned, Map.Entry<Integer, List<RecipeRun>>... bankedItems) {
        final Map<Integer, List<RecipeRun>> byBankedItem = new HashMap<>();
        for (Map.Entry<Integer, List<RecipeRun>> entry : bankedItems) {
            byBankedItem.put(entry.getKey(), entry.getValue());
        }

        return RecipeGrouper.group(byBankedItem, List.of(ROW_A, ROW_B), owned);
    }

    private static Map.Entry<Integer, List<RecipeRun>> banked(int itemId, RecipeRun... runs) {
        return new java.util.AbstractMap.SimpleEntry<>(itemId, Arrays.asList(runs));
    }

    private static ItemQuantities owned(int... pairs) {
        final Map<Integer, Integer> items = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            items.put(pairs[i], pairs[i + 1]);
        }
        return ItemQuantities.counted(items);
    }

    private static RecipeRun run(Recipe recipe, double runs) {
        return new RecipeRun(recipe, runs);
    }

    private static Recipe recipe(int id, int primary, int output) {
        return new Recipe(id, new Ingredient(primary, 1), new Ingredient[0],
                new Ingredient(output, 1), "test", 1f, 0);
    }
}
