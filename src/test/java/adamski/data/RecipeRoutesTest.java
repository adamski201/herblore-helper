package adamski.data;

import adamski.domain.models.Recipe;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void optionsAreEveryRecipeAnItemCanTake() {
        final List<Integer> outputs = RecipeRoutes.optionsFor(ItemID.RANARRVIAL).stream()
                .map(recipe -> recipe.getOutput().getItemId())
                .collect(Collectors.toList());

        assertEquals(2, outputs.size());
        assertTrue(outputs.contains(ItemID._1DOSE1DEFENSE));
        assertTrue(outputs.contains(ItemID._1DOSEPRAYERRESTORE));
    }

    @Test
    public void anItemWithOneOptionHasNoChoiceToMake() {
        assertEquals(1, RecipeRoutes.optionsFor(ItemID.UNIDENTIFIED_RANARR).size());
    }

    @Test
    public void aTerminalTakesNoRecipe() {
        assertTrue(RecipeRoutes.optionsFor(ItemID._1DOSE1DEFENSE).isEmpty());
    }

    @Test
    public void selectionHoldsOneRecipePerPrimary() {
        final Set<Integer> primaries = new HashSet<>();

        for (Recipe recipe : RecipeRoutes.defaultSelection()) {
            assertTrue("two recipes for primary " + recipe.getPrimary().getItemId(),
                    primaries.add(recipe.getPrimary().getItemId()));
        }
    }

    @Test
    public void selectionIsInDependencyOrder() {
        final List<Integer> order = RecipeDependencyResolver.order();

        int previous = -1;
        for (Recipe recipe : RecipeRoutes.defaultSelection()) {
            final int position = order.indexOf(recipe.getPrimary().getItemId());
            assertTrue(position > previous);
            previous = position;
        }
    }

    @Test
    public void everyItemWithAnOptionTakesTheFirstOne() {
        final Map<Integer, Recipe> byPrimary = byPrimary(RecipeRoutes.defaultSelection());

        assertEquals(44, byPrimary.get(ItemID.RANARRVIAL).getId());          // not r47 prayer restore
        assertEquals(87, byPrimary.get(ItemID._1DOSE2ENERGY).getId());       // not r88 extreme energy
        assertEquals(64, byPrimary.get(ItemID.CADANTINE_BLOODVIAL).getId()); // not r65 battlemage
    }

    @Test
    public void terminalsAreWhereTheSelectionActuallyStops() {
        final Map<Integer, Integer> terminals = RecipeRoutes.terminalByItem(RecipeRoutes.defaultSelection());

        assertEquals(Integer.valueOf(ItemID._1DOSE1DEFENSE), terminals.get(ItemID.RANARR_SEED));
        assertEquals(Integer.valueOf(ItemID._1DOSE1DEFENSE), terminals.get(ItemID.RANARRVIAL));
        assertEquals(Integer.valueOf(ItemID._1DOSEBASTION), terminals.get(ItemID.CADANTINE_BLOODVIAL));
    }

    @Test
    public void aBoundaryItemEndsItsOwnPathRatherThanTheNextOne() {
        final Map<Integer, Integer> terminals = RecipeRoutes.terminalByItem(RecipeRoutes.defaultSelection());

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
