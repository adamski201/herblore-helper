package adamski.app;

import adamski.domain.ItemQuantities;
import adamski.domain.ItemSource;
import adamski.domain.RecipeSelection;
import adamski.domain.ChainResult;
import adamski.domain.ChainItemXp;
import adamski.domain.ChainRecipeXp;
import adamski.domain.SecondaryBalance;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Drives the whole pipeline against the real recipe table, so these numbers move if it does.
 */
public class HerbloreAppTest {
    private static final double DELTA = 0.0001;

    /**
     * r5 degrime 7.5, r20 unf 0, r44 defence potion 75. Terminal - caviar mixes run off caviar.
     */
    private static final double GRIMY_RANARR_XP = 82.5;

    private HerbloreApp app;

    @Before
    public void setUp() {
        app = new HerbloreApp(new HerbloreStore());
    }

    @Test
    public void noResultUntilASourceIsRead() {
        assertNull(app.getResult());
    }

    @Test
    public void emptyBankIsZero() {
        final HerbloreResult result = bank(new HashMap<>());

        assertEquals(0, result.getTotalXp(), DELTA);
        assertTrue(result.getChainResults().isEmpty());
    }

    @Test
    public void terminalItemContributesNothing() {
        assertEquals(0, bank(items(ItemID.BRUTAL_1DOSE1DEFENSE, 4)).getTotalXp(), DELTA);
    }

    @Test
    public void grimyHerbIsWorthItsWholeChain() {
        final HerbloreResult result = bank(items(ItemID.UNIDENTIFIED_RANARR, 1));

        assertEquals(GRIMY_RANARR_XP, result.getTotalXp(), DELTA);
        assertEquals(7.5, xpByRecipe(result).get(5), DELTA);   // degrime
        assertEquals(75.0, xpByRecipe(result).get(44), DELTA); // defence potion
    }

    @Test
    public void caviarMixesAreDrivenByCaviarNotByThePotion() {
        // A defence potion on its own goes nowhere - the mix consumes brutal caviar
        assertEquals(0, bank(items(ItemID._1DOSE1DEFENSE, 10)).getTotalXp(), DELTA);

        // r95 energy mix, first caviar recipe in the table, 23xp per caviar
        final HerbloreResult caviar = bank(items(ItemID.BRUT_CAVIAR, 4));

        assertEquals(92.0, caviar.getTotalXp(), DELTA);
        assertEquals(92.0, xpByRecipe(caviar).get(95), DELTA);
    }

    @Test
    public void onlyOneCaviarMixIsReachableUntilConfigLands() {
        // All 23 mixes share brutal caviar, so first in table order takes the lot
        final HerbloreResult result = bank(items(ItemID.BRUT_CAVIAR, 1));

        assertEquals(1, xpByRecipe(result).size());
    }

    @Test
    public void zeroXpStepsAreOmittedFromTheBreakdown() {
        // r20 turns ranarr into ranarr unf and is worth nothing
        assertFalse(xpByRecipe(bank(items(ItemID.UNIDENTIFIED_RANARR, 1))).containsKey(20));
    }

    @Test
    public void totalIsTheSumOfTheBreakdown() {
        final HerbloreResult result = bank(items(ItemID.UNIDENTIFIED_RANARR, 13));

        final double summed = xpByRecipe(result).values().stream().mapToDouble(Double::doubleValue).sum();

        assertEquals(summed, result.getTotalXp(), DELTA);
    }

    @Test
    public void seedsCountAtTheNominalYield() {
        // r122 yields NOMINAL_HERB_YIELD grimy ranarr and is itself worth no xp
        assertEquals(8 * GRIMY_RANARR_XP, bank(items(ItemID.RANARR_SEED, 1)).getTotalXp(), DELTA);
    }

    @Test
    public void firstRecipeInTableOrderWins() {
        // Torstol feeds r30 (unf) and r81 (super combat); r30 wins, so r60 zamorak brew follows
        final Map<Integer, Double> xp = xpByRecipe(bank(items(ItemID.TORSTOL, 1)));

        assertTrue(xp.containsKey(60));
        assertFalse(xp.containsKey(81));
    }

    @Test
    public void firstRecipeWinsWhenBothCandidatesAreZeroXp() {
        // Toadflax feeds r21 and r32, both 0xp; r21 wins, so r45 agility follows, not r66 antidote+
        final Map<Integer, Double> xp = xpByRecipe(bank(items(ItemID.TOADFLAX, 1)));

        assertTrue(xp.containsKey(45));
        assertFalse(xp.containsKey(66));
    }

