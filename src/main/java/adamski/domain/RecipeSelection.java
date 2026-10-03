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
     * @param recipeByItemId item to the recipe it feeds, {@link #STOP}, or {@link #DEFAULT}. A
     *                       {@link #DEFAULT} entry is dropped, the same as leaving the item out, so
     *                       two selections that mean the same thing are equal.
     */
    public static RecipeSelection of(Map<Integer, Integer> recipeByItemId) {
        final Map<Integer, Integer> chosen = new HashMap<>(recipeByItemId);
        chosen.values().removeIf(recipeId -> recipeId == DEFAULT);

        return chosen.isEmpty() ? ALL_DEFAULT : new RecipeSelection(chosen);
    }

    /**
     * @param recipeByItemId entries to lay over the current selection: an item to the recipe it feeds,
     *                       {@link #STOP}, or {@link #DEFAULT} to drop the item back to its default
     * @return a new selection with those entries applied
     */
    public RecipeSelection with(Map<Integer, Integer> recipeByItemId) {
        final Map<Integer, Integer> merged = new HashMap<>(this.recipeByItemId);
        merged.putAll(recipeByItemId);

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
