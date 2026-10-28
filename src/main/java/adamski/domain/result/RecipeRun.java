package adamski.domain.result;

import adamski.domain.Recipe;
import lombok.Value;

/**
 * A recipe and how many times it can be run (according to owned items).
 */
@Value
public class RecipeRun {
    Recipe recipe;
    double runs;
    double outputQuantity;

    public RecipeRun(Recipe recipe, double runs) {
        this.recipe = recipe;
        this.runs = runs;
        this.outputQuantity = runs * recipe.getOutput().getQuantity();
    }
}
