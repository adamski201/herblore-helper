package adamski;

import adamski.data.Recipes;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(HerbloreConfig.GROUP)
public interface HerbloreConfig extends net.runelite.client.config.Config
{
    String GROUP = "herblore-helper";

    @ConfigSection(
        name = "Seed yields",
        description = "How many herbs each seed, and how much coral each frag, grows into. It depends on"
            + " your Farming level, compost, magic secateurs and the patch, so set your own average",
        position = 10,
        closedByDefault = true
    )
    String SEED_YIELDS = "seedYields";

    /**
     * Whether the panel hides disabled recipe rows (hidden as toggled via panel itself)
     */
    String OFF_ROWS_SHOWN = "offRowsShown";

    @ConfigItem(
        keyName = OFF_ROWS_SHOWN,
        name = "Show rows switched off",
        description = "Whether the panel lists rows set to make nothing",
        hidden = true
    )
    default boolean offRowsShown()
    {
        return true;
    }

    @ConfigItem(
        keyName = "countSecondariesMadeByRows",
        name = "Count secondaries made by rows",
        description = "Whether a row's output should contribute to the secondary balance e.g."
            + " super attacks from the irit row contributing to the secondaries needed for super combat potions",
        position = 1
    )
    default boolean countSecondariesMadeByRows()
    {
        return true;
    }

    @ConfigItem(
        keyName = "countSecondaryPrecursors",
        name = "Count secondary precursors",
        description = "Whether precursors to secondaries should be included in the secondary balance e.g. bird nests"
            + " towards crushed nests, unicorn horns towards unicorn horn dust",
        position = 2
    )
    default boolean countSecondaryPrecursors()
    {
        return true;
    }

    @Range(max = 100)
    @Units(Units.PERCENT)
    @ConfigItem(
        keyName = "caviarSuccessRate",
        name = "Caviar success rate",
        description = "How often cutting a leaping sturgeon gives caviar. Always 100% from 80 Cooking;"
            + " lower it if you will cut them below that",
        position = 3
    )
    default int caviarSuccessRate()
    {
        return 100;
    }

    @ConfigItem(
        keyName = "guamSeedYield",
        name = "Guam",
        description = "How many herbs each guam seed grows into",
        section = SEED_YIELDS,
        position = 0
    )
    default double guamSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "marrentillSeedYield",
        name = "Marrentill",
        description = "How many herbs each marrentill seed grows into",
        section = SEED_YIELDS,
        position = 1
    )
    default double marrentillSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "tarrominSeedYield",
        name = "Tarromin",
        description = "How many herbs each tarromin seed grows into",
        section = SEED_YIELDS,
        position = 2
    )
    default double tarrominSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "harralanderSeedYield",
        name = "Harralander",
        description = "How many herbs each harralander seed grows into",
        section = SEED_YIELDS,
        position = 3
    )
    default double harralanderSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "ranarrSeedYield",
        name = "Ranarr",
        description = "How many herbs each ranarr seed grows into",
        section = SEED_YIELDS,
        position = 4
    )
    default double ranarrSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "toadflaxSeedYield",
        name = "Toadflax",
        description = "How many herbs each toadflax seed grows into",
        section = SEED_YIELDS,
        position = 5
    )
    default double toadflaxSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "iritSeedYield",
        name = "Irit",
        description = "How many herbs each irit seed grows into",
        section = SEED_YIELDS,
        position = 6
    )
    default double iritSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "avantoeSeedYield",
        name = "Avantoe",
        description = "How many herbs each avantoe seed grows into",
        section = SEED_YIELDS,
        position = 7
    )
    default double avantoeSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "kwuarmSeedYield",
        name = "Kwuarm",
        description = "How many herbs each kwuarm seed grows into",
        section = SEED_YIELDS,
        position = 8
    )
    default double kwuarmSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "huascaSeedYield",
        name = "Huasca",
        description = "How many herbs each huasca seed grows into",
        section = SEED_YIELDS,
        position = 9
    )
    default double huascaSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "snapdragonSeedYield",
        name = "Snapdragon",
        description = "How many herbs each snapdragon seed grows into",
        section = SEED_YIELDS,
        position = 10
    )
    default double snapdragonSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "cadantineSeedYield",
        name = "Cadantine",
        description = "How many herbs each cadantine seed grows into",
        section = SEED_YIELDS,
        position = 11
    )
    default double cadantineSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "lantadymeSeedYield",
        name = "Lantadyme",
        description = "How many herbs each lantadyme seed grows into",
        section = SEED_YIELDS,
        position = 12
    )
    default double lantadymeSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "dwarfWeedSeedYield",
        name = "Dwarf weed",
        description = "How many herbs each dwarf weed seed grows into",
        section = SEED_YIELDS,
        position = 13
    )
    default double dwarfWeedSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "torstolSeedYield",
        name = "Torstol",
        description = "How many herbs each torstol seed grows into",
        section = SEED_YIELDS,
        position = 14
    )
    default double torstolSeedYield()
    {
        return Recipes.DEFAULT_HERB_YIELD;
    }

    @ConfigItem(
        keyName = "elkhornCoralYield",
        name = "Elkhorn coral",
        description = "How much elkhorn coral each frag grows into",
        section = SEED_YIELDS,
        position = 100
    )
    default double elkhornCoralYield()
    {
        return Recipes.DEFAULT_CORAL_YIELD;
    }

    @ConfigItem(
        keyName = "pillarCoralYield",
        name = "Pillar coral",
        description = "How much pillar coral each frag grows into",
        section = SEED_YIELDS,
        position = 101
    )
    default double pillarCoralYield()
    {
        return Recipes.DEFAULT_CORAL_YIELD;
    }

    @ConfigItem(
        keyName = "umbralCoralYield",
        name = "Umbral coral",
        description = "How much umbral coral each frag grows into",
        section = SEED_YIELDS,
        position = 102
    )
    default double umbralCoralYield()
    {
        return Recipes.DEFAULT_CORAL_YIELD;
    }
}
