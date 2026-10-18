package adamski.ui;

import adamski.domain.result.ChainItemXp;
import adamski.domain.result.ChainRecipeXp;
import adamski.domain.result.ChainResult;
import adamski.domain.Ingredient;
import adamski.domain.ItemQuantities;
import adamski.domain.Recipe;
import adamski.domain.yield.SecondaryBalance;
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
    private static final int POTION = 3;
    private static final int SECONDARY = 9;
    private static final int OTHER_SECONDARY = 8;

    private static final Map<Integer, String> NAMES = Map.of(
            HERB, "Grimy herb", CLEAN, "Clean herb", POTION, "Potion(1)",
            SECONDARY, "Eye of newt", OTHER_SECONDARY, "Snape grass");

    private static final Recipe CLEANING = new Recipe(10, new Ingredient(HERB, 1), new Ingredient[0],
            new Ingredient(CLEAN, 1), "test", 2.5f, 1);
    private static final Recipe MIXING = new Recipe(11, new Ingredient(CLEAN, 1),
            new Ingredient[]{new Ingredient(SECONDARY, 1)}, new Ingredient(POTION, 1), "test", 25f, 1);

    private final RowViewFactory factory = new RowViewFactory(NAMES::get, Map.of(HERB, List.of(CLEANING, MIXING)));

    /**
     * Steps in the order they happen, whatever order they arrive in, then the rest by level, lowest
     * first. The steps' own levels do not move them.
     */
    @Test
    public void optionsAreGroupedByStepThenTheRestByLevel() {
        final List<Recipe> shuffled = List.of(
                recipeMaking(10, "potion", 60),
                recipeMaking(11, "unf", 50),
                recipeMaking(12, "caviar", 70),
                recipeMaking(13, "degrime", 48),
                recipeMaking(14, "seed", 90),
                recipeMaking(15, "potion", 40));

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
        final SecondaryBalance balance = new SecondaryBalance(ItemQuantities.EMPTY,
                ItemQuantities.of(Map.of(SECONDARY, 5.0, OTHER_SECONDARY, -3.0)));

        assertEquals(List.of(new RowView.Line(-3, "Snape grass"), new RowView.Line(5, "Eye of newt")),
                factory.secondaryLinesOf(balance));
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
    private static Recipe recipeMaking(int productItemId, String tag, int level) {
        return new Recipe(productItemId, new Ingredient(HERB, 1), new Ingredient[0],
                new Ingredient(productItemId, 1), tag, 0f, level);
    }
}
