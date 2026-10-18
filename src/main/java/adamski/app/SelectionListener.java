package adamski.app;

import adamski.domain.RecipeSelection;

/**
 * Told when the user changes what a row makes. The selection is an input, not a result, so it has
 * a listener of its own rather than riding on {@link ResultListener}.
 */
public interface SelectionListener {
    /**
     * Called on the client thread after a pick changes the selection - before the first bank read
     * too, when no result is published. Not called when a saved selection is restored, so whoever
     * restored it is not told to save it again.
     */
    void onSelectionChanged(RecipeSelection selection);
}
