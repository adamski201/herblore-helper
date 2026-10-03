package adamski.domain;

import adamski.data.Recipes;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
        assertEquals(ItemID._1DOSE1DEFENSE, defaultProductOf(ItemID.RANARR_SEED));
        assertEquals(ItemID._1DOSEBASTION, defaultProductOf(ItemID.CADANTINE_BLOODVIAL));
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

    @Test
    public void aRouteFollowsTheFirstOptionUntilNothingIsMade() {
        final List<Recipe> route = GRAPH.findRoute(ItemID.AVANTOE_SEED, RecipeSelection.ALL_DEFAULT);

        assertEquals(ItemID.AVANTOE_SEED, route.get(0).getPrimary().getItemId());
        for (Recipe step : route) {
            assertEquals(GRAPH.recipeOptionsFor(step.getPrimary().getItemId()).get(0), step);
        }
        assertTrue(GRAPH.recipeOptionsFor(route.get(route.size() - 1).getOutput().getItemId()).isEmpty());
    }

    @Test
    public void aChosenStepIsTakenInsteadOfTheFirstOption() {
        final Recipe notTheDefault = GRAPH.recipeOptionsFor(ItemID.AVANTOEVIAL).get(1);
        final RecipeSelection selection =
                RecipeSelection.of(Map.of(ItemID.AVANTOEVIAL, notTheDefault.getId()));

        final List<Recipe> route = GRAPH.findRoute(ItemID.AVANTOE_SEED, selection);

        assertEquals(notTheDefault, stepFrom(route, ItemID.AVANTOEVIAL));
    }

    /**
     * The step that does the choosing sits on the unf vial, so it applies wherever the route starts.
     */
    @Test
    public void aChosenStepAppliesFromAnyRootAboveIt() {
        final Recipe notTheDefault = GRAPH.recipeOptionsFor(ItemID.AVANTOEVIAL).get(1);
        final RecipeSelection selection =
                RecipeSelection.of(Map.of(ItemID.AVANTOEVIAL, notTheDefault.getId()));

        for (int root : List.of(ItemID.AVANTOE_SEED, ItemID.UNIDENTIFIED_AVANTOE, ItemID.AVANTOE)) {
            assertEquals("root " + root, notTheDefault,
                    stepFrom(GRAPH.findRoute(root, selection), ItemID.AVANTOEVIAL));
        }
    }

    /**
     * Only the chosen step is pinned - the route carries on past it by first option, which is why
     * choosing a destination has to stop the chain at that destination as well.
     */
    @Test
    public void aRouteCarriesOnPastAChosenStep() {
        final Recipe notTheDefault = GRAPH.recipeOptionsFor(ItemID.AVANTOEVIAL).get(1);
        final RecipeSelection selection =
                RecipeSelection.of(Map.of(ItemID.AVANTOEVIAL, notTheDefault.getId()));

        final List<Recipe> route = GRAPH.findRoute(ItemID.AVANTOE_SEED, selection);

        assertNotEquals(notTheDefault, route.get(route.size() - 1));
        assertTrue(route.contains(notTheDefault));
    }

    private static int defaultProductOf(int itemId) {
        final List<Recipe> route = GRAPH.findRoute(itemId, RecipeSelection.ALL_DEFAULT);
        return route.get(route.size() - 1).getOutput().getItemId();
    }

    private static Recipe stepFrom(List<Recipe> route, int itemId) {
        return route.stream()
                .filter(recipe -> recipe.getPrimary().getItemId() == itemId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no step out of item " + itemId + " in " + route));
    }

    @Test
    public void aStoppedItemEndsTheRoute() {
        final RecipeSelection selection =
                RecipeSelection.of(Map.of(ItemID.AVANTOE, RecipeSelection.STOP));

        final List<Recipe> route = GRAPH.findRoute(ItemID.AVANTOE_SEED, selection);

        assertEquals(ItemID.AVANTOE, route.get(route.size() - 1).getOutput().getItemId());
    }

    /**
     * A saved step naming a recipe the table no longer offers is stale config, and must not delete
     * the chain out from under the user.
     */
    @Test
    public void aStaleStepFallsBackToTheFirstOption() {
        final RecipeSelection stale = RecipeSelection.of(Map.of(ItemID.AVANTOEVIAL, 99999));

        assertEquals(GRAPH.findRoute(ItemID.AVANTOE_SEED, RecipeSelection.ALL_DEFAULT),
                GRAPH.findRoute(ItemID.AVANTOE_SEED, stale));
    }

    @Test
    public void anItemThatMakesNothingHasNoRoute() {
        assertTrue(GRAPH.findRoute(ItemID._1DOSE1DEFENSE, RecipeSelection.ALL_DEFAULT).isEmpty());
    }

    /**
     * Super antifire and extended antifire both end at extended super antifire, but neither can
     * become the other - two chains that happen to share a product.
     */
    @Test
    public void independentRoutesCanShareOneProduct() {
        assertEquals(defaultProductOf(ItemID._1DOSE3ANTIDRAGON),
                defaultProductOf(ItemID._1DOSE2ANTIDRAGON));

        assertFalse(GRAPH.findItemsReachableFrom(ItemID._1DOSE3ANTIDRAGON)
                .contains(ItemID._1DOSE2ANTIDRAGON));
        assertFalse(GRAPH.findItemsReachableFrom(ItemID._1DOSE2ANTIDRAGON)
                .contains(ItemID._1DOSE3ANTIDRAGON));
    }

    @Test(expected = IllegalStateException.class)
    public void aCircularTableIsRejected() {
        new RecipeGraph(List.of(
                new Recipe(1, new Ingredient(1, 1), new Ingredient[0], new Ingredient(2, 1), "test", 1f, 0),
                new Recipe(2, new Ingredient(2, 1), new Ingredient[0], new Ingredient(1, 1), "test", 1f, 0)));
    }
}