    @Test
    public void secondaryOnlyItemsAreIgnored() {
        assertEquals(0, bank(items(ItemID.SNAPE_GRASS, 100)).getTotalXp(), DELTA);
    }

    @Test
    public void secondaryDemandComesFromTheSameRuns() {
        // One grimy ranarr runs r20 once (vial of water) and r44 once (white berries)
        final SecondaryBalance balance = bankBalance(items(ItemID.UNIDENTIFIED_RANARR, 1));

        assertEquals(1.0, balance.getDemanded().get(ItemID.VIAL_WATER), DELTA);
        assertEquals(1.0, balance.getDemanded().get(ItemID.WHITE_BERRIES), DELTA);
    }

    @Test
    public void owningTheSecondaryClearsTheShortfall() {
        final Map<Integer, Integer> bank = items(ItemID.UNIDENTIFIED_RANARR, 4);
        bank.put(ItemID.WHITE_BERRIES, 10);

        final SecondaryBalance balance = bankBalance(bank);

        assertEquals(-4.0, balance.getNet().get(ItemID.VIAL_WATER), DELTA); // none held
        assertEquals(6.0, balance.getNet().get(ItemID.WHITE_BERRIES), DELTA); // 10 held, 4 needed
    }

    @Test
    public void partitioningTheBankDoesNotChangeTheTotal() {
        final Map<Integer, Integer> bank = items(ItemID.UNIDENTIFIED_RANARR, 1);
        bank.put(ItemID.RANARRVIAL, 1);

        final HerbloreResult result = bank(bank);

        assertEquals(GRIMY_RANARR_XP + 75.0, result.getTotalXp(), DELTA);
        assertEquals(150.0, xpByRecipe(result).get(44), DELTA); // one run from each banked item
    }

    @Test
    public void everyMaturityOfAHerbIsAStageOnOneRow() {
        final Map<Integer, Integer> bank = items(ItemID.RANARR_SEED, 2);
        bank.put(ItemID.UNIDENTIFIED_RANARR, 40);
        bank.put(ItemID.RANARRVIAL, 25);

        app.updateItems(source(ItemSource.Bank, bank));

        final ChainResult ranarr = app.getResult().getChainResults().get(0);

        assertEquals(Arrays.asList(ItemID.RANARR_SEED, ItemID.UNIDENTIFIED_RANARR, ItemID.RANARRVIAL),
                ranarr.getItemContributions().stream().map(ChainItemXp::getItemId).collect(Collectors.toList()));
    }

    @Test
    public void anItemThatCannotReachTheChosenProductGetsItsOwnRow() {
        final Map<Integer, Integer> bank = items(ItemID.CADANTINE, 10);
        bank.put(ItemID.CADANTINE_BLOODVIAL, 4);

        app.updateItems(source(ItemSource.Bank, bank));

        final Map<Integer, Integer> entryByTerminal = app.getResult().getChainResults().stream()
                .collect(Collectors.toMap(ChainResult::getProductItemId, ChainResult::getRootItemId));

        assertEquals(Integer.valueOf(ItemID.CADANTINE), entryByTerminal.get(ItemID._1DOSE2DEFENSE));
        assertEquals(Integer.valueOf(ItemID.CADANTINE_BLOODVIAL), entryByTerminal.get(ItemID._1DOSEBASTION));
    }

    @Test
    public void aRowKnowsHowMuchItMakesAndWhatItCosts() {
        // One grimy ranarr runs r44 once, making 3 doses of defence potion from 1 white berry
        app.updateItems(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        final ChainResult ranarr = app.getResult().getChainResults().get(0);

        assertEquals(3.0, ranarr.getOutputQuantity(), DELTA);
        assertEquals(1.0, ranarr.getSecondaryDemand().get(ItemID.WHITE_BERRIES), DELTA);
        assertEquals(1.0, ranarr.getItemContributions().get(0).getQuantity(), DELTA);
    }

    @Test
    public void aChainIsOneRowEvenWhereItCrossesIntoAnotherPotion() {
        // Harralander runs to stat restore and on into guthix balance - one chain, one result
        app.updateItems(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_HARRALANDER, 20)));

