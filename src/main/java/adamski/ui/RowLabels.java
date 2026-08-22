package adamski.ui;

import net.runelite.api.gameval.ItemID;

import java.util.HashMap;
import java.util.Map;

/**
 * What to call a row whose entry item's own name reads badly. A row fed by ranarr seeds is a ranarr
 * row, not a seed row.
 * <p>
 * Display only - a missing entry means the row is named after the entry item itself, which is
 * visibly slightly off and affects no number.
 */
final class RowLabels {
    private static final Map<Integer, Integer> NAMED_AFTER = new HashMap<>();

    static {
        herb(ItemID.GUAM_LEAF, ItemID.GUAM_SEED, ItemID.UNIDENTIFIED_GUAM, ItemID.GUAMVIAL);
        herb(ItemID.MARENTILL, ItemID.MARRENTILL_SEED, ItemID.UNIDENTIFIED_MARENTILL, ItemID.MARRENTILLVIAL);
        herb(ItemID.TARROMIN, ItemID.TARROMIN_SEED, ItemID.UNIDENTIFIED_TARROMIN, ItemID.TARROMINVIAL);
        herb(ItemID.HARRALANDER, ItemID.HARRALANDER_SEED, ItemID.UNIDENTIFIED_HARRALANDER, ItemID.HARRALANDERVIAL);
        herb(ItemID.RANARR_WEED, ItemID.RANARR_SEED, ItemID.UNIDENTIFIED_RANARR, ItemID.RANARRVIAL);
        herb(ItemID.TOADFLAX, ItemID.TOADFLAX_SEED, ItemID.UNIDENTIFIED_TOADFLAX, ItemID.TOADFLAXVIAL);
        herb(ItemID.IRIT_LEAF, ItemID.IRIT_SEED, ItemID.UNIDENTIFIED_IRIT, ItemID.IRITVIAL);
        herb(ItemID.AVANTOE, ItemID.AVANTOE_SEED, ItemID.UNIDENTIFIED_AVANTOE, ItemID.AVANTOEVIAL);
        herb(ItemID.KWUARM, ItemID.KWUARM_SEED, ItemID.UNIDENTIFIED_KWUARM, ItemID.KWUARMVIAL);
        herb(ItemID.HUASCA, ItemID.HUASCA_SEED, ItemID.UNIDENTIFIED_HUASCA, ItemID.HUASCAVIAL);
        herb(ItemID.SNAPDRAGON, ItemID.SNAPDRAGON_SEED, ItemID.UNIDENTIFIED_SNAPDRAGON, ItemID.SNAPDRAGONVIAL);
        herb(ItemID.CADANTINE, ItemID.CADANTINE_SEED, ItemID.UNIDENTIFIED_CADANTINE, ItemID.CADANTINEVIAL);
        herb(ItemID.LANTADYME, ItemID.LANTADYME_SEED, ItemID.UNIDENTIFIED_LANTADYME, ItemID.LANTADYMEVIAL);
        herb(ItemID.DWARF_WEED, ItemID.DWARF_WEED_SEED, ItemID.UNIDENTIFIED_DWARF_WEED, ItemID.DWARFWEEDVIAL);
        herb(ItemID.TORSTOL, ItemID.TORSTOL_SEED, ItemID.UNIDENTIFIED_TORSTOL, ItemID.TORSTOLVIAL);

        herb(ItemID.CORAL_ELKHORN, ItemID.CORAL_ELKHORN_FRAG, ItemID.ELKHORNVIAL);
        herb(ItemID.CORAL_PILLAR, ItemID.CORAL_PILLAR_FRAG, ItemID.PILLARVIAL);
        herb(ItemID.CORAL_UMBRAL, ItemID.CORAL_UMBRAL_FRAG, ItemID.UMBRALVIAL);
    }

    private RowLabels() {
    }

    /**
     * @return the item this row should be named after, which is the entry item unless it reads badly
     */
    static int nameFor(int entryItemId) {
        return NAMED_AFTER.getOrDefault(entryItemId, entryItemId);
    }

    private static void herb(int namedAfter, int... members) {
        for (int member : members) {
            NAMED_AFTER.put(member, namedAfter);
        }
    }
}
