package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * A path, plus every banked item feeding it.
 * <p>
 * No terminal item: a path can hold several chains that never converge, since cadantine runs to a
 * super defence potion while a banked cadantine blood vial runs to bastion.
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
     * Least mature first.
     */
    private final List<RecipeStage> stages;

    public RecipeGroup(int pathItemId, List<RecipeStage> stages) {
        this.pathItemId = pathItemId;
        this.stages = List.copyOf(stages);
    }
}
