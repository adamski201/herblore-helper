package adamski.ui;

import javax.swing.Icon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A red circle struck through, for "Make nothing" - centred in a sprite's room so it lines up with
 * the item sprites beside it.
 */
final class NoEntryIcon implements Icon {
    private static final int DIAMETER = 16;

    private final Color color;

    NoEntryIcon(Color color) {
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));

        final int left = x + (StackIcon.WIDTH - DIAMETER) / 2;
        final int top = y + (StackIcon.HEIGHT - DIAMETER) / 2;
        g2.drawOval(left, top, DIAMETER, DIAMETER);

        // the slash runs corner to corner of the circle, top-left to bottom-right
        final double inset = DIAMETER * (1 - Math.sqrt(0.5)) / 2;
        g2.drawLine((int) Math.round(left + inset), (int) Math.round(top + inset),
                (int) Math.round(left + DIAMETER - inset), (int) Math.round(top + DIAMETER - inset));

        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return StackIcon.WIDTH;
    }

    @Override
    public int getIconHeight() {
        return StackIcon.HEIGHT;
    }
}
