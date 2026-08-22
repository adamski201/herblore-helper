package adamski.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * How much of each item, keyed by item id and measured in 1-dose units.
 * <p>
 * Fractional, because a recipe can run a fraction of a time, and signed, because the same shape
 * carries differences - a secondary shortfall and a bank delta are both negative quantities.
 */
@EqualsAndHashCode
@ToString
public final class ItemQuantities {
    public static final ItemQuantities EMPTY = new ItemQuantities(Collections.emptyMap());

    private final Map<Integer, Double> byItemId;

    private ItemQuantities(Map<Integer, Double> byItemId) {
        this.byItemId = Map.copyOf(byItemId);
    }

    public static ItemQuantities of(Map<Integer, Double> byItemId) {
        return byItemId.isEmpty() ? EMPTY : new ItemQuantities(byItemId);
    }

    /**
     * For whole counts, as read from a container.
     */
    public static ItemQuantities counted(Map<Integer, Integer> counts) {
        final Map<Integer, Double> quantities = new HashMap<>(counts.size());
        counts.forEach((itemId, count) -> quantities.put(itemId, (double) count));

        return of(quantities);
    }

    public double get(int itemId) {
        return byItemId.getOrDefault(itemId, 0d);
    }

    public Set<Integer> itemIds() {
        return byItemId.keySet();
    }

    public Map<Integer, Double> asMap() {
        return byItemId;
    }

    public boolean isEmpty() {
        return byItemId.isEmpty();
    }

    public void forEach(BiConsumer<Integer, Double> action) {
        byItemId.forEach(action);
    }

    public ItemQuantities plus(ItemQuantities other) {
        final Map<Integer, Double> summed = new HashMap<>(byItemId);
        other.forEach((itemId, quantity) -> summed.merge(itemId, quantity, Double::sum));

        return of(summed);
    }
}
