package adamski.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * One recipe's contribution to a recipe chain.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class ChainRecipeXp {
    private final Recipe recipe;

    private final double xp;

    public ChainRecipeXp(Recipe recipe, double xp) {
        this.recipe = recipe;
        this.xp = xp;
    }
}
