package adamski.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A chain of recipes to turn one banked item into one product.
 * <p>
 * Everything banked along the chain joins it rather than starting its own, which is why
 * holding ranarr seeds, grimy ranarr and ranarr unf gives one chain and not three.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class RecipeChain {
    private final int rootItemId;

    private final int productItemId;

    private final List<Recipe> recipes;

    /**
     * Everything this chain could end at instead. In the order the graph reached them, so
     * intermediates come before finished potions; how they are ordered for display is decided
     * elsewhere.
     */
    private final List<Integer> productOptions;

    /**
     * A chain with no options offered, for tests and for anywhere the route is all that matters.
     */
    public RecipeChain(List<Recipe> recipes) {
        this(recipes, Collections.emptyList());
    }

    public RecipeChain(List<Recipe> recipes, List<Integer> productOptions) {
        validateIsOrdered(recipes);

        this.recipes = List.copyOf(recipes);
        this.rootItemId = this.recipes.get(0).getPrimary().getItemId();
        this.productItemId = this.recipes.get(this.recipes.size() - 1).getOutput().getItemId();
        this.productOptions = List.copyOf(productOptions);
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
