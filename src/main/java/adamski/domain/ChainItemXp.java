package adamski.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * One item's contribution to a recipe chain.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class ChainItemXp {
    private final int itemId;

    /**
     * How much entered, in 1-dose units.
     */
    private final double quantity;

    private final List<RecipeRun> runs;

    private final double xp;

    public ChainItemXp(int itemId, double quantity, List<RecipeRun> runs, double xp) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.runs = List.copyOf(runs);
        this.xp = xp;
    }
}
