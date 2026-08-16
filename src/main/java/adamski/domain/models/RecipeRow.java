package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * A plan to turn one banked item into one product, and the recipes that do it.
 * <p>
 * Everything banked along the route joins this row rather than starting its own, which is why
 * holding ranarr seeds, grimy ranarr and ranarr unf gives one row and not three.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class RecipeRow {
    /**
     * The least mature banked item feeding this row, and what it is named after.
     */
    private final int entryItemId;

    private final int productItemId;

    private final List<Recipe> route;

    public RecipeRow(int entryItemId, int productItemId, List<Recipe> route) {
        this.entryItemId = entryItemId;
        this.productItemId = productItemId;
        this.route = List.copyOf(route);
    }

    public boolean uses(Recipe recipe) {
        return route.contains(recipe);
    }
}
