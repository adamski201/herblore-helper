package adamski;

import adamski.app.HerbloreApp;
import adamski.infrastructure.PotionStorageAdapter;
import adamski.infrastructure.RuneLiteAdapter;
import adamski.ui.HerblorePanel;
import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@PluginDescriptor(
        name = "Herblore Helper"
)
public class HerblorePlugin extends net.runelite.client.plugins.Plugin {
    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private EventBus eventBus;
    @Inject
    private RuneLiteAdapter adapter;
    @Inject
    private PotionStorageAdapter potionStorageAdapter;
    @Inject
    private HerbloreApp service;

    /**
     * Everything registered with the EventBus. Registering and unregistering the
     * whole list keeps startUp and shutDown symmetric as sources are added.
     */
    private final List<Object> subscribers = new ArrayList<>();

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
        service.addListener(panel);

        // LAST!
        // RuneLiteAdapter is the only subscriber - it drives the other sources so
        // that "sources settle, then publish" stays explicit program order.
        subscribers.addAll(Collections.singletonList(adapter));
        subscribers.forEach(eventBus::register);
    }

    @Override
    protected void shutDown() throws Exception {
        // FIRST!
        subscribers.forEach(eventBus::unregister);
        subscribers.clear();

        service.removeListener(panel);
        clientToolbar.removeNavigation(navButton);
        panel = null;
        navButton = null;
    }

    @Provides
    HerbloreConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HerbloreConfig.class);
    }
}
