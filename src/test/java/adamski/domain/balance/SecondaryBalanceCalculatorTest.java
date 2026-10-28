package adamski.domain.balance;

import adamski.domain.result.ChainItemXp;
import adamski.domain.result.ChainResult;
import adamski.domain.ItemQuantities;
import adamski.domain.SecondaryPrecursor;
import adamski.domain.balance.SecondaryBalance.ChainContribution;
import adamski.domain.balance.SecondaryBalance.PrecursorContribution;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Synthetic chains: items 50 and 51 are the secondaries under test, chains are rooted at 70 and up.
 */
public class SecondaryBalanceCalculatorTest {
    private static final double DELTA = 0.0001;

    // items 60 and 61 both become secondary 50; 62 becomes 50 at 4.5 apiece; 63 becomes 51
    private static final List<SecondaryPrecursor> PRECURSORS = List.of(
            new SecondaryPrecursor(60, 50, 1),
            new SecondaryPrecursor(61, 50, 1),
            new SecondaryPrecursor(62, 50, 4.5),
            new SecondaryPrecursor(63, 51, 1));

    // --- netting against what is held -------------------------------------

    @Test
    public void shortfallIsNegative() {
        final SecondaryBalance balance = calculate(owned(50, 10), needing(70, 50, 16));

        assertEquals(-6.0, balance.getNet().get(50), DELTA);
    }

    @Test
    public void spareIsPositive() {
        final SecondaryBalance balance = calculate(owned(50, 100), needing(70, 50, 16));

        assertEquals(84.0, balance.getNet().get(50), DELTA);
    }

    @Test
    public void owningNoneIsTheFullDemandAsAShortfall() {
        final SecondaryBalance balance = calculate(owned(), needing(70, 50, 7));

        assertEquals(-7.0, balance.getNet().get(50), DELTA);
    }

    @Test
    public void demandSumsAcrossChains() {
        final SecondaryBalance balance = calculate(owned(), needing(70, 50, 4), needing(71, 50, 10));

        assertEquals(14.0, balance.getDemanded().get(50), DELTA);
    }

    @Test
    public void itemsNothingDemandsAreAbsent() {
        // 99 is held but no chain consumes it, so the balance has nothing to say
        final SecondaryBalance balance = calculate(owned(99, 500), needing(70, 50, 1));

        assertFalse(balance.getNet().itemIds().contains(99));
        assertTrue(balance.getNet().itemIds().contains(50));
    }

    @Test
    public void noChainsIsEmpty() {
        final SecondaryBalance balance = calculate(owned(50, 10));

        assertTrue(balance.getDemanded().isEmpty());
        assertTrue(balance.getNet().isEmpty());
        assertTrue(balance.getEntries().isEmpty());
    }

    @Test
    public void theTotalsAgreeWithTheTallies() {
        final SecondaryBalance balance = calculateCounting(owned(50, 5, 60, 2, 63, 1),
                chain(70, 70, 0, demand(50, 4, 51, 12)));

        for (SecondaryBalance.BalanceEntry entry : balance.getEntries()) {
            assertEquals(entry.getQtyDemanded(), balance.getDemanded().get(entry.getSecondaryItemId()), DELTA);
            assertEquals(entry.net(), balance.getNet().get(entry.getSecondaryItemId()), DELTA);
        }
        assertEquals(2, balance.getEntries().size());
    }

    // --- what other chains make ---------------------------------------------

    @Test
    public void whatAChainMakesCountsTowardsTheSecondary() {
        final SecondaryBalance balance = calculate(owned(50, 1), needing(70, 50, 10), making(71, 50, 6));

        assertEquals(-3.0, balance.getNet().get(50), DELTA); // 1 held + 6 made - 10
    }

    @Test
    public void eachChainMakingTheSecondaryIsKeptApart() {
        final SecondaryBalance.BalanceEntry entry = entryOf(calculate(owned(),
                needing(70, 50, 10), making(71, 50, 6), making(72, 50, 2.5)), 50);

        assertEquals(List.of(new ChainContribution(71, 6.0), new ChainContribution(72, 2.5)), entry.getChainContributions());
        assertEquals(8.5, entry.totalFromChains(), DELTA);
    }

    @Test
    public void aChainOnlyCountsTowardsWhatItMakes() {
        final SecondaryBalance balance = calculate(owned(),
                chain(70, 70, 0, demand(50, 1, 51, 3)), making(71, 51, 6));

        assertEquals(-1.0, balance.getNet().get(50), DELTA);
        assertEquals(3.0, balance.getNet().get(51), DELTA);
    }

    @Test
    public void aChainMakingNoneOfItsProductIsLeftOut() {
        final SecondaryBalance.BalanceEntry entry = entryOf(calculate(owned(), needing(70, 50, 10), making(71, 50, 0)), 50);

        assertTrue(entry.getChainContributions().isEmpty());
    }

    @Test
    public void aProductNothingNeedsIsAbsent() {
        final SecondaryBalance balance = calculate(owned(), needing(70, 50, 1), making(71, 99, 6));

        assertFalse(balance.getNet().itemIds().contains(99));
    }

