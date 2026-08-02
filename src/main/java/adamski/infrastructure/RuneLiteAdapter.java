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
 */
@Singleton
public class RuneLiteAdapter {
    private final ItemManager itemManager;
    private final PotionStorageAdapter potionStorage;
    private final HerbloreApp app;

    private final Map<ItemSource, Map<Integer, Integer>> pending = new EnumMap<>(ItemSource.class);

    private final Map<Integer, ItemSource> inventoryItemSources = Map.of(
            InventoryID.BANK, ItemSource.Bank,
            InventoryID.SEED_VAULT, ItemSource.SeedVault
    );

    @Inject
    public RuneLiteAdapter(ItemManager itemManager, PotionStorageAdapter potionStorage, HerbloreApp app) {
        this.itemManager = itemManager;
        this.potionStorage = potionStorage;
        this.app = app;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        final var itemSource = inventoryItemSources.get(event.getContainerId());
        if (itemSource == null) return;

        pending.put(itemSource, getItemQuantitiesFromContainer(event.getItemContainer()));
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
        // Sources are gathered into an array and dispatched together so that simultaneous actions (e.g. deposit
        // inv to bank) dispatch one action rather than multiple.

        final var potionDoses = potionStorage.poll();
        if (potionDoses != null) {
            pending.put(ItemSource.PotionStorage, potionDoses);
        }

        if (pending.isEmpty()) return;

        app.sourcesUpdated(new EnumMap<>(pending));
        pending.clear();
    }

    private Map<Integer, Integer> getItemQuantitiesFromContainer(ItemContainer container) {
        if (container == null) return Collections.emptyMap();

        final Map<Integer, Integer> items = new HashMap<>();

        for (Item item : container.getItems()) {
            final var id = item.getId();

            if (id == -1 || item.getQuantity() <= 0) continue;

            // Filter placeholder items
            final var comp = itemManager.getItemComposition(id);
            if (comp.getPlaceholderTemplateId() != -1) continue;

            final var unnotedId = itemManager.canonicalize(id); // Merge noted items into unnoted form
            final var doses = item.getQuantity() * PotionDoses.doses(unnotedId); // Calculate doses
            final var canonicalId = PotionDoses.canonicalId(unnotedId); // Reduce dose variants to 1-dose units
            if (!HerbloreRecipes.isRelevantItem(canonicalId)) continue; // Filter for domain-relevant items

            items.merge(canonicalId, doses, Integer::sum);
        }

        return items;
    }
}
