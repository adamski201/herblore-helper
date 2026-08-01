package adamski.data;

import adamski.domain.models.Ingredient;
import adamski.domain.models.Recipe;
import net.runelite.api.gameval.ItemID;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Static Herblore recipe data, imported from osrs_herblore_recipes.xlsx.
 * <p>
 * Every ItemID here was verified against the running client's own item names
 * rather than inferred from the constant name - several constants are
 * misleading (CRUSHED_DRAGON_BONES is "Crushed superior dragon bones",
 * SANFEW_SALVE_* is "Sanfew serum", SUPERCOMPOST_POTION_* is "Compost potion",
 * MORT_SERUM* is "Serum 207").
 * <p>
 * <b>Decantable potions are expressed in 1-dose units.</b> A recipe that makes a
 * 3-dose attack potion is written as 3 x Attack potion(1), and one that consumes
 * a 4-dose super attack is written as 4 x Super attack(1). {@link PotionDoses}
 * converts what is actually in the bank into the same units. Items with no dose
 * variants (weapon poisons, haemostatic poultice, herbs, secondaries) are
 * quantity 1.
 * <p>
 * <b>xp is per craft, not per output unit.</b> Derive the number of crafts from
 * the inputs and multiply by xp - never multiply xp by the output quantity.
 * <p>
 * Levels are all 0 - the source sheet has no level column.
 */
public final class HerbloreRecipes {
    private static final Ingredient[] NONE = new Ingredient[0];

    /**
     * Placeholder herbs-per-seed for the "farming" rows. Real yield depends on
     * farming level, compost, diaries and secateurs, so it is expected to be
     * overridden per-user at calculation time rather than corrected here.
     */
    private static final int NOMINAL_HERB_YIELD = 8;

