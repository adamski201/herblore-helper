package adamski.app;

import java.util.Map;

public interface HerbloreListener {
    void onStateChanged(Map<Integer, Integer> itemQuantities);
}
