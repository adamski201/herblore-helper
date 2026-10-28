package adamski.ui;

import adamski.domain.result.ChainItemXp;
import adamski.domain.result.ChainRecipeXp;
import adamski.domain.result.ChainResult;
import adamski.domain.Ingredient;
import adamski.domain.ItemQuantities;
import adamski.domain.Recipe;
import adamski.domain.RecipeTag;
import adamski.domain.balance.SecondaryBalance;
import adamski.domain.balance.SecondaryBalance.ChainContribution;
import adamski.domain.balance.SecondaryBalance.PrecursorContribution;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Synthetic items: herb 1 is cleaned into 2, which with secondary 9 makes potion 3.
 */
public class RowViewFactoryTest {
    private static final double DELTA = 0.0001;

    private static final int HERB = 1;
    private static final int CLEAN = 2;
    // a real potion, since only potions have their "(1)" taken off
    private static final int POTION = ItemID._1DOSE1ATTACK;
    private static final int SECONDARY = 9;
    private static final int OTHER_SECONDARY = 8;
    // two kinds of nest, which the game names alike, and a scale - all precursors
    private static final int NEST = 7;
    private static final int SEED_NEST = 6;
    private static final int SCALE = 5;

    private static final Map<Integer, String> NAMES = Map.of(
            HERB, "Grimy herb", CLEAN, "Clean herb", POTION, "Potion(1)",
            SECONDARY, "Eye of newt", OTHER_SECONDARY, "Snape grass",
            NEST, "Bird nest", SEED_NEST, "Bird nest", SCALE, "Lava scale");

    private static final Recipe CLEANING = new Recipe(10, new Ingredient(HERB, 1), new Ingredient[0],
            new Ingredient(CLEAN, 1), RecipeTag.MISC, 2.5f, 1);
    private static final Recipe MIXING = new Recipe(11, new Ingredient(CLEAN, 1),
            new Ingredient[]{new Ingredient(SECONDARY, 1)}, new Ingredient(POTION, 1), RecipeTag.MISC, 25f, 1);

    private final RowViewFactory factory = new RowViewFactory(NAMES::get, Map.of(HERB, List.of(CLEANING, MIXING)));

    /**
     * Steps in the order they happen, whatever order they arrive in, then the rest by level, lowest
     * first. The steps' own levels do not move them.
     */
    @Test
    public void optionsAreGroupedByStepThenTheRestByLevel() {
        final List<Recipe> shuffled = List.of(
                recipeMaking(10, RecipeTag.POTION, 60),
                recipeMaking(11, RecipeTag.UNF, 50),
                recipeMaking(12, RecipeTag.CAVIAR, 70),
                recipeMaking(13, RecipeTag.DEGRIME, 48),
                recipeMaking(14, RecipeTag.SEED, 90),
                recipeMaking(15, RecipeTag.POTION, 40));

        final RowViewFactory factory = new RowViewFactory(itemId -> "item " + itemId, Map.of(HERB, shuffled));
        final RowView row = factory.rowOf(new ChainResult(HERB, POTION,
                List.of(new ChainItemXp(HERB, 10, List.of(), 0)), List.of(), 0, ItemQuantities.EMPTY, 0));

        assertEquals(List.of(HERB, 14, 13, 11, 15, 10, 12), row.getProductOptions().stream()
                .map(RowView.ProductOption::getItemId)
                .collect(Collectors.toList()));
    }

    @Test
    public void makingNothingIsOfferedFirstAsTheRootItself() {
        final List<RowView.ProductOption> options = factory.rowOf(potionRow()).getProductOptions();

        assertEquals(new RowView.ProductOption(HERB, RowViewFactory.MAKE_NOTHING), options.get(0));
        assertEquals(List.of("Make nothing", "Clean herb", "Potion"), options.stream()
                .map(RowView.ProductOption::getName)
                .collect(Collectors.toList()));
    }

    @Test
    public void aRowEndingAtItsRootMakesNothing() {
        final RowView row = factory.rowOf(new ChainResult(HERB, HERB,
                List.of(new ChainItemXp(HERB, 10, List.of(), 0)), List.of(), 0, ItemQuantities.EMPTY, 0));

        assertTrue(row.isSwitchedOff());
        assertEquals("Make nothing", row.getProductName());
    }

    @Test
    public void potionsLoseTheirOneDoseSuffix() {
        assertEquals("Potion", factory.rowOf(potionRow()).getProductName());
    }

    @Test
    public void theRootQuantityIsWhatIsHeldOfTheRootAlone() {
        assertEquals(10, factory.rowOf(potionRow()).getRootQuantity(), DELTA);
    }

    /**
     * Banked potions are already made - held, but worth nothing more.
     */
    @Test
    public void itemsWorthNothingAreLeftOutOfTheBreakdownByItem() {
        assertEquals(List.of("10 × Grimy herb", "4 × Clean herb"), factory.rowOf(potionRow()).getXpByItem().stream()
                .map(RowView.Line::getName)
                .collect(Collectors.toList()));
    }

