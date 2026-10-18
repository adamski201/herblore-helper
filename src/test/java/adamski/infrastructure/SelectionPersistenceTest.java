package adamski.infrastructure;

import adamski.domain.route.RecipeSelection;
import com.google.gson.Gson;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * The encoding only - saving and loading go through ConfigManager, which needs a running client.
 */
public class SelectionPersistenceTest {
    private final SelectionPersistence persistence = new SelectionPersistence(null, null, new Gson(), null);

    @Test
    public void aSelectionSurvivesBeingSaved() {
        final RecipeSelection chosen = RecipeSelection.of(Map.of(249, 20, 199, RecipeSelection.STOP));

        assertEquals(chosen, persistence.decode(persistence.encode(chosen)));
    }

    @Test
    public void nothingSavedIsTheDefaults() {
        assertEquals(RecipeSelection.ALL_DEFAULT, persistence.decode(null));
        assertEquals(RecipeSelection.ALL_DEFAULT, persistence.decode(""));
    }

    @Test
    public void anUnreadableSaveIsTheDefaults() {
        assertEquals(RecipeSelection.ALL_DEFAULT, persistence.decode("{not json"));
        assertEquals(RecipeSelection.ALL_DEFAULT, persistence.decode("[1, 2, 3]"));
    }

    @Test
    public void anEntryWithNoRecipeIsDropped() {
        assertEquals(RecipeSelection.of(Map.of(199, 20)), persistence.decode("{\"249\": null, \"199\": 20}"));
    }
}
