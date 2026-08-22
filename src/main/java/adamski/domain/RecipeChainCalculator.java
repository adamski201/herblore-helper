package adamski.domain;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out which chains are produced from owned items.
 * <p>
 * The least mature banked item roots a chain and everything along it joins that chain. Anything that
 * cannot reach the chain's product roots a chain of its own - which is what separates banked
 * cadantine blood vials from the cadantine going to super defence.
 */
public final class RecipeChainCalculator {
    private final RecipeGraph graph;

    public RecipeChainCalculator(RecipeGraph graph) {
        this.graph = graph;
    }

    /**
     * @param owned         what the player holds
     * @param productByItem the chosen product per chain, keyed by the chain's root item
     */
    public List<RecipeChain> calculate(ItemQuantities owned, Map<Integer, Integer> productByItem) {
        final List<Integer> banked = new ArrayList<>(owned.itemIds());

        // Banked items sorted by recipe dependency order (i.e. maturity)
        // This ordering ensures that chains begin with their root i.e. seed rather than herb
        banked.sort(Comparator.comparingInt(graph::maturityOf));

        final Set<Integer> claimed = new HashSet<>();
        final List<RecipeChain> chains = new ArrayList<>();

        for (Integer itemId : banked) {
            if (claimed.contains(itemId)) continue;

            final int product = productByItem.getOrDefault(itemId, graph.findDefaultProduct(itemId));
            final List<Recipe> route = graph.findShortestRoute(itemId, product);
            if (route.isEmpty()) continue;

            chains.add(new RecipeChain(route));

            claimed.add(itemId);
            for (Recipe recipe : route) {
                claimed.add(recipe.getOutput().getItemId());
            }
        }

        return Collections.unmodifiableList(chains);
    }
}
