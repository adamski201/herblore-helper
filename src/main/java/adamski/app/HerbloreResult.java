package adamski.app;

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
     * Counted sources merged, in 1-dose units.
     */
    private final Map<Integer, Integer> owned;

    private final List<RecipeGroup> paths;

    /**
     * The sum of the paths, so the headline figure always agrees with what is listed under it.
     */
    private final double totalXp;

    private final SecondaryBalance secondaryBalance;

    public HerbloreResult(Map<Integer, Integer> owned,
                          List<RecipeGroup> paths,
                          SecondaryBalance secondaryBalance) {
        this.owned = Map.copyOf(owned);
        this.paths = List.copyOf(paths);
        this.totalXp = this.paths.stream().mapToDouble(RecipeGroup::getXp).sum();
        this.secondaryBalance = secondaryBalance;
    }
}
