package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * What the planned recipe runs need in secondaries, and how that compares to what is held.
 * <p>
 * Only items some recipe actually consumes as a secondary appear. An item nothing demands has
 * nothing to say about it, and netting a primary here would be meaningless - primaries are consumed
 * by the cascade, not bought.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class SecondaryBalance {
    /**
     * Total quantity required across every recipe that runs, keyed by item id.
     */
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
