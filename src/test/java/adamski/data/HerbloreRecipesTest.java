package adamski.data;

import adamski.domain.RecipeGraph;
import adamski.domain.Recipe;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HerbloreRecipesTest {
    @Test
    public void relevantItemsCoversEveryRole() {
        // primary, output and secondary respectively
        assertTrue(Recipes.isRelevantItem(ItemID.UNIDENTIFIED_RANARR));
        assertTrue(Recipes.isRelevantItem(ItemID.RANARRVIAL));
        assertTrue(Recipes.isRelevantItem(ItemID.SNAPE_GRASS));
    }

    @Test
    public void relevantItemsAreOneDoseOnly() {
        assertTrue(Recipes.isRelevantItem(ItemID._1DOSE2ATTACK));
        assertFalse(Recipes.isRelevantItem(ItemID._4DOSE2ATTACK));
    }

    @Test
    public void unrelatedItemsAreFiltered() {
        assertFalse(Recipes.isRelevantItem(ItemID.COINS));
        assertFalse(Recipes.isRelevantItem(ItemID.ABYSSAL_WHIP));
    }

    /**
     * Picking a product names a route, unless two recipes turn the same item into the same thing.
     * Then only the secondaries differ and the picker has no way to ask which you meant.
     */
    @Test
    public void noTwoRecipesShareAPrimaryAndOutput() {
        final Map<String, List<Integer>> byPrimaryAndOutput = new LinkedHashMap<>();

        for (Recipe recipe : Recipes.all()) {
            final String pair = recipe.getPrimary().getItemId() + " -> " + recipe.getOutput().getItemId();
            byPrimaryAndOutput.computeIfAbsent(pair, k -> new ArrayList<>()).add(recipe.getId());
        }

        byPrimaryAndOutput.forEach((pair, recipeIds) -> assertEquals(
                "recipes " + recipeIds + " all turn " + pair, 1, recipeIds.size()));
    }

    /**
     * Two chains may only meet at a dead end. Antifire makes both extended antifire and super
     * antifire, and both make extended super antifire - but nothing consumes that, so the chains
     * share no recipe. A recipe taking it further would let one recipe sit on two chains, which
     * ChainResultCalculator attributes to whichever chain it saw last.
     */
    @Test
    public void whereTwoIndependentRoutesMeetNothingFollows() {
        final RecipeGraph graph = new RecipeGraph(Recipes.all());

        final Map<Integer, List<Recipe>> producers = new LinkedHashMap<>();
        for (Recipe recipe : Recipes.all()) {
            producers.computeIfAbsent(recipe.getOutput().getItemId(), k -> new ArrayList<>()).add(recipe);
        }

        producers.forEach((itemId, made) -> {
            if (made.size() < 2 || !convergeIndependently(graph, made)) return;

            assertTrue("item " + itemId + " is reached by independent routes and feeds "
                    + graph.recipeOptionsFor(itemId), graph.recipeOptionsFor(itemId).isEmpty());
        });
    }

    /**
     * Picking a product writes the shortest route to it, and a longer route cannot be picked unless
     * it is already the default. That only loses nothing while every route to a product is worth the
     * same xp - torstol to super combat direct, or by way of the unf vial, is 150 either way.
     */
    @Test
    public void everyRouteToAProductIsWorthTheSameXp() {
        final RecipeGraph graph = new RecipeGraph(Recipes.all());

        for (int from : everyItemInTheTable()) {
            final Map<Integer, Map<List<Integer>, Double>> xpByRouteByProduct = new LinkedHashMap<>();
            walkEveryRoute(graph, from, 1, 0, new ArrayList<>(), xpByRouteByProduct);

            xpByRouteByProduct.forEach((product, xpByRoute) -> {
                final double first = xpByRoute.values().iterator().next();

                for (double xp : xpByRoute.values()) {
                    assertEquals("item " + from + " reaches " + product + " by routes worth " + xpByRoute,
                            first, xp, 1e-6);
                }
            });
        }
    }

    /**
     * Records the xp of every route out of an item, per one of that item. The table is acyclic, as
     * RecipeGraph enforces, so this terminates.
     */
    private static void walkEveryRoute(RecipeGraph graph, int item, double held, double xp, List<Integer> route,
                                       Map<Integer, Map<List<Integer>, Double>> xpByRouteByProduct) {
        for (Recipe recipe : graph.recipeOptionsFor(item)) {
            final double runs = held / recipe.getPrimary().getQuantity();
            final double xpSoFar = xp + runs * recipe.getXp();
            final int output = recipe.getOutput().getItemId();

            final List<Integer> next = new ArrayList<>(route);
            next.add(recipe.getId());

            xpByRouteByProduct.computeIfAbsent(output, k -> new LinkedHashMap<>()).put(next, xpSoFar);
            walkEveryRoute(graph, output, runs * recipe.getOutput().getQuantity(), xpSoFar, next, xpByRouteByProduct);
        }
    }

    private static Set<Integer> everyItemInTheTable() {
        final Set<Integer> items = new LinkedHashSet<>();

        for (Recipe recipe : Recipes.all()) {
            items.add(recipe.getPrimary().getItemId());
            items.add(recipe.getOutput().getItemId());
        }

        return items;
    }

    /**
     * Independent means neither primary can become the other, so a chain through one is never a
     * chain through the other.
     */
    private static boolean convergeIndependently(RecipeGraph graph, List<Recipe> made) {
        for (int i = 0; i < made.size(); i++) {
            for (int j = i + 1; j < made.size(); j++) {
                final int one = made.get(i).getPrimary().getItemId();
                final int other = made.get(j).getPrimary().getItemId();

                if (!graph.findItemsReachableFrom(one).contains(other)
                        && !graph.findItemsReachableFrom(other).contains(one)) {
                    return true;
                }
            }
        }

        return false;
    }
}
