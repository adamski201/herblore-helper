package adamski.ui;

import adamski.domain.result.ChainItemXp;
import adamski.domain.result.ChainRecipeXp;
import adamski.domain.result.ChainResult;
import adamski.domain.ItemQuantities;
import adamski.domain.Recipe;
import adamski.domain.balance.SecondaryBalance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns results into what the panel shows, every name resolved. Names come from the function it is
 * given - ItemManager in the client, a lookup table in tests - so this runs without a client, like
 * the domain beneath it.
 * <p>
 * Called on the client thread, since that is where ItemManager can resolve names.
 */
final class RowViewFactory {
    static final String MAKE_NOTHING = "Make nothing";
    static final String NEEDED = "Needed";
    static final String HELD = "Held";

    /**
     * Everything is counted in single doses, so every potion is named as its 1-dose item. The suffix
     * says nothing the sprite beside it does not.
     */
    private static final Pattern ONE_DOSE_SUFFIX = Pattern.compile("\\s*\\(1\\)$");

    /**
     * Recipe tags for the steps on the way to a potion, in the order they happen.
     */
    private static final List<String> STEP_ORDER = List.of("seed", "degrime", "unf");

    private final IntFunction<String> nameOf;

    /**
     * For each item, the recipe making each product it could end at. Depends only on the recipe
     * table, so it is given once rather than carried on every result.
     */
    private final Map<Integer, List<Recipe>> productRecipesByItem;

    RowViewFactory(IntFunction<String> nameOf, Map<Integer, List<Recipe>> productRecipesByItem) {
        this.nameOf = itemId -> ONE_DOSE_SUFFIX.matcher(nameOf.apply(itemId)).replaceFirst("");
        this.productRecipesByItem = productRecipesByItem;
    }

    RowView rowOf(ChainResult chain) {
        final int rootItemId = chain.getRootItemId();
        final List<RowView.ProductOption> options = new ArrayList<>();

        // Picking the root as its own product is how the app is told to make nothing - so the
        // option carries the root, and a row already switched off has it selected
        options.add(new RowView.ProductOption(rootItemId, MAKE_NOTHING));
        for (Recipe recipe : inMenuOrder(productRecipesByItem.getOrDefault(rootItemId, List.of()))) {
            final int productItemId = recipe.getOutput().getItemId();
            options.add(new RowView.ProductOption(productItemId, nameOf.apply(productItemId)));
        }

        // a stopped row's root is listed as held, but contributes nothing worth a line
        final List<RowView.Line> xpByItem = new ArrayList<>();
        for (ChainItemXp contribution : chain.getItemContributions()) {
            if (contribution.getXp() != 0) {
                xpByItem.add(new RowView.Line(contribution.getXp(),
                        Amounts.abbreviate(contribution.getQuantity()) + " × " + nameOf.apply(contribution.getItemId())));
            }
        }

        final List<RowView.RecipeLine> xpByRecipe = new ArrayList<>();
        for (ChainRecipeXp contribution : chain.getRecipeContributions()) {
            xpByRecipe.add(new RowView.RecipeLine(contribution.getXp(),
                    nameOf.apply(contribution.getRecipe().getPrimary().getItemId()),
                    nameOf.apply(contribution.getRecipe().getOutput().getItemId())));
        }

        final int productItemId = chain.getProductItemId();

        return new RowView(
                rootItemId,
                nameOf.apply(rootItemId),
                chain.getRootQuantity(),
                chain.getXp(),
                productItemId,
                chain.makesNothing(),
                chain.makesNothing() ? MAKE_NOTHING : nameOf.apply(productItemId),
                chain.getOutputQuantity(),
                options,
                xpByItem,
                xpByRecipe,
                secondaryDemandLinesOf(chain.getSecondaryDemand()));
    }

    /**
     * The steps before a potion first, in the order they happen - seed to herb, degrime, unf - then
     * everything else in the order it unlocks, lowest level first. Ties keep the order the graph
     * found them in.
     */
    private static List<Recipe> inMenuOrder(List<Recipe> productRecipes) {
        final List<Recipe> ordered = new ArrayList<>(productRecipes);
        ordered.sort(Comparator
                .comparingInt(RowViewFactory::stepRankOf)
                .thenComparingInt(recipe -> isStep(recipe) ? 0 : recipe.getLevel()));
        return ordered;
    }

    /**
     * Where a step falls among the steps, with everything that is not one after them all.
     */
    private static int stepRankOf(Recipe recipe) {
        return isStep(recipe) ? STEP_ORDER.indexOf(recipe.getTag()) : STEP_ORDER.size();
    }

    private static boolean isStep(Recipe recipe) {
        return STEP_ORDER.contains(recipe.getTag());
    }

    /**
     * The panel-wide balance: shortfalls first, as negatives, then what is spare.
     */
    List<SecondaryView> secondariesOf(SecondaryBalance balance) {
        return balance.getEntries().stream()
                .sorted(Comparator.comparingDouble(SecondaryBalance.BalanceEntry::net))
                .map(this::secondaryOf)
                .collect(Collectors.toList());
    }

    private SecondaryView secondaryOf(SecondaryBalance.BalanceEntry secondary) {
        final List<RowView.Line> breakdown = new ArrayList<>();
        breakdown.add(new RowView.Line(secondary.getQtyDemanded(), NEEDED));
        breakdown.add(new RowView.Line(secondary.getQtyHeld(), HELD));

        // a row is titled with its root item's name, so the line reads as the row
        secondary.getChainContributions().stream()
                .sorted(Comparator.comparingDouble(SecondaryBalance.ChainContribution::getMakes).reversed())
                .forEach(chain -> breakdown.add(new RowView.Line(chain.getMakes(), nameOf.apply(chain.getRootItemId()))));

        // every kind of bird nest is called "Bird nest", so precursors are summed by name
        final Map<String, double[]> heldAndMadeByName = new LinkedHashMap<>();
        for (SecondaryBalance.PrecursorContribution precursor : secondary.getPrecursorContributions()) {
            final double[] heldAndMade = heldAndMadeByName.computeIfAbsent(nameOf.apply(precursor.getPrecursorItemId()), name -> new double[2]);
            heldAndMade[0] += precursor.getHeld();
            heldAndMade[1] += precursor.getMakes();
        }

        heldAndMadeByName.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, double[]> entry) -> entry.getValue()[1]).reversed())
                .forEach(entry -> breakdown.add(new RowView.Line(entry.getValue()[1],
                        Amounts.abbreviate(entry.getValue()[0]) + " × " + entry.getKey())));

        final int secondaryItemId = secondary.getSecondaryItemId();
        return new SecondaryView(secondaryItemId, nameOf.apply(secondaryItemId), secondary.net(), breakdown);
    }

    /**
     * One row's demand: most needed first, rounded up - a fraction of a secondary still has to be
     * bought whole.
     */
    private List<RowView.Line> secondaryDemandLinesOf(ItemQuantities demand) {
        return demand.asMap().entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .map(entry -> new RowView.Line(Math.ceil(entry.getValue()), nameOf.apply(entry.getKey())))
                .collect(Collectors.toList());
    }
}
