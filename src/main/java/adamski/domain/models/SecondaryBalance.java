package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * What the planned recipe runs need in secondaries, and how that compares to what is held. Only
 * items some recipe consumes as a secondary appear.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class SecondaryBalance {
    private final Map<Integer, Double> demanded;

    /**
     * Held minus demanded. Negative is a shortfall, positive is spare.
     */
    private final Map<Integer, Double> net;

    public SecondaryBalance(Map<Integer, Double> demanded, Map<Integer, Double> net) {
        this.demanded = Map.copyOf(demanded);
        this.net = Map.copyOf(net);
    }
}
