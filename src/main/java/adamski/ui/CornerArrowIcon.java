package adamski.ui;

import javax.swing.Icon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * A down-then-right arrow, leading from the root's name to what it becomes. Drawn rather than
 * typed because the RuneScape fonts have no such glyph.
 */
final class CornerArrowIcon implements Icon {
    private static final int SIZE = 10;

    private final Color color;

    CornerArrowIcon(Color color) {
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.translate(x, y);

        final Path2D shaft = new Path2D.Float();
        shaft.moveTo(2, 1);
        shaft.lineTo(2, 5.5);
        shaft.lineTo(8.5, 5.5);
        g2.draw(shaft);

        final Path2D head = new Path2D.Float();
        head.moveTo(6.5, 3.5);
        head.lineTo(8.5, 5.5);
        head.lineTo(6.5, 7.5);
        g2.draw(head);

        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
