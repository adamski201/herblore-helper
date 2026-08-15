package adamski.app;

import adamski.domain.models.ItemQuantities;
import adamski.domain.models.RecipeGroup;
import adamski.domain.models.SecondaryBalance;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Everything one bank read produced, published as a unit so the parts cannot disagree.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class HerbloreResult {
    /**
     * Counted sources merged.
     */
    private final ItemQuantities owned;

    private final List<RecipeGroup> paths;

    /**
     * The sum of the paths, so the headline figure always agrees with what is listed under it.
     */
    private final double totalXp;

    private final SecondaryBalance secondaryBalance;

    public HerbloreResult(ItemQuantities owned,
                          List<RecipeGroup> paths,
                          SecondaryBalance secondaryBalance) {
        this.owned = owned;
        this.paths = List.copyOf(paths);
        this.totalXp = this.paths.stream().mapToDouble(RecipeGroup::getXp).sum();
        this.secondaryBalance = secondaryBalance;
    }
}
