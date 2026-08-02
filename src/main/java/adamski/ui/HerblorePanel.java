package adamski.ui;

import adamski.app.HerbloreListener;
import adamski.domain.models.ItemSource;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class HerblorePanel extends PluginPanel implements HerbloreListener {
    private final JLabel status = new JLabel("No data yet");
    private final JTextArea contents = new JTextArea();

    public HerblorePanel() {
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
                               Map<ItemSource, Map<Integer, Integer>> delta) {
        // Called on the client thread; all Swing work has to happen on the EDT.
        SwingUtilities.invokeLater(() -> render(snapshot));
    }

    private void render(Map<ItemSource, Map<Integer, Integer>> snapshot) {
        final int distinct = snapshot.values().stream().mapToInt(Map::size).sum();
        setStatus(distinct + " distinct items across " + snapshot.size() + " sources");

        final StringBuilder sb = new StringBuilder();

        snapshot.forEach((source, items) -> {
            sb.append(source).append(" (").append(items.size()).append(")\n");

            items.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sb.append("  ")
                            .append(e.getKey())
                            .append(": ")
                            .append(e.getValue())
                            .append('\n'));

            sb.append('\n');
        });

        contents.setText(sb.toString());
    }

    void setStatus(String text) {
        status.setText(text);
    }
}
