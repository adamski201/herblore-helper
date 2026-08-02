package adamski.app;

import adamski.domain.models.ItemSource;

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
    private final Map<ItemSource, Map<Integer, Integer>> state = new EnumMap<>(ItemSource.class);

    /**
     * @return map of item quantities that changed (per source). Empty if nothing changed.
     */
    public Map<ItemSource, Map<Integer, Integer>> updateState(Map<ItemSource, Map<Integer, Integer>> incoming) {
        final Map<ItemSource, Map<Integer, Integer>> delta = new EnumMap<>(ItemSource.class);

        for (Map.Entry<ItemSource, Map<Integer, Integer>> entry : incoming.entrySet()) {
            final ItemSource source = entry.getKey();
            final Map<Integer, Integer> next = entry.getValue();
            final Map<Integer, Integer> prev = state.get(source);

            if (next.equals(prev)) continue;

            delta.put(source, prev == null ? Collections.emptyMap() : diff(prev, next));
            state.put(source, next);
        }

        return delta;
    }

    /**
     * @return an immutable snapshot of the app's state.
     */
    public Map<ItemSource, Map<Integer, Integer>> getState() {
        final Map<ItemSource, Map<Integer, Integer>> copy = new EnumMap<>(ItemSource.class);

        for (Map.Entry<ItemSource, Map<Integer, Integer>> entry : state.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }

        return Collections.unmodifiableMap(copy);
    }

    private static Map<Integer, Integer> diff(Map<Integer, Integer> prev, Map<Integer, Integer> next) {
        final Map<Integer, Integer> changes = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : next.entrySet()) {
            final int change = entry.getValue() - prev.getOrDefault(entry.getKey(), 0);
            if (change != 0) changes.put(entry.getKey(), change);
        }

        for (Map.Entry<Integer, Integer> entry : prev.entrySet()) {
            if (!next.containsKey(entry.getKey())) changes.put(entry.getKey(), -entry.getValue());
        }

        return changes;
    }
}
