package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * One banked item's contribution to a path. Holding ranarr seeds, grimy ranarr and ranarr unf gives
 * the ranarr path three stages.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class RecipeStage {
    private final int rootItemId;

    /**
     * Only the runs belonging to this path - a chain crossing into another contributes a stage
     * there too.
     */
    private final List<RecipeRun> runs;

    public RecipeStage(int rootItemId, List<RecipeRun> runs) {
        this.rootItemId = rootItemId;
        this.runs = List.copyOf(runs);
    }
}
