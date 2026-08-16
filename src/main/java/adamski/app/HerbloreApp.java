package adamski.app;

import adamski.data.Recipes;
import adamski.domain.calculators.RecipeGrouper;
import adamski.domain.calculators.RecipeGraph;
import adamski.domain.calculators.RecipeYieldCalculator;
import adamski.domain.calculators.RowPlanner;
import adamski.domain.calculators.SecondaryBalanceCalculator;
import adamski.domain.models.ItemQuantities;
import adamski.domain.models.ItemSource;
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
import java.util.stream.Collectors;

/**
 * Orchestrator - receives changes from the adapter, updates state via the store, runs the
 * calculators and publishes to listeners.
 */
@Slf4j
@Singleton
public class HerbloreApp {
    private static final Set<ItemSource> SOURCES =
            EnumSet.of(ItemSource.Bank, ItemSource.PotionStorage, ItemSource.SeedVault);

    /**
     * The chosen product per row, keyed by the row's entry item. Empty until config lands, so every
     * row takes its default.
     */
    private final Map<Integer, Integer> productByItem = new HashMap<>();

    private final List<HerbloreListener> listeners = new CopyOnWriteArrayList<>();

    private final HerbloreStore store;
    private final RecipeGraph graph;

    @Getter
    private volatile HerbloreResult result;

    @Inject
    public HerbloreApp(HerbloreStore store) {
        this.store = store;
        this.graph = new RecipeGraph(Recipes.all());
    }

    public void addListener(HerbloreListener listener) {
        listeners.add(listener);
    }

    public void removeListener(HerbloreListener listener) {
        listeners.remove(listener);
    }

    public void sourcesUpdated(Map<ItemSource, ItemQuantities> changed) {
        final var delta = store.updateState(changed);
        if (delta.isEmpty()) return;

        // Gather all owned items across item sources e.g. bank, seed vault
        final var ownedItems = mergeSources(store.getState());

        final var rows = RowPlanner.plan(ownedItems, productByItem, graph);
        final var selection = RowPlanner.select(rows, graph);

        final var recipeRunsByBankedItem = RecipeYieldCalculator.calculateByBankedItem(ownedItems, selection);

        final var yields = recipeRunsByBankedItem.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        result = new HerbloreResult(
                ownedItems,
                RecipeGrouper.group(recipeRunsByBankedItem, rows, ownedItems),
                SecondaryBalanceCalculator.calculate(yields, ownedItems));

        log.debug("sources changed: {}, banked xp: {}", delta.keySet(), result.getTotalXp());
        publishResult(result);
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
