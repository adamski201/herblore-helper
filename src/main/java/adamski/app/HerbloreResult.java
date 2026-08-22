package adamski.app;

import adamski.domain.SecondaryBalanceCalculator;
import adamski.domain.ItemQuantities;
import adamski.domain.ChainResult;
import adamski.domain.SecondaryBalance;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Everything one bank read produced, published as a unit so the parts cannot disagree.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class HerbloreResult {
    /**
     * Counted sources merged.
     */
    private final ItemQuantities owned;

    private final List<ChainResult> chainResults;

    /**
     * The sum of the chainResults, so the headline figure always agrees with what is listed under it.
     */
    private final double totalXp;

    /**
     * Netted from the chainResults' own demand, for the same reason.
     */
    private final SecondaryBalance secondaryBalance;

    public HerbloreResult(ItemQuantities owned, List<ChainResult> chainResults) {
        this.owned = owned;
        this.chainResults = List.copyOf(chainResults);
        this.totalXp = this.chainResults.stream().mapToDouble(ChainResult::getXp).sum();
        this.secondaryBalance = SecondaryBalanceCalculator.netAgainstOwned(sumDemandOf(this.chainResults), owned);
    }

    private static ItemQuantities sumDemandOf(List<ChainResult> chainResults) {
        ItemQuantities demanded = ItemQuantities.EMPTY;

        for (ChainResult chainResult : chainResults) {
            demanded = demanded.plus(chainResult.getSecondaryDemand());
        }

        return demanded;
    }
}
