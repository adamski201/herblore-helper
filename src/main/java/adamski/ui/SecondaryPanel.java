package adamski.ui;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * One secondary in the panel-wide balance: its name and net on one line, which folds open what is
 * needed, what is held, and the precursors counted towards it.
 * <p>
 * Immutable once built, like a row: the panel builds a new one for every result.
 */
class SecondaryPanel extends JPanel {
    private static final Color HOVER = ColorScheme.DARKER_GRAY_COLOR;

    /**
     * How much taller a secondary's line is than a plain line, as a fraction of its height.
     */
    private static final float EXTRA_HEIGHT = 0.2f;

    /**
     * @param onToggle told whether the line is now expanded, so the panel can keep it open across
     *                 results
     */
    SecondaryPanel(SecondaryView secondary, boolean expanded, Consumer<Boolean> onToggle) {
        setLayout(new BorderLayout());
        setOpaque(false);

        final Color netColor = secondary.getNet() < 0
                ? ColorScheme.PROGRESS_ERROR_COLOR
                : ColorScheme.PROGRESS_COMPLETE_COLOR;
        final JPanel summary = RowPanel.lineOf(secondary.getName(), Amounts.abbreviate(secondary.getNet()), netColor);
        summary.setBackground(HOVER);

        // taller than a plain line by EXTRA_HEIGHT, split above and below, so it is easier to click
        final int padding = Math.round(summary.getPreferredSize().height * EXTRA_HEIGHT / 2);
        summary.setBorder(new EmptyBorder(padding, 0, padding, 0));

        final JPanel detail = new JPanel(new DynamicGridLayout(0, 1, 0, 1));
        detail.setOpaque(false);
        detail.setBorder(new EmptyBorder(1, 8, 4, 0));
        for (RowView.Line line : secondary.getBreakdown()) {
            detail.add(RowPanel.lineOf(line.getName(), Amounts.abbreviate(line.getAmount()), ColorScheme.LIGHT_GRAY_COLOR));
        }
        detail.setVisible(expanded);

        summary.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final boolean nowExpanded = !detail.isVisible();
                detail.setVisible(nowExpanded);
                onToggle.accept(nowExpanded);
                revalidate();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                summary.setOpaque(true);
                summary.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                summary.setOpaque(false);
                summary.repaint();
            }
        });
        summary.setToolTipText("Click for what is needed and held");
        summary.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        add(summary, BorderLayout.NORTH);
        add(detail, BorderLayout.CENTER);
    }
}
