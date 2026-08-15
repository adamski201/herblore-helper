package adamski.domain.calculators;

import adamski.domain.models.Ingredient;
import adamski.domain.models.ItemQuantities;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeRun;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A synthetic table, so these prove the supplied recipes and their order are honoured rather than
 * exercising Recipes.
 */
public class RecipeYieldCalculatorTest {
    private static final Ingredient[] NONE = new Ingredient[0];
    private static final double DELTA = 0.0001;

    // 1 -> 2 -> 3, plus an alternative consumer of item 1
    private static final Recipe ONE_TO_TWO = recipe(10, 1, 1, 2, 1);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 1, 3, 1);
    private static final Recipe ONE_TO_FOUR = recipe(12, 1, 1, 4, 1);
    private static final Recipe PAIRS_TO_FIVE = recipe(13, 3, 2, 5, 2);

    @Test
    public void producedItemsFeedTheNextRecipe() {
        final List<RecipeRun> yields = calculate(1, 4, ONE_TO_TWO, TWO_TO_THREE);

        assertEquals(2, yields.size());
        assertRun(yields.get(0), ONE_TO_TWO, 4);
        assertRun(yields.get(1), TWO_TO_THREE, 4);
    }

    @Test
    public void theSuppliedRecipeDecidesWhatAnItemBecomes() {
        final List<RecipeRun> yields = calculate(1, 1, ONE_TO_FOUR, TWO_TO_THREE);

        // item 1 became item 4, so nothing ever reaches the recipe consuming item 2
        assertEquals(1, yields.size());
        assertRun(yields.get(0), ONE_TO_FOUR, 1);
    }

    @Test
    public void anItemNoSuppliedRecipeConsumesIsTerminal() {
        final List<RecipeRun> yields = calculate(1, 3, ONE_TO_TWO);

        assertEquals(1, yields.size());
        assertRun(yields.get(0), ONE_TO_TWO, 3);
    }

    @Test
    public void orderingIsLoadBearing() {
        // reversed, so item 2 is consumed before item 1 has produced any
        final List<RecipeRun> yields = calculate(1, 3, TWO_TO_THREE, ONE_TO_TWO);

        assertEquals(1, yields.size());
        assertRun(yields.get(0), ONE_TO_TWO, 3);
    }

    @Test
    public void runsAreFractionalWhenThePrimaryQuantityDoesNotDivide() {
        final List<RecipeRun> yields = calculate(3, 3, PAIRS_TO_FIVE);

        assertRun(yields.get(0), PAIRS_TO_FIVE, 1.5);
    }

    @Test
    public void outputQuantityMultipliesThroughTheChain() {
        // one of item 1 makes three of item 2, so the next recipe runs three times
        final Recipe oneToThreeOfTwo = recipe(14, 1, 1, 2, 3);

        final List<RecipeRun> yields = calculate(1, 2, oneToThreeOfTwo, TWO_TO_THREE);

        assertRun(yields.get(0), oneToThreeOfTwo, 2);
        assertRun(yields.get(1), TWO_TO_THREE, 6);
    }

    @Test
    public void recipesWithNothingAvailableAreOmitted() {
        final List<RecipeRun> yields = calculate(99, 5, ONE_TO_TWO, TWO_TO_THREE);

        assertTrue(yields.isEmpty());
    }

    @Test
    public void zeroQuantityYieldsNothing() {
        assertTrue(calculate(1, 0, ONE_TO_TWO).isEmpty());
    }

    @Test
    public void eachBankedItemKeepsItsOwnRuns() {
        final Map<Integer, Integer> owned = new HashMap<>();
        owned.put(1, 4);
        owned.put(2, 10);

        final Map<Integer, List<RecipeRun>> byBankedItem = RecipeYieldCalculator.calculateByBankedItem(
                ItemQuantities.counted(owned), Arrays.asList(ONE_TO_TWO, TWO_TO_THREE));

        assertEquals(2, byBankedItem.get(1).size()); // through both recipes
        assertEquals(1, byBankedItem.get(2).size()); // joins at the second
        assertRun(byBankedItem.get(2).get(0), TWO_TO_THREE, 10);
    }

    @Test
    public void bankedItemsThatProduceNothingAreLeftOut() {
        final Map<Integer, Integer> owned = new HashMap<>();
        owned.put(1, 4);
        owned.put(99, 500);

        final Map<Integer, List<RecipeRun>> byBankedItem = RecipeYieldCalculator.calculateByBankedItem(
                ItemQuantities.counted(owned), Collections.singletonList(ONE_TO_TWO));

        assertEquals(Collections.singleton(1), byBankedItem.keySet());
    }

    private static void assertRun(RecipeRun actual, Recipe expected, double runs) {
        assertEquals(expected, actual.getRecipe());
        assertEquals(runs, actual.getRuns(), DELTA);
    }

    private static List<RecipeRun> calculate(int itemId, int quantity, Recipe... orderedRecipes) {
        return RecipeYieldCalculator.calculate(itemId, quantity, Arrays.asList(orderedRecipes));
    }

    private static Recipe recipe(int id, int primaryId, int primaryQty, int outputId, int outputQty) {
        return new Recipe(id, new Ingredient(primaryId, primaryQty), NONE,
                new Ingredient(outputId, outputQty), "test", 0f, 0);
    }
}