    @Test
    public void aRecipeLineNamesItsInputAndOutput() {
        assertEquals(new RowView.RecipeLine(350, "Clean herb", "Potion"),
                factory.rowOf(potionRow()).getXpByRecipe().get(1));
    }

    @Test
    public void secondaryDemandIsRoundedUpMostNeededFirst() {
        assertEquals(List.of(new RowView.Line(14, "Eye of newt"), new RowView.Line(1, "Snape grass")),
                factory.rowOf(potionRow()).getSecondaryDemand());
    }

    @Test
    public void theSecondaryBalanceListsShortfallsFirst() {
        final SecondaryBalance balance = new SecondaryBalance(List.of(
                entry(SECONDARY, 5, 10, List.of(), List.of()),
                entry(OTHER_SECONDARY, 7, 4, List.of(), List.of())));

        final List<SecondaryView> secondaries = factory.secondariesOf(balance);

        assertEquals(List.of("Snape grass", "Eye of newt"),
                secondaries.stream().map(SecondaryView::getName).collect(Collectors.toList()));
        assertEquals(-3, secondaries.get(0).getNet(), DELTA);
        assertEquals(5, secondaries.get(1).getNet(), DELTA);
    }

    @Test
    public void aSecondaryWithoutPrecursorsBreaksDownIntoNeededAndHeld() {
        final SecondaryBalance balance = new SecondaryBalance(List.of(
                entry(SECONDARY, 5, 10, List.of(), List.of())));

        assertEquals(List.of(new RowView.Line(5, RowViewFactory.NEEDED), new RowView.Line(10, RowViewFactory.HELD)),
                factory.secondariesOf(balance).get(0).getBreakdown());
    }

    @Test
    public void precursorsAreListedByNameMostMadeFirst() {
        // 3 + 4 nests make 7; 2 scales make 9
        final SecondaryBalance balance = new SecondaryBalance(List.of(entry(SECONDARY, 30, 1, List.of(), List.of(
                new PrecursorContribution(NEST, 3, 3),
                new PrecursorContribution(SEED_NEST, 4, 4),
                new PrecursorContribution(SCALE, 2, 9)))));

        final SecondaryView secondary = factory.secondariesOf(balance).get(0);

        assertEquals(List.of(
                        new RowView.Line(30, RowViewFactory.NEEDED),
                        new RowView.Line(1, RowViewFactory.HELD),
                        new RowView.Line(9, "2 × Lava scale"),
                        new RowView.Line(7, "7 × Bird nest")),
                secondary.getBreakdown());
        assertEquals(-13, secondary.getNet(), DELTA);
    }

    @Test
    public void rowsMakingASecondaryAreListedByTitleBeforePrecursors() {
        // the herb row makes 6, the clean herb row 2, and one nest makes 1
        final SecondaryBalance balance = new SecondaryBalance(List.of(entry(SECONDARY, 20, 0,
                List.of(new ChainContribution(CLEAN, 2), new ChainContribution(HERB, 6)),
                List.of(new PrecursorContribution(NEST, 1, 1)))));

        final SecondaryView secondary = factory.secondariesOf(balance).get(0);

        assertEquals(List.of(
                        new RowView.Line(20, RowViewFactory.NEEDED),
                        new RowView.Line(0, RowViewFactory.HELD),
                        new RowView.Line(6, "Grimy herb"),
                        new RowView.Line(2, "Clean herb"),
                        new RowView.Line(1, "1 × Bird nest")),
                secondary.getBreakdown());
        assertEquals(-11, secondary.getNet(), DELTA);
    }

    private static SecondaryBalance.BalanceEntry entry(int secondaryItemId, double demanded, double held,
                                                       List<ChainContribution> chains,
                                                       List<PrecursorContribution> precursors) {
        return new SecondaryBalance.BalanceEntry(secondaryItemId, demanded, held, chains, precursors);
    }

    /**
     * 10 grimy herbs, 4 clean and 2 potions held, going to potions.
     */
    private static ChainResult potionRow() {
        return new ChainResult(HERB, POTION,
                List.of(
                        new ChainItemXp(HERB, 10, List.of(), 275),
                        new ChainItemXp(CLEAN, 4, List.of(), 100),
                        new ChainItemXp(POTION, 2, List.of(), 0)),
                List.of(new ChainRecipeXp(CLEANING, 25), new ChainRecipeXp(MIXING, 350)),
                14,
                ItemQuantities.of(Map.of(SECONDARY, 13.2, OTHER_SECONDARY, 0.4)),
                375);
    }

    /**
     * A recipe making the given item, tagged and levelled as the menu sorts by. What it takes is
     * irrelevant here.
     */
    private static Recipe recipeMaking(int productItemId, RecipeTag tag, int level) {
        return new Recipe(productItemId, new Ingredient(HERB, 1), new Ingredient[0],
                new Ingredient(productItemId, 1), tag, 0f, level);
    }
}
