package adamski.domain.balance;

import adamski.domain.result.ChainResult;
import adamski.domain.ItemQuantities;
import adamski.domain.SecondaryPrecursor;
import adamski.domain.balance.SecondaryBalance.ChainContribution;
import adamski.domain.balance.SecondaryBalance.PrecursorContribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the chains' planned runs will cost in secondaries, netted against what is held - and,
 * optionally, against what other chains make and what precursors held would become.
 */
public final class SecondaryBalanceCalculator {
    private SecondaryBalanceCalculator() {
    }

    /**
     * @param chainResults         every chain's result - what they demand, and what they make
     * @param owned                what the player holds
     * @param chainProductsCounted whether what one chain makes counts towards another's secondaries
     * @param precursors           what can be turned into a secondary and counted as one - empty to
     *                             count none
     */
    public static SecondaryBalance calculate(List<ChainResult> chainResults, ItemQuantities owned,
                                             boolean chainProductsCounted, List<SecondaryPrecursor> precursors) {
        final ItemQuantities demanded = sumDemand(chainResults);
        final Map<Integer, List<ChainContribution>> chainContributionsBySecondary =
                chainProductsCounted ? chainContributionsBySecondary(chainResults) : Map.of();
        final Map<Integer, List<PrecursorContribution>> precursorContributionsBySecondary =
                precursorContributionsBySecondary(precursors, owned);

        final List<SecondaryBalance.BalanceEntry> entries = new ArrayList<>();
        demanded.forEach((secondaryItemId, required) -> entries.add(new SecondaryBalance.BalanceEntry(
                secondaryItemId, required, owned.get(secondaryItemId),
                chainContributionsBySecondary.getOrDefault(secondaryItemId, List.of()),
                precursorContributionsBySecondary.getOrDefault(secondaryItemId, List.of()))));

        return new SecondaryBalance(entries);
    }

    private static ItemQuantities sumDemand(List<ChainResult> chainResults) {
        ItemQuantities demanded = ItemQuantities.EMPTY;

        for (ChainResult chainResult : chainResults) {
            demanded = demanded.plus(chainResult.getSecondaryDemand());
        }

        return demanded;
    }

    /**
     * For each product, what each chain makes of it. Only what the runs make counts - banked stock
     * of a product is held, and counted as held. A chain making nothing, or none of its product, is
     * left out.
     */
    private static Map<Integer, List<ChainContribution>> chainContributionsBySecondary(List<ChainResult> chainResults) {
        final Map<Integer, List<ChainContribution>> contributions = new HashMap<>();

        for (ChainResult chainResult : chainResults) {
            if (chainResult.makesNothing() || chainResult.getOutputQuantity() <= 0) continue;

            contributions.computeIfAbsent(chainResult.getProductItemId(), id -> new ArrayList<>())
                    .add(new ChainContribution(chainResult.getRootItemId(), chainResult.getOutputQuantity()));
        }

        return contributions;
    }

    /**
     * For each secondary, what each held precursor makes of it. Precursors not held are left out.
     */
    private static Map<Integer, List<PrecursorContribution>> precursorContributionsBySecondary(
            List<SecondaryPrecursor> precursors, ItemQuantities owned) {
        final Map<Integer, List<PrecursorContribution>> contributions = new HashMap<>();

        for (SecondaryPrecursor precursor : precursors) {
            final double held = owned.get(precursor.getItemId());
            if (held <= 0) continue;

            contributions.computeIfAbsent(precursor.getSecondaryItemId(), id -> new ArrayList<>())
                    .add(new PrecursorContribution(precursor.getItemId(), held, held * precursor.getYield()));
        }

        return contributions;
    }
}
