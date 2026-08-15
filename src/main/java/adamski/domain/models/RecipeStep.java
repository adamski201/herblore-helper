package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * One recipe's contribution to a path, summed over every banked item that reaches it. Degriming
 * ranarr is one step whether the herbs came from seeds or from the bank.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class RecipeStep {
    private final Recipe recipe;

    private final double xp;

    public RecipeStep(Recipe recipe, double xp) {
        this.recipe = recipe;
        this.xp = xp;
    }
}
