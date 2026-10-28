package adamski.ui;

import adamski.app.HerbloreApp;
import adamski.app.ResultListener;
import adamski.domain.Recipe;
import adamski.app.HerbloreResult;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HerblorePanel extends PluginPanel implements ResultListener {
    private final ItemManager itemManager;
    private final ClientThread clientThread;
    private final HerbloreApp app;
    private final RowViewFactory rowViewFactory;

    private final JLabel total = new JLabel("No data yet - open your bank");
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
     */
    public HerblorePanel(ItemManager itemManager, ClientThread clientThread, HerbloreApp app,
                         Map<Integer, List<Recipe>> productRecipesByItem) {
        this.itemManager = itemManager;
        this.clientThread = clientThread;
        this.app = app;
        this.rowViewFactory = new RowViewFactory(this::name, productRecipesByItem);

        setLayout(new BorderLayout());

        total.setFont(FontManager.getRunescapeBoldFont());
        total.setForeground(Color.WHITE);
        total.setBorder(new EmptyBorder(0, 0, 8, 0));

        rows.setOpaque(false);
        secondaries.setOpaque(false);
        secondaries.setBorder(new EmptyBorder(10, 0, 0, 0));

        final JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(total, BorderLayout.NORTH);
        content.add(rows, BorderLayout.CENTER);
        content.add(secondaries, BorderLayout.SOUTH);

        add(content, BorderLayout.NORTH);
    }

    @Override
    public void onResultChanged(HerbloreResult result) {
        // Names resolve here - ItemManager cannot be touched from the EDT
        final String totalText = String.format("Banked XP: %,.0f", result.getTotalXp());

        // Rows worth nothing stay listed - a row stopped at an unf vial still needs its picker.
        // Kept in the domain's order here; RowOrder decides where they sit.
        final List<RowView> rowViews = result.getChainResults().stream()
                .map(rowViewFactory::rowOf)
                .collect(Collectors.toList());

        final List<SecondaryView> secondaryViews = rowViewFactory.secondariesOf(result.getSecondaryBalance());
        final Integer pickedOnRootItemId = pickingOnRootItemId;

        SwingUtilities.invokeLater(() -> {
            final Runnable render = () -> render(totalText, rowViews, pickedOnRootItemId, secondaryViews);

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

    private void render(String totalText, List<RowView> rowViews, Integer pickedOnRootItemId,
                        List<SecondaryView> secondaryViews) {
        total.setText(totalText);

        final Map<Integer, RowView> rowByRootItemId = new HashMap<>();
        for (RowView row : rowViews) {
            rowByRootItemId.put(row.getRootItemId(), row);
        }

        final List<Integer> arranged = rowOrder.arrange(
                rowViews.stream().map(RowView::getRootItemId).collect(Collectors.toList()),
                pickedOnRootItemId);

        final Map<Integer, RowPanel> panels = new HashMap<>();
        rows.removeAll();
        for (int rootItemId : arranged) {
            final RowView row = rowByRootItemId.get(rootItemId);

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

        secondaries.removeAll();
        if (!secondaryViews.isEmpty()) {
            final JLabel heading = new JLabel("Secondaries");
            heading.setFont(FontManager.getRunescapeBoldFont());
            heading.setForeground(Color.WHITE);
            secondaries.add(heading);

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

    private AsyncBufferedImage spriteOf(int itemId) {
        return spriteByItemId.computeIfAbsent(itemId, itemManager::getImage);
    }

    private String name(int itemId) {
        return itemManager.getItemComposition(itemId).getName();
    }
}
