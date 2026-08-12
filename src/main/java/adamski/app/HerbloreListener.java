package adamski.app;

import adamski.domain.models.BankedXpResult;
import adamski.domain.models.ItemSource;
import adamski.domain.models.SecondaryBalance;

import java.util.Map;

public interface HerbloreListener {
    /**
     * Called on the client thread, so any RuneLite lookup has to happen before hopping to the EDT.
     */
    void onStateChanged(Map<ItemSource, Map<Integer, Integer>> snapshot,
                        Map<ItemSource, Map<Integer, Integer>> delta,
                        BankedXpResult bankedXp,
                        SecondaryBalance secondaryBalance);
}
