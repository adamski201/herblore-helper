package adamski.domain;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A fold over recipe runs. Ordering and propagation belong to RecipeYieldCalculator, so the runs
 * here are handed over directly rather than derived.
 */
public class BankedXpCalculatorTest {
    private static final Ingredient[] NONE = new Ingredient[0];
    private static final double DELTA = 0.0001;

    private static final Recipe WORTH_FIVE = recipe(10, 5f);
    private static final Recipe WORTH_TWENTY = recipe(11, 20f);
    private static final Recipe WORTH_NOTHING = recipe(12, 0f);

    @Test
    public void xpIsRunsTimesRecipeXp() {
        final BankedXpResult result = calculate(run(WORTH_FIVE, 4), run(WORTH_TWENTY, 3));

        assertEquals(20.0, result.getXpPerRecipeId().get(WORTH_FIVE.getId()), DELTA);
        assertEquals(60.0, result.getXpPerRecipeId().get(WORTH_TWENTY.getId()), DELTA);
        assertEquals(80.0, result.getTotal(), DELTA);
    }

    @Test
    public void fractionalRunsCarryThrough() {
        final BankedXpResult result = calculate(run(WORTH_TWENTY, 1.5));

        assertEquals(30.0, result.getTotal(), DELTA);
    }

    @Test
    public void zeroXpRecipesAreOmittedFromTheBreakdown() {
        final BankedXpResult result = calculate(run(WORTH_NOTHING, 9), run(WORTH_FIVE, 2));

        assertFalse(result.getXpPerRecipeId().containsKey(WORTH_NOTHING.getId()));
        assertEquals(10.0, result.getTotal(), DELTA);
    }

    @Test
    public void totalIsTheSumOfTheBreakdown() {
        final BankedXpResult result = calculate(run(WORTH_FIVE, 7), run(WORTH_TWENTY, 11), run(WORTH_NOTHING, 3));

        final double summed = result.getXpPerRecipeId().values().stream().mapToDouble(Double::doubleValue).sum();

        assertEquals(summed, result.getTotal(), DELTA);
    }

    /**
     * The breakdown is keyed by id, so the same recipe carrying different numbers - a modified
     * copy of the table - still lands on one entry rather than splitting in two.
     */
    @Test
    public void recipesSharingAnIdShareABreakdownEntry() {
        final BankedXpResult result = calculate(run(WORTH_TWENTY, 2), run(recipe(11, 40f), 1));

        assertEquals(1, result.getXpPerRecipeId().size());
        assertEquals(80.0, result.getXpPerRecipeId().get(WORTH_TWENTY.getId()), DELTA);
    }

    @Test
    public void aRecipeAppearingTwiceSumsRatherThanOverwriting() {
        final BankedXpResult result = calculate(run(WORTH_TWENTY, 4), run(WORTH_TWENTY, 6));

        assertEquals(200.0, result.getXpPerRecipeId().get(WORTH_TWENTY.getId()), DELTA);
        assertEquals(200.0, result.getTotal(), DELTA);
    }

    @Test
    public void noRunsIsZero() {
        final BankedXpResult result = BankedXpCalculator.calculate(Collections.emptyList());

        assertEquals(0, result.getTotal(), DELTA);
        assertTrue(result.getXpPerRecipeId().isEmpty());
    }

    private static BankedXpResult calculate(RecipeRun... yields) {
        final List<RecipeRun> list = Arrays.asList(yields);
        return BankedXpCalculator.calculate(list);
    }

    private static RecipeRun run(Recipe recipe, double runs) {
        return new RecipeRun(recipe, runs);
    }

    private static Recipe recipe(int id, float xp) {
        return new Recipe(id, new Ingredient(1, 1), NONE, new Ingredient(2, 1), "test", xp, 0);
    }
}
