package adamski;

import adamski.app.HerbloreApp;
import adamski.data.Recipes;
import adamski.domain.route.RecipeGraph;
import adamski.infrastructure.RuneLiteAdapter;
import adamski.infrastructure.SelectionPersistence;
import adamski.ui.HerblorePanel;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@PluginDescriptor(name = "Herblore Helper")
public class HerblorePlugin extends net.runelite.client.plugins.Plugin {
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private ItemManager itemManager;
    @Inject
    private ClientThread clientThread;
    @Inject
    private EventBus eventBus;
    @Inject
    private RuneLiteAdapter adapter;
    @Inject
    private SelectionPersistence selectionPersistence;
    @Inject
    private HerbloreApp app;
    @Inject
    private RecipeGraph graph;

    private final List<Object> subscribers = new ArrayList<>();

    private HerblorePanel panel;
    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception {
        panel = new HerblorePanel(itemManager, clientThread, app, graph.findProductRecipesByItem());

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
        navButton = NavigationButton.builder().tooltip("Herblore Helper").icon(icon).priority(100).panel(panel).build();

        clientToolbar.addNavigation(navButton);
        app.addResultListener(panel);
        app.addSelectionListener(selectionPersistence);

        // already logged in when the plugin is switched on, so no profile change is coming
        clientThread.invoke(selectionPersistence::load);

        // LAST!
        subscribers.add(adapter);
        subscribers.add(selectionPersistence);
        subscribers.forEach(eventBus::register);
    }

    @Override
    protected void shutDown() throws Exception {
        // FIRST!
        subscribers.forEach(eventBus::unregister);
        subscribers.clear();

        app.removeSelectionListener(selectionPersistence);
        app.removeResultListener(panel);
        clientToolbar.removeNavigation(navButton);
        panel = null;
        navButton = null;
    }

    @Provides
    HerbloreConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HerbloreConfig.class);
    }

    @Provides
    @Singleton
    RecipeGraph provideRecipeGraph() {
        return new RecipeGraph(Recipes.all());
    }
}
