package adamski.ui;

import lombok.Value;

import java.util.List;

/**
 * One line of the panel-wide secondary balance, with where it comes from, every name resolved.
 */
@Value
class SecondaryView {
    int itemId;

    String name;

    /**
     * Negative is a shortfall, positive is spare.
     */
    double net;

    /**
     * What is needed, what is held, what each row making it makes - titled as the row is - then
     * each precursor held with how much of the secondary it makes. Precursors sharing a name are
     * one line - every kind of bird nest is called "Bird nest".
     */
    List<RowView.Line> breakdown;
}
