package adamski.app;

public interface ResultListener {
    /**
     * Called on the client thread, so any RuneLite lookup has to happen before hopping to the EDT.
     */
    void onResultChanged(HerbloreResult result);
}
