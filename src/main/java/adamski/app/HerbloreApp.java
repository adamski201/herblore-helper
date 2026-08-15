package adamski.app;

import adamski.data.RecipePaths;
import adamski.data.RecipeRoutes;
import adamski.domain.calculators.RecipeGrouper;
import adamski.domain.calculators.RecipeYieldCalculator;
import adamski.domain.calculators.SecondaryBalanceCalculator;
import adamski.domain.models.ItemSource;
import adamski.domain.models.Recipe;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
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

    static final List<Recipe> RECIPES = RecipeRoutes.defaultSelection();

    private static final Map<Integer, Integer> TERMINAL_BY_ITEM = RecipeRoutes.terminalByItem(RECIPES);

    private final List<HerbloreListener> listeners = new CopyOnWriteArrayList<>();

    private final HerbloreStore store;

    @Getter
    private volatile HerbloreResult result;

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

        final var ownedItems = mergeSources(store.getState());

        final var recipeRunsByBankedItem = RecipeYieldCalculator.calculateByBankedItem(ownedItems, RECIPES);

        final var yields = recipeRunsByBankedItem.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        result = new HerbloreResult(
                ownedItems,
                RecipeGrouper.group(recipeRunsByBankedItem, RecipePaths.pathsByItem(), TERMINAL_BY_ITEM, RECIPES),
                SecondaryBalanceCalculator.calculate(yields, ownedItems));

        log.debug("sources changed: {}, banked xp: {}", delta.keySet(), result.getTotalXp());
        publishResult(result);
    }

    private static Map<Integer, Integer> mergeSources(Map<ItemSource, Map<Integer, Integer>> snapshot) {
        final Map<Integer, Integer> merged = new HashMap<>();

        for (ItemSource source : SOURCES) {
            snapshot.getOrDefault(source, Collections.emptyMap())
                    .forEach((itemId, quantity) -> merged.merge(itemId, quantity, Integer::sum));
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
