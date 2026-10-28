package adamski.data;

import adamski.domain.SecondaryPrecursor;
import net.runelite.api.gameval.ItemID;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Items ground or crushed into a secondary - no herblore xp, so they are not recipes.
 */
public final class SecondaryPrecursors {
    /**
     * Placeholder. The real number depends on Herblore level and the hard Wilderness diary.
     */
    private static final double LAVA_SHARDS_PER_SCALE = 4.5;

    private static final List<SecondaryPrecursor> PRECURSORS = List.of(
            new SecondaryPrecursor(ItemID.BIRD_NEST_EMPTY, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.BIRD_NEST_SEEDS, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.BIRD_NEST_SEEDS_JAN2019, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.BIRD_NEST_CHEAPSEEDS, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.BIRD_NEST_DECENTSEEDS, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.BIRD_NEST_DECENTSEEDS_JAN2019, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.BIRD_NEST_RING, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.BIRD_NEST_EGG_RED, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.BIRD_NEST_EGG_GREEN, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.BIRD_NEST_EGG_BLUE, ItemID.CRUSHED_BIRD_NEST, 1),
            new SecondaryPrecursor(ItemID.UNICORN_HORN, ItemID.UNICORN_HORN_DUST, 1),
            new SecondaryPrecursor(ItemID.CHOCOLATE_BAR, ItemID.CHOCOLATE_DUST, 1),
            new SecondaryPrecursor(ItemID.DESERT_GOAT_HORN, ItemID.GROUND_DESERT_GOAT_HORN, 1),
            new SecondaryPrecursor(ItemID.BLUE_DRAGON_SCALE, ItemID.DRAGON_SCALE_DUST, 1),
            new SecondaryPrecursor(ItemID.HUNTINGBEAST_SABRETEETH, ItemID.HUNTINGBEAST_SABRETEETH_DUST, 1),
            new SecondaryPrecursor(ItemID.NIHIL_SHARD, ItemID.NIHIL_DUST, 1),
            new SecondaryPrecursor(ItemID.DRAGON_BONES_SUPERIOR, ItemID.CRUSHED_DRAGON_BONES, 1),
            new SecondaryPrecursor(ItemID.SILVER_BAR, ItemID.SILVER_DUST, 1),
            new SecondaryPrecursor(ItemID.LAVA_SCALE, ItemID.LAVA_SHARD, LAVA_SHARDS_PER_SCALE)
    );

    private static final Set<Integer> PRECURSOR_ITEM_IDS = PRECURSORS.stream()
            .map(SecondaryPrecursor::getItemId)
            .collect(Collectors.toSet());

    private SecondaryPrecursors() {}

    public static List<SecondaryPrecursor> all() {
        return PRECURSORS;
    }

    public static boolean isPrecursor(int itemId) {
        return PRECURSOR_ITEM_IDS.contains(itemId);
    }
}
