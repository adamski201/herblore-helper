package adamski.data;

import net.runelite.api.gameval.ItemID;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which path each item belongs to. A path is a production chain the player treats as one thing,
 * identified by a representative item id.
 * <p>
 * Only primaries and outputs need a path - secondaries are bought rather than produced, so they
 * belong to no chain.
 */
public final class RecipePaths {
    public static final int NO_PATH = -1;

    private static final Map<Integer, Integer> PATH_BY_ITEM = new LinkedHashMap<>();
    private static final Set<Integer> PATHS = new LinkedHashSet<>();

    static {
        // --- herbs ---------------------------------------------------------
        createPath(ItemID.GUAM_LEAF,
                ItemID.GUAM_SEED, ItemID.UNIDENTIFIED_GUAM, ItemID.GUAMVIAL,
                ItemID._1DOSE1ATTACK);

        createPath(ItemID.MARENTILL,
                ItemID.MARRENTILL_SEED, ItemID.UNIDENTIFIED_MARENTILL, ItemID.MARRENTILLVIAL,
                ItemID._1DOSEANTIPOISON);

        createPath(ItemID.TARROMIN,
                ItemID.TARROMIN_SEED, ItemID.UNIDENTIFIED_TARROMIN, ItemID.TARROMINVIAL,
                ItemID._1DOSE1STRENGTH, ItemID.MORT_SERUM1);

        createPath(ItemID.HARRALANDER,
                ItemID.HARRALANDER_SEED, ItemID.UNIDENTIFIED_HARRALANDER, ItemID.HARRALANDERVIAL,
                ItemID._1DOSESTATRESTORE, ItemID._1DOSE1ENERGY, ItemID._1DOSECOMBAT,
                ItemID._1DOSEGOADING, ItemID.SUPERCOMPOST_POTION_1);

        createPath(ItemID.RANARR_WEED,
                ItemID.RANARR_SEED, ItemID.UNIDENTIFIED_RANARR, ItemID.RANARRVIAL,
                ItemID._1DOSE1DEFENSE, ItemID._1DOSEPRAYERRESTORE);

        createPath(ItemID.TOADFLAX,
                ItemID.TOADFLAX_SEED, ItemID.UNIDENTIFIED_TOADFLAX, ItemID.TOADFLAXVIAL,
                ItemID._1DOSE1AGILITY, ItemID._1DOSEPOTIONOFSARADOMIN,
                ItemID.UNFINISHED_ANTIDOTE_, ItemID.ANTIDOTE_1);

        createPath(ItemID.IRIT_LEAF,
                ItemID.IRIT_SEED, ItemID.UNIDENTIFIED_IRIT, ItemID.IRITVIAL,
                ItemID._1DOSE2ATTACK, ItemID._1DOSE2ANTIPOISON,
                ItemID.UNFINISHED_ANTIDOTE__, ItemID.ANTIDOTE__1);

        createPath(ItemID.AVANTOE,
                ItemID.AVANTOE_SEED, ItemID.UNIDENTIFIED_AVANTOE, ItemID.AVANTOEVIAL,
                ItemID._1DOSEFISHERSPOTION, ItemID._1DOSE2ENERGY, ItemID._1DOSEHUNTING);

        createPath(ItemID.KWUARM,
                ItemID.KWUARM_SEED, ItemID.UNIDENTIFIED_KWUARM, ItemID.KWUARMVIAL,
                ItemID._1DOSE2STRENGTH, ItemID.WEAPON_POISON);

        createPath(ItemID.HUASCA,
                ItemID.HUASCA_SEED, ItemID.UNIDENTIFIED_HUASCA, ItemID.HUASCAVIAL,
                ItemID._1DOSE1PRAYER_REGENERATION);

        createPath(ItemID.SNAPDRAGON,
                ItemID.SNAPDRAGON_SEED, ItemID.UNIDENTIFIED_SNAPDRAGON, ItemID.SNAPDRAGONVIAL,
                ItemID._1DOSE2RESTORE);

        createPath(ItemID.CADANTINE,
                ItemID.CADANTINE_SEED, ItemID.UNIDENTIFIED_CADANTINE, ItemID.CADANTINEVIAL,
                ItemID._1DOSE2DEFENSE, ItemID.CADANTINE_BLOODVIAL,
                ItemID._1DOSEBASTION, ItemID._1DOSEBATTLEMAGE);

        createPath(ItemID.LANTADYME,
                ItemID.LANTADYME_SEED, ItemID.UNIDENTIFIED_LANTADYME, ItemID.LANTADYMEVIAL,
                ItemID._1DOSE1ANTIDRAGON, ItemID._1DOSE1MAGIC);

        createPath(ItemID.DWARF_WEED,
                ItemID.DWARF_WEED_SEED, ItemID.UNIDENTIFIED_DWARF_WEED, ItemID.DWARFWEEDVIAL,
                ItemID._1DOSERANGERSPOTION, ItemID._1DOSEANCIENTBREW, ItemID._1DOSESTATRENEWAL);

        createPath(ItemID.TORSTOL,
                ItemID.TORSTOL_SEED, ItemID.UNIDENTIFIED_TORSTOL, ItemID.TORSTOLVIAL,
                ItemID._1DOSEPOTIONOFZAMORAK, ItemID._1DOSESURGE, ItemID._1DOSE2COMBAT);

        // --- coral ---------------------------------------------------------
        // JUDGEMENT: the poultice is elkhorn's finished potion; only the dressing upgrade splits out
        createPath(ItemID.CORAL_ELKHORN,
                ItemID.CORAL_ELKHORN_FRAG, ItemID.ELKHORNVIAL, ItemID.HAEMOSTATIC_POULTICE);

        createPath(ItemID.CORAL_PILLAR,
                ItemID.CORAL_PILLAR_FRAG, ItemID.PILLARVIAL,
                ItemID._1DOSE2FISHERSPOTION, ItemID._1DOSE2HUNTING);

        createPath(ItemID.CORAL_UMBRAL,
                ItemID.CORAL_UMBRAL_FRAG, ItemID.UMBRALVIAL, ItemID._1DOSEARMADYLBREW);

        // --- caviar --------------------------------------------------------
        createPath(ItemID.BRUT_CAVIAR,
                ItemID.BRUTAL_1DOSE1ENERGY, ItemID.BRUTAL_1DOSE1DEFENSE, ItemID.BRUTAL_1DOSE1AGILITY,
                ItemID.BRUTAL_1DOSECOMBAT, ItemID.BRUTAL_1DOSEPRAYERRESTORE, ItemID.BRUTAL_1DOSE2ATTACK,
                ItemID.BRUTAL_1DOSE2ANTIPOISON, ItemID.BRUTAL_1DOSEFISHERSPOTION, ItemID.BRUTAL_1DOSE2ENERGY,
                ItemID.BRUTAL_1DOSE1HUNTING, ItemID.BRUTAL_1DOSE2STRENGTH, ItemID.BRUTAL_1DOSE2RESTORE,
                ItemID.BRUTAL_1DOSE2DEFENSE, ItemID.BRUTAL_ANTIDOTE_1, ItemID.BRUTAL_1DOSE1ANTIDRAGON,
                ItemID.BRUTAL_1DOSERANGERSPOTION, ItemID.BRUTAL_1DOSE1MAGIC, ItemID.BRUTAL_1DOSEPOTIONOFZAMORAK,
                ItemID.BRUTAL_1DOSESTAMINA, ItemID.BRUTAL_1DOSE2ANTIDRAGON, ItemID.BRUTAL_1DOSEANCIENTBREW,
                ItemID.BRUTAL_1DOSE3ANTIDRAGON, ItemID.BRUTAL_1DOSE4ANTIDRAGON);

        // --- upgrades made from a finished potion --------------------------
        createPath(ItemID.WEAPON_POISON_,
                ItemID.VIAL_COCONUT_MILK,
                ItemID.UNFINISHED_WEAPON_POISON_, ItemID.UNFINISHED_WEAPON_POISON__,
                ItemID.WEAPON_POISON__);

        createPath(ItemID.BURGH_GUTHIX_BALANCE_1);

        createPath(ItemID._1DOSEHAEMOSTATICDRESSING);

        // JUDGEMENT: the three antifire upgrades are one path, not three
        createPath(ItemID._1DOSE3ANTIDRAGON,
                ItemID._1DOSE2ANTIDRAGON, ItemID._1DOSE4ANTIDRAGON);

        createPath(ItemID._1DOSESTAMINA,
                ItemID._1DOSE2STAMINA);

        createPath(ItemID._1DOSE3ENERGY);

        createPath(ItemID.ANTIVENOM1);

        createPath(ItemID.ANTIVENOM_1);

        createPath(ItemID.EXTENDED_ANTIVENOM_1);

        createPath(ItemID._1DOSEFORGOTTENBREW);

        // --- standalone ----------------------------------------------------
        createPath(ItemID.SANFEW_SALVE_1_DOSE,
                ItemID.SNAKE_WEED);
    }

    private RecipePaths() {
    }

    /**
     * @return the representative item id of this item's path, or {@link #NO_PATH}
     */
    public static int pathOf(int itemId) {
        return PATH_BY_ITEM.getOrDefault(itemId, NO_PATH);
    }

    /**
     * @return every path, as representative item ids, in declaration order
     */
    public static Set<Integer> paths() {
        return Collections.unmodifiableSet(PATHS);
    }

    public static Map<Integer, Integer> PathsByItem() {
        return Collections.unmodifiableMap(PATH_BY_ITEM);
    }

    /**
     * The representative belongs to its own path, so it is not repeated in {@code members}.
     */
    private static void createPath(int representative, int... members) {
        PATHS.add(representative);
        assign(representative, representative);

        for (int member : members) {
            assign(member, representative);
        }
    }

    private static void assign(int itemId, int path) {
        final Integer existing = PATH_BY_ITEM.put(itemId, path);

        if (existing != null) {
            throw new IllegalStateException(
                    "item " + itemId + " is on two paths: " + existing + " and " + path);
        }
    }

}
