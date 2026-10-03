package adamski.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * The final XP & output quantity produced by a chain of recipes.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class ChainResult {
    private final int rootItemId;
    private final int productItemId;
    private final List<Integer> productOptions;

    /**
     * Total XP sliced by the items in this chain.
     */
    private final List<ChainItemXp> itemContributions;

    /**
     * Total XP sliced by the recipes in this chain (excluding 0 xp recipes).
     */
    private final List<ChainRecipeXp> recipeContributions;

    private final double outputQuantity;

    private final ItemQuantities secondaryDemand;

    private final double xp;

    public ChainResult(int rootItemId,
                       int productItemId,
                       List<Integer> productOptions,
                       List<ChainItemXp> itemContributions,
                       List<ChainRecipeXp> recipeContributions,
                       double outputQuantity,
                       ItemQuantities secondaryDemand,
                       double xp) {
        if (itemContributions.isEmpty())
            throw new IllegalArgumentException("a chain result needs at least one item contributing to it");

        this.rootItemId = rootItemId;
        this.productItemId = productItemId;
        this.productOptions = List.copyOf(productOptions);
        this.itemContributions = List.copyOf(itemContributions);
        this.recipeContributions = List.copyOf(recipeContributions);
        this.outputQuantity = outputQuantity;
        this.secondaryDemand = secondaryDemand;
        this.xp = xp;
    }
}
