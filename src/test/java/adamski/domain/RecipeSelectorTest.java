package adamski.domain;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Synthetic recipes: 1 -> 2 -> 3 -> 4, with 3 also able to make 5, and a stranded 6 -> 7.
 */
public class RecipeSelectorTest {
    private static final Recipe ONE_TO_TWO = recipe(10, 1, 2);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 3);
    private static final Recipe THREE_TO_FOUR = recipe(12, 3, 4);
    private static final Recipe THREE_TO_FIVE = recipe(13, 3, 5);
    private static final Recipe SIX_TO_SEVEN = recipe(14, 6, 7);

    private static final RecipeGraph GRAPH = new RecipeGraph(
            Arrays.asList(ONE_TO_TWO, TWO_TO_THREE, THREE_TO_FOUR, THREE_TO_FIVE, SIX_TO_SEVEN));

    private static final RecipeSelector SELECTOR = new RecipeSelector(GRAPH);

    @Test
    public void aProductIsStoredAsTheStepsAlongTheRouteToIt() {
        final RecipeSelection selection = choose(1, 5);

        assertEquals(10, selection.recipeIdFor(1));
        assertEquals(11, selection.recipeIdFor(2));
        assertEquals(13, selection.recipeIdFor(3));
    }

    /**
     * Only the steps are pinned, so without this the chain would carry on past the product down its
     * own first option.
     */
    @Test
    public void theChainIsStoppedAtTheProduct() {
        assertEquals(RecipeSelection.STOP, choose(1, 3).recipeIdFor(3));
    }

    @Test
    public void aProductThatMakesNothingNeedsNoStop() {
        assertEquals(RecipeSelection.DEFAULT, choose(1, 4).recipeIdFor(4));
    }

    /**
     * The step doing the choosing sits on the item that branches, so it reads the same whichever
     * item on the chain the route was worked out from.
     */
    @Test
    public void theSameProductFromAnyRootStoresTheSameStep() {
        assertEquals(choose(3, 5).recipeIdFor(3), choose(1, 5).recipeIdFor(3));
    }

    @Test
    public void aShorterRouteLeavesNothingBehindIt() {
        final RecipeSelection far = choose(1, 5);
        final RecipeSelection near = SELECTOR.select(far, 1, 2);

        assertEquals("the step past the new product is gone",
                RecipeSelection.DEFAULT, near.recipeIdFor(3));
        assertEquals(RecipeSelection.STOP, near.recipeIdFor(2));
    }

    @Test
    public void goingBackToDefaultClearsTheStepsTheChainWasTaking() {
        final RecipeSelection chosen = choose(1, 5);

        final RecipeSelection cleared =
                SELECTOR.select(chosen, 1, RecipeSelection.DEFAULT);

        assertEquals(RecipeSelection.ALL_DEFAULT, cleared);
    }

    @Test
    public void anUnreachableProductIsRefused() {
        final RecipeSelection current = choose(1, 5);

        assertSame(current, SELECTOR.select(current, 1, 7));
    }

    @Test
    public void aProductBehindTheRootIsRefused() {
        assertSame(RecipeSelection.ALL_DEFAULT,
                SELECTOR.select(RecipeSelection.ALL_DEFAULT, 3, 1));
    }

    /**
     * Two chains on the same table keep their own steps, because a step belongs to one item.
     */
    @Test
    public void choosingForOneChainLeavesAnotherAlone() {
        final RecipeSelection both =
                SELECTOR.select(choose(1, 2), 3, 5);

        assertEquals(RecipeSelection.STOP, both.recipeIdFor(2));
        assertEquals(13, both.recipeIdFor(3));
    }

    private static RecipeSelection choose(int rootItemId, int productItemId) {
        return SELECTOR.select(RecipeSelection.ALL_DEFAULT, rootItemId, productItemId);
    }

    private static Recipe recipe(int id, int primary, int output) {
        return new Recipe(id, new Ingredient(primary, 1), new Ingredient[0],
                new Ingredient(output, 1), "test", 1f, 0);
    }
}
