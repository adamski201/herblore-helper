package adamski.infrastructure;

import adamski.data.HerbloreRecipes;
import adamski.data.PotionDoses;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the bank's Potion Storage.
 * <p>
 * Unlike the item containers, the store has no change event of its own. It is
 * driven by varps and by the bank interface rebuilding, so it is marked dirty as
 * those arrive and drained once per client tick - a burst of varp changes within
 * one tick collapses into a single read, and the read never observes half-applied
 * state.
 * <p>
 * This class does not subscribe to the EventBus. RuneLiteAdapter feeds it raw
 * event data and drains it, so that the ordering between "sources settle" and
 * "publish once" is explicit program order rather than dispatch priority. It
 * still owns the knowledge of <em>which</em> varps and script matter - callers
 * hand over ids without interpreting them.
 * <p>
 * Everything here runs on the client thread: the read runs a game script and
 * reads the int stack.
 */
@Singleton
public class PotionStorageAdapter {
    private final Client client;

    private Map<Integer, Integer> contents = Collections.emptyMap();
    private boolean dirty;

    /**
     * Varps the store transmits on, harvested from the widget the first time it
     * exists. Null until the bank has been opened at least once.
     */
    private Set<Integer> varps;

    @Inject
    public PotionStorageAdapter(Client client) {
        this.client = client;
    }

    public void scriptFired(int scriptId) {
        // The bank interface rebuilding is the non-varp way the store can change.
        if (scriptId == ScriptID.BANKMAIN_FINISHBUILDING) {
            dirty = true;
        }
    }

    public void varbitChanged(int varpId) {
        if (varps != null && varps.contains(varpId)) {
            dirty = true;
        }
    }

    /**
     * Drains the dirty flag, re-reading the store if it was set.
     *
     * @return true only if the contents actually changed
     */
    public boolean drain() {
        if (!dirty) {
            return false;
        }
        dirty = false;

        captureVarps();

        final Map<Integer, Integer> updated = read();
        if (updated.equals(contents)) {
            return false;
        }

        contents = updated;
        return true;
    }

    /**
     * @return potion storage contents keyed by 1-dose item id, valued in doses
     */
    public Map<Integer, Integer> contents() {
        return Collections.unmodifiableMap(contents);
    }

    /**
     * The varps are only discoverable from the widget, which does not exist until
     * the bank has been opened. Until then we rely on BANKMAIN_FINISHBUILDING alone
     * - which is also what creates the widget, so this self-arms on the first visit.
     */
    private void captureVarps() {
        if (varps != null) {
            return;
        }

        final Widget widget = client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
        if (widget == null) {
            return;
        }

        final int[] triggers = widget.getVarTransmitTrigger();
        if (triggers == null) {
            return;
        }

        varps = new HashSet<>();
        Arrays.stream(triggers).forEach(varps::add);
    }

    private Map<Integer, Integer> read() {
        final Map<Integer, Integer> doses = new HashMap<>();

        for (int groupEnumId : new int[]{EnumID.POTIONSTORE_POTIONS, EnumID.POTIONSTORE_UNFINISHED_POTIONS}) {
            final EnumComposition group = client.getEnum(groupEnumId);
            if (group == null) {
                continue;
            }

            for (int potionEnumId : group.getIntVals()) {
                final EnumComposition potion = client.getEnum(potionEnumId);
                if (potion == null) {
                    continue;
                }

                client.runScript(ScriptID.POTIONSTORE_DOSES, potionEnumId);
                final int stored = client.getIntStack()[0];
                if (stored <= 0) {
                    continue;
                }

                // Quantities are already 1 dose, but ItemId may need to be normalised
                final int canonicalId = PotionDoses.canonicalId(potion.getIntValue(1));

                if (!HerbloreRecipes.isRelevantItem(canonicalId)) {
                    continue;
                }

                doses.merge(canonicalId, stored, Integer::sum);
            }
        }

        return doses;
    }
}
