package adamski.domain.calculators;

import adamski.domain.models.Recipe;
import adamski.domain.models.RecipeGroup;
import adamski.domain.models.RecipeRun;
import adamski.domain.models.RecipeStage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sorts cascade output by path.
 * <p>
 * A run belongs to the path of the item it <em>produces</em>, so one root's chain can cross paths -
 * harralander seeds make stat restore on the harralander path and then guthix balance on its own.
 */
public final class RecipeGrouper {
    private RecipeGrouper() {
    }

    /**
     * @param byRoot          one run list per owned item id
     * @param pathByItem      item id to the representative item of its path
     * @param selectedRecipes the same list the cascade was given, used only for ordering
     * @return one group per path that produced anything, each with its stages least mature first
     */
    public static List<RecipeGroup> group(Map<Integer, List<RecipeRun>> byRoot,
                                          Map<Integer, Integer> pathByItem,
                                          List<Recipe> selectedRecipes) {
        final Map<Integer, Integer> position = new HashMap<>();
        for (int i = 0; i < selectedRecipes.size(); i++) {
            position.put(selectedRecipes.get(i).getId(), i);
        }

        // Taken from the unsplit run list - a stage only holds the subset belonging to one path
        final Map<Integer, Integer> maturity = new HashMap<>();
        final Map<Integer, Map<Integer, List<RecipeRun>>> runsByPathByRoot = new HashMap<>();

        byRoot.forEach((rootItemId, runs) -> {
            if (runs.isEmpty()) return;

            maturity.put(rootItemId, positionOf(position, runs.get(0)));

            for (RecipeRun run : runs) {
                final Integer path = pathByItem.get(run.getRecipe().getOutput().getItemId());

                // Guarded by RecipePathsTest - a dropped run would leave the total above the paths
                if (path == null) continue;

                runsByPathByRoot
                        .computeIfAbsent(path, k -> new HashMap<>())
                        .computeIfAbsent(rootItemId, k -> new ArrayList<>())
                        .add(run);
            }
        });

        final List<RecipeGroup> groups = new ArrayList<>(runsByPathByRoot.size());

        runsByPathByRoot.forEach((pathItemId, byRootForPath) -> {
            final List<RecipeStage> stages = byRootForPath.entrySet().stream()
                    .sorted(Comparator
                            .comparingInt((Map.Entry<Integer, List<RecipeRun>> e) -> maturity.get(e.getKey()))
                            .thenComparingInt(Map.Entry::getKey))
                    .map(e -> new RecipeStage(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            groups.add(new RecipeGroup(pathItemId, stages));
        });

        groups.sort(Comparator
                .comparingInt((RecipeGroup group) -> earliestRecipe(position, group))
                .thenComparingInt(RecipeGroup::getPathItemId));

        return groups;
    }

    private static int positionOf(Map<Integer, Integer> position, RecipeRun run) {
        return position.getOrDefault(run.getRecipe().getId(), Integer.MAX_VALUE);
    }

    private static int earliestRecipe(Map<Integer, Integer> position, RecipeGroup group) {
        return group.getStages().stream()
                .flatMap(stage -> stage.getRuns().stream())
                .mapToInt(run -> positionOf(position, run))
                .min()
                .orElse(Integer.MAX_VALUE);
    }
}
