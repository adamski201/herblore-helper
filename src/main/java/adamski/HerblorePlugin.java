package adamski;

import adamski.app.HerbloreApp;
import adamski.app.HerbloreSettings;
import adamski.data.Recipes;
import adamski.data.SecondaryPrecursors;
import adamski.domain.route.RecipeGraph;
import adamski.domain.SecondaryPrecursor;
import adamski.infrastructure.RuneLiteAdapter;
import adamski.infrastructure.SelectionPersistence;
import adamski.ui.HerblorePanel;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

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
    @Inject
    private HerbloreConfig config;
    @Inject
    private ConfigManager configManager;

    private final List<Object> subscribers = new ArrayList<>();

    private HerblorePanel panel;
    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception {
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
        panel = new HerblorePanel(itemManager, clientThread, app, graph.findProductRecipesByItem(), icon,
                this::openSettings, config.offRowsShown(),
                shown -> configManager.setConfiguration(HerbloreConfig.GROUP, HerbloreConfig.OFF_ROWS_SHOWN, shown));

        navButton = NavigationButton.builder().tooltip("Herblore Helper").icon(icon).priority(100).panel(panel).build();

        clientToolbar.addNavigation(navButton);
        app.addResultListener(panel);
        app.addSelectionListener(selectionPersistence);

        // already logged in when the plugin is switched on, so no profile change is coming
        clientThread.invoke(selectionPersistence::load);
        clientThread.invoke(adapter::readCurrentXp);
        applyConfig();

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

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        // ignore events from other plugins
        if (!HerbloreConfig.GROUP.equals(event.getGroup())) return;

        applyConfig();
    }

    private void openSettings() {
        eventBus.post(new OverlayMenuClicked(
                new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, null, null), new SettingsOverlay(this)));
    }

    private static final class SettingsOverlay extends Overlay {
        SettingsOverlay(net.runelite.client.plugins.Plugin plugin) {
            super(plugin);
        }

        @Override
        public Dimension render(Graphics2D graphics) {
            return null;
        }
    }

    private void applyConfig() {
        final HerbloreSettings settings = new HerbloreSettings(
                config.countSecondariesMadeByRows(),
                config.countSecondaryPrecursors(),
                config.caviarSuccessRate() / 100.0,
                seedYields());

        clientThread.invoke(() -> app.applySettings(settings));
    }

    private Map<Integer, Double> seedYields() {
        return Map.ofEntries(
                entry(ItemID.GUAM_SEED, config.guamSeedYield()),
                entry(ItemID.MARRENTILL_SEED, config.marrentillSeedYield()),
                entry(ItemID.TARROMIN_SEED, config.tarrominSeedYield()),
                entry(ItemID.HARRALANDER_SEED, config.harralanderSeedYield()),
                entry(ItemID.RANARR_SEED, config.ranarrSeedYield()),
                entry(ItemID.TOADFLAX_SEED, config.toadflaxSeedYield()),
                entry(ItemID.IRIT_SEED, config.iritSeedYield()),
                entry(ItemID.AVANTOE_SEED, config.avantoeSeedYield()),
                entry(ItemID.KWUARM_SEED, config.kwuarmSeedYield()),
                entry(ItemID.HUASCA_SEED, config.huascaSeedYield()),
                entry(ItemID.SNAPDRAGON_SEED, config.snapdragonSeedYield()),
                entry(ItemID.CADANTINE_SEED, config.cadantineSeedYield()),
                entry(ItemID.LANTADYME_SEED, config.lantadymeSeedYield()),
                entry(ItemID.DWARF_WEED_SEED, config.dwarfWeedSeedYield()),
                entry(ItemID.TORSTOL_SEED, config.torstolSeedYield()),
                entry(ItemID.CORAL_ELKHORN_FRAG, config.elkhornCoralYield()),
                entry(ItemID.CORAL_PILLAR_FRAG, config.pillarCoralYield()),
                entry(ItemID.CORAL_UMBRAL_FRAG, config.umbralCoralYield()));
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

    @Provides
    @Singleton
    List<SecondaryPrecursor> providePrecursors() {
        return SecondaryPrecursors.all();
    }
}
