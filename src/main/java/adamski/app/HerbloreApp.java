package adamski.app;

import adamski.domain.models.ItemSource;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orchestrator - receives changes from Adapter, updates state via store, and publishes data to listeners
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

    public void sourcesUpdated(Map<ItemSource, Map<Integer, Integer>> changed) {
        log.debug("sources updated: {}", changed.keySet());
    }

    private void publish(Map<Integer, Integer> itemQuantities) {
        for (HerbloreListener listener : listeners) {
            listener.onStateChanged(itemQuantities);
        }
    }
}
