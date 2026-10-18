package adamski.domain.route;


import adamski.domain.ItemQuantities;
import adamski.domain.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Works out which chains are produced from owned items.
 * <p>
 * The least mature banked item roots a chain and everything on its chosen route joins that chain.
 * Anything off that route roots a chain of its own - off the route, not unable to reach the product.
 * Banked torstol unf roots its own chain when torstol goes straight to super combat, though it makes
 * super combat too; it simply is not on the way.
 * <p>
 * A banked item told to stop still roots a chain, one with no recipes that makes nothing. Its row is
 * the only place left to change that choice once the items above it are used up.
 */
public final class RecipeChainResolver {
    private final RecipeGraph graph;

    public RecipeChainResolver(RecipeGraph graph) {
        this.graph = graph;
    }

    /**
     * @param owned     what the player holds
     * @param selection which recipe each item feeds
     * @return the chains, least mature root first. The panel relies on this to order rows of one
     * family - seed before grimy before unf.
     */
    public List<RecipeChain> resolve(ItemQuantities owned, RecipeSelection selection) {
        final List<Integer> banked = new ArrayList<>(owned.itemIds());

        // Banked items sorted by recipe dependency order (i.e. maturity)
        // This ordering ensures that chains begin with their root i.e. seed rather than herb
        banked.sort(Comparator.comparingInt(graph::maturityOf));

        final Set<Integer> claimed = new HashSet<>();
        final List<RecipeChain> chains = new ArrayList<>();

        for (Integer itemId : banked) {
            if (claimed.contains(itemId)) continue;

            // an item that can make nothing is no chain, but one that was told to stop still is
            if (graph.recipeOptionsFor(itemId).isEmpty()) continue;

            final List<Recipe> route = graph.findRoute(itemId, selection);
            chains.add(new RecipeChain(itemId, route));

            claimed.add(itemId);
            for (Recipe recipe : route) {
                claimed.add(recipe.getOutput().getItemId());
            }
        }

        return Collections.unmodifiableList(chains);
    }
}
