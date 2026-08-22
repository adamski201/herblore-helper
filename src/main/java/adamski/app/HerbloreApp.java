package adamski.app;

import adamski.data.Recipes;
import adamski.domain.Recipe;
import adamski.domain.RecipeChainCalculator;
import adamski.domain.RecipeGraph;
import adamski.domain.ChainResultCalculator;
import adamski.domain.ItemQuantities;
import adamski.domain.ItemSource;
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

    /**
     * The chosen product per chain, keyed by the chain's root item. Empty until config lands, so
     * every chain takes its default.
     */
    private final Map<Integer, Integer> productByItem = new HashMap<>();

    private final List<HerbloreListener> listeners = new CopyOnWriteArrayList<>();

    private final HerbloreStore store;

    /**
     * The recipe table in force. Swapped rather than mutated, because a recipe is a map key and
     * results hold on to the ones they were computed from.
     */
    @Getter
    private List<Recipe> recipes;

    private RecipeChainCalculator chainCalculator;

    @Getter
    private volatile HerbloreResult result;

    @Inject
    public HerbloreApp(HerbloreStore store) {
        this.store = store;
        adoptRecipes(Recipes.all());
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

        log.debug("sources changed: {}", delta.keySet());

        result = recalculate();
        publishResult(result);
    }

    private void adoptRecipes(List<Recipe> recipes) {
        this.recipes = List.copyOf(recipes);
        this.chainCalculator = new RecipeChainCalculator(new RecipeGraph(this.recipes));
    }

    private HerbloreResult recalculate() {
        // Gather all owned items across item sources e.g. bank, seed vault
        final var ownedItems = mergeSources(store.getState());

        // Determine which recipe chains will be used (based on product selection)
        final var recipeChains = chainCalculator.calculate(ownedItems, productByItem);

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
