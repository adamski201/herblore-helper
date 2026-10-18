package adamski.ui;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Opens the menu under its anchor on a press, and reports hovering so the caller can shade
 * whatever it listens on.
 * <p>
 * Swing closes an open menu on any press outside it, before the press gets here - so a press
 * on the opener would close the menu and reopen it at once. The press that just closed it is
 * left at that, so clicking the opener again closes the menu.
 * <p>
 * The menu is built on the first press, not with the row. Most menus are never opened, and
 * building one fetches a sprite per option.
 */
final class MenuOpener extends MouseAdapter {
    private final Supplier<JPopupMenu> buildMenu;
    private final JComponent anchor;
    private final Consumer<Boolean> onHover;

    private JPopupMenu menu;

    /**
     * When the press that last closed the menu happened.
     */
    private long closedByPressAt = -1;

    MenuOpener(Supplier<JPopupMenu> buildMenu, JComponent anchor, Consumer<Boolean> onHover) {
        this.buildMenu = buildMenu;
        this.anchor = anchor;
        this.onHover = onHover;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getWhen() == closedByPressAt) return;

        if (menu == null) menu = buildMenu();
        menu.show(anchor, 0, anchor.getHeight());
    }

    private JPopupMenu buildMenu() {
        final JPopupMenu built = buildMenu.get();
        built.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                final AWTEvent closedBy = EventQueue.getCurrentEvent();
                if (closedBy instanceof MouseEvent && closedBy.getID() == MouseEvent.MOUSE_PRESSED) {
                    closedByPressAt = ((MouseEvent) closedBy).getWhen();
                }
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        return built;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        onHover.accept(true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        onHover.accept(false);
    }
}
