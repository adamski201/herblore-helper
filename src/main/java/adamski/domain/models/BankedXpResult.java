package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@EqualsAndHashCode
@ToString
public final class BankedXpResult {
    private final double total;
    private final Map<Integer, Double> xpPerRecipe;

    public BankedXpResult(double total, Map<Integer, Double> xpPerRecipe) {
        this.total = total;
        this.xpPerRecipe = Map.copyOf(xpPerRecipe);
    }
}
