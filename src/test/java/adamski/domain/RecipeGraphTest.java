package adamski.domain;

import adamski.data.Recipes;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Against the real table, so these numbers move if it does.
 */
public class RecipeGraphTest {
    private static final RecipeGraph GRAPH = new RecipeGraph(Recipes.all());

    @Test
    public void optionsAreEveryRecipeAnItemCanTake() {
        final List<Integer> outputs = GRAPH.recipeOptionsFor(ItemID.RANARRVIAL).stream()
                .map(recipe -> recipe.getOutput().getItemId())
                .collect(Collectors.toList());

        assertEquals(2, outputs.size());
        assertTrue(outputs.contains(ItemID._1DOSE1DEFENSE));
        assertTrue(outputs.contains(ItemID._1DOSEPRAYERRESTORE));
    }

    @Test
    public void aFinishedPotionTakesNoRecipe() {
        assertTrue(GRAPH.recipeOptionsFor(ItemID._1DOSE1DEFENSE).isEmpty());
    }

    @Test
    public void thePickerOffersIntermediatesAsWellAsProducts() {
        final Set<Integer> reachable = GRAPH.findItemsReachableFrom(ItemID.RANARR_SEED);

        assertTrue(reachable.contains(ItemID.UNIDENTIFIED_RANARR));
        assertTrue(reachable.contains(ItemID.RANARR_WEED));
        assertTrue(reachable.contains(ItemID.RANARRVIAL));
        assertTrue(reachable.contains(ItemID._1DOSE1DEFENSE));
        assertTrue(reachable.contains(ItemID._1DOSEPRAYERRESTORE));
    }

    @Test
    public void reachabilityCrossesWhatUsedToBePathBoundaries() {
        // Avantoe reaches stamina through super energy, and extended stamina beyond it
        final Set<Integer> reachable = GRAPH.findItemsReachableFrom(ItemID.AVANTOE_SEED);

        assertTrue(reachable.contains(ItemID._1DOSE2ENERGY));
        assertTrue(reachable.contains(ItemID._1DOSESTAMINA));
        assertTrue(reachable.contains(ItemID._1DOSE2STAMINA));
    }

    @Test
    public void aFinishedPotionReachesNothing() {
        assertTrue(GRAPH.findItemsReachableFrom(ItemID._1DOSE1DEFENSE).isEmpty());
    }

    @Test
    public void aRouteIsTheRecipesInOrder() {
        final List<Integer> route = GRAPH.findShortestRoute(ItemID.RANARR_SEED, ItemID._1DOSE1DEFENSE).stream()
                .map(Recipe::getId)
                .collect(Collectors.toList());

        assertEquals(List.of(122, 5, 20, 44), route);
    }

    @Test
    public void theShortestRouteWins() {
        // r81 makes super combat from clean torstol; r30 then r82 gets there too, for an extra vial
        final List<Integer> route = GRAPH.findShortestRoute(ItemID.TORSTOL, ItemID._1DOSE2COMBAT).stream()
                .map(Recipe::getId)
                .collect(Collectors.toList());

        assertEquals(List.of(81), route);
    }

    @Test
    public void stoppingEarlyIsARouteInItsOwnRight() {
        final List<Integer> route = GRAPH.findShortestRoute(ItemID.RANARR_SEED, ItemID.RANARRVIAL).stream()
                .map(Recipe::getId)
                .collect(Collectors.toList());

        assertEquals(List.of(122, 5, 20), route);
    }

    @Test
    public void anUnreachableProductHasNoRoute() {
        assertTrue(GRAPH.findShortestRoute(ItemID.CADANTINE_BLOODVIAL, ItemID._1DOSE2DEFENSE).isEmpty());
        assertFalse(GRAPH.findShortestRoute(ItemID.CADANTINE_BLOODVIAL, ItemID._1DOSEBASTION).isEmpty());
    }

    @Test
    public void theDefaultProductIsWhereFirstOptionsLead() {
        assertEquals(ItemID._1DOSE1DEFENSE, GRAPH.findDefaultProduct(ItemID.RANARR_SEED));
        assertEquals(ItemID._1DOSEBASTION, GRAPH.findDefaultProduct(ItemID.CADANTINE_BLOODVIAL));
    }

    @Test
    public void anItemWithNoRecipesIsItsOwnDefault() {
        assertEquals(ItemID._1DOSE1DEFENSE, GRAPH.findDefaultProduct(ItemID._1DOSE1DEFENSE));
    }

    @Test
    public void nothingIsMoreMatureThanWhatItIsMadeFrom() {
        for (Recipe recipe : Recipes.all()) {
            assertTrue("r" + recipe.getId() + " outranks its own output",
                    GRAPH.maturityOf(recipe.getPrimary().getItemId())
                            < GRAPH.maturityOf(recipe.getOutput().getItemId()));
        }
    }

    @Test
    public void anItemTheTableNeverMentionsHasNoMaturity() {
        assertEquals(Integer.MAX_VALUE, GRAPH.maturityOf(ItemID.ABYSSAL_WHIP));
    }

    @Test(expected = IllegalStateException.class)
    public void aCircularTableIsRejected() {
        new RecipeGraph(List.of(
                new Recipe(1, new Ingredient(1, 1), new Ingredient[0], new Ingredient(2, 1), "test", 1f, 0),
                new Recipe(2, new Ingredient(2, 1), new Ingredient[0], new Ingredient(1, 1), "test", 1f, 0)));
    }
}
