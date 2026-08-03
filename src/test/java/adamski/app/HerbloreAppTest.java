package adamski.app;

import adamski.domain.models.BankedXpResult;
import adamski.domain.models.ItemSource;
import adamski.domain.models.SecondaryBalance;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Drives the whole pipeline - store, source merge, real recipe table, both calculators. These
 * numbers move if HerbloreRecipes is reordered or reweighted, which is the point.
 */
public class HerbloreAppTest {
    private static final double DELTA = 0.0001;

    /**
     * One grimy ranarr followed all the way down under first-in-table-order selection: r5 degrime
     * 7.5, r20 unf 0, then r44 defence potion 75.
     * <p>
     * The chain stops at the potion. Caviar mixes are driven by brutal caviar, not by the potion,
     * so a defence potion is terminal.
     */
    private static final double GRIMY_RANARR_XP = 82.5;

    private HerbloreApp app;

    @Before
    public void setUp() {
        app = new HerbloreApp(new HerbloreStore());
    }

    @Test
    public void noResultUntilASourceIsRead() {
        assertNull(app.getBankedXp());
    }

    @Test
    public void emptyBankIsZero() {
        final BankedXpResult result = bank(new HashMap<>());

        assertEquals(0, result.getTotal(), DELTA);
        assertTrue(result.getXpPerRecipe().isEmpty());
    }

    @Test
    public void terminalItemContributesNothing() {
        assertEquals(0, bank(items(ItemID.BRUTAL_1DOSE1DEFENSE, 4)).getTotal(), DELTA);
    }

    @Test
    public void grimyHerbIsWorthItsWholeChain() {
        final BankedXpResult result = bank(items(ItemID.UNIDENTIFIED_RANARR, 1));

        assertEquals(GRIMY_RANARR_XP, result.getTotal(), DELTA);
        assertEquals(7.5, result.getXpPerRecipe().get(5), DELTA);   // degrime
        assertEquals(75.0, result.getXpPerRecipe().get(44), DELTA); // defence potion
    }

    @Test
    public void caviarMixesAreDrivenByCaviarNotByThePotion() {
        // A defence potion on its own goes nowhere - the mix consumes brutal caviar
        assertEquals(0, bank(items(ItemID._1DOSE1DEFENSE, 10)).getTotal(), DELTA);

        // r95 energy mix, first caviar recipe in the table, 23xp per caviar
        final BankedXpResult caviar = bank(items(ItemID.BRUT_CAVIAR, 4));

        assertEquals(92.0, caviar.getTotal(), DELTA);
        assertEquals(92.0, caviar.getXpPerRecipe().get(95), DELTA);
    }

    @Test
    public void onlyOneCaviarMixIsReachableUntilConfigLands() {
        // All 23 mixes now share brutal caviar as their primary, and the selection is one recipe
        // per primary, so first in table order takes the lot.
        final BankedXpResult result = bank(items(ItemID.BRUT_CAVIAR, 1));

        assertEquals(1, result.getXpPerRecipe().size());
    }

    @Test
    public void zeroXpStepsAreOmittedFromTheBreakdown() {
        // r20 turns ranarr into ranarr unf and is worth nothing
        assertFalse(bank(items(ItemID.UNIDENTIFIED_RANARR, 1)).getXpPerRecipe().containsKey(20));
    }

    @Test
    public void totalIsTheSumOfTheBreakdown() {
        final BankedXpResult result = bank(items(ItemID.UNIDENTIFIED_RANARR, 13));

        final double summed = result.getXpPerRecipe().values().stream().mapToDouble(Double::doubleValue).sum();

        assertEquals(summed, result.getTotal(), DELTA);
    }

    @Test
    public void seedsCountAtTheNominalYield() {
        // r122 yields NOMINAL_HERB_YIELD grimy ranarr and is itself worth no xp
        assertEquals(8 * GRIMY_RANARR_XP, bank(items(ItemID.RANARR_SEED, 1)).getTotal(), DELTA);
    }

    @Test
    public void firstRecipeInTableOrderWins() {
        // TORSTOL feeds r30 (unf, 0xp) and r81 (super combat, 150xp). r30 comes first in the table,
        // so torstol becomes unf and goes on to r60 zamorak brew rather than super combat.
        final BankedXpResult result = bank(items(ItemID.TORSTOL, 1));

        assertTrue(result.getXpPerRecipe().containsKey(60));
        assertFalse(result.getXpPerRecipe().containsKey(81));
    }

    @Test
    public void firstRecipeWinsWhenBothCandidatesAreZeroXp() {
        // TOADFLAX feeds r21 (toadflax unf) and r32 (antidote+ unf), both worth nothing. r21 comes
        // first, so the chain continues through r45 agility potion, not r66 antidote+.
        final BankedXpResult result = bank(items(ItemID.TOADFLAX, 1));

        assertTrue(result.getXpPerRecipe().containsKey(45));
        assertFalse(result.getXpPerRecipe().containsKey(66));
    }

    @Test
    public void secondaryOnlyItemsAreIgnored() {
        // Snape grass is a secondary of r47 and nothing's primary, so it is worth nothing alone
        assertEquals(0, bank(items(ItemID.SNAPE_GRASS, 100)).getTotal(), DELTA);
    }

    @Test
    public void secondaryDemandComesFromTheSameRuns() {
        // One grimy ranarr runs r20 once (a vial of water) and r44 once (white berries), and
        // nothing else is consumed on that chain
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
    public void seedVaultCounts() {
        app.sourcesUpdated(source(ItemSource.SeedVault, items(ItemID.RANARR_SEED, 1)));

        assertEquals(8 * GRIMY_RANARR_XP, app.getBankedXp().getTotal(), DELTA);
    }

    @Test
    public void inventoryDoesNotCount() {
        // Still open whether banking a herb run should read as a gain or as net zero
        app.sourcesUpdated(source(ItemSource.Inventory, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        assertEquals(0, app.getBankedXp().getTotal(), DELTA);
    }

    @Test
    public void countedSourcesAreSummedTogether() {
        final Map<ItemSource, Map<Integer, Integer>> snapshot = new EnumMap<>(ItemSource.class);
        snapshot.put(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1));
        snapshot.put(ItemSource.PotionStorage, items(ItemID.UNIDENTIFIED_RANARR, 1));

        app.sourcesUpdated(snapshot);

        assertEquals(2 * GRIMY_RANARR_XP, app.getBankedXp().getTotal(), DELTA);
    }

    @Test
    public void resultCoversEverySourceSeenSoFarNotJustTheChangedOne() {
        app.sourcesUpdated(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));
        app.sourcesUpdated(source(ItemSource.PotionStorage, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        assertEquals(2 * GRIMY_RANARR_XP, app.getBankedXp().getTotal(), DELTA);
    }

    @Test
    public void unchangedUpdateLeavesTheResultAlone() {
        app.sourcesUpdated(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));
        final BankedXpResult first = app.getBankedXp();

        app.sourcesUpdated(source(ItemSource.Bank, items(ItemID.UNIDENTIFIED_RANARR, 1)));

        assertEquals(first, app.getBankedXp());
    }

    private BankedXpResult bank(Map<Integer, Integer> bankItems) {
        app.sourcesUpdated(source(ItemSource.Bank, bankItems));
        return app.getBankedXp();
    }

    private SecondaryBalance bankBalance(Map<Integer, Integer> bankItems) {
        app.sourcesUpdated(source(ItemSource.Bank, bankItems));
        return app.getSecondaryBalance();
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
