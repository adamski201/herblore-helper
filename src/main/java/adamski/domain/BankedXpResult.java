package adamski.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@EqualsAndHashCode
@ToString
final class BankedXpResult {
    private final double total;

    /**
     * Keyed by recipe id rather than by the recipe itself, so a lookup cannot miss because the
     * recipe it is asking about came from a differently-modified copy of the table.
     */
    private final Map<Integer, Double> xpPerRecipeId;

    public BankedXpResult(double total, Map<Integer, Double> xpPerRecipeId) {
        this.total = total;
        this.xpPerRecipeId = Map.copyOf(xpPerRecipeId);
    }
}
