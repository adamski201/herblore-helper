package adamski.app;

import adamski.data.HerbloreRecipes;
import adamski.data.RecipeDependencyResolver;
import adamski.domain.calculators.BankedXpCalculator;
import adamski.domain.calculators.RecipeYieldCalculator;
import adamski.domain.calculators.SecondaryBalanceCalculator;
import adamski.domain.models.BankedXpResult;
import adamski.domain.models.ItemSource;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeRun;
import adamski.domain.models.SecondaryBalance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orchestrator - receives changes from the adapter, updates state via the store, runs the
 * calculators and publishes to listeners.
 * <p>
 * The recipe table, the source merge and the yield cascade all get resolved here. The calculators
 * themselves are domain rules and know none of it.
 */
@Slf4j
@Singleton
public class HerbloreApp {
    /**
     * Which containers count towards the total. Inventory is never populated by the adapter, and
     * whether it should count as banked is still open.
     */
    private static final Set<ItemSource> COUNTED_SOURCES =
            EnumSet.of(ItemSource.Bank, ItemSource.PotionStorage, ItemSource.SeedVault);

    /**
     * One recipe per primary item, in dependency order.
     * <p>
     * First in table order wins where several recipes consume the same item - a placeholder for
     * user config, which will pick one recipe per primary. Only the choice changes, not the shape.
     */
    private static final List<Recipe> RECIPES;

    static {
        final Map<Integer, Recipe> selection = new HashMap<>();
        for (Recipe recipe : HerbloreRecipes.all()) {
            selection.putIfAbsent(recipe.getPrimary().getItemId(), recipe);
        }

        final List<Recipe> ordered = new ArrayList<>(selection.size());
        for (Integer itemId : RecipeDependencyResolver.order()) {
            final Recipe recipe = selection.get(itemId);
            if (recipe != null) ordered.add(recipe);
        }

        RECIPES = Collections.unmodifiableList(ordered);
    }

    private final List<HerbloreListener> listeners = new CopyOnWriteArrayList<>();

    private final HerbloreStore store;

    @Getter
    private volatile BankedXpResult bankedXp;

    @Getter
    private volatile SecondaryBalance secondaryBalance;

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

        final var snapshot = store.getState();
        final var owned = merge(snapshot);

        // Resolved once, then folded twice. Sharing the runs is what stops the XP total and the
        // shopping list from being computed off different numbers.
        final List<RecipeRun> yields = RecipeYieldCalculator.calculate(owned, RECIPES);
        bankedXp = BankedXpCalculator.calculate(yields);
        secondaryBalance = SecondaryBalanceCalculator.calculate(yields, owned);

        log.debug("sources changed: {}, banked xp: {}", delta.keySet(), bankedXp.getTotal());
        publish(snapshot, delta, bankedXp, secondaryBalance);
    }

    private static Map<Integer, Integer> merge(Map<ItemSource, Map<Integer, Integer>> snapshot) {
        final Map<Integer, Integer> merged = new HashMap<>();

        for (ItemSource source : COUNTED_SOURCES) {
            snapshot.getOrDefault(source, Collections.emptyMap())
                    .forEach((itemId, quantity) -> merged.merge(itemId, quantity, Integer::sum));
        }

        return merged;
    }

    private void publish(Map<ItemSource, Map<Integer, Integer>> snapshot,
                         Map<ItemSource, Map<Integer, Integer>> delta,
                         BankedXpResult bankedXp,
                         SecondaryBalance secondaryBalance) {
        for (HerbloreListener listener : listeners) {
            try {
                listener.onStateChanged(snapshot, delta, bankedXp, secondaryBalance);
            } catch (Exception e) {
                log.warn("listener {} threw", listener.getClass().getSimpleName(), e);
            }
        }
    }
}
