package adamski.ui;

import lombok.Value;

import java.util.List;

/**
 * One row as the panel shows it, with every name already resolved. Built on the client thread,
 * where ItemManager can be used, and drawn on the EDT, where it cannot.
 */
@Value
class RowView {
    /**
     * What a product is picked against - the row's entry item, which the app needs back.
     */
    int rootItemId;

    /**
     * The item the row starts from, which is also what it is called - two torstol rows, one from
     * grimy herbs and one from unf vials, are told apart by it.
     */
    String rootItemName;

    /**
     * How much of the root item is held, in 1-dose units.
     */
    double rootQuantity;

    double xp;

    int productItemId;

    /**
     * Set to make nothing - shown as one dim line until a product is picked again.
     */
    boolean switchedOff;

    /**
     * What the row ends at, or "Make nothing" when it ends where it starts.
     */
    String productName;

    /**
     * How much of the product is made, in 1-dose units.
     */
    double outputQuantity;

    List<ProductOption> productOptions;

    /**
     * What each held item contributed, named with how much of it is held.
     */
    List<Line> xpByItem;

    List<RecipeLine> xpByRecipe;

    /**
     * How much of each secondary the row's recipes use up, whole.
     */
    List<Line> secondaryDemand;

    @Value
    static class ProductOption {
        int itemId;
        String name;
    }

    @Value
    static class Line {
        double amount;
        String name;
    }

    /**
     * What one recipe contributed, named by what goes in and what comes out.
     */
    @Value
    static class RecipeLine {
        double xp;
        String inputName;
        String outputName;
    }
}
