package adamski.app;

import adamski.domain.ItemQuantities;
import adamski.domain.ItemSource;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Application state
 */
@Singleton
public class HerbloreStore {
    private final Map<ItemSource, ItemQuantities> state = new EnumMap<>(ItemSource.class);

    /**
     * @return what changed per source, as signed quantities. Empty if nothing changed.
     */
    public Map<ItemSource, ItemQuantities> updateState(Map<ItemSource, ItemQuantities> incoming) {
        final Map<ItemSource, ItemQuantities> delta = new EnumMap<>(ItemSource.class);

        for (Map.Entry<ItemSource, ItemQuantities> entry : incoming.entrySet()) {
            final ItemSource source = entry.getKey();
            final ItemQuantities next = entry.getValue();
            final ItemQuantities prev = state.get(source);

            if (next.equals(prev)) continue;

            delta.put(source, prev == null ? ItemQuantities.EMPTY : diff(prev, next));
            state.put(source, next);
        }

        return delta;
    }

    /**
     * @return an immutable snapshot of the app's state.
     */
    public Map<ItemSource, ItemQuantities> getState() {
        return Collections.unmodifiableMap(new EnumMap<>(state));
    }

    private static ItemQuantities diff(ItemQuantities prev, ItemQuantities next) {
        final Map<Integer, Double> changes = new HashMap<>();

        next.forEach((itemId, quantity) -> {
            final double change = quantity - prev.get(itemId);
            if (change != 0) changes.put(itemId, change);
        });

        prev.forEach((itemId, quantity) -> {
            if (!next.itemIds().contains(itemId)) changes.put(itemId, -quantity);
        });

        return ItemQuantities.of(changes);
    }
}
