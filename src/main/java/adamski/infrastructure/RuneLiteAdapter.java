package adamski.infrastructure;

import adamski.app.HerbloreApp;
import adamski.data.HerbloreRecipes;
import adamski.data.PotionDoses;
import adamski.domain.models.ItemSource;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapts RuneLite events into domain language.
 * <p>
 * The single EventBus subscriber. Sources accumulate in {@link #pending} as
 * events arrive and are flushed together once per client tick, so HerbloreApp
 * never observes half-applied state - a bank deposit changes two containers and
 * fires two events, and both settle before anything downstream sees either.
 * <p>
 * Sources with no event of their own are driven from here rather than
 * subscribing themselves. That is deliberate: the flush has to happen after
 * every source has settled, and expressing that as program order in
 * {@link #onClientTick} is clearer than spreading @Subscribe priorities across
 * classes. Raw ids are forwarded without interpretation - each source still
 * decides what is relevant to it.
 */
@Singleton
public class RuneLiteAdapter {
    private final ItemManager itemManager;
    private final PotionStorageAdapter potionStorage;
    private final HerbloreApp app;

    private final Map<ItemSource, Map<Integer, Integer>> pending = new EnumMap<>(ItemSource.class);

    @Inject
    public RuneLiteAdapter(ItemManager itemManager, PotionStorageAdapter potionStorage, HerbloreApp app) {
        this.itemManager = itemManager;
        this.potionStorage = potionStorage;
        this.app = app;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.BANK) {
            return;
        }

        pending.put(ItemSource.Bank, getItemQuantitiesFromContainer(event.getItemContainer()));
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        potionStorage.scriptFired(event.getScriptId());
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        potionStorage.varbitChanged(event.getVarpId());
    }

    @Subscribe
    public void onClientTick(ClientTick event) {
        // Sources settle first, then publish once. The order here is the point.
        if (potionStorage.drain()) {
            pending.put(ItemSource.PotionStorage, potionStorage.contents());
        }

        flush();
    }

    private void flush() {
        if (pending.isEmpty()) {
            return;
        }

        app.sourcesUpdated(new EnumMap<>(pending));
        pending.clear();
    }

    private Map<Integer, Integer> getItemQuantitiesFromContainer(ItemContainer container) {
        if (container == null) {
            return Collections.emptyMap();
        }

        final Map<Integer, Integer> items = new HashMap<>();

        for (Item item : container.getItems()) {
            final var id = item.getId();

            if (id == -1 || item.getQuantity() <= 0) continue;

            // Filter placeholder items
            final var comp = itemManager.getItemComposition(id);
            if (comp.getPlaceholderTemplateId() != -1) continue;

            // Merge noted items into unnoted form
            final var unnotedId = itemManager.canonicalize(id);

            // Reduce dose variants to 1-dose units
            final var canonicalId = PotionDoses.canonicalId(unnotedId);
            if (!HerbloreRecipes.isRelevantItem(canonicalId)) continue;

            items.merge(canonicalId, item.getQuantity() * PotionDoses.doses(unnotedId), Integer::sum);
        }

        return items;
    }
}
