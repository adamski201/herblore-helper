package adamski.ui;

import adamski.app.HerbloreApp;
import adamski.app.ResultListener;
import adamski.domain.Recipe;
import adamski.app.HerbloreResult;
import adamski.ui.icon.CogIcon;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HerblorePanel extends PluginPanel implements ResultListener {
    /**
     * The title's icon, at most this wide and tall.
     */
    private static final int LOGO_SIZE = 20;

    /**
     * RuneLite's own eye pair (from Screen Markers and Loot Tracker, BSD-2), copied in rather than
     * read from those plugins' folders, which could move. Recoloured to match the cog.
     */
    private static final int EYE_SIZE = 14;
    private static final BufferedImage EYE_OPEN = ImageUtil.resizeImage(
            ImageUtil.loadImageResource(HerblorePanel.class, "eye_open.png"), EYE_SIZE, EYE_SIZE);
    private static final BufferedImage EYE_SHUT = ImageUtil.resizeImage(
            ImageUtil.loadImageResource(HerblorePanel.class, "eye_shut.png"), EYE_SIZE, EYE_SIZE);

    /**
     * The title's and heading's buttons - the cog and the eye - share these, so they match.
     */
    private static final Color BUTTON = new Color(0x8C, 0x8C, 0x8C);
    private static final Color BUTTON_HOVER = Color.WHITE;

    private final ItemManager itemManager;
    private final ClientThread clientThread;
    private final HerbloreApp app;
    private final RowViewFactory rowViewFactory;

    private final JLabel noData = new JLabel("No data yet - open your bank");
    private final XpBar xpBar = new XpBar();
    private final JPanel rows = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
    private final JPanel secondaries = new JPanel(new DynamicGridLayout(0, 1, 0, 1));

    /**
     * Which rows are open, by root item, so they stay open when the rows are rebuilt. EDT only.
     */
    private final Set<Integer> expandedRootItemIds = new HashSet<>();

    /**
     * Which secondaries are open, by item, for the same reason. EDT only.
     */
    private final Set<Integer> expandedSecondaryItemIds = new HashSet<>();

    /**
     * Where each row sits, kept between results. EDT only.
     */
    private final RowOrder rowOrder = new RowOrder();

    /**
     * What each row showed last time, and the panel showing it, by root item. A row whose view has
     * not changed is kept as it is - no rebuild, and its hover, open menu and icons are left alone.
     * EDT only.
     */
    private Map<Integer, RowView> shownRowByRootItemId = new HashMap<>();
    private Map<Integer, RowPanel> panelByRootItemId = new HashMap<>();

    /**
     * Every sprite asked for, kept here rather than left to ItemManager's cache, which holds 128
     * across every plugin. A sprite evicted there comes back blank until the client thread redraws
     * it, which flickers. A few hundred herblore items at most. EDT only.
     */
    private final Map<Integer, AsyncBufferedImage> spriteByItemId = new HashMap<>();

    /**
     * The row whose picker is being applied. Set and cleared on the client thread around
     * selectProduct, which publishes before it returns - so a result seen while this is set is
     * exactly the one the pick produced, and no bank update can be mistaken for it.
     */
    private Integer pickingOnRootItemId;

    /**
     * Whether rows switched off are listed. EDT only.
     */
    private boolean offRowsShown;

    /**
     * Saves the eye's state when it is clicked, so it is the same next session. Called on the EDT.
     */
    private final Consumer<Boolean> offRowsShownSaver;

    /**
     * The rows as last arranged, every one including those switched off, so the eye can redraw them
     * without waiting for a result. EDT only.
     */
    private List<Integer> arrangedRootItemIds = List.of();

    /**
     * The "Recipes" heading, with the eye on its right. Built once, so its hover survives redraws.
     */
    private final JPanel recipesHeading;
    private final JLabel eye = new JLabel();

    /**
     * Whether the panel is open. EDT only.
     */
    private boolean active;

    /**
     * The latest result to arrive while the panel was closed, drawn when it opens. Only the latest
     * is kept - a closed panel cannot be picked on, so the results skipped carry no pick for
     * RowOrder to miss. EDT only.
     */
    private Runnable pendingRender;

    /**
     * @param productRecipesByItem for each item, the recipe making each product it could end at -
     *                             what a row's menu offers. Fixed by the table, so given once here.
     * @param icon                 the plugin's icon, for the title
     * @param openSettings         opens this plugin's settings. Called on the EDT.
     * @param offRowsShown         whether rows switched off are listed, as saved last session
     * @param offRowsShownSaver    saves that when the eye is clicked. Called on the EDT.
     */
    public HerblorePanel(ItemManager itemManager, ClientThread clientThread, HerbloreApp app,
                         Map<Integer, List<Recipe>> productRecipesByItem, BufferedImage icon,
                         Runnable openSettings, boolean offRowsShown, Consumer<Boolean> offRowsShownSaver) {
        this.itemManager = itemManager;
        this.clientThread = clientThread;
        this.app = app;
        this.offRowsShown = offRowsShown;
        this.offRowsShownSaver = offRowsShownSaver;
        this.rowViewFactory = new RowViewFactory(this::name, productRecipesByItem);

        setLayout(new BorderLayout());

        noData.setFont(FontManager.getRunescapeFont());
        noData.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        noData.setHorizontalAlignment(JLabel.CENTER);

        // the bar takes the label's place once a result arrives
        xpBar.setVisible(false);
        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 8, 0));
        header.add(titleOf(icon, openSettings), BorderLayout.NORTH);
        header.add(noData, BorderLayout.CENTER);
        header.add(xpBar, BorderLayout.SOUTH);

        recipesHeading = recipesHeading();

        rows.setOpaque(false);
        secondaries.setOpaque(false);
        secondaries.setBorder(new EmptyBorder(10, 0, 0, 0));

        final JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(header, BorderLayout.NORTH);
        content.add(rows, BorderLayout.CENTER);
        content.add(secondaries, BorderLayout.SOUTH);

        add(content, BorderLayout.NORTH);
    }

    /**
     * The plugin's icon on the left, its name in the middle, and a cog on the right that opens the
     * settings.
     */
    private static JPanel titleOf(BufferedImage icon, Runnable openSettings) {
        final JLabel name = new JLabel("Herblore Helper", JLabel.CENTER);
        name.setFont(FontManager.getRunescapeBoldFont());
        name.setForeground(Color.WHITE);

        // a little larger than the cog, which is mostly empty space, keeping the icon's shape
        final JLabel logo = new JLabel(new ImageIcon(ImageUtil.resizeImage(icon, LOGO_SIZE, LOGO_SIZE, true)));

        final JLabel settings = new JLabel(new CogIcon(BUTTON));
        settings.setToolTipText("Settings");
        settings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settings.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openSettings.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                settings.setIcon(new CogIcon(BUTTON_HOVER));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                settings.setIcon(new CogIcon(BUTTON));
            }
        });

        // the icon and the cog take the same width, so the name centres on the panel
        final Dimension corner = new Dimension(LOGO_SIZE, LOGO_SIZE);
        logo.setPreferredSize(corner);
        settings.setPreferredSize(corner);

        // a thin line under the title sets it apart from what the panel shows below
        final JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.setBorder(new CompoundBorder(
                new CompoundBorder(new EmptyBorder(0, 0, 8, 0),
                        new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR)),
                new EmptyBorder(0, 0, 6, 0)));
        title.add(logo, BorderLayout.WEST);
        title.add(name, BorderLayout.CENTER);
        title.add(settings, BorderLayout.EAST);
        return title;
    }

    @Override
    public void onResultChanged(HerbloreResult result) {
        final XpBarView xpBarView = XpBarView.of(result.getCurrentXp(), result.getTotalXp());

        // Names resolve here - ItemManager cannot be touched from the EDT

        // Rows worth nothing stay listed - a row stopped at an unf vial still needs its picker.
        // Kept in the domain's order here; RowOrder decides where they sit.
        final List<RowView> rowViews = result.getChainResults().stream()
                .map(rowViewFactory::rowOf)
                .collect(Collectors.toList());

        final List<SecondaryView> secondaryViews = rowViewFactory.secondariesOf(result.getSecondaryBalance());
        final Integer pickedOnRootItemId = pickingOnRootItemId;

        SwingUtilities.invokeLater(() -> {
            final Runnable render = () -> render(xpBarView, rowViews, pickedOnRootItemId, secondaryViews);

            if (active) {
                render.run();
            } else {
                pendingRender = render;
            }
        });
    }

    @Override
    public void onActivate() {
        active = true;

        if (pendingRender != null) {
            pendingRender.run();
            pendingRender = null;
        }
    }

    @Override
    public void onDeactivate() {
        active = false;
    }

    private void render(XpBarView xpBarView, List<RowView> rowViews, Integer pickedOnRootItemId,
                        List<SecondaryView> secondaryViews) {
        noData.setVisible(false);
        xpBar.setVisible(true);
        xpBar.setView(xpBarView);

        final Map<Integer, RowView> rowByRootItemId = new HashMap<>();
        for (RowView row : rowViews) {
            rowByRootItemId.put(row.getRootItemId(), row);
        }

        arrangedRootItemIds = rowOrder.arrange(
                rowViews.stream().map(RowView::getRootItemId).collect(Collectors.toList()),
                pickedOnRootItemId);
        drawRows(rowByRootItemId);

        secondaries.removeAll();
        if (!secondaryViews.isEmpty()) {
            secondaries.add(heading("Secondaries"));

            for (SecondaryView secondary : secondaryViews) {
                final int itemId = secondary.getItemId();
                secondaries.add(new SecondaryPanel(secondary, expandedSecondaryItemIds.contains(itemId),
                        expanded -> {
                            if (expanded) expandedSecondaryItemIds.add(itemId);
                            else expandedSecondaryItemIds.remove(itemId);
                        }));
            }
        }

        revalidate();
        repaint();
    }

    /**
     * Called on the EDT by a row's picker. The app is only ever driven from the client thread, which
     * is also where it publishes, so the new result comes back through onResultChanged as usual.
     */
    private void selectProduct(int rootItemId, int productItemId) {
        clientThread.invoke(() -> {
            pickingOnRootItemId = rootItemId;
            try {
                app.selectProduct(rootItemId, productItemId);
            } finally {
                pickingOnRootItemId = null;
            }
        });
    }

    /**
     * Lists the rows as last arranged, leaving out those switched off while the eye is shut. A row
     * whose view has not changed keeps its panel.
     */
    private void drawRows(Map<Integer, RowView> rowByRootItemId) {
        final Map<Integer, RowPanel> panels = new HashMap<>();
        rows.removeAll();
        if (!arrangedRootItemIds.isEmpty()) {
            rows.add(recipesHeading);
        }

        for (int rootItemId : arrangedRootItemIds) {
            final RowView row = rowByRootItemId.get(rootItemId);
            if (!offRowsShown && row.isSwitchedOff()) continue;

            RowPanel panel = panelByRootItemId.get(rootItemId);
            if (panel == null || !row.equals(shownRowByRootItemId.get(rootItemId))) {
                panel = new RowPanel(row, expandedRootItemIds.contains(rootItemId),
                        expanded -> {
                            if (expanded) expandedRootItemIds.add(rootItemId);
                            else expandedRootItemIds.remove(rootItemId);
                        },
                        this::selectProduct,
                        this::spriteOf);
            }

            panels.put(rootItemId, panel);
            rows.add(panel);
        }

        shownRowByRootItemId = rowByRootItemId;
        panelByRootItemId = panels;
    }

    /**
     * "Recipes", with an eye on the right that shows or hides rows switched off.
     */
    private JPanel recipesHeading() {
        updateEye(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                offRowsShown = !offRowsShown;
                offRowsShownSaver.accept(offRowsShown);
                updateEye(true);
                drawRows(shownRowByRootItemId);
                revalidate();
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                updateEye(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateEye(false);
            }
        });

        final JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(heading("Recipes"), BorderLayout.WEST);
        heading.add(eye, BorderLayout.EAST);
        return heading;
    }

    private void updateEye(boolean hovered) {
        final BufferedImage image = offRowsShown ? EYE_OPEN : EYE_SHUT;
        eye.setIcon(new ImageIcon(tint(image, hovered ? BUTTON_HOVER : BUTTON)));
        eye.setToolTipText(offRowsShown ? "Hide rows switched off" : "Show rows switched off");
    }

    /**
     * The image in one colour, each pixel keeping its own transparency. The eye's shape is drawn
     * entirely in transparency, so ImageUtil.fillImage - which makes every visible pixel solid -
     * turns it into a blob.
     */
    private static BufferedImage tint(BufferedImage image, Color color) {
        final BufferedImage tinted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final int rgb = color.getRGB() & 0xFFFFFF;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                tinted.setRGB(x, y, (image.getRGB(x, y) & 0xFF000000) | rgb);
            }
        }

        return tinted;
    }

    private static JLabel heading(String text) {
        final JLabel heading = new JLabel(text);
        heading.setFont(FontManager.getRunescapeBoldFont());
        heading.setForeground(Color.WHITE);
        return heading;
    }

    private AsyncBufferedImage spriteOf(int itemId) {
        return spriteByItemId.computeIfAbsent(itemId, itemManager::getImage);
    }

    private String name(int itemId) {
        return itemManager.getItemComposition(itemId).getName();
    }
}
