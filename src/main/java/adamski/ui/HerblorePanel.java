package adamski.ui;

import adamski.app.HerbloreListener;
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
    public void onStateChanged(Map<Integer, Integer> itemQuantities) {
        // Called on the client thread; all Swing work has to happen on the EDT.
        SwingUtilities.invokeLater(() -> render(itemQuantities));
    }

    private void render(Map<Integer, Integer> itemQuantities) {
        setStatus(itemQuantities.size() + " distinct items");

        final StringBuilder sb = new StringBuilder();
        itemQuantities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey())
                        .append(": ")
                        .append(e.getValue())
                        .append('\n'));

        contents.setText(sb.toString());
    }

    void setStatus(String text) {
        status.setText(text);
    }
}
