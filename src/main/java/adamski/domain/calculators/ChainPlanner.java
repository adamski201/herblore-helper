package adamski.domain.calculators;

import adamski.domain.models.ChainPlan;
import adamski.domain.models.ItemQuantities;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeChain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out which chains the bank produces, before anything is calculated.
 * <p>
 * The least mature banked item roots a chain and everything along it joins that chain. Anything that
 * cannot reach the chain's product roots a chain of its own - which is what separates banked
 * cadantine blood vials from the cadantine going to super defence.
 */
public final class ChainPlanner {
    private final RecipeGraph graph;

    public ChainPlanner(RecipeGraph graph) {
        this.graph = graph;
    }

    /**
     * @param owned         what the player holds
     * @param productByItem the chosen product per chain, keyed by the chain's root item
     */
    public ChainPlan plan(ItemQuantities owned, Map<Integer, Integer> productByItem) {
        final List<RecipeChain> chains = chains(owned, productByItem);

        return new ChainPlan(chains, selection(chains));
    }

    private List<RecipeChain> chains(ItemQuantities owned, Map<Integer, Integer> productByItem) {
        final List<Integer> banked = new ArrayList<>(owned.itemIds());
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

        return chains;
    }

    /**
     * Least mature first, so the cascade always produces an item before something consumes it.
     */
    private List<Recipe> selection(List<RecipeChain> chains) {
        final Map<Integer, Recipe> byPrimary = new HashMap<>();

        for (RecipeChain chain : chains) {
            for (Recipe recipe : chain.getRecipes()) {
                final Recipe taken = byPrimary.putIfAbsent(recipe.getPrimary().getItemId(), recipe);

                if (taken != null && !taken.equals(recipe)) {
                    throw new IllegalStateException("chains disagree on what item " + recipe.getPrimary().getItemId() + " becomes: r" + taken.getId() + " and r" + recipe.getId());
                }
            }
        }

        final List<Recipe> ordered = new ArrayList<>(byPrimary.values());
        ordered.sort(Comparator.comparingInt(recipe -> graph.maturityOf(recipe.getPrimary().getItemId())));

        return ordered;
    }
}
