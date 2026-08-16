package adamski.domain.calculators;

import adamski.data.Recipes;
import adamski.domain.models.Recipe;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecipeDependencyResolverTest {
    private static final RecipeDependencyResolver DEPENDENCIES = new RecipeDependencyResolver(Recipes.all());

    @Test
    public void shippedTableResolves() {
        assertFalse(DEPENDENCIES.order().isEmpty());
    }

    @Test
    public void orderHoldsEveryPrimaryAndOutputExactlyOnce() {
        final Set<Integer> expected = new HashSet<>();
        for (Recipe recipe : Recipes.all()) {
            expected.add(recipe.getPrimary().getItemId());
            expected.add(recipe.getOutput().getItemId());
        }

        final List<Integer> order = DEPENDENCIES.order();

        assertEquals(expected.size(), order.size());
        assertEquals(expected, new HashSet<>(order));
    }

    @Test
    public void everyRecipeHasItsPrimaryBeforeItsOutput() {
        final List<Integer> order = DEPENDENCIES.order();

        for (Recipe recipe : Recipes.all()) {
            final int primary = order.indexOf(recipe.getPrimary().getItemId());
            final int output = order.indexOf(recipe.getOutput().getItemId());

            assertTrue("recipe " + recipe.getId() + " outputs before its primary", primary < output);
        }
    }

    @Test
    public void torstolChainIsOrdered() {
        assertOrdered(ItemID.TORSTOL_SEED, ItemID.UNIDENTIFIED_TORSTOL, ItemID.TORSTOL, ItemID.TORSTOLVIAL);
    }

    @Test
    public void staminaChainIsOrdered() {
        // r87 super energy -> stamina, r89 stamina -> extended stamina
        assertOrdered(ItemID._1DOSE2ENERGY, ItemID._1DOSESTAMINA, ItemID._1DOSE2STAMINA);
    }

    @Test
    public void anItemWithTwoProducersReturnsBoth() {
        final List<Recipe> producers = DEPENDENCIES.producersOf(ItemID._1DOSE2COMBAT);

        final Set<Integer> ids = new HashSet<>();
        producers.forEach(r -> ids.add(r.getId()));

        assertEquals(new HashSet<>(java.util.Arrays.asList(81, 82)), ids);
    }

    @Test
    public void terminalItemHasNoProducers() {
        assertTrue(DEPENDENCIES.producersOf(ItemID.VIAL_WATER).isEmpty());
    }

    @Test
    public void unknownItemReturnsEmptyRatherThanNull() {
        assertTrue(DEPENDENCIES.producersOf(ItemID.ABYSSAL_WHIP).isEmpty());
    }

    private static void assertOrdered(int... itemIds) {
        final List<Integer> order = DEPENDENCIES.order();

        for (int i = 1; i < itemIds.length; i++) {
            final int before = order.indexOf(itemIds[i - 1]);
            final int after = order.indexOf(itemIds[i]);

            assertTrue(itemIds[i - 1] + " should precede " + itemIds[i], before >= 0 && before < after);
        }
    }
}
