package adamski.domain.balance;

import adamski.domain.ItemQuantities;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Whether you have enough secondaries for the planned recipe runs.
 * <p>
 * Holds one {@link BalanceEntry} per secondary the runs need, plus totals across them. Secondaries
 * nothing needs are left out.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class SecondaryBalance {
    private final List<BalanceEntry> entries;
    private final ItemQuantities demanded;
    private final ItemQuantities net;

    public SecondaryBalance(List<BalanceEntry> entries) {
        this.entries = List.copyOf(entries);

        final Map<Integer, Double> demanded = new HashMap<>();
        final Map<Integer, Double> net = new HashMap<>();
        for (BalanceEntry entry : this.entries) {
            demanded.put(entry.getSecondaryItemId(), entry.getQtyDemanded());
            net.put(entry.getSecondaryItemId(), entry.net());
        }

        this.demanded = ItemQuantities.of(demanded);
        this.net = ItemQuantities.of(net);
    }

    /**
     * One secondary's balance, and where the supply comes from.
     * <p>
     * For example, crushed bird nests: 10 needed, 3 held, 4 made from bird nests. Net is -3.
     */
    @Value
    public static class BalanceEntry {
        int secondaryItemId;
        double qtyDemanded;
        double qtyHeld;

        /**
         * One per chain that makes the secondary, in chain order.
         * <p>
         * Empty when no chain makes it, or when "Count secondaries made by rows" is off.
         */
        List<ChainContribution> chainContributions;

        /**
         * One per precursor held, in table order.
         * <p>
         * Empty when none are held, or when "Count secondary precursors" is off.
         */
        List<PrecursorContribution> precursorContributions;

        public double totalFromChains() {
            return chainContributions.stream().mapToDouble(ChainContribution::getMakes).sum();
        }

        public double totalFromPrecursors() {
            return precursorContributions.stream().mapToDouble(PrecursorContribution::getMakes).sum();
        }

        public double net() {
            return qtyHeld + totalFromChains() + totalFromPrecursors() - qtyDemanded;
        }
    }

    /**
     * How much of a secondary one chain will make - 30 super attacks from the irit row, say.
     */
    @Value
    public static class ChainContribution {
        int rootItemId;
        double makes;
    }

    /**
     * How much of a secondary one held precursor will make - 10 lava scales making 45 shards, say.
     */
    @Value
    public static class PrecursorContribution {
        int precursorItemId;
        double held;
        double makes;
    }
}