        assertEquals(1, app.getResult().getChainResults().size());
        assertEquals(ItemID.UNIDENTIFIED_HARRALANDER, app.getResult().getChainResults().get(0).getRootItemId());
    }

    @Test
    public void caviarMixesAreTheirOwnRowNotTheHerbs() {
        final Map<Integer, Integer> bank = items(ItemID.UNIDENTIFIED_HARRALANDER, 20);
        bank.put(ItemID.BRUT_CAVIAR, 10);

        app.updateItems(source(ItemSource.Bank, bank));

        final Set<Integer> entries = app.getResult().getChainResults().stream()
                .map(ChainResult::getRootItemId)
                .collect(Collectors.toSet());

        assertTrue(entries.contains(ItemID.UNIDENTIFIED_HARRALANDER));
        assertTrue(entries.contains(ItemID.BRUT_CAVIAR));
    }


    @Test
    public void seedVaultCounts() {
        app.updateItems(source(ItemSource.SeedVault, items(ItemID.RANARR_SEED, 1)));

        assertEquals(8 * GRIMY_RANARR_XP, app.getResult().getTotalXp(), DELTA);
    }

    @Test
    public void inventoryDoesNotCount() {
        // Still open whether banking a herb run should read as a gain or as net zero
        app.updateItems(source(ItemSource.Inventory, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        assertEquals(0, app.getResult().getTotalXp(), DELTA);
    }

    @Test
    public void countedSourcesAreSummedTogether() {
        final Map<ItemSource, ItemQuantities> snapshot = new EnumMap<>(ItemSource.class);
        snapshot.put(ItemSource.Bank, ItemQuantities.counted(items(ItemID.UNIDENTIFIED_RANARR, 1)));
        snapshot.put(ItemSource.PotionStorage, ItemQuantities.counted(items(ItemID.UNIDENTIFIED_RANARR, 1)));

        app.updateItems(snapshot);

        assertEquals(2 * GRIMY_RANARR_XP, app.getResult().getTotalXp(), DELTA);
    }

    @Test
    public void resultCoversEverySourceSeenSoFarNotJustTheChangedOne() {
        app.updateItems(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));
        app.updateItems(source(ItemSource.PotionStorage, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        assertEquals(2 * GRIMY_RANARR_XP, app.getResult().getTotalXp(), DELTA);
    }

    @Test
    public void unchangedUpdateLeavesTheResultAlone() {
        app.updateItems(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));
        final HerbloreResult first = app.getResult();

        app.updateItems(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        assertEquals(first, app.getResult());
    }

    @Test
    public void selectingAProductReroutesTheChain() {
        final ChainResult before = bank(items(ItemID.UNIDENTIFIED_AVANTOE, 1)).getChainResults().get(0);
        assertEquals(ItemID._1DOSEFISHERSPOTION, before.getProductItemId());

        app.selectProduct(before.getRootItemId(), ItemID._1DOSEHUNTING);

        final ChainResult after = app.getResult().getChainResults().get(0);
        assertEquals(ItemID._1DOSEHUNTING, after.getProductItemId());
        assertNotEquals(before.getXp(), after.getXp(), DELTA);
    }

    /**
     * The whole point of storing a step against an item rather than a destination against a chain.
     * The chain is rooted at the seed while there are seeds and at the grimy herb once they are
     * gone, but the step that does the choosing sits on the unf vial and never moves.
     */
    @Test
    public void aSelectionSurvivesTheBankDraining() {
        final ChainResult withSeeds = bank(items(ItemID.AVANTOE_SEED, 10)).getChainResults().get(0);
        assertEquals(ItemID.AVANTOE_SEED, withSeeds.getRootItemId());

        app.selectProduct(withSeeds.getRootItemId(), ItemID._1DOSEHUNTING);

        final ChainResult drained = bank(items(ItemID.UNIDENTIFIED_AVANTOE, 20)).getChainResults().get(0);

        assertEquals(ItemID.UNIDENTIFIED_AVANTOE, drained.getRootItemId());
        assertEquals("the choice outlives the item that made it", ItemID._1DOSEHUNTING, drained.getProductItemId());
    }

    /**
     * Super antifire and extended antifire both end at extended super antifire but neither can
     * become the other, so they are two chains and must not share a choice.
     */
    @Test
    public void independentChainsEndingAtOneProductAreKeptApart() {
        final Map<Integer, Integer> both = items(ItemID._1DOSE3ANTIDRAGON, 4);
        both.put(ItemID._1DOSE2ANTIDRAGON, 4);

        final List<ChainResult> chains = bank(both).getChainResults();

        assertEquals(2, chains.size());
        assertEquals(chains.get(0).getProductItemId(), chains.get(1).getProductItemId());
        assertNotEquals(chains.get(0).getRootItemId(), chains.get(1).getRootItemId());
    }

    /**
     * Torstol reaches super combat directly and through the unf vial, so choosing it on the torstol
     * row leaves banked torstol vials off the route entirely. They root a row of their own and take
     * their own choice - which one selection per chain could not express, because both rows sit on
     * the same chain.
     */
    @Test
    public void twoRowsOnOneHerbCanBothBeSetToTheSameProduct() {
        final Map<Integer, Integer> both = items(ItemID.TORSTOL, 10);
        both.put(ItemID.TORSTOLVIAL, 10);
        bank(both);

        app.selectProduct(ItemID.TORSTOL, ItemID._1DOSE2COMBAT);
        app.selectProduct(ItemID.TORSTOLVIAL, ItemID._1DOSE2COMBAT);

        final List<ChainResult> chains = app.getResult().getChainResults();

        assertEquals(2, chains.size());
        assertEquals(ItemID._1DOSE2COMBAT, chains.get(0).getProductItemId());
        assertEquals(ItemID._1DOSE2COMBAT, chains.get(1).getProductItemId());
    }

    /**
     * Stopping the seed row at the clean herb leaves banked unf vials below the cut. They root a row
     * of their own and hold a choice the row above cannot overwrite.
     */
    @Test
    public void aRowBelowACutHoldsAChoiceOfItsOwn() {
        final Map<Integer, Integer> both = items(ItemID.AVANTOE_SEED, 10);
        both.put(ItemID.AVANTOEVIAL, 100);
        bank(both);

        app.selectProduct(ItemID.AVANTOE_SEED, ItemID.AVANTOE);
        app.selectProduct(ItemID.AVANTOEVIAL, ItemID._1DOSEHUNTING);

        final List<ChainResult> chains = app.getResult().getChainResults();

        assertEquals(2, chains.size());
        assertEquals(ItemID.AVANTOE, chains.get(0).getProductItemId());
        assertEquals(ItemID._1DOSEHUNTING, chains.get(1).getProductItemId());
    }

    /**
     * A product the row cannot reach names no route, so there is nothing to store.
     */
    @Test
    public void selectingAnUnreachableProductChangesNothing() {
        final ChainResult before = bank(items(ItemID.UNIDENTIFIED_AVANTOE, 1)).getChainResults().get(0);

        app.selectProduct(ItemID.UNIDENTIFIED_AVANTOE, ItemID.AVANTOE_SEED);

        assertEquals(before, app.getResult().getChainResults().get(0));
    }

    @Test
    public void aSelectionMadeBeforeAnySourceIsReadPublishesNothing() {
        app.selectProduct(ItemID.AVANTOE_SEED, ItemID._1DOSEHUNTING);

        assertNull(app.getResult());
    }

    @Test
    public void chainsCarryTheProductsTheyCouldEndAtInstead() {
        final ChainResult chain = bank(items(ItemID.UNIDENTIFIED_AVANTOE, 1)).getChainResults().get(0);

        assertTrue(chain.getProductOptions().contains(ItemID._1DOSEHUNTING));
        assertTrue("intermediates count - stopping at an unf is a valid choice",
                chain.getProductOptions().contains(ItemID.AVANTOEVIAL));
    }

    @Test
    public void puttingAChainBackOnItsDefaultRestoresTheDefaultProduct() {
        final ChainResult chain = bank(items(ItemID.UNIDENTIFIED_AVANTOE, 1)).getChainResults().get(0);

        app.selectProduct(chain.getRootItemId(), ItemID._1DOSEHUNTING);
        app.selectProduct(chain.getRootItemId(), RecipeSelection.DEFAULT);

        assertEquals(ItemID._1DOSEFISHERSPOTION, app.getResult().getChainResults().get(0).getProductItemId());
    }

    private HerbloreResult bank(Map<Integer, Integer> bankItems) {
        app.updateItems(source(ItemSource.Bank, bankItems));
        return app.getResult();
    }

    /**
     * The recipe contributions of every chainResult, flattened - a recipe belongs to one chainResult, so nothing collides.
     */
    private static Map<Integer, Double> xpByRecipe(HerbloreResult result) {
        return result.getChainResults().stream()
                .flatMap(chainResult -> chainResult.getRecipeContributions().stream())
                .collect(Collectors.toMap(step -> step.getRecipe().getId(), ChainRecipeXp::getXp));
    }

    private SecondaryBalance bankBalance(Map<Integer, Integer> bankItems) {
        app.updateItems(source(ItemSource.Bank, bankItems));
        return app.getResult().getSecondaryBalance();
    }

    private static Map<ItemSource, ItemQuantities> source(ItemSource source, Map<Integer, Integer> items) {
        final Map<ItemSource, ItemQuantities> snapshot = new EnumMap<>(ItemSource.class);
        snapshot.put(source, ItemQuantities.counted(items));
        return snapshot;
    }

    private static Map<Integer, Integer> items(int itemId, int quantity) {
        final Map<Integer, Integer> items = new HashMap<>();
        items.put(itemId, quantity);
        return items;
    }
}
