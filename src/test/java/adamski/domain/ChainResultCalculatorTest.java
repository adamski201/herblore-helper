package adamski.domain;

import adamski.data.Recipes;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Synthetic recipes: 1 -> 2 -> 3 -> 4 is one chain, and a stranded 6 -> 7 is another. Every recipe
 * is worth 1xp a run.
 */
public class ChainResultCalculatorTest {
    private static final Recipe ONE_TO_TWO = recipe(10, 1, 2);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 3);
    private static final Recipe THREE_TO_FOUR = recipe(12, 3, 4);
    private static final Recipe SIX_TO_SEVEN = recipe(14, 6, 7);

    private static final RecipeChain CHAIN_A =
            new RecipeChain(Arrays.asList(ONE_TO_TWO, TWO_TO_THREE, THREE_TO_FOUR));
    private static final RecipeChain CHAIN_B =
            new RecipeChain(Collections.singletonList(SIX_TO_SEVEN));

    @Test
    public void nothingBankedIsNoGroups() {
        assertTrue(ChainResultCalculator.calculate(List.of(CHAIN_A), ItemQuantities.EMPTY).isEmpty());
    }

    @Test
    public void aChainNothingIsBankedAgainstIsLeftOut() {
        assertEquals(1, calculate(owned(1, 8)).size());
    }

    @Test
    public void twoBankedItemsOnOneChainCollapseIntoOneGroup() {
        final List<ChainResult> results = calculate(owned(1, 8, 3, 5));

        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getItemContributions().size());
        assertEquals(1, results.get(0).getEntryItemId());
        assertEquals(4, results.get(0).getProductItemId());
    }

    @Test
    public void itemContributionsComeBackLeastMatureFirst() {
        final List<ChainResult> results = calculate(owned(3, 1, 2, 1, 1, 1));

        assertEquals(List.of(1, 2, 3), results.get(0).getItemContributions().stream()
                .map(ChainItemXp::getEntryItemId)
                .collect(Collectors.toList()));
    }

    @Test
    public void aStrandedItemIsItsOwnGroup() {
        final List<ChainResult> results = calculate(owned(1, 2, 6, 3));

        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getEntryItemId());
        assertEquals(6, results.get(1).getEntryItemId());
    }

    @Test
    public void itemContributionQuantityIsWhatIsHeld() {
        assertEquals(6.0, calculate(owned(1, 6)).get(0).getItemContributions().get(0).getQuantity(), 0.0001);
    }

    @Test
    public void itemContributionXpIsTheFoldOfItsOwnRuns() {
        final List<ChainResult> results = calculate(owned(1, 8, 3, 5));

        assertEquals(24.0, results.get(0).getItemContributions().get(0).getXp(), 0.0001); // three recipes
        assertEquals(5.0, results.get(0).getItemContributions().get(1).getXp(), 0.0001);  // joins at the last
        assertEquals(29.0, results.get(0).getXp(), 0.0001);
    }

    @Test
    public void aStepSumsOneRecipeAcrossEveryItemThatReachesIt() {
        final Map<Integer, Double> xpByRecipe = calculate(owned(1, 8, 3, 5)).get(0).getRecipeContributions().stream()
                .collect(Collectors.toMap(step -> step.getRecipe().getId(), ChainRecipeXp::getXp));

        assertEquals(8.0, xpByRecipe.get(10), 0.0001);
        assertEquals(13.0, xpByRecipe.get(12), 0.0001); // 8 from item 1, 5 from item 3
    }

    @Test
    public void recipeContributionsComeBackInChainOrder() {
        assertEquals(List.of(10, 11, 12), calculate(owned(3, 1, 1, 1)).get(0).getRecipeContributions().stream()
                .map(step -> step.getRecipe().getId())
                .collect(Collectors.toList()));
    }

    @Test
    public void aStepWorthNoXpIsLeftOut() {
        final Recipe freeStep = new Recipe(11, new Ingredient(2, 1), new Ingredient[0],
                new Ingredient(3, 1), "test", 0f, 0);

        final RecipeChain chain = new RecipeChain(Arrays.asList(freeStep, THREE_TO_FOUR));

        final List<ChainResult> results = ChainResultCalculator.calculate(List.of(chain), owned(2, 4));

        assertEquals(List.of(12), results.get(0).getRecipeContributions().stream()
                .map(step -> step.getRecipe().getId())
                .collect(Collectors.toList()));
    }

    @Test
    public void outputQuantityCountsOnlyTheProduct() {
        final Recipe threeToFourTriples = new Recipe(12, new Ingredient(3, 1), new Ingredient[0],
                new Ingredient(4, 3), "test", 1f, 0);

        final RecipeChain chain = new RecipeChain(Collections.singletonList(threeToFourTriples));

        final List<ChainResult> results = ChainResultCalculator.calculate(List.of(chain), owned(3, 5));

        assertEquals(15.0, results.get(0).getOutputQuantity(), 0.0001);
    }

    @Test
    public void secondaryDemandCoversOnlyTheGroupsOwnRuns() {
        final Recipe needsTwoOfItem50 = new Recipe(12, new Ingredient(3, 1),
                new Ingredient[]{new Ingredient(50, 2)}, new Ingredient(4, 1), "test", 1f, 0);

        final RecipeChain chain = new RecipeChain(Collections.singletonList(needsTwoOfItem50));

        final List<ChainResult> results = ChainResultCalculator.calculate(List.of(chain, CHAIN_B), owned(3, 4, 6, 9));

        assertEquals(2, results.size());
        assertEquals(8.0, results.get(0).getSecondaryDemand().get(50), 0.0001);
        assertTrue(results.get(1).getSecondaryDemand().isEmpty());
    }

    /**
     * Grouping splits the work up per chain, so the xp it totals has to match one flat cascade over
     * every recipe at once - nothing lost between chains, nothing counted twice.
     */
    @Test
    public void groupingPerChainLosesNoXpAgainstOneFlatCascade() {
        final Map<Integer, Integer> bank = new HashMap<>();
        bank.put(ItemID.UNIDENTIFIED_RANARR, 40);
        bank.put(ItemID.RANARRVIAL, 25);
        bank.put(ItemID.BRUT_CAVIAR, 12);
        bank.put(ItemID.HARRALANDER_SEED, 3);

        final ItemQuantities owned = ItemQuantities.counted(bank);

        final List<RecipeChain> chains = new RecipeChainCalculator(new RecipeGraph(Recipes.all()))
                .calculate(owned, Collections.emptyMap());

        final List<Recipe> everyRecipe = chains.stream()
                .flatMap(chain -> chain.getRecipes().stream())
                .collect(Collectors.toList());

        final List<RecipeRun> ungrouped = new ArrayList<>();
        owned.forEach((itemId, quantity) ->
                ungrouped.addAll(RecipeYieldCalculator.cascade(itemId, quantity, everyRecipe)));

        final double grouped = ChainResultCalculator.calculate(chains, owned).stream()
                .mapToDouble(ChainResult::getXp)
                .sum();

        assertEquals(BankedXpCalculator.calculate(ungrouped).getTotal(), grouped, 0.0001);
    }

    private static List<ChainResult> calculate(ItemQuantities owned) {
        return ChainResultCalculator.calculate(List.of(CHAIN_A, CHAIN_B), owned);
    }

    private static ItemQuantities owned(int... pairs) {
        final Map<Integer, Integer> items = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            items.put(pairs[i], pairs[i + 1]);
        }
        return ItemQuantities.counted(items);
    }

    private static Recipe recipe(int id, int primary, int output) {
        return new Recipe(id, new Ingredient(primary, 1), new Ingredient[0],
                new Ingredient(output, 1), "test", 1f, 0);
    }
}
