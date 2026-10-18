package adamski.ui;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Rows are known by their root item. Incoming lists are in the domain's order, least mature first.
 */
public class RowOrderTest {
    private final RowOrder order = new RowOrder();

    @Test
    public void herbsByLevelThenCoralThenCaviarThenTheRest() {
        final List<Integer> arranged = order.arrange(List.of(
                ItemID._1DOSE2ENERGY,
                ItemID.BRUT_CAVIAR,
                ItemID.CORAL_PILLAR_FRAG,
                ItemID.UNIDENTIFIED_TORSTOL,
                ItemID.GUAM_SEED), null);

        assertEquals(List.of(
                ItemID.GUAM_SEED,
                ItemID.UNIDENTIFIED_TORSTOL,
                ItemID.CORAL_PILLAR_FRAG,
                ItemID.BRUT_CAVIAR,
                ItemID._1DOSE2ENERGY), arranged);
    }

    @Test
    public void rowsOfOneHerbKeepTheDomainsOrder() {
        assertEquals(List.of(ItemID.TORSTOL, ItemID.TORSTOLVIAL),
                order.arrange(List.of(ItemID.TORSTOL, ItemID.TORSTOLVIAL), null));
    }

    @Test
    public void aNewRowFromTheBankGoesToItsFamilysPlace() {
        order.arrange(List.of(ItemID.UNIDENTIFIED_GUAM, ItemID.UNIDENTIFIED_TORSTOL), null);

        assertEquals(List.of(ItemID.UNIDENTIFIED_GUAM, ItemID.UNIDENTIFIED_AVANTOE, ItemID.UNIDENTIFIED_TORSTOL),
                order.arrange(List.of(ItemID.UNIDENTIFIED_GUAM, ItemID.UNIDENTIFIED_AVANTOE,
                        ItemID.UNIDENTIFIED_TORSTOL), null));
    }

    @Test
    public void aRowThatIsGoneLeavesTheOthersWhereTheyWere() {
        order.arrange(List.of(ItemID.UNIDENTIFIED_GUAM, ItemID.UNIDENTIFIED_AVANTOE, ItemID.UNIDENTIFIED_TORSTOL), null);

        assertEquals(List.of(ItemID.UNIDENTIFIED_GUAM, ItemID.UNIDENTIFIED_TORSTOL),
                order.arrange(List.of(ItemID.UNIDENTIFIED_GUAM, ItemID.UNIDENTIFIED_TORSTOL), null));
    }

    @Test
    public void aRowCreatedByAPickGoesStraightAfterTheRowPickedOn() {
        order.arrange(List.of(ItemID.UNIDENTIFIED_AVANTOE, ItemID.UNIDENTIFIED_KWUARM), null);

        // avantoe sent away from super energy, so the banked super energy roots a row of its own
        assertEquals(List.of(ItemID.UNIDENTIFIED_AVANTOE, ItemID._1DOSE2ENERGY, ItemID.UNIDENTIFIED_KWUARM),
                order.arrange(List.of(ItemID.UNIDENTIFIED_AVANTOE, ItemID.UNIDENTIFIED_KWUARM,
                        ItemID._1DOSE2ENERGY), ItemID.UNIDENTIFIED_AVANTOE));
    }

    @Test
    public void aRowPlacedByAPickStaysThereAfterwards() {
        order.arrange(List.of(ItemID.UNIDENTIFIED_AVANTOE, ItemID.UNIDENTIFIED_KWUARM), null);
        order.arrange(List.of(ItemID.UNIDENTIFIED_AVANTOE, ItemID.UNIDENTIFIED_KWUARM,
                ItemID._1DOSE2ENERGY), ItemID.UNIDENTIFIED_AVANTOE);

        assertEquals(List.of(ItemID.UNIDENTIFIED_AVANTOE, ItemID._1DOSE2ENERGY, ItemID.UNIDENTIFIED_KWUARM,
                        ItemID.UNIDENTIFIED_TORSTOL),
                order.arrange(List.of(ItemID.UNIDENTIFIED_AVANTOE, ItemID.UNIDENTIFIED_KWUARM,
                        ItemID.UNIDENTIFIED_TORSTOL, ItemID._1DOSE2ENERGY), null));
    }

    @Test
    public void severalRowsFromOnePickFollowItEarlierStageFirst() {
        order.arrange(List.of(ItemID.RANARR_SEED, ItemID.UNIDENTIFIED_TORSTOL), null);

        // ranarr stopped at the seed's grimy herb, leaving banked clean ranarr and unf to root rows
        assertEquals(List.of(ItemID.RANARR_SEED, ItemID.RANARR_WEED, ItemID.RANARRVIAL, ItemID.UNIDENTIFIED_TORSTOL),
                order.arrange(List.of(ItemID.RANARR_SEED, ItemID.RANARR_WEED, ItemID.RANARRVIAL,
                        ItemID.UNIDENTIFIED_TORSTOL), ItemID.RANARR_SEED));
    }

    /**
     * Seeds used up, so grimy ranarr roots the row now. That is a new root, but it lands in the
     * ranarr slot, so the row does not appear to move.
     */
    @Test
    public void aRowWhoseRootMovesUpKeepsItsPlace() {
        order.arrange(List.of(ItemID.RANARR_SEED, ItemID.AVANTOE_SEED), null);

        assertEquals(List.of(ItemID.UNIDENTIFIED_RANARR, ItemID.AVANTOE_SEED),
                order.arrange(List.of(ItemID.AVANTOE_SEED, ItemID.UNIDENTIFIED_RANARR), null));
    }
}
