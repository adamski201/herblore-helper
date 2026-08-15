package adamski.domain.models;

import lombok.Value;

/**
 * A recipe and how many times the player's items allow it to be run. Fractional - see
 * RecipeYieldCalculator for why.
 */
@Value
public class RecipeRun {
    Recipe recipe;
    double runs;
}
