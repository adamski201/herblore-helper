package adamski.domain.result;

import adamski.domain.Recipe;
import adamski.domain.route.RecipeChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class YieldOverrides {
    private YieldOverrides() {
    }

    /**
     * Rebuilds the recipe chains with the provided yield overrides.
     */
    public static List<RecipeChain> apply(List<RecipeChain> chains, Map<Integer, Double> yieldOverridesByRecipeId) {
        final List<RecipeChain> applied = new ArrayList<>(chains.size());

        for (RecipeChain chain : chains) {
            final List<Recipe> recipes = new ArrayList<>(chain.getRecipes().size());

            for (Recipe recipe : chain.getRecipes()) {
                final Double yield = yieldOverridesByRecipeId.get(recipe.getId());
                recipes.add(yield == null ? recipe : recipe.withOutputQuantity(yield));
            }

            applied.add(new RecipeChain(chain.getRootItemId(), recipes));
        }

        return applied;
    }
}
