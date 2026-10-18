package adamski.ui.icon;

import javax.swing.Icon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

/**
 * A small downward triangle, the usual sign of a menu.
 */
public final class CaretIcon implements Icon {
    private static final int WIDTH = 5;
    private static final int HEIGHT = 3;

    private final Color color;

    public CaretIcon(Color color) {
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(color);
        g.fillPolygon(new int[]{x, x + WIDTH, x + WIDTH / 2}, new int[]{y, y, y + HEIGHT}, 3);
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
