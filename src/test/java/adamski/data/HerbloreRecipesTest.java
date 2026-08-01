package adamski.data;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HerbloreRecipesTest {
    @Test
    public void tableIsComplete() {
        assertEquals(132, HerbloreRecipes.all().size());
    }

    @Test
    public void relevantItemsCoversEveryRole() {
        // primary, output and secondary respectively
        assertTrue(HerbloreRecipes.isRelevantItem(ItemID.UNIDENTIFIED_RANARR));
        assertTrue(HerbloreRecipes.isRelevantItem(ItemID.RANARRVIAL));
        assertTrue(HerbloreRecipes.isRelevantItem(ItemID.SNAPE_GRASS));
    }

    @Test
    public void relevantItemsAreOneDoseOnly() {
        assertTrue(HerbloreRecipes.isRelevantItem(ItemID._1DOSE2ATTACK));
        assertFalse(HerbloreRecipes.isRelevantItem(ItemID._4DOSE2ATTACK));
    }

    @Test
    public void unrelatedItemsAreFiltered() {
        assertFalse(HerbloreRecipes.isRelevantItem(ItemID.COINS));
        assertFalse(HerbloreRecipes.isRelevantItem(ItemID.ABYSSAL_WHIP));
    }
}
