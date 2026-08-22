package adamski.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * What one chain yields against what is owned: its xp, broken down both by the item it came from
 * and by the recipe that earned it, plus what it makes and what it consumes.
 * <p>
 * A chain ends at several products when something banked cannot reach the chosen one - cadantine runs
 * to super defence while a banked cadantine blood vial runs to bastion - and each end is its own result.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class ChainResult {
    /**
     * The least mature owned item feeding this chain, and what the result is named after.
     */
    private final int entryItemId;

    /**
     * The product this chain makes.
     */
    private final int productItemId;

    /**
     * The same xp split by the owned item it came from, least mature first.
     */
    private final List<ChainItemXp> itemContributions;

    /**
     * The same xp split by the recipe that earned it, in dependency order. Recipes worth no xp are
     * left out, so this is not the whole chain.
     */
    private final List<ChainRecipeXp> recipeContributions;

    /**
     * How much of the product is made, in 1-dose units.
     */
    private final double outputQuantity;

    private final ItemQuantities secondaryDemand;

    private final double xp;

    public ChainResult(int entryItemId,
                       int productItemId,
                       List<ChainItemXp> itemContributions,
                       List<ChainRecipeXp> recipeContributions,
                       double outputQuantity,
                       ItemQuantities secondaryDemand,
                       double xp) {
        if (itemContributions.isEmpty())
            throw new IllegalArgumentException("a chain result needs at least one item contributing to it");

        this.entryItemId = entryItemId;
        this.productItemId = productItemId;
        this.itemContributions = List.copyOf(itemContributions);
        this.recipeContributions = List.copyOf(recipeContributions);
        this.outputQuantity = outputQuantity;
        this.secondaryDemand = secondaryDemand;
        this.xp = xp;
    }
}
