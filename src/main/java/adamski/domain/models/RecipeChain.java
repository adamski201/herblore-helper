package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * A chain of recipes to turn one banked item into one product.
 * <p>
 * Everything banked along the chain joins it rather than starting its own, which is why
 * holding ranarr seeds, grimy ranarr and ranarr unf gives one row and not three.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class RecipeChain {
    private final int rootItemId;

    private final int productItemId;

    private final List<Recipe> recipes;

    public RecipeChain(List<Recipe> recipes) {
        validateIsOrdered(recipes);

        this.recipes = List.copyOf(recipes);
        this.rootItemId = this.recipes.get(0).getPrimary().getItemId();
        this.productItemId = this.recipes.get(this.recipes.size() - 1).getOutput().getItemId();
    }

    private static void validateIsOrdered(List<Recipe> recipes) {
        if (recipes.isEmpty()) throw new IllegalArgumentException("a chain needs at least one recipe");

        for (int i = 1; i < recipes.size(); i++) {
            final Recipe recipe = recipes.get(i);
            final int made = recipes.get(i - 1).getOutput().getItemId();

            if (recipe.getPrimary().getItemId() != made) {
                throw new IllegalArgumentException("r" + recipe.getId() + " takes item "
                        + recipe.getPrimary().getItemId() + ", but the chain is holding item " + made);
            }
        }
    }
}