    @Test
    public void notCountingChainProductsIsTheBalanceWithoutThem() {
        final SecondaryBalance balance = SecondaryBalanceCalculator.calculate(
                List.of(needing(70, 50, 10), making(71, 50, 6)), owned(), false, List.of());

        assertTrue(entryOf(balance, 50).getChainContributions().isEmpty());
        assertEquals(-10.0, balance.getNet().get(50), DELTA);
    }

    // --- precursors ---------------------------------------------------------

    @Test
    public void precursorsCountTowardsTheirSecondary() {
        final SecondaryBalance balance = calculateCounting(owned(50, 100, 60, 50), needing(70, 50, 200));

        assertEquals(-50.0, balance.getNet().get(50), DELTA); // 100 held + 50 from precursors - 200
    }

    @Test
    public void precursorsAreCountedAtWhatTheyMake() {
        final SecondaryBalance balance = calculateCounting(owned(62, 10), needing(70, 50, 50));

        assertEquals(-5.0, balance.getNet().get(50), DELTA); // 10 × 4.5 = 45
    }

    @Test
    public void severalPrecursorsAddUp() {
        final SecondaryBalance.BalanceEntry entry = entryOf(calculateCounting(owned(60, 3, 61, 4), needing(70, 50, 10)), 50);

        assertEquals(7.0, entry.totalFromPrecursors(), DELTA);
        assertEquals(List.of(new PrecursorContribution(60, 3, 3), new PrecursorContribution(61, 4, 4)),
                entry.getPrecursorContributions());
    }

    @Test
    public void aPrecursorOnlyCountsTowardsItsOwnSecondary() {
        final SecondaryBalance balance = calculateCounting(owned(63, 20), chain(70, 70, 0, demand(50, 2, 51, 6)));

        assertEquals(-2.0, balance.getNet().get(50), DELTA);
        assertEquals(14.0, balance.getNet().get(51), DELTA);
    }

    @Test
    public void aPrecursorWhoseSecondaryNothingNeedsIsAbsent() {
        final SecondaryBalance balance = calculateCounting(owned(63, 20), needing(70, 50, 1));

        assertFalse(balance.getNet().itemIds().contains(51));
    }

    @Test
    public void precursorsNotHeldAreLeftOutOfTheEntry() {
        final SecondaryBalance.BalanceEntry entry = entryOf(calculateCounting(owned(60, 3), needing(70, 50, 10)), 50);

        assertEquals(List.of(new PrecursorContribution(60, 3, 3)), entry.getPrecursorContributions());
    }

    @Test
    public void notCountingPrecursorsIsTheBalanceWithoutThem() {
        assertEquals(calculate(owned(50, 100), needing(70, 50, 200)),
                calculate(owned(50, 100, 60, 50), needing(70, 50, 200)));
        assertEquals(-100.0, calculate(owned(50, 100, 60, 50), needing(70, 50, 200)).getNet().get(50), DELTA);
    }

    // --- helpers ------------------------------------------------------------

    private static SecondaryBalance.BalanceEntry entryOf(SecondaryBalance balance, int secondaryItemId) {
        return balance.getEntries().stream()
                .filter(entry -> entry.getSecondaryItemId() == secondaryItemId)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Chain products counted, precursors not.
     */
    private static SecondaryBalance calculate(ItemQuantities owned, ChainResult... chains) {
        return SecondaryBalanceCalculator.calculate(Arrays.asList(chains), owned, true, List.of());
    }

    private static SecondaryBalance calculateCounting(ItemQuantities owned, ChainResult... chains) {
        return SecondaryBalanceCalculator.calculate(Arrays.asList(chains), owned, true, PRECURSORS);
    }

    /**
     * A chain that makes nothing and needs some of one secondary.
     */
    private static ChainResult needing(int rootItemId, int secondaryItemId, double quantity) {
        return chain(rootItemId, rootItemId, 0, demand(secondaryItemId, quantity));
    }

    /**
     * A chain that needs nothing and makes some of one product.
     */
    private static ChainResult making(int rootItemId, int productItemId, double quantity) {
        return chain(rootItemId, productItemId, quantity, ItemQuantities.EMPTY);
    }

    private static ChainResult chain(int rootItemId, int productItemId, double outputQuantity, ItemQuantities demand) {
        return new ChainResult(rootItemId, productItemId,
                List.of(new ChainItemXp(rootItemId, 1, List.of(), 0)), List.of(),
                outputQuantity, demand, 0);
    }

    /**
     * @param pairs alternating item id and quantity
     */
    private static ItemQuantities demand(double... pairs) {
        final Map<Integer, Double> demand = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            demand.put((int) pairs[i], pairs[i + 1]);
        }
        return ItemQuantities.of(demand);
    }

    /**
     * @param pairs alternating item id and quantity
     */
    private static ItemQuantities owned(int... pairs) {
        final Map<Integer, Integer> items = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            items.put(pairs[i], pairs[i + 1]);
        }
        return ItemQuantities.counted(items);
    }
}
