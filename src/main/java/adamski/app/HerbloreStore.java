package adamski.app;

import adamski.domain.ItemQuantities;
import adamski.domain.ItemSource;
import adamski.domain.route.RecipeSelection;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Singleton
public class HerbloreStore {
    private final Map<ItemSource, ItemQuantities> itemsBySource = new EnumMap<>(ItemSource.class);
    private RecipeSelection selection = RecipeSelection.ALL_DEFAULT;
    private HerbloreSettings settings = HerbloreSettings.DEFAULT;

    /**
     * @return true if any source differs from what is already held
     */
    public boolean updateItems(Map<ItemSource, ItemQuantities> incoming) {
        boolean changed = false;

        for (Map.Entry<ItemSource, ItemQuantities> entry : incoming.entrySet()) {
            final ItemQuantities previous = itemsBySource.put(entry.getKey(), entry.getValue());
            if (!entry.getValue().equals(previous)) changed = true;
        }

        return changed;
    }

    /**
     * @return an immutable snapshot of what is held, per source
     */
    public Map<ItemSource, ItemQuantities> itemsBySource() {
        return Collections.unmodifiableMap(new EnumMap<>(itemsBySource));
    }

    /**
     * @return true if it differs from what is already held
     */
    public boolean updateSelection(RecipeSelection next) {
        if (next.equals(selection)) return false;

        selection = next;

        return true;
    }

    /**
     * @return which recipe each item feeds. Immutable, so it is a snapshot by construction.
     */
    public RecipeSelection selection() {
        return selection;
    }

    /**
     * @return true if it differs from what is already held
     */
    public boolean updateSettings(HerbloreSettings next) {
        if (next.equals(settings)) return false;

        settings = next;

        return true;
    }

    /**
     * @return the player's settings. Immutable, so it is a snapshot by construction.
     */
    public HerbloreSettings settings() {
        return settings;
    }
}
