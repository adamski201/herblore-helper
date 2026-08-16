package adamski.domain.models;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Synthetic recipes: 1 -> 2 -> 3 -> 4.
 */
public class RecipeChainTest {
    private static final Recipe ONE_TO_TWO = recipe(10, 1, 2);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 3);
    private static final Recipe THREE_TO_FOUR = recipe(12, 3, 4);

    @Test
    public void theRootAndProductAreTheEndsOfTheChain() {
        final RecipeChain chain = new RecipeChain(Arrays.asList(ONE_TO_TWO, TWO_TO_THREE, THREE_TO_FOUR));

        assertEquals(1, chain.getRootItemId());
        assertEquals(4, chain.getProductItemId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void aChainNeedsARecipe() {
        new RecipeChain(Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void everyRecipeMustTakeWhatTheLastOneMade() {
        new RecipeChain(Arrays.asList(ONE_TO_TWO, THREE_TO_FOUR));
    }

    private static Recipe recipe(int id, int primary, int output) {
        return new Recipe(id, new Ingredient(primary, 1), new Ingredient[0],
                new Ingredient(output, 1), "test", 1f, 0);
    }
}
