package adamski.data;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

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
}
