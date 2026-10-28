package adamski;

import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(HerbloreConfig.GROUP)
public interface HerbloreConfig extends net.runelite.client.config.Config
{
    String GROUP = "herblore-helper";

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
}
