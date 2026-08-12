package adamski.domain.calculators;

import adamski.domain.models.Ingredient;
import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeGroup;
import adamski.domain.models.RecipeRun;
import adamski.domain.models.RecipeStage;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Synthetic recipes - the real table encodes first-in-table-order selection, which is a placeholder.
 * <p>
 * 1 -> 2 -> 3 -> 4 is one path, and 4 -> 5 is a second hanging off its end.
 */
public class RecipeGrouperTest {
    private static final int PATH_A = 2;
    private static final int PATH_B = 5;

    private static final Recipe ONE_TO_TWO = recipe(10, 1, 2);
    private static final Recipe TWO_TO_THREE = recipe(11, 2, 3);
    private static final Recipe THREE_TO_FOUR = recipe(12, 3, 4);
    private static final Recipe FOUR_TO_FIVE = recipe(13, 4, 5);

    private static final List<Recipe> RECIPES =
            Arrays.asList(ONE_TO_TWO, TWO_TO_THREE, THREE_TO_FOUR, FOUR_TO_FIVE);

    private static final Map<Integer, Integer> PATHS = paths();

    @Test
    public void emptyInputIsNoPaths() {
        assertTrue(RecipeGrouper.group(Collections.emptyMap(), PATHS, RECIPES).isEmpty());
    }

    @Test
    public void twoRootsOnOneChainCollapseIntoOnePath() {
        final List<RecipeGroup> groups = group(
                root(1, run(ONE_TO_TWO, 8), run(TWO_TO_THREE, 8), run(THREE_TO_FOUR, 8)),
                root(3, run(THREE_TO_FOUR, 5)));

        final RecipeGroup a = find(groups, PATH_A);

        assertEquals(2, a.getStages().size());
        assertEquals(1, a.getStages().get(0).getRootItemId());
        assertEquals(3, a.getStages().get(1).getRootItemId());
    }

    @Test
    public void stagesComeBackLeastMatureFirst() {
        // Declared deepest-first to prove the order is computed, not incidental
        final List<RecipeGroup> groups = group(
                root(3, run(THREE_TO_FOUR, 1)),
                root(2, run(TWO_TO_THREE, 1), run(THREE_TO_FOUR, 1)),
                root(1, run(ONE_TO_TWO, 1), run(TWO_TO_THREE, 1), run(THREE_TO_FOUR, 1)));

        final List<Integer> roots = find(groups, PATH_A).getStages().stream()
                .map(RecipeStage::getRootItemId)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(1, 2, 3), roots);
    }

    @Test
    public void aRunLandsOnItsOutputsPathNotItsPrimarys() {
        final List<RecipeGroup> groups = group(root(4, run(FOUR_TO_FIVE, 3)));

        assertEquals(1, groups.size());
        assertEquals(PATH_B, groups.get(0).getPathItemId());
    }

    @Test
    public void oneRootCanFeedTwoPaths() {
        final List<RecipeGroup> groups = group(
                root(1, run(ONE_TO_TWO, 2), run(TWO_TO_THREE, 2), run(THREE_TO_FOUR, 2), run(FOUR_TO_FIVE, 2)));

        assertEquals(2, groups.size());

        assertEquals(1, find(groups, PATH_A).getStages().get(0).getRootItemId());
        assertEquals(1, find(groups, PATH_B).getStages().get(0).getRootItemId());
    }

    @Test
    public void aStageOnlyHoldsTheRunsBelongingToItsPath() {
        final List<RecipeGroup> groups = group(
                root(1, run(ONE_TO_TWO, 2), run(TWO_TO_THREE, 2), run(THREE_TO_FOUR, 2), run(FOUR_TO_FIVE, 2)));

        assertEquals(3, find(groups, PATH_A).getStages().get(0).getRuns().size());
        assertEquals(1, find(groups, PATH_B).getStages().get(0).getRuns().size());
    }

    @Test
    public void everyRunIsKeptExactlyOnce() {
        final List<RecipeGroup> groups = group(
                root(1, run(ONE_TO_TWO, 2), run(TWO_TO_THREE, 2), run(THREE_TO_FOUR, 2), run(FOUR_TO_FIVE, 2)),
                root(3, run(THREE_TO_FOUR, 9), run(FOUR_TO_FIVE, 9)));

        final long kept = groups.stream()
                .flatMap(g -> g.getStages().stream())
                .flatMap(s -> s.getRuns().stream())
                .count();

        assertEquals(6, kept);
    }

    @Test
    public void anOutputWithNoPathIsDropped() {
        final Recipe orphan = recipe(14, 5, 6);

        final Map<Integer, List<RecipeRun>> byRoot = new HashMap<>();
        byRoot.put(5, Collections.singletonList(run(orphan, 1)));

        assertTrue(RecipeGrouper.group(byRoot, PATHS, Collections.singletonList(orphan)).isEmpty());
    }

    @Test
    public void rootsThatProduceNothingAreAbsent() {
        final Map<Integer, List<RecipeRun>> byRoot = new HashMap<>();
        byRoot.put(99, Collections.emptyList());

        assertTrue(RecipeGrouper.group(byRoot, PATHS, RECIPES).isEmpty());
    }

    @Test
    public void pathsComeBackInDependencyOrder() {
        final List<RecipeGroup> groups = group(
                root(4, run(FOUR_TO_FIVE, 1)),
                root(1, run(ONE_TO_TWO, 1)));

        assertEquals(Arrays.asList(PATH_A, PATH_B),
                groups.stream().map(RecipeGroup::getPathItemId).collect(Collectors.toList()));
    }

    @Test
    public void runsKeepTheirQuantities() {
        final List<RecipeGroup> groups = group(root(1, run(ONE_TO_TWO, 7.5)));

        assertEquals(7.5, find(groups, PATH_A).getStages().get(0).getRuns().get(0).getRuns(), 0.0001);
    }

    @SafeVarargs
    private static List<RecipeGroup> group(Map.Entry<Integer, List<RecipeRun>>... roots) {
        final Map<Integer, List<RecipeRun>> byRoot = new HashMap<>();
        for (Map.Entry<Integer, List<RecipeRun>> entry : roots) {
            byRoot.put(entry.getKey(), entry.getValue());
        }

        return RecipeGrouper.group(byRoot, PATHS, RECIPES);
    }

    private static Map.Entry<Integer, List<RecipeRun>> root(int itemId, RecipeRun... runs) {
        return new java.util.AbstractMap.SimpleEntry<>(itemId, Arrays.asList(runs));
    }

    private static RecipeGroup find(List<RecipeGroup> groups, int pathItemId) {
        return groups.stream()
                .filter(g -> g.getPathItemId() == pathItemId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no group for path " + pathItemId));
    }

    private static RecipeRun run(Recipe recipe, double runs) {
        return new RecipeRun(recipe, runs);
    }

    private static Recipe recipe(int id, int primary, int output) {
        return new Recipe(id, new Ingredient(primary, 1), new Ingredient[0],
                new Ingredient(output, 1), "test", 1f, 0);
    }

    private static Map<Integer, Integer> paths() {
        final Map<Integer, Integer> paths = new HashMap<>();
        paths.put(1, PATH_A);
        paths.put(2, PATH_A);
        paths.put(3, PATH_A);
        paths.put(4, PATH_A);
        paths.put(5, PATH_B);
        return paths;
    }
}
