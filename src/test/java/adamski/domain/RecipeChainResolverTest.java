package adamski.domain;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Synthetic recipes: 1 -> 2 -> 3 -> 4, with 3 also able to make 5, and a stranded 6 -> 7.
 */
public class RecipeChainResolverTest {
    private static final Recipe ONE_TO_TWO = recipe(10, 1, 2);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 3);
    private static final Recipe THREE_TO_FOUR = recipe(12, 3, 4);
    private static final Recipe THREE_TO_FIVE = recipe(13, 3, 5);
    private static final Recipe SIX_TO_SEVEN = recipe(14, 6, 7);

    private static final RecipeGraph GRAPH = new RecipeGraph(
            Arrays.asList(ONE_TO_TWO, TWO_TO_THREE, THREE_TO_FOUR, THREE_TO_FIVE, SIX_TO_SEVEN));

    private static final RecipeChainResolver RESOLVER = new RecipeChainResolver(GRAPH);

    @Test
    public void oneBankedItemIsOneChain() {
        final List<RecipeChain> chains = calculate(owned(1, 10));

        assertEquals(1, chains.size());
        assertEquals(1, chains.get(0).getRootItemId());
        assertEquals(4, chains.get(0).getProductItemId()); // first option all the way down
    }

    @Test
    public void anythingAlongTheChainJoinsIt() {
        final List<RecipeChain> chains = calculate(owned(1, 10, 2, 5, 3, 8));

        assertEquals(1, chains.size());
        assertEquals(1, chains.get(0).getRootItemId());
    }

    @Test
    public void theLeastMatureItemRootsTheChain() {
        assertEquals(1, calculate(owned(3, 8, 1, 10)).get(0).getRootItemId());
    }

    @Test
    public void anItemThatCannotReachTheProductRootsItsOwnChain() {
        final List<RecipeChain> chains = calculate(owned(1, 10, 6, 4));

        assertEquals(2, chains.size());
        assertEquals(List.of(1, 6), chains.stream()
                .map(RecipeChain::getRootItemId)
                .collect(Collectors.toList()));
    }

    @Test
    public void anItemDownstreamOfTheChosenProductStrandsIntoItsOwnChain() {
        // stop at item 2
    final List<RecipeChain> chains = RESOLVER.resolve(owned(1, 10, 3, 5), stopAt(2));

        assertEquals(2, chains.size());
        assertEquals(2, chains.get(0).getProductItemId());
        assertEquals(3, chains.get(1).getRootItemId());
    }

    @Test
    public void aChainCutWhereItsRootStandsProducesNothing() {
        // the pile of item 1 has been used up, so the cut now lands on the item rooting the chain
        assertTrue(RESOLVER.resolve(owned(2, 10), stopAt(2)).isEmpty());
    }

    @Test
    public void anItemBelowACutThatHasBeenReachedRootsItsOwnChain() {
        // the chain is cut at item 2 and the pile of item 1 is gone, so item 3 is left to itself
        final List<RecipeChain> chains = RESOLVER.resolve(owned(2, 10, 3, 5), stopAt(2));

        assertEquals(1, chains.size());
        assertEquals(3, chains.get(0).getRootItemId());
        assertEquals(4, chains.get(0).getProductItemId());
        assertEquals(List.of(12), chains.get(0).getRecipes().stream()
                .map(Recipe::getId)
                .collect(Collectors.toList()));
    }

    @Test
    public void anItemThatMakesNothingIsNoChain() {
        assertTrue(calculate(owned(4, 100)).isEmpty());
    }

    @Test
    public void choosingTheProductChoosesTheChain() {
        final List<RecipeChain> chains = RESOLVER.resolve(owned(1, 10), choose(3, 13));

        assertEquals(List.of(10, 11, 13), chains.get(0).getRecipes().stream()
                .map(Recipe::getId)
                .collect(Collectors.toList()));
    }

    @Test
    public void theChainsTogetherCoverEveryRecipe() {
        final List<Recipe> recipes = recipesOf(calculate(owned(1, 10, 6, 4)));

        assertEquals(Set.of(10, 11, 12, 14), recipes.stream()
                .map(Recipe::getId)
                .collect(Collectors.toSet()));
    }

    @Test
    public void chainOrderRunsProducersBeforeConsumers() {
        final List<Recipe> recipes = recipesOf(calculate(owned(1, 10, 6, 4)));

        for (int earlier = 0; earlier < recipes.size(); earlier++) {
            for (int later = earlier + 1; later < recipes.size(); later++) {
                assertNotEquals("a consumer runs before its producer",
                        recipes.get(later).getOutput().getItemId(),
                        recipes.get(earlier).getPrimary().getItemId());
            }
        }
    }

    @Test
    public void noPrimaryIsClaimedTwice() {
        final List<Recipe> recipes = recipesOf(calculate(owned(1, 10, 2, 5, 3, 8)));

        assertEquals(recipes.size(), recipes.stream()
                .map(recipe -> recipe.getPrimary().getItemId())
                .distinct()
                .count());
    }

    /**
     * What RecipeYieldCalculator does with the chains it is given.
     */
    private static List<Recipe> recipesOf(List<RecipeChain> chains) {
        return chains.stream()
                .flatMap(chain -> chain.getRecipes().stream())
                .collect(Collectors.toList());
    }

    private static List<RecipeChain> calculate(ItemQuantities owned) {
        return RESOLVER.resolve(owned, RecipeSelection.ALL_DEFAULT);
    }

    /**
     * One item told to make nothing, so the chain ends there and anything below it is left over.
     */
    private static RecipeSelection stopAt(int itemId) {
        return RecipeSelection.of(Map.of(itemId, RecipeSelection.STOP));
    }

    /**
     * One item told to take a recipe other than the first the table offers.
     */
    private static RecipeSelection choose(int itemId, int recipeId) {
        return RecipeSelection.of(Map.of(itemId, recipeId));
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
