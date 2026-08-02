package adamski.app;

import adamski.domain.models.ItemSource;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
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

    private final HerbloreStore store;

    @Inject
    public HerbloreApp(HerbloreStore store) {
        this.store = store;
    }

    public void addListener(HerbloreListener listener) {
        listeners.add(listener);
    }

    public void removeListener(HerbloreListener listener) {
        listeners.remove(listener);
    }

    public void sourcesUpdated(Map<ItemSource, Map<Integer, Integer>> changed) {
        final var delta = store.updateState(changed);
        if (delta.isEmpty()) return;

        log.debug("sources changed: {}", delta.keySet());
        publish(store.getState(), delta);
    }

    private void publish(Map<ItemSource, Map<Integer, Integer>> snapshot,
                         Map<ItemSource, Map<Integer, Integer>> delta) {
        for (HerbloreListener listener : listeners) {
            try {
                listener.onStateChanged(snapshot, delta);
            } catch (Exception e) {
                log.warn("listener {} threw", listener.getClass().getSimpleName(), e);
            }
        }
    }
}
