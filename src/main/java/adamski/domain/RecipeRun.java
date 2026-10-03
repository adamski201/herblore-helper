package adamski.domain;

import lombok.Value;

/**
 * A recipe and how many times it can be run (according to owned items).
 */
@Value
public class RecipeRun {
    Recipe recipe;
    double runs;

    /**
     * What those runs make, in 1-dose units. Resolved here rather than recomputed by every reader.
     */
    double outputQuantity;

    public RecipeRun(Recipe recipe, double runs) {
        this.recipe = recipe;
        this.runs = runs;
        this.outputQuantity = runs * recipe.getOutput().getQuantity();
    }
}
