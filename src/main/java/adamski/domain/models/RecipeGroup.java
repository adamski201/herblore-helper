package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * One path ending at one product, with its xp broken down by the item it came from and by the recipe
 * that earned it.
 * <p>
 * A path ends at several products when something banked cannot reach the chosen one - cadantine runs
 * to super defence while a banked cadantine blood vial runs to bastion - and each end is its own group.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class RecipeGroup {
    /**
     * The least mature banked item feeding this row, and what it is named after.
     */
    private final int entryItemId;

    /**
     * The product this row makes.
     */
    private final int productItemId;

    /**
     * Least mature first.
     */
    private final List<RecipeStage> stages;

    /**
     * In dependency order. Recipes worth no xp are left out, so this is not the whole chain.
     */
    private final List<RecipeStep> steps;

    /**
     * How much of the product is made, in 1-dose units.
     */
    private final double outputQuantity;

    private final ItemQuantities secondaryDemand;

    private final double xp;

    public RecipeGroup(int entryItemId,
                       int productItemId,
                       List<RecipeStage> stages,
                       List<RecipeStep> steps,
                       double outputQuantity,
                       ItemQuantities secondaryDemand,
                       double xp) {
        if (stages.isEmpty()) throw new IllegalArgumentException("a group needs at least one stage");

        this.entryItemId = entryItemId;
        this.productItemId = productItemId;
        this.stages = List.copyOf(stages);
        this.steps = List.copyOf(steps);
        this.outputQuantity = outputQuantity;
        this.secondaryDemand = secondaryDemand;
        this.xp = xp;
    }
}
