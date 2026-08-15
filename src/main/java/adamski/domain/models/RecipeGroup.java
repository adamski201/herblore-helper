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
     * The item representing the path.
     */
    private final int pathItemId;

    /**
     * The product this ends at.
     */
    private final int terminalItemId;

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

    public RecipeGroup(int pathItemId,
                       int terminalItemId,
                       List<RecipeStage> stages,
                       List<RecipeStep> steps,
                       double outputQuantity,
                       ItemQuantities secondaryDemand,
                       double xp) {
        if (stages.isEmpty()) throw new IllegalArgumentException("a group needs at least one stage");

        this.pathItemId = pathItemId;
        this.terminalItemId = terminalItemId;
        this.stages = List.copyOf(stages);
        this.steps = List.copyOf(steps);
        this.outputQuantity = outputQuantity;
        this.secondaryDemand = secondaryDemand;
        this.xp = xp;
    }

    /**
     * The least mature item entering this path, which is what the row is named after.
     */
    public int getEntryItemId() {
        return stages.get(0).getEntryItemId();
    }
}
