package net.runelite.client.plugins.externals.praysaver;

import com.google.inject.Provides;

import java.awt.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.externals.utils.ExtUtils;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
        name = "Pray Saver",
        description = "Save your pray while you slay",
        tags = {"slayer"},
        type = PluginType.UTILITY
)
@Slf4j
@SuppressWarnings("unused")
@PluginDependency(ExtUtils.class)
public class PraySaver extends Plugin {
    @Inject
    private Client client;
    @Inject
    private KeyManager keyManager;
    @Inject
    private PraySaverConfig config;
    @Inject
    private ExtUtils utils;

    private boolean running = false;
    private int curTick = 0;
    private ExecutorService executor;

    @Provides
    PraySaverConfig getConfig(ConfigManager manager) {
        return manager.getConfig(PraySaverConfig.class);
    }

    @Override
    protected void startUp() {
        executor = Executors.newFixedThreadPool(1);
        if (client.getGameState() == GameState.LOGGED_IN) {
            keyManager.registerKeyListener(hotkey);
        }
    }

    @Override
    protected void shutDown() {
        executor.shutdown();
        running = false;
        keyManager.unregisterKeyListener(hotkey);
    }

    @Subscribe
    public void onGameTick() {
        if (running) {
            if (curTick == 10 && client.getVar(Varbits.QUICK_PRAYER) == 1) {
                toggle();
            } else if (curTick < 10) {
                curTick += 1;
            }
        }
    }

    @Subscribe
    public void onHitsplat(HitsplatApplied event) {
        if (event.getActor() == client.getLocalPlayer()) {
            curTick = 0;
        }

        if (config.bumMode() && client.getVar(Varbits.QUICK_PRAYER) == 0) {
            toggle();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            keyManager.unregisterKeyListener(hotkey);
            running = false;
            return;
        }
        keyManager.registerKeyListener(hotkey);
    }

    private void toggle() {
        final Widget widget = client.getWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);

        if (widget == null) {
            log.debug("Quick pray: Can't find valid widget");
            return;
        }

        utils.click(widget.getBounds());
    }

    private void dispatchError(String error) {
        String str = ColorUtil.wrapWithColorTag("Pray Saver", Color.MAGENTA)
                + " has encountered an "
                + ColorUtil.wrapWithColorTag("error", Color.RED)
                + ": "
                + error;

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", str, null);
    }

    private final HotkeyListener hotkey = new HotkeyListener(() -> config.hotkey()) {
        @Override
        public void hotkeyPressed() {
            running = !running;
        }
    };
}
