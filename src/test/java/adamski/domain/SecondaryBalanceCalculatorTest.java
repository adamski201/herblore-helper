package adamski.domain;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecondaryBalanceCalculatorTest {
    private static final double DELTA = 0.0001;

    // item 50 and 51 are the secondaries under test
    private static final Recipe NEEDS_ONE_OF_50 = recipe(10, 1, new Ingredient(50, 1));
    private static final Recipe NEEDS_TWO_OF_50 = recipe(11, 2, new Ingredient(50, 2));
    private static final Recipe NEEDS_BOTH = recipe(12, 3, new Ingredient(50, 1), new Ingredient(51, 3));
    private static final Recipe NEEDS_NOTHING = recipe(13, 4);

    @Test
    public void demandIsRunsTimesSecondaryQuantity() {
        final SecondaryBalance balance = calculate(owned(), run(NEEDS_TWO_OF_50, 6));

        assertEquals(12.0, balance.getDemanded().get(50), DELTA);
    }

    @Test
    public void demandSumsAcrossRecipes() {
        final SecondaryBalance balance = calculate(owned(), run(NEEDS_ONE_OF_50, 4), run(NEEDS_TWO_OF_50, 5));

        assertEquals(14.0, balance.getDemanded().get(50), DELTA);
    }

    @Test
    public void aRecipeCanDemandSeveralSecondaries() {
        final SecondaryBalance balance = calculate(owned(), run(NEEDS_BOTH, 2));

        assertEquals(2.0, balance.getDemanded().get(50), DELTA);
        assertEquals(6.0, balance.getDemanded().get(51), DELTA);
    }

    @Test
    public void shortfallIsNegative() {
        final SecondaryBalance balance = calculate(owned(50, 10), run(NEEDS_TWO_OF_50, 8));

        assertEquals(-6.0, balance.getNet().get(50), DELTA);
    }

    @Test
    public void spareIsPositive() {
        final SecondaryBalance balance = calculate(owned(50, 100), run(NEEDS_TWO_OF_50, 8));

        assertEquals(84.0, balance.getNet().get(50), DELTA);
    }

    @Test
    public void owningNoneIsTheFullDemandAsAShortfall() {
        final SecondaryBalance balance = calculate(owned(), run(NEEDS_ONE_OF_50, 7));

        assertEquals(-7.0, balance.getNet().get(50), DELTA);
    }

    @Test
    public void itemsNothingDemandsAreAbsent() {
        // 99 is held but no recipe consumes it, so the balance has nothing to say
        final SecondaryBalance balance = calculate(owned(99, 500), run(NEEDS_ONE_OF_50, 1));

        assertFalse(balance.getNet().itemIds().contains(99));
        assertTrue(balance.getNet().itemIds().contains(50));
    }

    @Test
    public void recipesWithoutSecondariesContributeNothing() {
        final SecondaryBalance balance = calculate(owned(), run(NEEDS_NOTHING, 100));

        assertTrue(balance.getDemanded().isEmpty());
        assertTrue(balance.getNet().isEmpty());
    }

    @Test
    public void noRunsIsEmpty() {
        final SecondaryBalance balance = calculate(owned(50, 10));

        assertTrue(balance.getDemanded().isEmpty());
        assertTrue(balance.getNet().isEmpty());
    }

    @Test
    public void fractionalRunsCarryThrough() {
        final SecondaryBalance balance = calculate(owned(50, 2), run(NEEDS_TWO_OF_50, 1.5));

        assertEquals(3.0, balance.getDemanded().get(50), DELTA);
        assertEquals(-1.0, balance.getNet().get(50), DELTA);
    }

    private static SecondaryBalance calculate(ItemQuantities owned, RecipeRun... yields) {
        return SecondaryBalanceCalculator.netAgainstOwned(
                SecondaryBalanceCalculator.sumDemand(Arrays.asList(yields)), owned);
    }

    private static RecipeRun run(Recipe recipe, double runs) {
        return new RecipeRun(recipe, runs);
    }

    private static ItemQuantities owned(int... pairs) {
        final Map<Integer, Integer> items = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            items.put(pairs[i], pairs[i + 1]);
        }
        return ItemQuantities.counted(items);
    }

    private static Recipe recipe(int id, int primaryId, Ingredient... secondaries) {
        return new Recipe(id, new Ingredient(primaryId, 1), secondaries,
                new Ingredient(primaryId + 100, 1), "test", 0f, 0);
    }
}
