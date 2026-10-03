package adamski.app;

import adamski.data.Recipes;
import adamski.domain.RecipeChainResolver;
import adamski.domain.RecipeSelection;
import adamski.domain.RecipeGraph;
import adamski.domain.RecipeSelector;
import adamski.domain.ChainResultCalculator;
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

    private final List<HerbloreListener> listeners = new CopyOnWriteArrayList<>();

    private final HerbloreStore store;

    private final RecipeChainResolver chainResolver;

    private final RecipeSelector selector;

    @Getter
    private volatile HerbloreResult result;

    @Inject
    public HerbloreApp(HerbloreStore store) {
        this.store = store;

        final RecipeGraph graph = new RecipeGraph(Recipes.all());
        this.chainResolver = new RecipeChainResolver(graph);
        this.selector = new RecipeSelector(graph);
    }

    /**
     * Points one chain at a product and republishes against what is already held.
     * <p>
     * Nothing is published until a source has been read, so a selection made before the first bank
     * read is remembered and takes effect when that read arrives.
     *
     * @param rootItemId    the item this chain starts from, as carried by the chain result
     * @param productItemId what it should end at, or {@link RecipeSelection#DEFAULT} for its default
     */
    public void selectProduct(int rootItemId, int productItemId) {
        var newSelection = selector.select(store.selection(), rootItemId, productItemId);
        var selectionChanged = store.updateSelection(newSelection);

        if (!selectionChanged) return;

        log.debug("item {} set to make item {}", rootItemId, productItemId);
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

    public void addListener(HerbloreListener listener) {
        listeners.add(listener);
    }

    public void removeListener(HerbloreListener listener) {
        listeners.remove(listener);
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

    private void publishResult(HerbloreResult result) {
        for (HerbloreListener listener : listeners) {
            try {
                listener.onResultChanged(result);
            } catch (Exception e) {
                log.warn("listener {} threw", listener.getClass().getSimpleName(), e);
            }
        }
    }
}
