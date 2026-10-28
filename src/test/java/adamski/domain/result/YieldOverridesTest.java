package adamski.domain.result;

import adamski.domain.Ingredient;
import adamski.domain.ItemQuantities;
import adamski.domain.Recipe;
import adamski.domain.RecipeTag;
import adamski.domain.route.RecipeChain;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class YieldOverridesTest {
    private static final double DELTA = 1e-9;

    private static final Recipe ONE_TO_TWO = recipe(10, 1, 2);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 3);
    private static final RecipeChain CHAIN = new RecipeChain(List.of(ONE_TO_TWO, TWO_TO_THREE));

    @Test
    public void anOverrideReplacesItsRecipesOutput() {
        final RecipeChain applied = applyTo(CHAIN, Map.of(10, 0.85));

        assertEquals(0.85, applied.getRecipes().get(0).getOutput().getQuantity(), DELTA);
        assertEquals(2, applied.getRecipes().get(0).getOutput().getItemId());
    }

    @Test
    public void aRecipeWithNoOverrideIsLeftAlone() {
        assertSame(TWO_TO_THREE, applyTo(CHAIN, Map.of(10, 0.85)).getRecipes().get(1));
    }

    @Test
    public void aStoppedChainKeepsItsRoot() {
        final RecipeChain stopped = new RecipeChain(5, Collections.emptyList());

        assertEquals(5, applyTo(stopped, Map.of(10, 0.85)).getRootItemId());
    }

    @Test
    public void anOverrideScalesTheChainsOutputButNotItsXp() {
        final RecipeChain chain = new RecipeChain(List.of(ONE_TO_TWO));

        final ChainResult result = ChainResultCalculator.calculate(
                List.of(applyTo(chain, Map.of(10, 0.25))), ItemQuantities.of(Map.of(1, 10.0))).get(0);

        assertEquals(2.5, result.getOutputQuantity(), DELTA);
        assertEquals(10.0, result.getXp(), DELTA); // the attempts still run
    }

    private static RecipeChain applyTo(RecipeChain chain, Map<Integer, Double> overrides) {
        return YieldOverrides.apply(List.of(chain), overrides).get(0);
    }

    private static Recipe recipe(int id, int primary, int output) {
        return new Recipe(id, new Ingredient(primary, 1), new Ingredient[0],
                new Ingredient(output, 1), RecipeTag.MISC, 1f, 0);
    }
}
