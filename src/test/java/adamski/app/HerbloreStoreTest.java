package adamski.app;

import adamski.domain.ItemQuantities;
import adamski.domain.ItemSource;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
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
    public void firstReadIsABaselineNotAGain() {
        final var delta = store.updateState(batch(ItemSource.Bank, items(1, 10, 2, 20)));

        assertEquals(Collections.singleton(ItemSource.Bank), delta.keySet());
        assertTrue("first read must not report the whole bank as gained", delta.get(ItemSource.Bank).isEmpty());
    }

    @Test
    public void unchangedResubmissionYieldsNothing() {
        store.updateState(batch(ItemSource.Bank, items(1, 10)));

        assertTrue(store.updateState(batch(ItemSource.Bank, items(1, 10))).isEmpty());
    }

    @Test
    public void increaseIsPositive() {
        store.updateState(batch(ItemSource.Bank, items(1, 10)));

        final var delta = store.updateState(batch(ItemSource.Bank, items(1, 13)));

        assertEquals(items(1, 3), delta.get(ItemSource.Bank));
    }

    @Test
    public void decreaseIsNegative() {
        store.updateState(batch(ItemSource.Bank, items(1, 10)));

        final var delta = store.updateState(batch(ItemSource.Bank, items(1, 4)));

        assertEquals(items(1, -6), delta.get(ItemSource.Bank));
    }

    @Test
    public void newItemIsItsFullQuantity() {
        store.updateState(batch(ItemSource.Bank, items(1, 10)));

        final var delta = store.updateState(batch(ItemSource.Bank, items(1, 10, 2, 7)));

        assertEquals(items(2, 7), delta.get(ItemSource.Bank));
    }

    @Test
    public void disappearingItemIsNegatedInFull() {
        store.updateState(batch(ItemSource.Bank, items(1, 10, 2, 7)));

        final var delta = store.updateState(batch(ItemSource.Bank, items(1, 10)));

        assertEquals(items(2, -7), delta.get(ItemSource.Bank));
    }

    @Test
    public void unchangedItemsAreAbsentFromTheDelta() {
        store.updateState(batch(ItemSource.Bank, items(1, 10, 2, 7)));

        final var delta = store.updateState(batch(ItemSource.Bank, items(1, 10, 2, 9)));

        assertEquals(items(2, 2), delta.get(ItemSource.Bank));
    }

    @Test
    public void onlyMovedSourcesAppearInTheDelta() {
        final Map<ItemSource, ItemQuantities> first = new EnumMap<>(ItemSource.class);
        first.put(ItemSource.Bank, items(1, 10));
        first.put(ItemSource.PotionStorage, items(2, 5));
        store.updateState(first);

        final Map<ItemSource, ItemQuantities> second = new EnumMap<>(ItemSource.class);
        second.put(ItemSource.Bank, items(1, 10)); // unchanged
        second.put(ItemSource.PotionStorage, items(2, 8));

        final var delta = store.updateState(second);

        assertEquals(Collections.singleton(ItemSource.PotionStorage), delta.keySet());
        assertEquals(items(2, 3), delta.get(ItemSource.PotionStorage));
    }

    @Test
    public void snapshotCarriesEverySourceSeenSoFar() {
        store.updateState(batch(ItemSource.Bank, items(1, 10)));
        store.updateState(batch(ItemSource.PotionStorage, items(2, 5)));

        final var snapshot = store.getState();

        assertEquals(items(1, 10), snapshot.get(ItemSource.Bank));
        assertEquals(items(2, 5), snapshot.get(ItemSource.PotionStorage));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void snapshotIsImmutable() {
        store.updateState(batch(ItemSource.Bank, items(1, 10)));

        store.getState().put(ItemSource.PotionStorage, ItemQuantities.EMPTY);
    }

    @Test
    public void snapshotDoesNotObserveALaterApply() {
        store.updateState(batch(ItemSource.Bank, items(1, 10)));
        final var snapshot = store.getState();

        store.updateState(batch(ItemSource.PotionStorage, items(2, 5)));
        store.updateState(batch(ItemSource.Bank, items(1, 99)));

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
}
