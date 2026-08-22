package adamski.infrastructure;

import adamski.data.Recipes;
import adamski.data.PotionDoses;
import adamski.domain.ItemQuantities;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

/**
 * Tracks the bank's Potion Storage.
 */
@Singleton
public class PotionStorageAdapter {
    /**
      Unlike the item containers, the RuneLite API doesn't expose a change event. This class handles that by listening
     to when the bank interface finishes rebuilding or when varps indicating a potion storage interface rebuild arrive,
     then reading the potion storage.
     */

    private final Client client;

    private boolean dirty;

    private Set<Integer> varps;

    @Inject
    public PotionStorageAdapter(Client client) {
        this.client = client;
    }

    public void scriptFired(int scriptId) {
        // The bank interface rebuilding is the non-varp way the store can change.
        if (scriptId == ScriptID.BANKMAIN_FINISHBUILDING) {
            dirty = true;

            if (varps == null) {
                initialiseVarps();
            }
        }
    }

    public void varbitChanged(int varpId) {
        if (varps != null && varps.contains(varpId)) {
            dirty = true;
        }
    }

    /**
     * @return a map of potion doses (by canonical, single-dose item ID) if a change occurred; otherwise null
     */
    public ItemQuantities poll() {
        if (!dirty) return null;
        dirty = false;
        return ItemQuantities.counted(getPotions());
    }

    private void initialiseVarps() {
        final Widget widget = client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
        if (widget == null) return;

        final int[] triggers = widget.getVarTransmitTrigger();
        if (triggers == null || triggers.length == 0) return;

        varps = new HashSet<>();
        Arrays.stream(triggers).forEach(varps::add);
    }

    private Map<Integer, Integer> getPotions() {
        final Map<Integer, Integer> doses = new HashMap<>();

        for (int groupEnumId : new int[]{EnumID.POTIONSTORE_POTIONS, EnumID.POTIONSTORE_UNFINISHED_POTIONS}) {
            final EnumComposition group = client.getEnum(groupEnumId);
            if (group == null) continue;

            for (int potionEnumId : group.getIntVals()) {
                final EnumComposition potion = client.getEnum(potionEnumId);
                if (potion == null) continue;

                // Read potion storage for current potion
                client.runScript(ScriptID.POTIONSTORE_DOSES, potionEnumId);
                final int stored = client.getIntStack()[0];
                if (stored <= 0) continue;

                // Quantities are already 1 dose, but ItemId may need to be normalised
                final int canonicalId = PotionDoses.canonicalId(potion.getIntValue(1));

                // Filter for domain-relevant item IDs
                if (!Recipes.isRelevantItem(canonicalId)) continue;

                doses.merge(canonicalId, stored, Integer::sum);
            }
        }

        return doses;
    }
}
