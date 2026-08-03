package adamski.ui;

import adamski.app.HerbloreListener;
import adamski.data.HerbloreRecipes;
import adamski.domain.models.BankedXpResult;
import adamski.domain.models.ItemSource;
import adamski.domain.models.Recipe;
import adamski.domain.models.SecondaryBalance;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public class HerblorePanel extends PluginPanel implements HerbloreListener {
    /**
     * Recipes are identified by id in the result; the panel labels them by what they consume.
     */
    private static final Map<Integer, Integer> PRIMARY_BY_RECIPE = HerbloreRecipes.all().stream()
            .collect(Collectors.toMap(Recipe::getId, recipe -> recipe.getPrimary().getItemId()));

    private final ItemManager itemManager;

    private final JLabel status = new JLabel("No data yet");
    private final JTextArea contents = new JTextArea();

    public HerblorePanel(ItemManager itemManager) {
        this.itemManager = itemManager;

        setLayout(new BorderLayout());

        contents.setEditable(false);
        contents.setLineWrap(false);
        contents.setOpaque(false);
        contents.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        contents.setForeground(Color.WHITE);

        add(status, BorderLayout.NORTH);
        add(contents, BorderLayout.CENTER);
    }

    @Override
    public void onStateChanged(Map<ItemSource, Map<Integer, Integer>> snapshot,
                               Map<ItemSource, Map<Integer, Integer>> delta,
                               BankedXpResult bankedXp,
                               SecondaryBalance secondaryBalance) {
        // Called on the client thread. Item names have to be resolved here, because ItemManager
        // cannot be touched from the EDT - only the finished strings cross over.
        final String text = formatItems(snapshot) + formatXp(bankedXp) + formatSecondaries(secondaryBalance);
        final String total = String.format("Banked XP: %,.0f", bankedXp.getTotal());

        SwingUtilities.invokeLater(() -> {
            status.setText(total);
            contents.setText(text);
        });
    }

    private String formatItems(Map<ItemSource, Map<Integer, Integer>> snapshot) {
        final StringBuilder sb = new StringBuilder();

        snapshot.forEach((source, items) -> {
            sb.append(source).append('\n');

            items.entrySet().stream()
                    .collect(Collectors.toMap(e -> name(e.getKey()), Map.Entry::getValue, Integer::sum))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .forEach(e -> sb.append(String.format("%7s  %s%n", abbreviate(e.getValue()), e.getKey())));

            sb.append('\n');
        });

        return sb.toString();
    }

    /**
     * One line per recipe that earned anything, labelled by what it consumes. The calculator omits
     * zero-XP recipes, so every line here is a real contribution.
     */
    private String formatXp(BankedXpResult bankedXp) {
        final StringBuilder sb = new StringBuilder("XP by primary\n");

        bankedXp.getXpPerRecipe().entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .forEach(e -> sb.append(String.format("%7s  %s%n",
                        abbreviate(e.getValue()), name(PRIMARY_BY_RECIPE.get(e.getKey())))));

        return sb.toString();
    }

    /**
     * Secondaries you are short of, worst first, then the ones you have spare. Only items some
     * recipe actually consumes appear - the balance has nothing to say about anything else.
     */
    private String formatSecondaries(SecondaryBalance balance) {
        if (balance.getNet().isEmpty()) return "";

        final StringBuilder sb = new StringBuilder("\nSecondaries\n");

        balance.getNet().entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> sb.append(String.format("%7s  %s%n", abbreviate(e.getValue()), name(e.getKey()))));

        return sb.toString();
    }

    private String name(int itemId) {
        return itemManager.getItemComposition(itemId).getName();
    }

    /**
     * The panel is about 32 monospace characters wide, so numbers go first in a narrow column -
     * anything right-aligned past the item name gets clipped off the edge and silently vanishes.
     */
    private static String abbreviate(double value) {
        final String sign = value < 0 ? "-" : "";
        final double magnitude = Math.abs(value);

        if (magnitude >= 1_000_000) return sign + String.format("%.1fm", magnitude / 1_000_000);
        if (magnitude >= 10_000) return sign + String.format("%.0fk", magnitude / 1_000);
        if (magnitude >= 1_000) return sign + String.format("%.1fk", magnitude / 1_000);

        return sign + String.format("%.0f", magnitude);
    }
}
