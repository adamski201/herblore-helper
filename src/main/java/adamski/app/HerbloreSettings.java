package adamski.app;

import lombok.Value;
import lombok.With;

@Value
@With
public class HerbloreSettings {
    public static final HerbloreSettings DEFAULT = new HerbloreSettings(true, true, 1);

    /**
     * Whether secondaries made by a recipe chain count towards the secondaries other chains need
     * e.g. super attacks and super combat potions.
     */
    boolean chainProductsCounted;

    /**
     * Whether items that are refined into secondaries - bird nests for crushed nests - are considered
     * alongside the secondary itself.
     */
    boolean precursorsCounted;

    /**
     * How often cutting a leaping sturgeon gives caviar, from 0 to 1.
     */
    double caviarSuccessRate;
}
