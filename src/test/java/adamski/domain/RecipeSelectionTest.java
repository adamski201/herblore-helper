package adamski.domain;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RecipeSelectionTest {
    @Test
    public void aDefaultEntryIsTheSameAsNoEntry() {
        assertEquals(RecipeSelection.ALL_DEFAULT, RecipeSelection.of(Map.of(1, RecipeSelection.DEFAULT)));
        assertEquals(RecipeSelection.of(Map.of(2, 10)),
                RecipeSelection.of(Map.of(1, RecipeSelection.DEFAULT, 2, 10)));
    }

    @Test
    public void withDropsAnItemSetBackToDefault() {
        final RecipeSelection chosen = RecipeSelection.of(Map.of(1, 10, 2, 11));

        assertEquals(RecipeSelection.of(Map.of(2, 11)), chosen.with(Map.of(1, RecipeSelection.DEFAULT)));
    }
}
