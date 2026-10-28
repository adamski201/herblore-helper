package adamski.app;

import lombok.Value;
import lombok.With;

import java.util.Map;

@Value
@With
public class HerbloreSettings {
    public static final HerbloreSettings DEFAULT = new HerbloreSettings(true, true, 1, Map.of());

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

    /**
     * What each seed or coral frag grows into, keyed by its item id. One left out grows the table's
     * default.
     */
    Map<Integer, Double> seedYieldBySeedItemId;
}
