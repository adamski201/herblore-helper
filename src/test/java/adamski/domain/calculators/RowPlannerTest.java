package adamski.domain.calculators;

import adamski.domain.models.Ingredient;
import adamski.domain.models.ItemQuantities;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeRow;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Synthetic recipes: 1 -> 2 -> 3 -> 4, with 3 also able to make 5, and a stranded 6 -> 7.
 */
public class RowPlannerTest {
    private static final Recipe ONE_TO_TWO = recipe(10, 1, 2);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 3);
    private static final Recipe THREE_TO_FOUR = recipe(12, 3, 4);
    private static final Recipe THREE_TO_FIVE = recipe(13, 3, 5);
    private static final Recipe SIX_TO_SEVEN = recipe(14, 6, 7);

    private static final RecipeGraph GRAPH = new RecipeGraph(
            Arrays.asList(ONE_TO_TWO, TWO_TO_THREE, THREE_TO_FOUR, THREE_TO_FIVE, SIX_TO_SEVEN));

    @Test
    public void oneBankedItemIsOneRow() {
        final List<RecipeRow> rows = plan(owned(1, 10));

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getEntryItemId());
        assertEquals(4, rows.get(0).getProductItemId()); // first option all the way down
    }

    @Test
    public void anythingOnTheRouteJoinsTheSameRow() {
        final List<RecipeRow> rows = plan(owned(1, 10, 2, 5, 3, 8));

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getEntryItemId());
    }

    @Test
    public void theLeastMatureItemNamesTheRow() {
        final List<RecipeRow> rows = plan(owned(3, 8, 1, 10));

        assertEquals(1, rows.get(0).getEntryItemId());
    }

    @Test
    public void anItemThatCannotReachTheProductStartsItsOwnRow() {
        final List<RecipeRow> rows = plan(owned(1, 10, 6, 4));

        assertEquals(2, rows.size());
        assertEquals(List.of(1, 6), rows.stream()
                .map(RecipeRow::getEntryItemId)
                .collect(Collectors.toList()));
    }

    @Test
    public void anItemDownstreamOfTheChosenProductStrandsIntoItsOwnRow() {
        final Map<Integer, Integer> chosen = new HashMap<>();
        chosen.put(1, 2); // stop at item 2

        final List<RecipeRow> rows = RowPlanner.plan(owned(1, 10, 3, 5), chosen, GRAPH);

        assertEquals(2, rows.size());
        assertEquals(2, rows.get(0).getProductItemId());
        assertEquals(3, rows.get(1).getEntryItemId());
    }

    @Test
    public void anItemThatMakesNothingIsNoRow() {
        assertTrue(plan(owned(4, 100)).isEmpty());
    }

    @Test
    public void choosingTheProductChoosesTheRoute() {
        final Map<Integer, Integer> chosen = new HashMap<>();
        chosen.put(1, 5);

        final List<RecipeRow> rows = RowPlanner.plan(owned(1, 10), chosen, GRAPH);

        assertEquals(List.of(10, 11, 13), rows.get(0).getRoute().stream()
                .map(Recipe::getId)
                .collect(Collectors.toList()));
    }

    @Test
    public void theSelectionIsEveryRowsRoute() {
        final List<Recipe> selection = RowPlanner.select(plan(owned(1, 10, 6, 4)), GRAPH);

        assertEquals(Set.of(10, 11, 12, 14), selection.stream()
                .map(Recipe::getId)
                .collect(Collectors.toSet()));
    }

    @Test
    public void theSelectionRunsProducersBeforeConsumers() {
        final List<Recipe> selection = RowPlanner.select(plan(owned(1, 10, 6, 4)), GRAPH);

        for (int earlier = 0; earlier < selection.size(); earlier++) {
            for (int later = earlier + 1; later < selection.size(); later++) {
                assertNotEquals("a consumer runs before its producer",
                        selection.get(later).getOutput().getItemId(),
                        selection.get(earlier).getPrimary().getItemId());
            }
        }
    }

    @Test
    public void noPrimaryIsClaimedTwice() {
        final List<Recipe> selection = RowPlanner.select(plan(owned(1, 10, 2, 5, 3, 8)), GRAPH);

        assertEquals(selection.size(), selection.stream()
                .map(recipe -> recipe.getPrimary().getItemId())
                .distinct()
                .count());
    }

    private static List<RecipeRow> plan(ItemQuantities owned) {
        return RowPlanner.plan(owned, Collections.emptyMap(), GRAPH);
    }

    private static ItemQuantities owned(int... pairs) {
        final Map<Integer, Integer> items = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            items.put(pairs[i], pairs[i + 1]);
        }
        return ItemQuantities.counted(items);
    }

    private static Recipe recipe(int id, int primary, int output) {
        return new Recipe(id, new Ingredient(primary, 1), new Ingredient[0],
                new Ingredient(output, 1), "test", 1f, 0);
    }
}
