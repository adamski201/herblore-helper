package adamski.app;

import adamski.domain.ItemQuantities;
import adamski.domain.ItemSource;
import adamski.domain.route.RecipeSelection;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HerbloreStoreTest {
    private HerbloreStore store;

    @Before
    public void setUp() {
        store = new HerbloreStore();
    }

    @Test
    public void aFirstReadIsAChange() {
        assertTrue(store.updateItems(batch(ItemSource.Bank, items(1, 10, 2, 20))));
    }

    @Test
    public void anUnchangedResubmissionIsNoChange() {
        store.updateItems(batch(ItemSource.Bank, items(1, 10)));

        assertFalse(store.updateItems(batch(ItemSource.Bank, items(1, 10))));
    }

    @Test
    public void aMovedQuantityIsAChange() {
        store.updateItems(batch(ItemSource.Bank, items(1, 10)));

        assertTrue(store.updateItems(batch(ItemSource.Bank, items(1, 13))));
    }

    @Test
    public void anUnchangedSourceDoesNotHideAMovedOne() {
        final Map<ItemSource, ItemQuantities> first = new EnumMap<>(ItemSource.class);
        first.put(ItemSource.Bank, items(1, 10));
        first.put(ItemSource.PotionStorage, items(2, 5));
        store.updateItems(first);

        final Map<ItemSource, ItemQuantities> second = new EnumMap<>(ItemSource.class);
        second.put(ItemSource.Bank, items(1, 10)); // unchanged
        second.put(ItemSource.PotionStorage, items(2, 8));

        assertTrue(store.updateItems(second));
        assertEquals(items(2, 8), store.itemsBySource().get(ItemSource.PotionStorage));
    }

    @Test
    public void snapshotCarriesEverySourceSeenSoFar() {
        store.updateItems(batch(ItemSource.Bank, items(1, 10)));
        store.updateItems(batch(ItemSource.PotionStorage, items(2, 5)));

        final var snapshot = store.itemsBySource();

        assertEquals(items(1, 10), snapshot.get(ItemSource.Bank));
        assertEquals(items(2, 5), snapshot.get(ItemSource.PotionStorage));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void snapshotIsImmutable() {
        store.updateItems(batch(ItemSource.Bank, items(1, 10)));

        store.itemsBySource().put(ItemSource.PotionStorage, ItemQuantities.EMPTY);
    }

    @Test
    public void snapshotDoesNotObserveALaterApply() {
        store.updateItems(batch(ItemSource.Bank, items(1, 10)));
        final var snapshot = store.itemsBySource();

        store.updateItems(batch(ItemSource.PotionStorage, items(2, 5)));
        store.updateItems(batch(ItemSource.Bank, items(1, 99)));

        assertEquals(1, snapshot.size());
        assertEquals(items(1, 10), snapshot.get(ItemSource.Bank));
    }

    private static Map<ItemSource, ItemQuantities> batch(ItemSource source, ItemQuantities items) {
        final Map<ItemSource, ItemQuantities> batch = new EnumMap<>(ItemSource.class);
        batch.put(source, items);
        return batch;
    }

    /**
     * @param pairs alternating itemId and quantity
     */
    private static ItemQuantities items(int... pairs) {
        final Map<Integer, Integer> items = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            items.put(pairs[i], pairs[i + 1]);
        }
        return ItemQuantities.counted(items);
    }

    @Test
    public void anUntouchedStoreIsAllDefault() {
        assertEquals(RecipeSelection.ALL_DEFAULT, store.selection());
    }

    @Test
    public void aNewSelectionIsRemembered() {
        final RecipeSelection next = RecipeSelection.of(Map.of(50, 1234));

        assertTrue(store.updateSelection(next));
        assertEquals(next, store.selection());
    }

    @Test
    public void selectingWhatIsAlreadyHeldChangesNothing() {
        store.updateSelection(RecipeSelection.of(Map.of(50, 1234)));

        assertFalse(store.updateSelection(RecipeSelection.of(Map.of(50, 1234))));
    }

    @Test
    public void goingBackToAllDefaultCounts() {
        store.updateSelection(RecipeSelection.of(Map.of(50, 1234)));

        assertTrue(store.updateSelection(RecipeSelection.ALL_DEFAULT));
        assertEquals(RecipeSelection.DEFAULT, store.selection().recipeIdFor(50));
    }
}
