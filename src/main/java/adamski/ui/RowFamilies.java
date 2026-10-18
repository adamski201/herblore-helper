package adamski.ui;

import net.runelite.api.gameval.ItemID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which family a row belongs to, and where that family sits in the panel. A row fed by ranarr seeds
 * is a ranarr row, listed with the other herbs in level order.
 * <p>
 * Display only - a row of no family here is listed after every family, which is visibly slightly
 * off and affects no number.
 */
final class RowFamilies {
    private static final Map<Integer, Integer> FAMILY_BY_ITEM_ID = new HashMap<>();

    /**
     * Families in the order the panel lists them: herbs by level, then coral, then caviar.
     */
    private static final List<Integer> FAMILY_ORDER = new ArrayList<>();

    static {
        family(ItemID.GUAM_LEAF, ItemID.GUAM_SEED, ItemID.UNIDENTIFIED_GUAM, ItemID.GUAMVIAL);
        family(ItemID.MARENTILL, ItemID.MARRENTILL_SEED, ItemID.UNIDENTIFIED_MARENTILL, ItemID.MARRENTILLVIAL);
        family(ItemID.TARROMIN, ItemID.TARROMIN_SEED, ItemID.UNIDENTIFIED_TARROMIN, ItemID.TARROMINVIAL);
        family(ItemID.HARRALANDER, ItemID.HARRALANDER_SEED, ItemID.UNIDENTIFIED_HARRALANDER, ItemID.HARRALANDERVIAL);
        family(ItemID.RANARR_WEED, ItemID.RANARR_SEED, ItemID.UNIDENTIFIED_RANARR, ItemID.RANARRVIAL);
        family(ItemID.TOADFLAX, ItemID.TOADFLAX_SEED, ItemID.UNIDENTIFIED_TOADFLAX, ItemID.TOADFLAXVIAL);
        family(ItemID.IRIT_LEAF, ItemID.IRIT_SEED, ItemID.UNIDENTIFIED_IRIT, ItemID.IRITVIAL);
        family(ItemID.AVANTOE, ItemID.AVANTOE_SEED, ItemID.UNIDENTIFIED_AVANTOE, ItemID.AVANTOEVIAL);
        family(ItemID.KWUARM, ItemID.KWUARM_SEED, ItemID.UNIDENTIFIED_KWUARM, ItemID.KWUARMVIAL);
        family(ItemID.HUASCA, ItemID.HUASCA_SEED, ItemID.UNIDENTIFIED_HUASCA, ItemID.HUASCAVIAL);
        family(ItemID.SNAPDRAGON, ItemID.SNAPDRAGON_SEED, ItemID.UNIDENTIFIED_SNAPDRAGON, ItemID.SNAPDRAGONVIAL);
        family(ItemID.CADANTINE, ItemID.CADANTINE_SEED, ItemID.UNIDENTIFIED_CADANTINE, ItemID.CADANTINEVIAL);
        family(ItemID.LANTADYME, ItemID.LANTADYME_SEED, ItemID.UNIDENTIFIED_LANTADYME, ItemID.LANTADYMEVIAL);
        family(ItemID.DWARF_WEED, ItemID.DWARF_WEED_SEED, ItemID.UNIDENTIFIED_DWARF_WEED, ItemID.DWARFWEEDVIAL);
        family(ItemID.TORSTOL, ItemID.TORSTOL_SEED, ItemID.UNIDENTIFIED_TORSTOL, ItemID.TORSTOLVIAL);

        family(ItemID.CORAL_ELKHORN, ItemID.CORAL_ELKHORN_FRAG, ItemID.ELKHORNVIAL);
        family(ItemID.CORAL_PILLAR, ItemID.CORAL_PILLAR_FRAG, ItemID.PILLARVIAL);
        family(ItemID.CORAL_UMBRAL, ItemID.CORAL_UMBRAL_FRAG, ItemID.UMBRALVIAL);

        family(ItemID.BRUT_CAVIAR);
    }

    private RowFamilies() {
    }

    /**
     * @return the item that stands for this row's family, or the entry item if it has none
     */
    private static int familyOf(int entryItemId) {
        return FAMILY_BY_ITEM_ID.getOrDefault(entryItemId, entryItemId);
    }

    /**
     * @return where this row's family is listed, lower first. A row of no family here comes after
     * all of them.
     */
    static int familyRankOf(int entryItemId) {
        final int rank = FAMILY_ORDER.indexOf(familyOf(entryItemId));
        return rank < 0 ? Integer.MAX_VALUE : rank;
    }

    private static void family(int family, int... members) {
        FAMILY_ORDER.add(family);

        for (int member : members) {
            FAMILY_BY_ITEM_ID.put(member, family);
        }
    }
}
