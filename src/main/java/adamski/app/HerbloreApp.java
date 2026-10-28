package adamski.app;

import adamski.domain.route.RecipeChainResolver;
import adamski.domain.route.RecipeSelection;
import adamski.domain.route.RecipeGraph;
import adamski.domain.route.RecipeSelector;
import adamski.domain.result.ChainResultCalculator;
import adamski.domain.result.YieldOverrides;
import adamski.data.Recipes;
import adamski.domain.ItemQuantities;
import adamski.domain.ItemSource;
import adamski.domain.SecondaryPrecursor;
import adamski.domain.balance.SecondaryBalanceCalculator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.HashMap;
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

    private final List<SecondaryPrecursor> precursors;

    @Getter
    private volatile HerbloreResult result;

    @Inject
    public HerbloreApp(HerbloreStore store, RecipeGraph graph, List<SecondaryPrecursor> precursors) {
        this.store = store;
        this.chainResolver = new RecipeChainResolver(graph);
        this.selector = new RecipeSelector(graph);
        this.precursors = List.copyOf(precursors);
    }

    /**
     * Replaces every setting at once, and republishes against what is already held if any changed.
     */
    public void applySettings(HerbloreSettings settings) {
        if (!store.updateSettings(settings)) return;

        log.debug("settings changed: {}", settings);
        if (result == null) return;

        result = recalculate();
        publishResult(result);
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
     * Replaces the whole selection with one saved earlier, and republishes the result.
     */
    public void restoreSelection(RecipeSelection saved) {
        if (!store.updateSelection(saved)) return;

        log.debug("selection restored: {}", saved);
        if (result == null) return;

        result = recalculate();
        publishResult(result);
    }

    public void updateItems(Map<ItemSource, ItemQuantities> changed) {
        if (!store.updateItems(changed)) return;

        log.debug("sources read: {}", changed.keySet());

        result = recalculate();
        publishResult(result);
    }

    public void updateCurrentXp(int xp) {
        if (!store.updateCurrentXp(xp)) return;

        log.debug("herblore xp: {}", xp);
        if (result == null) return;

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
        final var settings = store.settings();

        // Gather all owned items across item sources e.g. bank, seed vault
        final var ownedItems = mergeSources(store.itemsBySource());

        // Determine which recipe chains will be used (based on product selection)
        final var routedChains = chainResolver.resolve(ownedItems, store.selection());

        // Override recipe yields with those configured by player's settings
        final var recipeChains = YieldOverrides.apply(routedChains, getRecipeYieldOverrides(settings));

        // Calculate the XP & quantity result for each chain
        final var chainResults = ChainResultCalculator.calculate(recipeChains, ownedItems);

        // Calculate the total surplus/deficit of secondaries
        final var secondaryBalance = SecondaryBalanceCalculator.calculate(
                chainResults,
                ownedItems,
                settings.isChainProductsCounted(),
                settings.isPrecursorsCounted() ? precursors : List.of());

        return new HerbloreResult(ownedItems, store.currentXp(), chainResults, secondaryBalance);
    }

    private static Map<Integer, Double> getRecipeYieldOverrides(HerbloreSettings settings) {
        final Map<Integer, Double> overrides = new HashMap<>();
        overrides.put(Recipes.CUT_LEAPING_STURGEON_RECIPE_ID, settings.getCaviarSuccessRate());
        settings.getSeedYieldBySeedItemId().forEach((seedItemId, yield) ->
                overrides.put(Recipes.seedRecipeIdOf(seedItemId), yield));

        return overrides;
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