    private static final List<Recipe> RECIPES = Arrays.asList(
            // --- degrime -------------------------------------------------
            new Recipe(1, new Ingredient(ItemID.UNIDENTIFIED_GUAM, 1), NONE, new Ingredient(ItemID.GUAM_LEAF, 1), "degrime", 2.5f, 0),
            new Recipe(2, new Ingredient(ItemID.UNIDENTIFIED_MARENTILL, 1), NONE, new Ingredient(ItemID.MARENTILL, 1), "degrime", 3.8f, 0),
            new Recipe(3, new Ingredient(ItemID.UNIDENTIFIED_TARROMIN, 1), NONE, new Ingredient(ItemID.TARROMIN, 1), "degrime", 5f, 0),
            new Recipe(4, new Ingredient(ItemID.UNIDENTIFIED_HARRALANDER, 1), NONE, new Ingredient(ItemID.HARRALANDER, 1), "degrime", 6.3f, 0),
            new Recipe(5, new Ingredient(ItemID.UNIDENTIFIED_RANARR, 1), NONE, new Ingredient(ItemID.RANARR_WEED, 1), "degrime", 7.5f, 0),
            new Recipe(6, new Ingredient(ItemID.UNIDENTIFIED_TOADFLAX, 1), NONE, new Ingredient(ItemID.TOADFLAX, 1), "degrime", 8f, 0),
            new Recipe(7, new Ingredient(ItemID.UNIDENTIFIED_IRIT, 1), NONE, new Ingredient(ItemID.IRIT_LEAF, 1), "degrime", 8.8f, 0),
            new Recipe(8, new Ingredient(ItemID.UNIDENTIFIED_AVANTOE, 1), NONE, new Ingredient(ItemID.AVANTOE, 1), "degrime", 10f, 0),
            new Recipe(9, new Ingredient(ItemID.UNIDENTIFIED_KWUARM, 1), NONE, new Ingredient(ItemID.KWUARM, 1), "degrime", 11.3f, 0),
            new Recipe(10, new Ingredient(ItemID.UNIDENTIFIED_HUASCA, 1), NONE, new Ingredient(ItemID.HUASCA, 1), "degrime", 11.8f, 0),
            new Recipe(11, new Ingredient(ItemID.UNIDENTIFIED_SNAPDRAGON, 1), NONE, new Ingredient(ItemID.SNAPDRAGON, 1), "degrime", 11.8f, 0),
            new Recipe(12, new Ingredient(ItemID.UNIDENTIFIED_CADANTINE, 1), NONE, new Ingredient(ItemID.CADANTINE, 1), "degrime", 12.5f, 0),
            new Recipe(13, new Ingredient(ItemID.UNIDENTIFIED_LANTADYME, 1), NONE, new Ingredient(ItemID.LANTADYME, 1), "degrime", 13.1f, 0),
            new Recipe(14, new Ingredient(ItemID.UNIDENTIFIED_DWARF_WEED, 1), NONE, new Ingredient(ItemID.DWARF_WEED, 1), "degrime", 13.8f, 0),
            new Recipe(15, new Ingredient(ItemID.UNIDENTIFIED_TORSTOL, 1), NONE, new Ingredient(ItemID.TORSTOL, 1), "degrime", 15f, 0),

            // --- unfinished potions (no xp on creation) ------------------
            new Recipe(16, new Ingredient(ItemID.GUAM_LEAF, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.GUAMVIAL, 1), "unf", 0f, 0),
            new Recipe(17, new Ingredient(ItemID.MARENTILL, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.MARRENTILLVIAL, 1), "unf", 0f, 0),
            new Recipe(18, new Ingredient(ItemID.TARROMIN, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.TARROMINVIAL, 1), "unf", 0f, 0),
            new Recipe(19, new Ingredient(ItemID.HARRALANDER, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.HARRALANDERVIAL, 1), "unf", 0f, 0),
            new Recipe(20, new Ingredient(ItemID.RANARR_WEED, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.RANARRVIAL, 1), "unf", 0f, 0),
            new Recipe(21, new Ingredient(ItemID.TOADFLAX, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.TOADFLAXVIAL, 1), "unf", 0f, 0),
            new Recipe(22, new Ingredient(ItemID.IRIT_LEAF, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.IRITVIAL, 1), "unf", 0f, 0),
            new Recipe(23, new Ingredient(ItemID.AVANTOE, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.AVANTOEVIAL, 1), "unf", 0f, 0),
            new Recipe(24, new Ingredient(ItemID.KWUARM, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.KWUARMVIAL, 1), "unf", 0f, 0),
            new Recipe(25, new Ingredient(ItemID.HUASCA, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.HUASCAVIAL, 1), "unf", 0f, 0),
            new Recipe(26, new Ingredient(ItemID.SNAPDRAGON, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.SNAPDRAGONVIAL, 1), "unf", 0f, 0),
            new Recipe(27, new Ingredient(ItemID.CADANTINE, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.CADANTINEVIAL, 1), "unf", 0f, 0),
            new Recipe(28, new Ingredient(ItemID.LANTADYME, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.LANTADYMEVIAL, 1), "unf", 0f, 0),
            new Recipe(29, new Ingredient(ItemID.DWARF_WEED, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.DWARFWEEDVIAL, 1), "unf", 0f, 0),
            new Recipe(30, new Ingredient(ItemID.TORSTOL, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.TORSTOLVIAL, 1), "unf", 0f, 0),
            new Recipe(31, new Ingredient(ItemID.CADANTINE, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_BLOOD, 1)},
                    new Ingredient(ItemID.CADANTINE_BLOODVIAL, 1), "unf", 0f, 0),
            new Recipe(32, new Ingredient(ItemID.TOADFLAX, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_COCONUT_MILK, 1)},
                    new Ingredient(ItemID.UNFINISHED_ANTIDOTE_, 1), "unf", 0f, 0),
            new Recipe(33, new Ingredient(ItemID.IRIT_LEAF, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_COCONUT_MILK, 1)},
                    new Ingredient(ItemID.UNFINISHED_ANTIDOTE__, 1), "unf", 0f, 0),
            new Recipe(34, new Ingredient(ItemID.VIAL_COCONUT_MILK, 1), new Ingredient[]{new Ingredient(ItemID.CACTUS_SPINE, 1)},
                    new Ingredient(ItemID.UNFINISHED_WEAPON_POISON_, 1), "unf", 0f, 0),
            new Recipe(35, new Ingredient(ItemID.VIAL_COCONUT_MILK, 1), new Ingredient[]{new Ingredient(ItemID.NIGHTSHADE, 1)},
                    new Ingredient(ItemID.UNFINISHED_WEAPON_POISON__, 1), "unf", 0f, 0),
            new Recipe(36, new Ingredient(ItemID.CORAL_ELKHORN, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.ELKHORNVIAL, 1), "unf", 0f, 0),
            new Recipe(37, new Ingredient(ItemID.CORAL_PILLAR, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.PILLARVIAL, 1), "unf", 0f, 0),
            new Recipe(38, new Ingredient(ItemID.CORAL_UMBRAL, 1), new Ingredient[]{new Ingredient(ItemID.VIAL_WATER, 1)},
                    new Ingredient(ItemID.UMBRALVIAL, 1), "unf", 0f, 0),

            // --- finished potions ----------------------------------------
            new Recipe(39, new Ingredient(ItemID.GUAMVIAL, 1), new Ingredient[]{new Ingredient(ItemID.EYE_OF_NEWT, 1)},
                    new Ingredient(ItemID._1DOSE1ATTACK, 3), "potion", 25f, 0),
            new Recipe(40, new Ingredient(ItemID.MARRENTILLVIAL, 1), new Ingredient[]{new Ingredient(ItemID.UNICORN_HORN_DUST, 1)},
                    new Ingredient(ItemID._1DOSEANTIPOISON, 3), "potion", 37.5f, 0),
            new Recipe(41, new Ingredient(ItemID.TARROMINVIAL, 1), new Ingredient[]{new Ingredient(ItemID.LIMPWURT_ROOT, 1)},
                    new Ingredient(ItemID._1DOSE1STRENGTH, 3), "potion", 50f, 0),
            new Recipe(42, new Ingredient(ItemID.HARRALANDERVIAL, 1), new Ingredient[]{new Ingredient(ItemID.RED_SPIDERS_EGGS, 1)},
                    new Ingredient(ItemID._1DOSESTATRESTORE, 3), "potion", 62.5f, 0),
            new Recipe(43, new Ingredient(ItemID.HARRALANDERVIAL, 1), new Ingredient[]{new Ingredient(ItemID.CHOCOLATE_DUST, 1)},
                    new Ingredient(ItemID._1DOSE1ENERGY, 3), "potion", 67.5f, 0),
            new Recipe(44, new Ingredient(ItemID.RANARRVIAL, 1), new Ingredient[]{new Ingredient(ItemID.WHITE_BERRIES, 1)},
                    new Ingredient(ItemID._1DOSE1DEFENSE, 3), "potion", 75f, 0),
            new Recipe(45, new Ingredient(ItemID.TOADFLAXVIAL, 1), new Ingredient[]{new Ingredient(ItemID.TOADS_LEGS, 1)},
                    new Ingredient(ItemID._1DOSE1AGILITY, 3), "potion", 80f, 0),
            new Recipe(46, new Ingredient(ItemID.HARRALANDERVIAL, 1), new Ingredient[]{new Ingredient(ItemID.GROUND_DESERT_GOAT_HORN, 1)},
                    new Ingredient(ItemID._1DOSECOMBAT, 3), "potion", 84f, 0),
            new Recipe(47, new Ingredient(ItemID.RANARRVIAL, 1), new Ingredient[]{new Ingredient(ItemID.SNAPE_GRASS, 1)},
                    new Ingredient(ItemID._1DOSEPRAYERRESTORE, 3), "potion", 87.5f, 0),
            new Recipe(48, new Ingredient(ItemID.IRITVIAL, 1), new Ingredient[]{new Ingredient(ItemID.EYE_OF_NEWT, 1)},
                    new Ingredient(ItemID._1DOSE2ATTACK, 3), "potion", 100f, 0),
            new Recipe(49, new Ingredient(ItemID.IRITVIAL, 1), new Ingredient[]{new Ingredient(ItemID.UNICORN_HORN_DUST, 1)},
                    new Ingredient(ItemID._1DOSE2ANTIPOISON, 3), "potion", 106.3f, 0),
            new Recipe(50, new Ingredient(ItemID.AVANTOEVIAL, 1), new Ingredient[]{new Ingredient(ItemID.SNAPE_GRASS, 1)},
                    new Ingredient(ItemID._1DOSEFISHERSPOTION, 3), "potion", 112.5f, 0),
            new Recipe(51, new Ingredient(ItemID.AVANTOEVIAL, 1), new Ingredient[]{new Ingredient(ItemID.MORTMYREMUSHROOM, 1)},
                    new Ingredient(ItemID._1DOSE2ENERGY, 3), "potion", 117.5f, 0),
            new Recipe(52, new Ingredient(ItemID.AVANTOEVIAL, 1), new Ingredient[]{new Ingredient(ItemID.HUNTINGBEAST_SABRETEETH_DUST, 1)},
                    new Ingredient(ItemID._1DOSEHUNTING, 3), "potion", 120f, 0),
            new Recipe(53, new Ingredient(ItemID.KWUARMVIAL, 1), new Ingredient[]{new Ingredient(ItemID.LIMPWURT_ROOT, 1)},
                    new Ingredient(ItemID._1DOSE2STRENGTH, 3), "potion", 125f, 0),
            // Weapon poisons have no dose variants, so the sheet's ":3" does not apply.
            new Recipe(54, new Ingredient(ItemID.KWUARMVIAL, 1), new Ingredient[]{new Ingredient(ItemID.DRAGON_SCALE_DUST, 1)},
                    new Ingredient(ItemID.WEAPON_POISON, 1), "potion", 137.5f, 0),
            new Recipe(55, new Ingredient(ItemID.SNAPDRAGONVIAL, 1), new Ingredient[]{new Ingredient(ItemID.RED_SPIDERS_EGGS, 1)},
                    new Ingredient(ItemID._1DOSE2RESTORE, 3), "potion", 142.5f, 0),
            new Recipe(56, new Ingredient(ItemID.CADANTINEVIAL, 1), new Ingredient[]{new Ingredient(ItemID.WHITE_BERRIES, 1)},
                    new Ingredient(ItemID._1DOSE2DEFENSE, 3), "potion", 150f, 0),
            new Recipe(57, new Ingredient(ItemID.LANTADYMEVIAL, 1), new Ingredient[]{new Ingredient(ItemID.DRAGON_SCALE_DUST, 1)},
                    new Ingredient(ItemID._1DOSE1ANTIDRAGON, 3), "potion", 157.5f, 0),
            new Recipe(58, new Ingredient(ItemID.DWARFWEEDVIAL, 1), new Ingredient[]{new Ingredient(ItemID.WINE_OF_ZAMORAK, 1)},
                    new Ingredient(ItemID._1DOSERANGERSPOTION, 3), "potion", 162.5f, 0),
            new Recipe(59, new Ingredient(ItemID.LANTADYMEVIAL, 1), new Ingredient[]{new Ingredient(ItemID.CACTUS_POTATO, 1)},
                    new Ingredient(ItemID._1DOSE1MAGIC, 3), "potion", 172.5f, 0),
            new Recipe(60, new Ingredient(ItemID.TORSTOLVIAL, 1), new Ingredient[]{new Ingredient(ItemID.JANGERBERRIES, 1)},
                    new Ingredient(ItemID._1DOSEPOTIONOFZAMORAK, 3), "potion", 175f, 0),
            new Recipe(61, new Ingredient(ItemID.TOADFLAXVIAL, 1), new Ingredient[]{new Ingredient(ItemID.CRUSHED_BIRD_NEST, 1)},
                    new Ingredient(ItemID._1DOSEPOTIONOFSARADOMIN, 3), "potion", 180f, 0),
            new Recipe(62, new Ingredient(ItemID.DWARFWEEDVIAL, 1), new Ingredient[]{new Ingredient(ItemID.NIHIL_DUST, 1)},
                    new Ingredient(ItemID._1DOSEANCIENTBREW, 3), "potion", 190f, 0),
            new Recipe(63, new Ingredient(ItemID.DWARFWEEDVIAL, 1), new Ingredient[]{new Ingredient(ItemID.LILY_OF_THE_SANDS, 1)},
                    new Ingredient(ItemID._1DOSESTATRENEWAL, 3), "potion", 200f, 0),
            new Recipe(64, new Ingredient(ItemID.CADANTINE_BLOODVIAL, 1), new Ingredient[]{new Ingredient(ItemID.WINE_OF_ZAMORAK, 1)},
                    new Ingredient(ItemID._1DOSEBASTION, 3), "potion", 155f, 0),
            new Recipe(65, new Ingredient(ItemID.CADANTINE_BLOODVIAL, 1), new Ingredient[]{new Ingredient(ItemID.CACTUS_POTATO, 1)},
                    new Ingredient(ItemID._1DOSEBATTLEMAGE, 3), "potion", 155f, 0),
            new Recipe(66, new Ingredient(ItemID.UNFINISHED_ANTIDOTE_, 1), new Ingredient[]{new Ingredient(ItemID.YEW_ROOTS, 1)},
                    new Ingredient(ItemID.ANTIDOTE_1, 3), "potion", 155f, 0),
            new Recipe(67, new Ingredient(ItemID.UNFINISHED_ANTIDOTE__, 1), new Ingredient[]{new Ingredient(ItemID.MAGIC_ROOTS, 1)},
                    new Ingredient(ItemID.ANTIDOTE__1, 3), "potion", 177.5f, 0),
            new Recipe(68, new Ingredient(ItemID.UNFINISHED_WEAPON_POISON_, 1), new Ingredient[]{new Ingredient(ItemID.RED_SPIDERS_EGGS, 1)},
                    new Ingredient(ItemID.WEAPON_POISON_, 1), "potion", 190f, 0),
            new Recipe(69, new Ingredient(ItemID.UNFINISHED_WEAPON_POISON__, 1), new Ingredient[]{new Ingredient(ItemID.POISONIVY_BERRIES, 1)},
                    new Ingredient(ItemID.WEAPON_POISON__, 1), "potion", 190f, 0),
            new Recipe(70, new Ingredient(ItemID.HARRALANDERVIAL, 1), new Ingredient[]{new Ingredient(ItemID.ALDARIUM, 1)},
                    new Ingredient(ItemID._1DOSEGOADING, 3), "potion", 132f, 0),
            new Recipe(71, new Ingredient(ItemID.HUASCAVIAL, 1), new Ingredient[]{new Ingredient(ItemID.ALDARIUM, 1)},
                    new Ingredient(ItemID._1DOSE1PRAYER_REGENERATION, 3), "potion", 132f, 0),
            new Recipe(72, new Ingredient(ItemID.TARROMINVIAL, 1), new Ingredient[]{new Ingredient(ItemID.ASHES, 1)},
                    new Ingredient(ItemID.MORT_SERUM1, 3), "potion", 50f, 0),
            new Recipe(73, new Ingredient(ItemID.HARRALANDERVIAL, 1), new Ingredient[]{new Ingredient(ItemID.FOSSIL_VOLCANIC_ASH, 1)},
                    new Ingredient(ItemID.SUPERCOMPOST_POTION_1, 3), "potion", 60f, 0),
            new Recipe(74, new Ingredient(ItemID._1DOSESTATRESTORE, 3),
                    new Ingredient[]{new Ingredient(ItemID.GARLIC, 1), new Ingredient(ItemID.SILVER_DUST, 1)},
                    new Ingredient(ItemID.BURGH_GUTHIX_BALANCE_1, 3), "potion", 50f, 0),
            new Recipe(75, new Ingredient(ItemID.PILLARVIAL, 1), new Ingredient[]{new Ingredient(ItemID.HADDOCK_EYE, 1)},
                    new Ingredient(ItemID._1DOSE2FISHERSPOTION, 3), "potion", 140.5f, 0),
            new Recipe(76, new Ingredient(ItemID.PILLARVIAL, 1), new Ingredient[]{new Ingredient(ItemID.CRAB_PASTE, 1)},
                    new Ingredient(ItemID._1DOSE2HUNTING, 3), "potion", 154f, 0),
            new Recipe(77, new Ingredient(ItemID.UMBRALVIAL, 1), new Ingredient[]{new Ingredient(ItemID.RAINBOW_CRAB_PASTE, 1)},
                    new Ingredient(ItemID._1DOSEARMADYLBREW, 3), "potion", 205f, 0),
            new Recipe(78, new Ingredient(ItemID.ELKHORNVIAL, 1), new Ingredient[]{new Ingredient(ItemID.SQUID_PASTE, 1)},
                    new Ingredient(ItemID.HAEMOSTATIC_POULTICE, 1), "misc", 27f, 0),
            new Recipe(79, new Ingredient(ItemID.HAEMOSTATIC_POULTICE, 1), new Ingredient[]{new Ingredient(ItemID.COTTON_YARN, 1)},
                    new Ingredient(ItemID._1DOSEHAEMOSTATICDRESSING, 1), "misc", 100f, 0),
            new Recipe(80, new Ingredient(ItemID.TORSTOLVIAL, 1), new Ingredient[]{new Ingredient(ItemID.DEMONIC_TALLOW, 1)},
                    new Ingredient(ItemID._1DOSESURGE, 3), "potion", 185f, 0),
            new Recipe(81, new Ingredient(ItemID.TORSTOL, 1),
                    new Ingredient[]{new Ingredient(ItemID._1DOSE2ATTACK, 4), new Ingredient(ItemID._1DOSE2STRENGTH, 4), new Ingredient(ItemID._1DOSE2DEFENSE, 4)},
                    new Ingredient(ItemID._1DOSE2COMBAT, 4), "potion", 150f, 0),
            new Recipe(82, new Ingredient(ItemID.TORSTOLVIAL, 1),
                    new Ingredient[]{new Ingredient(ItemID._1DOSE2ATTACK, 4), new Ingredient(ItemID._1DOSE2STRENGTH, 4), new Ingredient(ItemID._1DOSE2DEFENSE, 4)},
                    new Ingredient(ItemID._1DOSE2COMBAT, 4), "potion", 150f, 0),
            new Recipe(83, new Ingredient(ItemID._1DOSE1ANTIDRAGON, 4), new Ingredient[]{new Ingredient(ItemID.CRUSHED_DRAGON_BONES, 1)},
                    new Ingredient(ItemID._1DOSE3ANTIDRAGON, 4), "potion", 130f, 0),
            new Recipe(84, new Ingredient(ItemID.SNAKE_WEED, 1),
                    new Ingredient[]{new Ingredient(ItemID._1DOSE2RESTORE, 4), new Ingredient(ItemID.UNICORN_HORN_DUST, 1), new Ingredient(ItemID.NAIL_BEAST_NAIL, 1)},
                    new Ingredient(ItemID.SANFEW_SALVE_1_DOSE, 4), "potion", 192f, 0),
            new Recipe(85, new Ingredient(ItemID.TORSTOL, 1), new Ingredient[]{new Ingredient(ItemID.ANTIVENOM1, 4)},
                    new Ingredient(ItemID.ANTIVENOM_1, 4), "potion", 125f, 0),
            new Recipe(86, new Ingredient(ItemID.TORSTOLVIAL, 1), new Ingredient[]{new Ingredient(ItemID.ANTIVENOM1, 4)},
                    new Ingredient(ItemID.ANTIVENOM_1, 4), "potion", 125f, 0),
            new Recipe(87, new Ingredient(ItemID._1DOSE2ENERGY, 1), new Ingredient[]{new Ingredient(ItemID.AMYLASE, 1)},
                    new Ingredient(ItemID._1DOSESTAMINA, 1), "potion", 25.5f, 0),
            new Recipe(88, new Ingredient(ItemID._1DOSE2ENERGY, 1), new Ingredient[]{new Ingredient(ItemID.YELLOW_FIN, 1)},
                    new Ingredient(ItemID._1DOSE3ENERGY, 1), "potion", 21f, 0),
            new Recipe(89, new Ingredient(ItemID._1DOSESTAMINA, 1), new Ingredient[]{new Ingredient(ItemID.MARLIN_SCALES, 1)},
                    new Ingredient(ItemID._1DOSE2STAMINA, 1), "potion", 27.5f, 0),
            new Recipe(90, new Ingredient(ItemID.ANTIDOTE__1, 1), new Ingredient[]{new Ingredient(ItemID.SNAKEBOSS_SCALE, 5)},
                    new Ingredient(ItemID.ANTIVENOM1, 1), "potion", 30f, 0),
            new Recipe(91, new Ingredient(ItemID.ANTIVENOM_1, 1), new Ingredient[]{new Ingredient(ItemID.ARAXYTE_VENOM_SACK, 1)},
                    new Ingredient(ItemID.EXTENDED_ANTIVENOM_1, 1), "potion", 20f, 0),
            new Recipe(92, new Ingredient(ItemID._1DOSE1ANTIDRAGON, 1), new Ingredient[]{new Ingredient(ItemID.LAVA_SHARD, 1)},
                    new Ingredient(ItemID._1DOSE2ANTIDRAGON, 1), "potion", 27.5f, 0),
            new Recipe(93, new Ingredient(ItemID._1DOSE3ANTIDRAGON, 1), new Ingredient[]{new Ingredient(ItemID.LAVA_SHARD, 1)},
                    new Ingredient(ItemID._1DOSE4ANTIDRAGON, 1), "potion", 40f, 0),
            new Recipe(94, new Ingredient(ItemID._1DOSEANCIENTBREW, 1), new Ingredient[]{new Ingredient(ItemID.ANCIENT_ESSENCE, 20)},
                    new Ingredient(ItemID._1DOSEFORGOTTENBREW, 1), "potion", 36f, 0),

            // --- caviar mixes --------------------------------------------
            new Recipe(95, new Ingredient(ItemID._1DOSE1ENERGY, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE1ENERGY, 2), "caviar", 23f, 0),
            new Recipe(96, new Ingredient(ItemID._1DOSE1DEFENSE, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE1DEFENSE, 2), "caviar", 25f, 0),
            new Recipe(97, new Ingredient(ItemID._1DOSE1AGILITY, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE1AGILITY, 2), "caviar", 27f, 0),
            new Recipe(98, new Ingredient(ItemID._1DOSECOMBAT, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSECOMBAT, 2), "caviar", 28f, 0),
            new Recipe(99, new Ingredient(ItemID._1DOSEPRAYERRESTORE, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSEPRAYERRESTORE, 2), "caviar", 29f, 0),
            new Recipe(100, new Ingredient(ItemID._1DOSE2ATTACK, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE2ATTACK, 2), "caviar", 33f, 0),
            new Recipe(101, new Ingredient(ItemID._1DOSE2ANTIPOISON, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE2ANTIPOISON, 2), "caviar", 35f, 0),
            new Recipe(102, new Ingredient(ItemID._1DOSEFISHERSPOTION, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSEFISHERSPOTION, 2), "caviar", 38f, 0),
            new Recipe(103, new Ingredient(ItemID._1DOSE2ENERGY, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE2ENERGY, 2), "caviar", 39f, 0),
            new Recipe(104, new Ingredient(ItemID._1DOSEHUNTING, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE1HUNTING, 2), "caviar", 40f, 0),
            new Recipe(105, new Ingredient(ItemID._1DOSE2STRENGTH, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE2STRENGTH, 2), "caviar", 42f, 0),
            new Recipe(106, new Ingredient(ItemID._1DOSE2RESTORE, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE2RESTORE, 2), "caviar", 48f, 0),
            new Recipe(107, new Ingredient(ItemID._1DOSE2DEFENSE, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE2DEFENSE, 2), "caviar", 50f, 0),
            new Recipe(108, new Ingredient(ItemID.ANTIDOTE_1, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_ANTIDOTE_1, 2), "caviar", 52f, 0),
            new Recipe(109, new Ingredient(ItemID._1DOSE1ANTIDRAGON, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE1ANTIDRAGON, 2), "caviar", 53f, 0),
            new Recipe(110, new Ingredient(ItemID._1DOSERANGERSPOTION, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSERANGERSPOTION, 2), "caviar", 54f, 0),
            new Recipe(111, new Ingredient(ItemID._1DOSE1MAGIC, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE1MAGIC, 2), "caviar", 57f, 0),
            new Recipe(112, new Ingredient(ItemID._1DOSEPOTIONOFZAMORAK, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSEPOTIONOFZAMORAK, 2), "caviar", 58f, 0),
            new Recipe(113, new Ingredient(ItemID._1DOSESTAMINA, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSESTAMINA, 2), "caviar", 60f, 0),
            new Recipe(114, new Ingredient(ItemID._1DOSE2ANTIDRAGON, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE2ANTIDRAGON, 2), "caviar", 61f, 0),
            new Recipe(115, new Ingredient(ItemID._1DOSEANCIENTBREW, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSEANCIENTBREW, 2), "caviar", 63f, 0),
            new Recipe(116, new Ingredient(ItemID._1DOSE3ANTIDRAGON, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE3ANTIDRAGON, 2), "caviar", 70f, 0),
            new Recipe(117, new Ingredient(ItemID._1DOSE4ANTIDRAGON, 2), new Ingredient[]{new Ingredient(ItemID.BRUT_CAVIAR, 1)},
                    new Ingredient(ItemID.BRUTAL_1DOSE4ANTIDRAGON, 2), "caviar", 78f, 0),

            // --- seeds -> grimy herbs ------------------------------------
            // Farming, not Herblore: 0 Herblore xp. These exist only so seeds can
            // cascade into the herb chain. Output quantity is a nominal yield and
            // is expected to be overridden per-user at calculation time.
            new Recipe(118, new Ingredient(ItemID.GUAM_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_GUAM, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(119, new Ingredient(ItemID.MARRENTILL_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_MARENTILL, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(120, new Ingredient(ItemID.TARROMIN_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_TARROMIN, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(121, new Ingredient(ItemID.HARRALANDER_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_HARRALANDER, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(122, new Ingredient(ItemID.RANARR_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_RANARR, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(123, new Ingredient(ItemID.TOADFLAX_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_TOADFLAX, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(124, new Ingredient(ItemID.IRIT_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_IRIT, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(125, new Ingredient(ItemID.AVANTOE_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_AVANTOE, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(126, new Ingredient(ItemID.KWUARM_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_KWUARM, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(127, new Ingredient(ItemID.HUASCA_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_HUASCA, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(128, new Ingredient(ItemID.SNAPDRAGON_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_SNAPDRAGON, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(129, new Ingredient(ItemID.CADANTINE_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_CADANTINE, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(130, new Ingredient(ItemID.LANTADYME_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_LANTADYME, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(131, new Ingredient(ItemID.DWARF_WEED_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_DWARF_WEED, NOMINAL_HERB_YIELD), "farming", 0f, 0),
            new Recipe(132, new Ingredient(ItemID.TORSTOL_SEED, 1), NONE, new Ingredient(ItemID.UNIDENTIFIED_TORSTOL, NOMINAL_HERB_YIELD), "farming", 0f, 0)
    );

    private HerbloreRecipes() {
    }

    public static List<Recipe> all() {
        return Collections.unmodifiableList(RECIPES);
    }
}
