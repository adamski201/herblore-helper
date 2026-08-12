package adamski.data;

import adamski.domain.models.Recipe;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class RecipePathsTest {
    /**
     * Stops the table going stale - an unassigned item would put xp in the total but in no path.
     */
    @Test
    public void everyProducedItemHasAPath() {
        final Set<Integer> missing = new LinkedHashSet<>();

        for (Recipe recipe : HerbloreRecipes.all()) {
            for (int itemId : new int[]{recipe.getPrimary().getItemId(), recipe.getOutput().getItemId()}) {
                if (RecipePaths.pathOf(itemId) == RecipePaths.NO_PATH) missing.add(itemId);
            }
        }

        assertEquals("items appearing as a primary or output with no path: " + missing,
                0, missing.size());
    }

    @Test
    public void everyPathIsOnItself() {
        for (Integer path : RecipePaths.paths()) {
            assertEquals(path.intValue(), RecipePaths.pathOf(path));
        }
    }

    @Test
    public void secondariesHaveNoPath() {
        assertEquals(RecipePaths.NO_PATH, RecipePaths.pathOf(ItemID.VIAL_WATER));
        assertEquals(RecipePaths.NO_PATH, RecipePaths.pathOf(ItemID.SNAPE_GRASS));
        assertEquals(RecipePaths.NO_PATH, RecipePaths.pathOf(ItemID.WHITE_BERRIES));
    }

    @Test
    public void unknownItemHasNoPath() {
        assertEquals(RecipePaths.NO_PATH, RecipePaths.pathOf(ItemID.COINS));
    }

    @Test
    public void aHerbChainIsOnePathFromSeedToPotion() {
        final int ranarr = RecipePaths.pathOf(ItemID.RANARR_WEED);

        assertEquals(ranarr, RecipePaths.pathOf(ItemID.RANARR_SEED));
        assertEquals(ranarr, RecipePaths.pathOf(ItemID.UNIDENTIFIED_RANARR));
        assertEquals(ranarr, RecipePaths.pathOf(ItemID.RANARRVIAL));
        assertEquals(ranarr, RecipePaths.pathOf(ItemID._1DOSE1DEFENSE));
        assertEquals(ranarr, RecipePaths.pathOf(ItemID._1DOSEPRAYERRESTORE));
    }

    @Test
    public void everyCaviarMixIsTheSamePath() {
        final int caviar = RecipePaths.pathOf(ItemID.BRUT_CAVIAR);

        assertEquals(caviar, RecipePaths.pathOf(ItemID.BRUTAL_1DOSE1ENERGY));
        assertEquals(caviar, RecipePaths.pathOf(ItemID.BRUTAL_1DOSE4ANTIDRAGON));
        assertNotEquals(RecipePaths.pathOf(ItemID._1DOSE1ENERGY), caviar);
    }

    @Test
    public void anUpgradeMadeFromAFinishedPotionIsItsOwnPath() {
        final int stamina = RecipePaths.pathOf(ItemID._1DOSESTAMINA);

        assertNotEquals(RecipePaths.pathOf(ItemID._1DOSE2ENERGY), stamina);
        assertNotEquals(RecipePaths.pathOf(ItemID.AVANTOE), stamina);
        assertEquals(stamina, RecipePaths.pathOf(ItemID._1DOSE2STAMINA));
    }

    @Test
    public void guthixBalanceIsNotOnTheHarralanderPath() {
        assertNotEquals(RecipePaths.pathOf(ItemID.HARRALANDER),
                RecipePaths.pathOf(ItemID.BURGH_GUTHIX_BALANCE_1));
    }

    @Test
    public void theDressingIsNotOnTheElkhornPath() {
        assertEquals(RecipePaths.pathOf(ItemID.CORAL_ELKHORN), RecipePaths.pathOf(ItemID.HAEMOSTATIC_POULTICE));
        assertNotEquals(RecipePaths.pathOf(ItemID.CORAL_ELKHORN),
                RecipePaths.pathOf(ItemID._1DOSEHAEMOSTATICDRESSING));
    }

    @Test
    public void pathsAreDistinct() {
        assertEquals(RecipePaths.paths().size(), new LinkedHashSet<>(RecipePaths.paths()).size());
        assertTrue(RecipePaths.paths().size() > 15);
    }
}
