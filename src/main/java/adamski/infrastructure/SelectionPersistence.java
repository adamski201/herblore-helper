package adamski.infrastructure;

import adamski.app.HerbloreApp;
import adamski.app.SelectionListener;
import adamski.domain.route.RecipeSelection;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps each account's product choices across restarts, in that account's RuneLite profile - banks
 * differ between accounts, and so do the choices made against them.
 * <p>
 * Saves on every pick; loads on login, on switching account, and when the plugin starts. Loading
 * always replaces the whole selection, so an account with nothing saved starts from the defaults
 * rather than inheriting the last account's choices.
 * <p>
 * Saved as JSON of item id to recipe id ({@code -1} to stop). Recipe ids are what is stored, so
 * they must never be renumbered - a renumbered id would silently point a saved choice at a
 * different recipe.
 */
@Slf4j
@Singleton
public class SelectionPersistence implements SelectionListener {
    static final String CONFIG_GROUP = "herblore-helper";
    static final String SELECTION_KEY = "selection";

    private static final Type RECIPE_BY_ITEM_ID = new TypeToken<Map<Integer, Integer>>() {
    }.getType();

    private final ConfigManager configManager;
    private final ClientThread clientThread;
    private final Gson gson;
    private final HerbloreApp app;

    @Inject
    public SelectionPersistence(ConfigManager configManager, ClientThread clientThread, Gson gson, HerbloreApp app) {
        this.configManager = configManager;
        this.clientThread = clientThread;
        this.gson = gson;
        this.app = app;
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event) {
        // the app is only driven from the client thread; invoke runs at once if already there
        clientThread.invoke(this::load);
    }

    /**
     * Restores the current account's saved choices, or the defaults if it has none. Client thread
     * only.
     */
    public void load() {
        app.restoreSelection(decode(configManager.getRSProfileConfiguration(CONFIG_GROUP, SELECTION_KEY)));
    }

    @Override
    public void onSelectionChanged(RecipeSelection selection) {
        // all defaults is the same as nothing saved, so it leaves nothing behind
        if (selection.recipeByItemId().isEmpty()) {
            configManager.unsetRSProfileConfiguration(CONFIG_GROUP, SELECTION_KEY);
        } else {
            configManager.setRSProfileConfiguration(CONFIG_GROUP, SELECTION_KEY, encode(selection));
        }
    }

    String encode(RecipeSelection selection) {
        return gson.toJson(selection.recipeByItemId(), RECIPE_BY_ITEM_ID);
    }

    /**
     * @return the selection saved, or the defaults if nothing was or it cannot be read - a damaged
     * entry must never stop the panel working
     */
    RecipeSelection decode(String saved) {
        if (saved == null || saved.isEmpty()) return RecipeSelection.ALL_DEFAULT;

        try {
            final Map<Integer, Integer> recipeByItemId = gson.fromJson(saved, RECIPE_BY_ITEM_ID);
            if (recipeByItemId == null) return RecipeSelection.ALL_DEFAULT;

            final Map<Integer, Integer> readable = new HashMap<>(recipeByItemId);
            readable.values().removeIf(recipeId -> recipeId == null);

            return RecipeSelection.of(readable);
        } catch (JsonParseException e) {
            log.warn("ignoring an unreadable saved selection: {}", saved, e);
            return RecipeSelection.ALL_DEFAULT;
        }
    }
}
