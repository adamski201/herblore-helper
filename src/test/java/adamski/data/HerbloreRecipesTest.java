package adamski.data;

import adamski.domain.models.Recipe;
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
}
