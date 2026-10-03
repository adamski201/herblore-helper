package adamski.app;

import adamski.domain.ItemQuantities;
import adamski.domain.ItemSource;
import adamski.domain.RecipeSelection;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class HerbloreStore {
    private final Map<ItemSource, ItemQuantities> itemsBySource = new EnumMap<>(ItemSource.class);
    private RecipeSelection selection = RecipeSelection.ALL_DEFAULT;

    /**
     * @return what changed per source, as signed quantities. Empty if nothing changed.
     */
    public Map<ItemSource, ItemQuantities> updateItems(Map<ItemSource, ItemQuantities> incoming) {
        final Map<ItemSource, ItemQuantities> delta = new EnumMap<>(ItemSource.class);

        for (Map.Entry<ItemSource, ItemQuantities> entry : incoming.entrySet()) {
            final ItemSource source = entry.getKey();
            final ItemQuantities next = entry.getValue();
            final ItemQuantities prev = itemsBySource.get(source);

            if (next.equals(prev)) continue;

            delta.put(source, prev == null ? ItemQuantities.EMPTY : diff(prev, next));
            itemsBySource.put(source, next);
        }

        return delta;
    }

    /**
     * @return an immutable snapshot of what is held, per source
     */
    public Map<ItemSource, ItemQuantities> itemsBySource() {
        return Collections.unmodifiableMap(new EnumMap<>(itemsBySource));
    }

    /**
     * @return true if it differs from what is already held
     */
    public boolean updateSelection(RecipeSelection next) {
        if (next.equals(selection)) return false;

        selection = next;

        return true;
    }

    /**
     * @return which recipe each item feeds. Immutable, so it is a snapshot by construction.
     */
    public RecipeSelection selection() {
        return selection;
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
