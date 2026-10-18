package adamski.app;

import adamski.domain.route.RecipeChainResolver;
import adamski.domain.route.RecipeSelection;
import adamski.domain.route.RecipeGraph;
import adamski.domain.route.RecipeSelector;
import adamski.domain.result.ChainResultCalculator;
import adamski.domain.ItemQuantities;
import adamski.domain.ItemSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orchestrator - receives changes from the adapter, updates state via the store, runs the
 * calculators and publishes to listeners.
 */
@Slf4j
@Singleton
public class HerbloreApp {
    private static final Set<ItemSource> SOURCES =
            EnumSet.of(ItemSource.Bank, ItemSource.PotionStorage, ItemSource.SeedVault);

    private final List<ResultListener> resultListeners = new CopyOnWriteArrayList<>();

    private final List<SelectionListener> selectionListeners = new CopyOnWriteArrayList<>();

    private final HerbloreStore store;

    private final RecipeChainResolver chainResolver;

    private final RecipeSelector selector;

    @Getter
    private volatile HerbloreResult result;

    @Inject
    public HerbloreApp(HerbloreStore store, RecipeGraph graph) {
        this.store = store;
        this.chainResolver = new RecipeChainResolver(graph);
        this.selector = new RecipeSelector(graph);
    }

    /**
     * Points one chain at a product and republishes against what is already held.
     * @param rootItemId    the item this chain starts from
     * @param productItemId the item the chain will end at, {@code rootItemId} itself to make nothing, or
     *                      {@link RecipeSelection#DEFAULT} for its default
     */
    public void selectProduct(int rootItemId, int productItemId) {
        var newSelection = selector.select(store.selection(), rootItemId, productItemId);
        var selectionChanged = store.updateSelection(newSelection);

        if (!selectionChanged) return;

        log.debug("item {} set to make item {}", rootItemId, productItemId);
        publishSelection(newSelection);

        if (result == null) return;

        result = recalculate();
        publishResult(result);
    }

    /**
     * Replaces the whole selection with one saved earlier.
     */
    public void restoreSelection(RecipeSelection saved) {
        if (!store.updateSelection(saved)) return;

        log.debug("selection restored: {}", saved);
        if (result == null) return;

        result = recalculate();
        publishResult(result);
    }

    public void updateItems(Map<ItemSource, ItemQuantities> changed) {
        final var delta = store.updateItems(changed);
        if (delta.isEmpty()) return;

        log.debug("sources changed: {}", delta.keySet());

        result = recalculate();
        publishResult(result);
    }

    public void addResultListener(ResultListener listener) {
        resultListeners.add(listener);
    }

    public void removeResultListener(ResultListener listener) {
        resultListeners.remove(listener);
    }

    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        selectionListeners.remove(listener);
    }

    private HerbloreResult recalculate() {
        // Gather all owned items across item sources e.g. bank, seed vault
        final var ownedItems = mergeSources(store.itemsBySource());

        // Determine which recipe chains will be used (based on product selection)
        final var recipeChains = chainResolver.resolve(ownedItems, store.selection());

        // Calculate the XP & quantity result for each chain
        final var chainResults = ChainResultCalculator.calculate(recipeChains, ownedItems);

        return new HerbloreResult(ownedItems, chainResults);
    }

    private static ItemQuantities mergeSources(Map<ItemSource, ItemQuantities> snapshot) {
        ItemQuantities merged = ItemQuantities.EMPTY;

        for (ItemSource source : SOURCES) {
            merged = merged.plus(snapshot.getOrDefault(source, ItemQuantities.EMPTY));
        }

        return merged;
    }

    private void publishSelection(RecipeSelection selection) {
        for (SelectionListener listener : selectionListeners) {
            try {
                listener.onSelectionChanged(selection);
            } catch (Exception e) {
                log.warn("selection listener {} threw", listener.getClass().getSimpleName(), e);
            }
        }
    }

    private void publishResult(HerbloreResult result) {
        for (ResultListener listener : resultListeners) {
            try {
                listener.onResultChanged(result);
            } catch (Exception e) {
                log.warn("listener {} threw", listener.getClass().getSimpleName(), e);
            }
        }
    }
}
