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
 * A plain arrow pointing right, from a recipe's input to its output. Drawn for the same reason
 * as CornerArrowIcon.
 */
final class RightArrowIcon implements Icon {
    private static final int WIDTH = 8;
    private static final int HEIGHT = 7;

    private final Color color;

    RightArrowIcon(Color color) {
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.translate(x, y);

        final Path2D arrow = new Path2D.Float();
        arrow.moveTo(0.5, 3.5);
        arrow.lineTo(7, 3.5);
        arrow.moveTo(4.5, 1);
        arrow.lineTo(7, 3.5);
        arrow.lineTo(4.5, 6);
        g2.draw(arrow);

        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return WIDTH;
    }

    @Override
    public int getIconHeight() {
        return HEIGHT;
    }
}
