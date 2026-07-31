package adamski;

import adamski.components.HerblorePanel;
import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
        name = "Herblore Helper"
)
public class HerblorePlugin extends net.runelite.client.plugins.Plugin {
    @Inject
    private ClientToolbar clientToolbar;

    private HerblorePanel panel;
    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception {
        panel = new HerblorePanel();

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

        navButton = NavigationButton.builder()
                .tooltip("Herblore Helper")
                .icon(icon)
                .priority(100)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navButton);
    }

    @Provides
    HerbloreConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HerbloreConfig.class);
    }
}
