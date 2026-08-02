package adamski.app;

import adamski.domain.models.ItemSource;

import java.util.Map;

public interface HerbloreListener {
    void onStateChanged(Map<ItemSource, Map<Integer, Integer>> snapshot,
                        Map<ItemSource, Map<Integer, Integer>> delta);
}
