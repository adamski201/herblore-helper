package adamski.components;

import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class HerblorePanel extends PluginPanel {
    private final JLabel status = new JLabel("No data yet");

    public HerblorePanel() {
        setLayout(new BorderLayout());
        add(status, BorderLayout.NORTH);
    }

    void setStatus(String text) {
        status.setText(text);
    }
}
