package adamski.ui.icon;

import javax.swing.Icon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * A cog, for the button that opens the plugin's settings.
 */
public final class CogIcon implements Icon {
    private static final int SIZE = 14;
    private static final int TEETH = 8;

    private final Color color;

    public CogIcon(Color color) {
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        final double centre = SIZE / 2.0;
        final double bodyRadius = SIZE * 0.34;
        final double toothLength = SIZE * 0.16;
        final double toothWidth = SIZE * 0.18;
        final double holeRadius = SIZE * 0.15;

        final Area cog = new Area(new Ellipse2D.Double(
                centre - bodyRadius, centre - bodyRadius, bodyRadius * 2, bodyRadius * 2));

        // each tooth sticks straight up from the body, then is turned into place around the centre
        final Rectangle2D tooth = new Rectangle2D.Double(
                centre - toothWidth / 2, centre - bodyRadius - toothLength, toothWidth, toothLength + 1);
        for (int i = 0; i < TEETH; i++) {
            cog.add(new Area(AffineTransform.getRotateInstance(2 * Math.PI * i / TEETH, centre, centre)
                    .createTransformedShape(tooth)));
        }

        cog.subtract(new Area(new Ellipse2D.Double(
                centre - holeRadius, centre - holeRadius, holeRadius * 2, holeRadius * 2)));

        g2.translate(x, y);
        g2.fill(cog);
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
