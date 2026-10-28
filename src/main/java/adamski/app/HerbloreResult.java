package adamski.app;

import adamski.domain.ItemQuantities;
import adamski.domain.result.ChainResult;
import adamski.domain.balance.SecondaryBalance;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
public final class HerbloreResult {
    private final ItemQuantities owned;
    private final List<ChainResult> chainResults;
    private final double totalXp;
    private final SecondaryBalance secondaryBalance;

    public HerbloreResult(ItemQuantities owned, List<ChainResult> chainResults, SecondaryBalance secondaryBalance) {
        this.owned = owned;
        this.chainResults = List.copyOf(chainResults);
        this.totalXp = this.chainResults.stream().mapToDouble(ChainResult::getXp).sum();
        this.secondaryBalance = secondaryBalance;
    }
}
