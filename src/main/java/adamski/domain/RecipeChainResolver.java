package adamski.domain;


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
 */
public final class RecipeChainResolver {
    private final RecipeGraph graph;

    public RecipeChainResolver(RecipeGraph graph) {
        this.graph = graph;
    }

    /**
     * @param owned     what the player holds
     * @param selection which recipe each item feeds
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

            final List<Recipe> route = graph.findRoute(itemId, selection);
            if (route.isEmpty()) continue;

            chains.add(new RecipeChain(route, new ArrayList<>(graph.findItemsReachableFrom(itemId))));

            claimed.add(itemId);
            for (Recipe recipe : route) {
                claimed.add(recipe.getOutput().getItemId());
            }
        }

        return Collections.unmodifiableList(chains);
    }
}
