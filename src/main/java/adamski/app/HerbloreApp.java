package adamski.app;

import adamski.domain.models.Container;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orchestrator. Receives changes from the adapter, updates state, and publishes
 * to listeners when something actually changed.
 */
@Slf4j
@Singleton
public class HerbloreApp {
    private final List<HerbloreListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(HerbloreListener listener) {
        listeners.add(listener);
    }

    public void removeListener(HerbloreListener listener) {
        listeners.remove(listener);
    }

    /**
     * A consolidated update covering every source that changed this tick. Sources
     * stay tagged - they are never merged, because calculators draw on different
     * subsets of them.
     */
    public void sourcesUpdated(Map<Container, Map<Integer, Integer>> changed) {
        log.debug("sources updated: {}", changed.keySet());

        // TODO: push each source into the store, then recalculate if anything
        // really changed. Publishing the bank alone keeps the debug panel working.
        final Map<Integer, Integer> bank = changed.get(Container.Bank);
        if (bank != null) {
            publish(Collections.unmodifiableMap(bank));
        }
    }

    private void publish(Map<Integer, Integer> itemQuantities) {
        for (HerbloreListener listener : listeners) {
            listener.onStateChanged(itemQuantities);
        }
    }
}
