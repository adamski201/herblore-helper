package adamski.ui;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A full-width light grey line, filled blue as far through the banked level as the bank reaches. Under
 * it, the current level on the left, the banked xp in the middle and the banked level on the
 * right. EDT only.
 */
final class XpBar extends JComponent {
    private static final Color TRACK = ColorScheme.LIGHT_GRAY_COLOR;
    private static final Color FILL = new Color(0x3A, 0x8E, 0xE6);
    private static final Color LEVEL_TEXT = ColorScheme.LIGHT_GRAY_COLOR;
    private static final Color XP_TEXT = Color.WHITE;

    private static final int HEIGHT = 28;
    private static final int LINE_THICKNESS = 2;

    /**
     * Space between the line and the text under it.
     */
    private static final int TEXT_GAP = 6;

    private XpBarView view;

    XpBar() {
        setPreferredSize(new Dimension(0, HEIGHT));
    }

    void setView(XpBarView view) {
        this.view = view;
        setToolTipText(String.format("Level %d now, %d once %,.0f banked xp is made",
                view.getCurrentLevel(), view.getBankedLevel(), view.getBankedXp()));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (view == null) return;

        final Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final int width = getWidth();

            g.setColor(TRACK);
            g.fillRect(0, 0, width, LINE_THICKNESS);
            g.setColor(FILL);
            g.fillRect(0, 0, (int) Math.round(width * view.getProgress()), LINE_THICKNESS);

            g.setFont(FontManager.getRunescapeFont());
            final FontMetrics metrics = g.getFontMetrics();
            final int baseline = LINE_THICKNESS + TEXT_GAP + metrics.getAscent();

            final String current = String.valueOf(view.getCurrentLevel());
            final String banked = String.valueOf(view.getBankedLevel());
            final String xp = bankedText(view.getBankedXp());

            g.setColor(LEVEL_TEXT);
            g.drawString(current, 0, baseline);
            g.drawString(banked, width - metrics.stringWidth(banked), baseline);

            g.setColor(XP_TEXT);
            g.drawString(xp, (width - metrics.stringWidth(xp)) / 2, baseline);
        } finally {
            g.dispose();
        }
    }

    /**
     * Short but precise enough to watch move: "+3.179m xp banked", "+450.1k xp banked", "+820 xp banked".
     */
    static String bankedText(double xp) {
        final String amount;
        if (xp >= 1_000_000) {
            amount = String.format("%.3fm", xp / 1_000_000);
        } else if (xp >= 1_000) {
            amount = String.format("%.1fk", xp / 1_000);
        } else {
            amount = String.format("%.0f", xp);
        }

        return "+" + amount + " xp banked";
    }
}
