package adamski.ui.icon;

import adamski.ui.Amounts;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

/**
 * An item sprite with a quantity drawn over its top-left corner, in the game's stack colours - yellow
 * below 100K, white in thousands, green in millions - but shortened like every other amount in the
 * panel, so a 15,510 dose stack reads "16k".
 * <p>
 * ItemManager can draw the quantity itself, but it caches per item <em>and</em> quantity, and draws
 * synchronously when asked on the client thread - every change of quantity meant a fresh model
 * render. This takes the plain sprite, cached once per item, and paints the number on top.
 */
public final class StackIcon implements Icon {
    static final int WIDTH = 36;
    static final int HEIGHT = 32;

    private static final Color YELLOW = new Color(0xffff00);
    private static final Color GREEN = new Color(0x00ff80);

    private final AsyncBufferedImage sprite;
    private final String quantity;
    private final Color color;

    /**
     * @param quantity rounded to a whole number; nothing is drawn for less than one
     * @param doses    whether the stack is a potion's doses, which keep a decimal up to 50k
     */
    public StackIcon(AsyncBufferedImage sprite, double quantity, boolean doses) {
        this.sprite = sprite;

        final long whole = Math.round(quantity);
        if (whole < 1) {
            this.quantity = null;
        } else {
            this.quantity = doses ? Amounts.abbreviateDoses(whole) : Amounts.abbreviate(whole);
        }

        if (whole < 100_000) {
            this.color = YELLOW;
        } else if (whole < 10_000_000) {
            this.color = Color.WHITE;
        } else {
            this.color = GREEN;
        }
    }

    /**
     * Repaints the component once the sprite has been drawn, since it starts out blank.
     */
    public void repaintWhenLoaded(Component component) {
        sprite.onLoaded(component::repaint);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(sprite, x, y, null);

        if (quantity == null) return;

        g.setFont(FontManager.getRunescapeSmallFont());
        g.setColor(Color.BLACK);
        g.drawString(quantity, x + 1, y + 11);
        g.setColor(color);
        g.drawString(quantity, x, y + 10);
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
