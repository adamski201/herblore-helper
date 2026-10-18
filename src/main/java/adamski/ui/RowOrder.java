package adamski.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where each row sits in the panel, remembered between results so rows do not move about.
 * <p>
 * A row is known by its root item. Rows still there keep their places and rows that are gone are
 * dropped. A new row created by picking a product goes straight after the row it was picked on;
 * any other new row goes to its family's place - herbs by level, then coral, then caviar, then the
 * rest, earlier stages first within a family.
 * <p>
 * A row whose root moves up as the bank drains - seeds used up, grimy herbs left - is a new row by
 * this measure, but it lands in its family's place, which is where it already was.
 */
final class RowOrder {
    private List<Integer> rootItemIds = new ArrayList<>();

    /**
     * @param incoming              the root of every row in the new result, in the order the domain
     *                              gives them - least mature first, which sets the order within a
     *                              family
     * @param pickedOnRootItemId    the row whose picker produced this result, or null if something
     *                              else did
     * @return the same roots, in the order to show them
     */
    List<Integer> arrange(List<Integer> incoming, Integer pickedOnRootItemId) {
        final Map<Integer, Integer> incomingIndex = new HashMap<>();
        for (int i = 0; i < incoming.size(); i++) {
            incomingIndex.put(incoming.get(i), i);
        }

        final Comparator<Integer> familyOrder = Comparator
                .comparingInt(RowFamilies::familyRankOf)
                .thenComparingInt(incomingIndex::get);

        final List<Integer> arranged = new ArrayList<>(rootItemIds);
        arranged.retainAll(incomingIndex.keySet());

        final List<Integer> added = new ArrayList<>(incoming);
        added.removeAll(rootItemIds);

        if (pickedOnRootItemId != null && arranged.contains(pickedOnRootItemId)) {
            arranged.addAll(arranged.indexOf(pickedOnRootItemId) + 1, added);
        } else {
            for (int rootItemId : added) {
                arranged.add(placeFor(rootItemId, arranged, familyOrder), rootItemId);
            }
        }

        rootItemIds = arranged;

        return List.copyOf(arranged);
    }

    /**
     * After the last row that belongs before it. The rows are not wholly in family order - a row
     * placed by a pick stays straight after the row it was picked on, wherever its own family would
     * put it - so this scans rather than searching, and from the bottom: a row pinned behind its
     * picker must not pull a new row up ahead of the families it really follows.
     */
    private static int placeFor(int rootItemId, List<Integer> arranged, Comparator<Integer> familyOrder) {
        for (int i = arranged.size() - 1; i >= 0; i--) {
            if (familyOrder.compare(arranged.get(i), rootItemId) < 0) return i + 1;
        }

        return 0;
    }
}
