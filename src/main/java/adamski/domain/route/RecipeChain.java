package adamski.domain.route;

import adamski.domain.Recipe;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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
     * A chain starting at the first recipe's primary, so it needs at least one recipe.
     */
    public RecipeChain(List<Recipe> recipes) {
        this(firstPrimaryOf(recipes), recipes);
    }

    /**
     * @param rootItemId the banked item the chain starts from
     * @param recipes    the route it takes, in order. Empty when the root itself has been told to
     *                   stop, which makes the root its own product - the chain still exists so the
     *                   choice that stopped it can be changed.
     */
    public RecipeChain(int rootItemId, List<Recipe> recipes) {
        validateIsOrdered(rootItemId, recipes);

        this.rootItemId = rootItemId;
        this.recipes = List.copyOf(recipes);
        this.productItemId = this.recipes.isEmpty()
                ? rootItemId
                : this.recipes.get(this.recipes.size() - 1).getOutput().getItemId();    }

    private static int firstPrimaryOf(List<Recipe> recipes) {
        if (recipes.isEmpty()) throw new IllegalArgumentException("a chain without recipes needs its root named");

        return recipes.get(0).getPrimary().getItemId();
    }

    private static void validateIsOrdered(int rootItemId, List<Recipe> recipes) {
        int holding = rootItemId;

        for (Recipe recipe : recipes) {
            if (recipe.getPrimary().getItemId() != holding) {
                throw new IllegalArgumentException("r" + recipe.getId() + " takes item "
                        + recipe.getPrimary().getItemId() + ", but the chain is holding item " + holding);
            }

            holding = recipe.getOutput().getItemId();
        }
    }
}
