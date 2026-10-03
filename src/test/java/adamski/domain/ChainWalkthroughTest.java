package adamski.domain;

import adamski.data.Recipes;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * A hands-on walkthrough of the pipeline on one small slice of the table.
 * <p>
 * Not a behaviour test - it exists to be read, run and stepped through. Each method is one stage
 * and runs on its own, so a breakpoint on the call lets you step into the real code.
 * <p>
 * Change {@link #SUBSET} and {@link #OWNED} to look at a different slice.
 */
public class ChainWalkthroughTest {

    /**
     * The only knob. Recipe ids from Recipes.java - everything else follows from these.
     * <p>
     * Currently the antifire diamond: r93 super antifire + lava shard, and r136 extended antifire
     * x4 + crushed dragon bones. Two separate routes converging on extended super antifire, which
     * is the one place in the table where a chain cannot be named by the product it makes.
     * <p>
     * The avantoe slice, for comparison: {@code List.of(125, 8, 23, 50, 51, 52, 87, 89)} - r125
     * seed to grimy, r8 degrime, r23 unf, then r50 fishing / r51 super energy / r52 hunter, and
     * r87 super energy to stamina, r89 stamina to extended stamina.
     */
    private static final List<Integer> SUBSET = List.of(93, 136);

    /**
     * 20 doses each of extended antifire and super antifire - the two siblings of the diamond.
     * <p>
     * For the avantoe slice: {@code AVANTOE_SEED, 10, UNIDENTIFIED_AVANTOE, 20, _1DOSE2ENERGY, 400}.
     */
    private static final ItemQuantities OWNED = owned(
            ItemID._1DOSE2ANTIDRAGON, 20,
            ItemID._1DOSE3ANTIDRAGON, 20);

    private static final List<Recipe> TABLE = Recipes.all().stream()
            .filter(recipe -> SUBSET.contains(recipe.getId()))
            .collect(Collectors.toList());

    private static final RecipeGraph GRAPH = new RecipeGraph(TABLE);

    // ------------------------------------------------------------- stage 1

    /**
     * RecipeGraph turns the table into edges. Breakpoint its constructor to watch sortByDependency
     * hand out the maturity indices.
     */
    @Test
    public void stage1_theGraph() {
        heading("STAGE 1 - the graph");

        for (Recipe recipe : TABLE) {
            System.out.println("  r" + recipe.getId() + " [" + recipe.getTag() + "] "
                    + name(recipe.getPrimary().getItemId()) + " x" + recipe.getPrimary().getQuantity()
                    + "  ->  " + name(recipe.getOutput().getItemId()) + " x" + recipe.getOutput().getQuantity()
                    + "   " + recipe.getXp() + "xp");
        }

        System.out.println("\n  what each owned item can do:");
        for (Integer itemId : sortedByMaturity()) {
            System.out.println("    " + name(itemId));
            System.out.println("       maturity       = " + GRAPH.maturityOf(itemId));
            System.out.println("       options        = " + GRAPH.recipeOptionsFor(itemId).stream()
                    .map(r -> "r" + r.getId() + " to " + name(r.getOutput().getItemId()))
                    .collect(Collectors.joining(", ")));
            System.out.println("       default route  = " + GRAPH.findRoute(itemId, RecipeSelection.ALL_DEFAULT).stream()
                    .map(r -> "r" + r.getId() + " to " + name(r.getOutput().getItemId()))
                    .collect(Collectors.joining(", ")));
            System.out.println("       picker offers  = " + GRAPH.findItemsReachableFrom(itemId).stream()
                    .map(ChainWalkthroughTest::name).collect(Collectors.joining(", ")));
        }
    }

    // ------------------------------------------------------------- stage 2

    /**
     * RecipeChainResolver.resolve - step in and watch `claimed` fill up.
     * <p>
     * On the antifire slice neither sibling is on the other's route, so both root a chain even
     * though the second one's product is already claimed. On the avantoe slice the interesting
     * moment is the opposite: grimy avantoe is skipped because the seed's route claimed it.
     */
    @Test
    public void stage2_planTheChains() {
        heading("STAGE 2 - plan the chains");

        RecipeSelection selection = RecipeSelection.ALL_DEFAULT;
        // Pin a chain to a product other than its default, e.g. on the avantoe slice:
        // selection = RecipeSelection.of(Map.of(ItemID.AVANTOEVIAL, 51));

        final List<RecipeChain> chains = new RecipeChainResolver(GRAPH).resolve(OWNED, selection);

        for (RecipeChain chain : chains) {
            System.out.println("  chain: " + name(chain.getRootItemId())
                    + " -> " + name(chain.getProductItemId()));

            for (Recipe recipe : chain.getRecipes()) {
                System.out.println("      r" + recipe.getId() + "  " + name(recipe.getPrimary().getItemId())
                        + " -> " + name(recipe.getOutput().getItemId()));
            }
        }

        assertEquals("neither sibling can reach the other, so each roots its own chain",
                2, chains.size());
    }

    // ------------------------------------------------------------- stage 3

    /**
     * RecipeYieldCalculator.cascade, for ONE owned item at a time against ONE chain. Watch the
     * `available` map inside cascade carrying each recipe's output into the next.
     * <p>
     * Note r136 takes its primary x4, so 20 doses is 5 runs rather than 20.
     */
    @Test
    public void stage3_cascadeOneItem() {
        heading("STAGE 3 - cascade each owned item through each chain");

        for (RecipeChain chain : new RecipeChainResolver(GRAPH).resolve(OWNED, RecipeSelection.ALL_DEFAULT)) {
            System.out.println("  chain: " + name(chain.getRootItemId())
                    + " -> " + name(chain.getProductItemId()));

            for (Integer itemId : sortedByMaturity()) {
                final List<RecipeRun> runs =
                        RecipeYieldCalculator.cascade(itemId, OWNED.get(itemId), chain.getRecipes());

                if (runs.isEmpty()) {
                    System.out.println("      " + name(itemId) + " contributes nothing to this chain");
                    continue;
                }

                System.out.println("      starting from " + OWNED.get(itemId) + " x " + name(itemId));
                for (RecipeRun run : runs) {
                    System.out.println("          r" + run.getRecipe().getId()
                            + "  runs=" + run.getRuns()
                            + "  makes=" + run.getOutputQuantity()
                            + " " + name(run.getRecipe().getOutput().getItemId())
                            + "  xp=" + (run.getRuns() * run.getRecipe().getXp()));
                }
            }
        }
    }

    // ------------------------------------------------------------- stage 4

    /**
     * The two projections of one RecipeRun list - by owned item, and by recipe. Both sum to the
     * chain's xp.
     */
    @Test
    public void stage4_theWholeResult() {
        heading("STAGE 4 - chain results");

        final List<RecipeChain> chains = new RecipeChainResolver(GRAPH).resolve(OWNED, RecipeSelection.ALL_DEFAULT);

        for (ChainResult result : ChainResultCalculator.calculate(chains, OWNED)) {
            System.out.println("  " + name(result.getRootItemId()) + " -> " + name(result.getProductItemId())
                    + "   xp=" + result.getXp()
                    + "   makes=" + result.getOutputQuantity() + " doses");

            System.out.println("     by item:");
            for (ChainItemXp item : result.getItemContributions()) {
                System.out.println("        " + item.getQuantity() + " x " + name(item.getItemId())
                        + "   ->   " + item.getXp() + "xp");
            }

            System.out.println("     by recipe:");
            for (ChainRecipeXp recipe : result.getRecipeContributions()) {
                System.out.println("        r" + recipe.getRecipe().getId() + "   ->   " + recipe.getXp() + "xp");
            }

            System.out.println("     secondaries needed:");
            result.getSecondaryDemand().forEach((id, qty) ->
                    System.out.println("        " + qty + " x " + name(id)));
        }
    }

    // ------------------------------------------------------------- helpers

    private static List<Integer> sortedByMaturity() {
        return OWNED.itemIds().stream()
                .sorted(Comparator.comparingInt(GRAPH::maturityOf))
                .collect(Collectors.toList());
    }

    private static ItemQuantities owned(int... pairs) {
        final Map<Integer, Integer> items = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) items.put(pairs[i], pairs[i + 1]);

        return ItemQuantities.counted(items);
    }

    private static void heading(String text) {
        System.out.println("\n===== " + text + " =====");
    }

    /**
     * ItemManager lives in the client, so the handful of names in play are spelled out here.
     * Both slices are listed so switching SUBSET does not need this touched.
     */
    private static final Map<Integer, String> NAMES = new LinkedHashMap<>();

    static {
        // antifire
        NAMES.put(ItemID._1DOSE1ANTIDRAGON, "antifire(1)");
        NAMES.put(ItemID._1DOSE2ANTIDRAGON, "extended antifire(1)");
        NAMES.put(ItemID._1DOSE3ANTIDRAGON, "super antifire(1)");
        NAMES.put(ItemID._1DOSE4ANTIDRAGON, "extended super antifire(1)");
        NAMES.put(ItemID.LAVA_SHARD, "lava shard");
        NAMES.put(ItemID.CRUSHED_DRAGON_BONES, "crushed dragon bones");

        // avantoe
        NAMES.put(ItemID.AVANTOE_SEED, "avantoe seed");
        NAMES.put(ItemID.UNIDENTIFIED_AVANTOE, "grimy avantoe");
        NAMES.put(ItemID.AVANTOE, "avantoe");
        NAMES.put(ItemID.AVANTOEVIAL, "avantoe unf");
        NAMES.put(ItemID._1DOSEFISHERSPOTION, "fishing potion(1)");
        NAMES.put(ItemID._1DOSE2ENERGY, "super energy(1)");
        NAMES.put(ItemID._1DOSEHUNTING, "hunter potion(1)");
        NAMES.put(ItemID._1DOSESTAMINA, "stamina(1)");
        NAMES.put(ItemID._1DOSE2STAMINA, "extended stamina(1)");
        NAMES.put(ItemID.VIAL_WATER, "vial of water");
        NAMES.put(ItemID.SNAPE_GRASS, "snape grass");
        NAMES.put(ItemID.MORTMYREMUSHROOM, "mort myre fungus");
        NAMES.put(ItemID.HUNTINGBEAST_SABRETEETH_DUST, "sabre teeth dust");
        NAMES.put(ItemID.AMYLASE, "amylase crystal");
        NAMES.put(ItemID.MARLIN_SCALES, "marlin scales");
    }

    private static String name(int itemId) {
        return NAMES.getOrDefault(itemId, "item " + itemId);
    }
}
