package adamski.ui;

import adamski.app.HerbloreListener;
import adamski.app.HerbloreResult;
import adamski.domain.ItemQuantities;
import adamski.domain.ChainResult;
import adamski.domain.ChainItemXp;
import adamski.domain.ChainRecipeXp;
import adamski.domain.SecondaryBalance;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HerblorePanel extends PluginPanel implements HerbloreListener {
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
    public void onResultChanged(HerbloreResult result) {
        // Names resolve here - ItemManager cannot be touched from the EDT
        final String text = formatChainResults(result.getChainResults())
                + formatSecondaries(result.getSecondaryBalance())
                + formatItems(result.getOwned());

        final String total = String.format("Banked XP: %,.0f", result.getTotalXp());

        SwingUtilities.invokeLater(() -> {
            status.setText(total);
            contents.setText(text);
        });
    }

    private String formatChainResults(List<ChainResult> chainResults) {
        final StringBuilder sb = new StringBuilder("XP by row\n");

        chainResults.stream()
                .filter(chainResult -> chainResult.getXp() > 0)
                .sorted(Comparator.comparingDouble(ChainResult::getXp).reversed())
                .forEach(chainResult -> {
                    sb.append(String.format("%7s  %s -> %s%n", abbreviate(chainResult.getXp()),
                            name(RowLabels.nameFor(chainResult.getEntryItemId())), name(chainResult.getProductItemId())));

                    sb.append(String.format("%7s    %s%n", abbreviate(chainResult.getOutputQuantity()), "made"));

                    sb.append("         by item\n");
                    for (ChainItemXp contribution : chainResult.getItemContributions()) {
                        if (contribution.getXp() == 0) continue;
                        sb.append(String.format("%7s    %s%n", abbreviate(contribution.getXp()),
                                name(contribution.getEntryItemId())));
                    }

                    sb.append("         by recipe\n");
                    for (ChainRecipeXp contribution : chainResult.getRecipeContributions()) {
                        sb.append(String.format("%7s    %s%n", abbreviate(contribution.getXp()),
                                name(contribution.getRecipe().getOutput().getItemId())));
                    }
                });

        return sb.toString();
    }

    private String formatSecondaries(SecondaryBalance balance) {
        if (balance.getNet().isEmpty()) return "";

        final StringBuilder sb = new StringBuilder("\nSecondaries\n");

        balance.getNet().asMap().entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> sb.append(String.format("%7s  %s%n", abbreviate(e.getValue()), name(e.getKey()))));

        return sb.toString();
    }

    private String formatItems(ItemQuantities owned) {
        final StringBuilder sb = new StringBuilder("\nBanked\n");

        owned.asMap().entrySet().stream()
                .collect(Collectors.toMap(e -> name(e.getKey()), Map.Entry::getValue, Double::sum))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(e -> sb.append(String.format("%7s  %s%n", abbreviate(e.getValue()), e.getKey())));

        return sb.toString();
    }

    private String name(int itemId) {
        return itemManager.getItemComposition(itemId).getName();
    }

    /**
     * Numbers go first - the panel is about 32 monospace characters wide, and anything right-aligned
     * past the item name is clipped off the edge without warning.
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
