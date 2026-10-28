package adamski.data;

import adamski.domain.Ingredient;
import adamski.domain.SecondaryPrecursor;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecondaryPrecursorsTest {
    /**
     * A precursor the recipe table also knew would be counted twice - once as what it is, once as
     * the secondary it becomes - and as a primary it would root a row of its own.
     */
    @Test
    public void noPrecursorIsInTheRecipeTable() {
        for (SecondaryPrecursor precursor : SecondaryPrecursors.all()) {
            assertFalse("item " + precursor.getItemId(), Recipes.isRelevantItem(precursor.getItemId()));
        }
    }

    /**
     * Otherwise nothing would ever ask for what it makes, and it would never be counted.
     */
    @Test
    public void everyPrecursorBecomesASecondarySomeRecipeNeeds() {
        final Set<Integer> secondaries = Recipes.all().stream()
                .flatMap(recipe -> recipe.getSecondaries().stream())
                .map(Ingredient::getItemId)
                .collect(Collectors.toSet());

        for (SecondaryPrecursor precursor : SecondaryPrecursors.all()) {
            assertTrue("item " + precursor.getSecondaryItemId(), secondaries.contains(precursor.getSecondaryItemId()));
        }
    }

    @Test
    public void everyPrecursorMakesSomething() {
        for (SecondaryPrecursor precursor : SecondaryPrecursors.all()) {
            assertTrue("item " + precursor.getItemId(), precursor.getYield() > 0);
        }
    }

    /**
     * Listed twice, it would be counted twice.
     */
    @Test
    public void noPrecursorIsListedTwice() {
        final Set<Integer> seen = new HashSet<>();

        for (SecondaryPrecursor precursor : SecondaryPrecursors.all()) {
            assertTrue("item " + precursor.getItemId(), seen.add(precursor.getItemId()));
        }
    }

    @Test
    public void precursorsAreRecognised() {
        assertTrue(SecondaryPrecursors.isPrecursor(ItemID.BIRD_NEST_EMPTY));
        assertFalse(SecondaryPrecursors.isPrecursor(ItemID.CRUSHED_BIRD_NEST));
        assertFalse(SecondaryPrecursors.isPrecursor(ItemID.COINS));
    }
}
