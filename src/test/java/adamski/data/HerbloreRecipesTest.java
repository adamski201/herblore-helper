package adamski.data;

import adamski.domain.RecipeGraph;
import adamski.domain.Recipe;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
