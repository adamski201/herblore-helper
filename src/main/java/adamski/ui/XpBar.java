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
 * The banked level above a full-width line showing how far through it towards the next the bank
 * reaches, in bright blue. A track in muted blue means the bank gains at least one whole level;
 * a grey track means it gains none, and the part of the level already held shows in muted blue
 * before the bank's share. Under it, the current level on the left - where you start - the banked
 * xp in the middle, and on the right the level the fill is heading for. EDT only.
 */
final class XpBar extends JComponent {
    private static final Color TRACK = ColorScheme.LIGHT_GRAY_COLOR;
    private static final Color FILL = new Color(0x3A, 0x8E, 0xE6);

    /**
     * Xp already covered without this level's fill: the whole track once the bank gains a level,
     * or what the player already has of the level when it does not.
     */
    private static final Color COVERED = new Color(0x3A, 0x58, 0x7A);
    private static final Color LEVEL_TEXT = ColorScheme.LIGHT_GRAY_COLOR;
    private static final Color XP_TEXT = Color.WHITE;

    private static final int LINE_THICKNESS = 2;

    /**
     * Space between the line and the text either side of it.
     */
    private static final int TEXT_GAP = 6;

    private XpBarView view;

    XpBar() {
        // a line of text above the bar and one below
        final FontMetrics above = getFontMetrics(FontManager.getRunescapeFont());
        final FontMetrics below = getFontMetrics(FontManager.getRunescapeFont());
        setPreferredSize(new Dimension(0, heightOf(above) + TEXT_GAP + LINE_THICKNESS + TEXT_GAP + heightOf(below)));
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

            // the banked level
            g.setFont(FontManager.getRunescapeFont());
            final FontMetrics above = g.getFontMetrics();
            final String level = "Level " + view.getBankedLevel();
            g.setColor(XP_TEXT);
            g.drawString(level, (width - above.stringWidth(level)) / 2, above.getAscent());

            final int lineTop = heightOf(above) + TEXT_GAP;
            // a gained level turns the whole track blue; otherwise what is already held of the level
            // shows in the muted blue, with the bank's share in bright blue after it
            final int start = (int) Math.round(width * view.getStartProgress());
            final int end = (int) Math.round(width * view.getProgress());
            g.setColor(view.levelGained() ? COVERED : TRACK);
            g.fillRect(0, lineTop, width, LINE_THICKNESS);
            g.setColor(COVERED);
            g.fillRect(0, lineTop, start, LINE_THICKNESS);
            g.setColor(FILL);
            g.fillRect(start, lineTop, end - start, LINE_THICKNESS);

            g.setFont(FontManager.getRunescapeFont());
            final FontMetrics metrics = g.getFontMetrics();
            final int baseline = lineTop + LINE_THICKNESS + TEXT_GAP + metrics.getAscent();

            final String current = String.valueOf(view.getCurrentLevel());
            final String next = String.valueOf(view.getNextLevel());
            final String xp = xpText(view.getBankedXp());

            // where you start on the left; on the right, the level the fill is heading for
            g.setColor(LEVEL_TEXT);
            g.drawString(current, 0, baseline);
            g.drawString(next, width - metrics.stringWidth(next), baseline);

            g.setColor(XP_TEXT);
            g.drawString(xp, (width - metrics.stringWidth(xp)) / 2, baseline);
        } finally {
            g.dispose();
        }
    }

    private static int heightOf(FontMetrics metrics) {
        return metrics.getAscent() + metrics.getDescent();
    }

    /**
     * Short but precise enough to watch move: "+3.179m xp", "+450.1k xp", "+820 xp".
     */
    static String xpText(double xp) {
        final String amount;
        if (xp >= 1_000_000) {
            amount = String.format("%.3fm", xp / 1_000_000);
        } else if (xp >= 1_000) {
            amount = String.format("%.1fk", xp / 1_000);
        } else {
            amount = String.format("%.0f", xp);
        }

        return "+" + amount + " xp";
    }
}
