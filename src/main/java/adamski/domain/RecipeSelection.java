package adamski.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * For each item, the recipe it goes into. {@link #STOP} designates to go
   no further. An item with no entry takes the first recipe the table offers.
 * <p>
 * The product is found by following the steps until one says {@link
   #STOP} or the item feeds nothing.
 */
public final class RecipeSelection {
    public static final int DEFAULT = 0;
    public static final int STOP = -1;

    public static final RecipeSelection ALL_DEFAULT = new RecipeSelection(Collections.emptyMap());

    private final Map<Integer, Integer> recipeByItemId;

    private RecipeSelection(Map<Integer, Integer> recipeByItemId) {
        this.recipeByItemId = recipeByItemId;
    }

    /**
     * @param recipeByItemId item to the recipe it feeds, or {@link #STOP}.
     */
    public static RecipeSelection of(Map<Integer, Integer> recipeByItemId) {
        return recipeByItemId.isEmpty()
                ? ALL_DEFAULT
                : new RecipeSelection(new HashMap<>(recipeByItemId));
    }

    /**
     * @param recipeByItemId entries to lay over the current selection: an item to the recipe it feeds,
     *                       {@link #STOP}, or {@link #DEFAULT} to drop the item back to its default
     * @return a new selection with those entries applied
     */
    public RecipeSelection with(Map<Integer, Integer> recipeByItemId) {
        final Map<Integer, Integer> merged = new HashMap<>(this.recipeByItemId);

        recipeByItemId.forEach((itemId, recipeId) -> {
            if (recipeId == DEFAULT) {
                merged.remove(itemId);
            } else {
                merged.put(itemId, recipeId);
            }
        });

        return of(merged);
    }

    /**
     * @return the recipe id this item feeds, {@link #STOP}, or {@link #DEFAULT} to take the first
     * option the table offers
     */
    public int recipeIdFor(int itemId) {
        return recipeByItemId.getOrDefault(itemId, DEFAULT);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RecipeSelection)) return false;

        return recipeByItemId.equals(((RecipeSelection) other).recipeByItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(recipeByItemId);
    }

    @Override
    public String toString() {
        return "RecipeSelection" + recipeByItemId;
    }
}
