package adamski.app;

import adamski.data.RecipePaths;
import adamski.domain.calculators.BankedXpCalculator;
import adamski.domain.calculators.RecipeYieldCalculator;
import adamski.domain.models.ItemSource;
import adamski.domain.models.RecipeGroup;
import adamski.domain.models.RecipeRun;
import adamski.domain.models.RecipeStage;
import adamski.domain.models.RecipeStep;
import adamski.domain.models.SecondaryBalance;
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
        assertTrue(result.getPaths().isEmpty());
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
    public void everyRunReachesAPathSoNoXpIsLost() {
        final Map<Integer, Integer> bank = items(ItemID.UNIDENTIFIED_RANARR, 40);
        bank.put(ItemID.RANARRVIAL, 25);
        bank.put(ItemID.BRUT_CAVIAR, 12);
        bank.put(ItemID.HARRALANDER_SEED, 3);

        // The total is the sum of the paths, so grouping is checked against the ungrouped runs
        final List<RecipeRun> ungrouped = RecipeYieldCalculator
                .calculateByBankedItem(bank, HerbloreApp.RECIPES).values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        assertEquals(BankedXpCalculator.calculate(ungrouped).getTotal(), bank(bank).getTotalXp(), DELTA);
    }

    @Test
    public void everyMaturityOfAHerbIsAStageOnOnePath() {
        final Map<Integer, Integer> bank = items(ItemID.RANARR_SEED, 2);
        bank.put(ItemID.UNIDENTIFIED_RANARR, 40);
        bank.put(ItemID.RANARRVIAL, 25);

        app.sourcesUpdated(source(ItemSource.Bank, bank));

        final RecipeGroup ranarr = app.getResult().getPaths().stream()
                .filter(group -> group.getPathItemId() == RecipePaths.pathOf(ItemID.RANARR_WEED))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertEquals(Arrays.asList(ItemID.RANARR_SEED, ItemID.UNIDENTIFIED_RANARR, ItemID.RANARRVIAL),
                ranarr.getStages().stream().map(RecipeStage::getEntryItemId).collect(Collectors.toList()));
    }

    @Test
    public void anItemThatCannotReachTheChosenProductGetsItsOwnRow() {
        final Map<Integer, Integer> bank = items(ItemID.CADANTINE, 10);
        bank.put(ItemID.CADANTINE_BLOODVIAL, 4);

        app.sourcesUpdated(source(ItemSource.Bank, bank));

        final Map<Integer, Integer> entryByTerminal = app.getResult().getPaths().stream()
                .filter(group -> group.getPathItemId() == RecipePaths.pathOf(ItemID.CADANTINE))
                .collect(Collectors.toMap(RecipeGroup::getTerminalItemId, RecipeGroup::getEntryItemId));

        assertEquals(Integer.valueOf(ItemID.CADANTINE), entryByTerminal.get(ItemID._1DOSE2DEFENSE));
        assertEquals(Integer.valueOf(ItemID.CADANTINE_BLOODVIAL), entryByTerminal.get(ItemID._1DOSEBASTION));
    }

    @Test
    public void aRowKnowsHowMuchItMakesAndWhatItCosts() {
        // One grimy ranarr runs r44 once, making 3 doses of defence potion from 1 white berry
        app.sourcesUpdated(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        final RecipeGroup ranarr = app.getResult().getPaths().get(0);

        assertEquals(3.0, ranarr.getOutputQuantity(), DELTA);
        assertEquals(1.0, ranarr.getSecondaryDemand().get(ItemID.WHITE_BERRIES), DELTA);
        assertEquals(1.0, ranarr.getStages().get(0).getQuantity(), DELTA);
    }

    @Test
    public void caviarMixesAreTheirOwnPathNotTheHerbs() {
        final Map<Integer, Integer> bank = items(ItemID.UNIDENTIFIED_HARRALANDER, 20);
        bank.put(ItemID.BRUT_CAVIAR, 10);

        app.sourcesUpdated(source(ItemSource.Bank, bank));

        final Set<Integer> paths = app.getResult().getPaths().stream()
                .map(RecipeGroup::getPathItemId)
                .collect(Collectors.toSet());

        assertTrue(paths.contains(RecipePaths.pathOf(ItemID.HARRALANDER)));
        assertTrue(paths.contains(RecipePaths.pathOf(ItemID.BRUT_CAVIAR)));
    }


    @Test
    public void seedVaultCounts() {
        app.sourcesUpdated(source(ItemSource.SeedVault, items(ItemID.RANARR_SEED, 1)));

        assertEquals(8 * GRIMY_RANARR_XP, app.getResult().getTotalXp(), DELTA);
    }

    @Test
    public void inventoryDoesNotCount() {
        // Still open whether banking a herb run should read as a gain or as net zero
        app.sourcesUpdated(source(ItemSource.Inventory, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        assertEquals(0, app.getResult().getTotalXp(), DELTA);
    }

    @Test
    public void countedSourcesAreSummedTogether() {
        final Map<ItemSource, Map<Integer, Integer>> snapshot = new EnumMap<>(ItemSource.class);
        snapshot.put(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1));
        snapshot.put(ItemSource.PotionStorage, items(ItemID.UNIDENTIFIED_RANARR, 1));

        app.sourcesUpdated(snapshot);

        assertEquals(2 * GRIMY_RANARR_XP, app.getResult().getTotalXp(), DELTA);
    }

    @Test
    public void resultCoversEverySourceSeenSoFarNotJustTheChangedOne() {
        app.sourcesUpdated(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));
        app.sourcesUpdated(source(ItemSource.PotionStorage, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        assertEquals(2 * GRIMY_RANARR_XP, app.getResult().getTotalXp(), DELTA);
    }

    @Test
    public void unchangedUpdateLeavesTheResultAlone() {
        app.sourcesUpdated(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));
        final HerbloreResult first = app.getResult();

        app.sourcesUpdated(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        assertEquals(first, app.getResult());
    }

    private HerbloreResult bank(Map<Integer, Integer> bankItems) {
        app.sourcesUpdated(source(ItemSource.Bank, bankItems));
        return app.getResult();
    }

    /**
     * The steps of every path, flattened - a recipe belongs to one path, so nothing collides.
     */
    private static Map<Integer, Double> xpByRecipe(HerbloreResult result) {
        return result.getPaths().stream()
                .flatMap(path -> path.getSteps().stream())
                .collect(Collectors.toMap(step -> step.getRecipe().getId(), RecipeStep::getXp));
    }

    private SecondaryBalance bankBalance(Map<Integer, Integer> bankItems) {
        app.sourcesUpdated(source(ItemSource.Bank, bankItems));
        return app.getResult().getSecondaryBalance();
    }

    private static Map<ItemSource, Map<Integer, Integer>> source(ItemSource source, Map<Integer, Integer> items) {
        final Map<ItemSource, Map<Integer, Integer>> snapshot = new EnumMap<>(ItemSource.class);
        snapshot.put(source, items);
        return snapshot;
    }

    private static Map<Integer, Integer> items(int itemId, int quantity) {
        final Map<Integer, Integer> items = new HashMap<>();
        items.put(itemId, quantity);
        return items;
    }
}
