package net.runelite.client.plugins.detachedcamera;

import com.google.inject.Provides;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;

import org.pf4j.Extension;

@Extension
@PluginDescriptor(
        name = "Detached Camera",
        description = "Unbind camera from player character",
        enabledByDefault = false,
        tags = {"hotkey, skilling, bossing, utility"}
)
@Slf4j
public class DetachedCameraPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private KeyManager keyManager;
    @Inject
    private DetachedCameraConfig config;
	@Inject
	private InfoBoxManager infoBoxManager;

    private int oculusX;
    private int oculusY;

    private boolean doIt;

    private final HotkeyListener hotkey = new HotkeyListener(() -> config.hotkey()) {
        @Override
        public void hotkeyPressed() {
            log.info("Toggled detached camera");
            if (client.getOculusOrbState() == 0) {
                client.setOculusOrbState(1);
                client.setOculusOrbNormalSpeed(36);
            } else {
                client.setOculusOrbState(0);
                client.setOculusOrbNormalSpeed(12);
            }
            //toggleInfoBox(client.getOculusOrbState() != 0);
            dispatchError(client.getOculusOrbState() != 0 ? "Detached cam enabled" : "Detached cam disabled");
        }
    };

    void toggleInfoBox(boolean show) {
        if (show) {
            infoBoxManager.addInfoBox(new DetachedCameraIndicator(ImageUtil.loadImageResource(getClass(), "buffed.png"), this));
        } else {
            infoBoxManager.removeIf(t -> t instanceof DetachedCameraIndicator);
        }
    }

    @Provides
    DetachedCameraConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(DetachedCameraConfig.class);
    }

    @Override
    protected void startUp() {
        keyManager.registerKeyListener(hotkey);
    }

    @Override
    protected void shutDown() {
        infoBoxManager.removeIf(t -> t instanceof DetachedCameraIndicator);
        keyManager.unregisterKeyListener(hotkey);
    }

   @Subscribe
   public void onGameStateChanged(GameStateChanged event) {
        // If det camera enabled and client is world hopping
        if (event.getGameState().equals(GameState.HOPPING) && client.getOculusOrbState() != 0) {
            doIt = true;
            log.info("bouta do it");
        }
   }

   @Subscribe
   public void onGameTick(GameTick tick) {
        oculusX = client.getOculusOrbFocalPointX();
        oculusY = client.getOculusOrbFocalPointY();

        // onGameTick implies game is active again
        if (doIt) {
            // reset camera
            // client.setOculusOrbState(0);
            client.setOculusOrbState(1);
            client.setOculusOrbFocalPointX(oculusX);
            client.setOculusOrbFocalPointY(oculusY);

            log.info("doin it");

            doIt = false;
        }
   }

    private void dispatchError(String msg) {
        String str = ColorUtil.wrapWithColorTag(msg, Color.RED);
        client.addChatMessage(ChatMessageType.PRIVATECHAT, this.getClass().getSimpleName(), str, null);
    }
}