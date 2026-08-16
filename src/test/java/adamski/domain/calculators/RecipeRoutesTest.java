package adamski.domain.calculators;

import adamski.data.RecipePaths;
import adamski.data.Recipes;
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
    private static final RecipeRoutes ROUTES = new RecipeRoutes(Recipes.all(), RecipePaths.pathsByItem());
    private static final RecipeDependencyResolver DEPENDENCIES = new RecipeDependencyResolver(Recipes.all());

    @Test
    public void aPathEndsWhereNothingOnItConsumesTheProduct() {
        assertTrue(ROUTES.terminalsOf(ItemID.RANARR_WEED).contains(ItemID._1DOSE1DEFENSE));
        assertTrue(ROUTES.terminalsOf(ItemID.RANARR_WEED).contains(ItemID._1DOSEPRAYERRESTORE));
    }

    @Test
    public void anIntermediateIsNotATerminal() {
        assertFalse(ROUTES.terminalsOf(ItemID.RANARR_WEED).contains(ItemID.RANARRVIAL));
        assertFalse(ROUTES.terminalsOf(ItemID.RANARR_WEED).contains(ItemID.RANARR_WEED));
    }

    @Test
    public void aChainCrossingIntoAnotherPathEndsAtTheBoundary() {
        // Guthix balance consumes stat restore, but it is a different path
        assertTrue(ROUTES.terminalsOf(ItemID.HARRALANDER).contains(ItemID._1DOSESTATRESTORE));
    }

    @Test
    public void optionsAreEveryRecipeAnItemCanTake() {
        final List<Integer> outputs = ROUTES.optionsFor(ItemID.RANARRVIAL).stream()
                .map(recipe -> recipe.getOutput().getItemId())
                .collect(Collectors.toList());

        assertEquals(2, outputs.size());
        assertTrue(outputs.contains(ItemID._1DOSE1DEFENSE));
        assertTrue(outputs.contains(ItemID._1DOSEPRAYERRESTORE));
    }

    @Test
    public void anItemWithOneOptionHasNoChoiceToMake() {
        assertEquals(1, ROUTES.optionsFor(ItemID.UNIDENTIFIED_RANARR).size());
    }

    @Test
    public void aTerminalTakesNoRecipe() {
        assertTrue(ROUTES.optionsFor(ItemID._1DOSE1DEFENSE).isEmpty());
    }

    @Test
    public void selectionHoldsOneRecipePerPrimary() {
        final Set<Integer> primaries = new HashSet<>();

        for (Recipe recipe : ROUTES.defaultSelection()) {
            assertTrue("two recipes for primary " + recipe.getPrimary().getItemId(),
                    primaries.add(recipe.getPrimary().getItemId()));
        }
    }

    @Test
    public void selectionIsInDependencyOrder() {
        final List<Integer> order = DEPENDENCIES.order();

        int previous = -1;
        for (Recipe recipe : ROUTES.defaultSelection()) {
            final int position = order.indexOf(recipe.getPrimary().getItemId());
            assertTrue(position > previous);
            previous = position;
        }
    }

    @Test
    public void everyItemWithAnOptionTakesTheFirstOne() {
        final Map<Integer, Recipe> byPrimary = byPrimary(ROUTES.defaultSelection());

        assertEquals(44, byPrimary.get(ItemID.RANARRVIAL).getId());          // not r47 prayer restore
        assertEquals(87, byPrimary.get(ItemID._1DOSE2ENERGY).getId());       // not r88 extreme energy
        assertEquals(64, byPrimary.get(ItemID.CADANTINE_BLOODVIAL).getId()); // not r65 battlemage
    }

    @Test
    public void terminalsAreWhereTheSelectionActuallyStops() {
        final Map<Integer, Integer> terminals = ROUTES.terminalByItem(ROUTES.defaultSelection());

        assertEquals(Integer.valueOf(ItemID._1DOSE1DEFENSE), terminals.get(ItemID.RANARR_SEED));
        assertEquals(Integer.valueOf(ItemID._1DOSE1DEFENSE), terminals.get(ItemID.RANARRVIAL));
        assertEquals(Integer.valueOf(ItemID._1DOSEBASTION), terminals.get(ItemID.CADANTINE_BLOODVIAL));
    }

    @Test
    public void aBoundaryItemEndsItsOwnPathRatherThanTheNextOne() {
        final Map<Integer, Integer> terminals = ROUTES.terminalByItem(ROUTES.defaultSelection());

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
