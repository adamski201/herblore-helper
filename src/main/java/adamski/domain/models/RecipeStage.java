package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * What one item contributes after entering a path. Holding ranarr seeds, grimy ranarr and ranarr unf
 * gives the ranarr path three stages.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class RecipeStage {
    private final int entryItemId;

    /**
     * How much entered, in 1-dose units.
     */
    private final double quantity;

    private final List<RecipeRun> runs;

    private final double xp;

    public RecipeStage(int entryItemId, double quantity, List<RecipeRun> runs, double xp) {
        this.entryItemId = entryItemId;
        this.quantity = quantity;
        this.runs = List.copyOf(runs);
        this.xp = xp;
    }
}
