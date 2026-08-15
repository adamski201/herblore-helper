package adamski.data;

import adamski.domain.models.Recipe;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Against the real table, so these numbers move if it does.
 */
public class RecipeRoutesTest {
    @Test
    public void aPathEndsWhereNothingOnItConsumesTheProduct() {
        assertTrue(RecipeRoutes.terminalsOf(ItemID.RANARR_WEED).contains(ItemID._1DOSE1DEFENSE));
        assertTrue(RecipeRoutes.terminalsOf(ItemID.RANARR_WEED).contains(ItemID._1DOSEPRAYERRESTORE));
    }

    @Test
    public void anIntermediateIsNotATerminal() {
        assertFalse(RecipeRoutes.terminalsOf(ItemID.RANARR_WEED).contains(ItemID.RANARRVIAL));
        assertFalse(RecipeRoutes.terminalsOf(ItemID.RANARR_WEED).contains(ItemID.RANARR_WEED));
    }

    @Test
    public void aChainCrossingIntoAnotherPathEndsAtTheBoundary() {
        // Guthix balance consumes stat restore, but it is a different path
        assertTrue(RecipeRoutes.terminalsOf(ItemID.HARRALANDER).contains(ItemID._1DOSESTATRESTORE));
    }

    @Test
    public void everyMaturityOfAHerbReachesItsProduct() {
        final Set<Integer> reaching = RecipeRoutes.itemsReaching(ItemID._1DOSE1DEFENSE);

        assertTrue(reaching.contains(ItemID.RANARR_SEED));
        assertTrue(reaching.contains(ItemID.UNIDENTIFIED_RANARR));
        assertTrue(reaching.contains(ItemID.RANARR_WEED));
        assertTrue(reaching.contains(ItemID.RANARRVIAL));
    }

    @Test
    public void theShorterRouteWins() {
        // r81 makes super combat from clean torstol; r30 then r82 costs a vial of water for the same xp
        assertEquals(81, RecipeRoutes.nextTowards(ItemID.TORSTOL, ItemID._1DOSE2COMBAT).getId());
    }

    @Test
    public void anItemThatCannotGetThereHasNoRoute() {
        assertNull(RecipeRoutes.nextTowards(ItemID.CADANTINE_BLOODVIAL, ItemID._1DOSE2DEFENSE));
        assertNotNull(RecipeRoutes.nextTowards(ItemID.CADANTINE_BLOODVIAL, ItemID._1DOSEBASTION));
    }

    @Test
    public void defaultsAreWhatTheTableOrderAlreadyReached() {
        final Map<Integer, Integer> defaults = RecipeRoutes.defaultTerminals();

        assertEquals(Integer.valueOf(ItemID._1DOSE1DEFENSE), defaults.get(ItemID.RANARR_WEED));
        assertEquals(Integer.valueOf(ItemID._1DOSE2DEFENSE), defaults.get(ItemID.CADANTINE));
    }

    @Test
    public void selectionHoldsOneRecipePerPrimary() {
        final List<Recipe> selection = RecipeRoutes.select(RecipeRoutes.defaultTerminals());
        final Set<Integer> primaries = new HashSet<>();

        for (Recipe recipe : selection) {
            assertTrue("two recipes for primary " + recipe.getPrimary().getItemId(),
                    primaries.add(recipe.getPrimary().getItemId()));
        }
    }

    @Test
    public void selectionIsInDependencyOrder() {
        final List<Recipe> selection = RecipeRoutes.select(RecipeRoutes.defaultTerminals());
        final List<Integer> order = RecipeDependencyResolver.order();

        int previous = -1;
        for (Recipe recipe : selection) {
            final int position = order.indexOf(recipe.getPrimary().getItemId());
            assertTrue(position > previous);
            previous = position;
        }
    }

    @Test
    public void anItemStrandedByTheChosenProductStillTakesItsOwnRoute() {
        // Cadantine goes to super defence, so blood vials can only go to bastion
        final Map<Integer, Recipe> byPrimary = byPrimary(RecipeRoutes.select(RecipeRoutes.defaultTerminals()));

        assertEquals(ItemID._1DOSEBASTION,
                byPrimary.get(ItemID.CADANTINE_BLOODVIAL).getOutput().getItemId());
    }

    @Test
    public void choosingTheProductRoutesEveryStepToIt() {
        final Map<Integer, Integer> chosen = RecipeRoutes.defaultTerminals();
        chosen.put(ItemID.CADANTINE, ItemID._1DOSEBASTION);

        final Map<Integer, Recipe> byPrimary = byPrimary(RecipeRoutes.select(chosen));

        assertEquals(ItemID.CADANTINE_BLOODVIAL, byPrimary.get(ItemID.CADANTINE).getOutput().getItemId());
        assertEquals(ItemID._1DOSEBASTION, byPrimary.get(ItemID.CADANTINE_BLOODVIAL).getOutput().getItemId());
    }

    @Test
    public void anItemWantedByTwoPathsGoesToTheLowerRecipeId() {
        // Super energy makes stamina (r87) or divine super energy (r88), on two different paths
        final Map<Integer, Recipe> byPrimary = byPrimary(RecipeRoutes.select(RecipeRoutes.defaultTerminals()));

        assertEquals(87, byPrimary.get(ItemID._1DOSE2ENERGY).getId());
    }

    @Test
    public void terminalsAreWhereTheSelectionActuallyStops() {
        final Map<Integer, Integer> terminals =
                RecipeRoutes.terminalByItem(RecipeRoutes.select(RecipeRoutes.defaultTerminals()));

        assertEquals(Integer.valueOf(ItemID._1DOSE1DEFENSE), terminals.get(ItemID.RANARR_SEED));
        assertEquals(Integer.valueOf(ItemID._1DOSE1DEFENSE), terminals.get(ItemID.RANARRVIAL));
        assertEquals(Integer.valueOf(ItemID._1DOSEBASTION), terminals.get(ItemID.CADANTINE_BLOODVIAL));
    }

    @Test
    public void aBoundaryItemEndsItsOwnPathRatherThanTheNextOne() {
        final Map<Integer, Integer> terminals =
                RecipeRoutes.terminalByItem(RecipeRoutes.select(RecipeRoutes.defaultTerminals()));

        assertEquals(Integer.valueOf(ItemID._1DOSESTATRESTORE), terminals.get(ItemID.HARRALANDER_SEED));
        assertEquals(Integer.valueOf(ItemID.BURGH_GUTHIX_BALANCE_1),
                terminals.get(ItemID.BURGH_GUTHIX_BALANCE_1));
    }

    private static Map<Integer, Recipe> byPrimary(List<Recipe> selection) {
        final Map<Integer, Recipe> byPrimary = new HashMap<>();
        for (Recipe recipe : selection) {
            byPrimary.put(recipe.getPrimary().getItemId(), recipe);
        }

        return byPrimary;
    }
}
