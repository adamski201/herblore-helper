package adamski.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Given a final product for a chain's root item, updates the intermediate steps inside the {@link RecipeSelection}.
 */
public final class RecipeSelector {
    private final RecipeGraph graph;

    public RecipeSelector(RecipeGraph graph) {
        this.graph = graph;
    }

    /**
     * Points a chain at a product, replacing the chain's previously assigned steps.
     *
     * @param current       what is stored now
     * @param rootItemId    the item the chain starts from
     * @param productItemId what it should end at, or {@link RecipeSelection#DEFAULT} for its default
     * @return the selection to store, or {@code current} unchanged if the product cannot be reached
     */
    public RecipeSelection select(RecipeSelection current, int rootItemId, int productItemId) {
        // clear the current route first
        final Map<Integer, Integer> recipeByItemId = new LinkedHashMap<>();
        for (Recipe recipe : graph.findRoute(rootItemId, current)) {
            recipeByItemId.put(recipe.getPrimary().getItemId(), RecipeSelection.DEFAULT);
            recipeByItemId.put(recipe.getOutput().getItemId(), RecipeSelection.DEFAULT);
        }

        if (productItemId == RecipeSelection.DEFAULT) return current.with(recipeByItemId);

        final List<Recipe> route = graph.findShortestRoute(rootItemId, productItemId);
        if (route.isEmpty()) return current;

        for (Recipe recipe : route) {
            recipeByItemId.put(recipe.getPrimary().getItemId(), recipe.getId());
        }

        // ensure the chain ends at the product
        if (!graph.recipeOptionsFor(productItemId).isEmpty()) {
            recipeByItemId.put(productItemId, RecipeSelection.STOP);
        }

        return current.with(recipeByItemId);
    }
}
