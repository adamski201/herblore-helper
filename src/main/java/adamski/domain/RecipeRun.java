package adamski.domain;

import lombok.Value;

/**
 * A recipe and how many times the player's items allow it to be run. Fractional - see
 * RecipeYieldCalculator for why.
 */
@Value
public class RecipeRun {
    Recipe recipe;
    double runs;

    /**
     * How much of the recipe's output this makes, in 1-dose units. Resolved once, when the run is
     * made, rather than recomputed by every reader - so a per-recipe dose bonus such as the
     * alchemist's amulet only has to reach {@link adamski.domain.RecipeYieldCalculator}.
     */
    double outputQuantity;

    /**
     * The recipe's own yield, with nothing modifying it.
     */
    public RecipeRun(Recipe recipe, double runs) {
        this(recipe, runs, runs * recipe.getOutput().getQuantity());
    }

    public RecipeRun(Recipe recipe, double runs, double outputQuantity) {
        this.recipe = recipe;
        this.runs = runs;
        this.outputQuantity = outputQuantity;
    }
}
