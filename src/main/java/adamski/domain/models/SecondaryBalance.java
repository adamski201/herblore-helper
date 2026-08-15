package adamski.domain.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * What the planned recipe runs need in secondaries, and how that compares to what is held. Only
 * items some recipe consumes as a secondary appear.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class SecondaryBalance {
    private final ItemQuantities demanded;

    /**
     * Held minus demanded. Negative is a shortfall, positive is spare.
     */
    private final ItemQuantities net;

    public SecondaryBalance(ItemQuantities demanded, ItemQuantities net) {
        this.demanded = demanded;
        this.net = net;
    }
}
