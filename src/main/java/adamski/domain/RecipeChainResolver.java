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
 * The least mature banked item roots a chain and everything along it joins that chain. Anything that
 * cannot reach the chain's product roots a chain of its own - which is what separates banked
 * cadantine blood vials from the cadantine going to super defence.
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
