package net.runelite.client.plugins.detachedcamera;

import com.google.inject.Provides;

import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import com.google.common.base.Strings;
import net.runelite.api.VarClientInt;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
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
import net.runelite.client.input.KeyListener;
import net.runelite.api.VarClientStr;

import org.pf4j.Extension;

@Extension
@PluginDescriptor(name = "Detached Camera", description = "Unbind camera from player character", enabledByDefault = false, tags = {
        "hotkey, skilling, bossing, utility" })
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
    @Inject
    private DetachedCameraListener inputListener;

    int oculusX;
    int oculusY;

    private boolean doIt;
    boolean chattingState = false;
    private boolean waitOnDialogs = false;

    private final HotkeyListener cameraHotkey = new HotkeyListener(() -> config.hotkey()) {
        @Override
        public void hotkeyPressed() {
            if (client.getOculusOrbState() == 0) {
                client.setOculusOrbState(1);
                client.setOculusOrbNormalSpeed(36);
            } else {
                client.setOculusOrbState(0);
                client.setOculusOrbNormalSpeed(12);
            }

            toggleInfoBox(client.getOculusOrbState() != 0);
        }
    };

    void toggleInfoBox(boolean show) {
        if (show) {
            infoBoxManager.addInfoBox(
                    new DetachedCameraIndicator(ImageUtil.loadImageResource(getClass(), "buffed.png"), this));
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
        keyManager.registerKeyListener(cameraHotkey);
        keyManager.registerKeyListener(inputListener);
    }

    @Override
    protected void shutDown() {
        infoBoxManager.removeIf(t -> t instanceof DetachedCameraIndicator);
        keyManager.unregisterKeyListener(cameraHotkey);
        keyManager.unregisterKeyListener(inputListener);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        // If det camera enabled and client is world hopping
        if (event.getGameState().equals(GameState.HOPPING) && client.getOculusOrbState() != 0) {
            doIt = true;
        }

        // Wait until game is responsive
        if (doIt && event.getGameState().equals(GameState.LOGGED_IN)) {
            // reset camera
            enable();

            doIt = false;
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (client.getOculusOrbState() != 0) {
            oculusX = client.getOculusOrbFocalPointX();
            oculusY = client.getOculusOrbFocalPointY();
        }

        if (dialogsOpen()) {
            waitOnDialogs = true;

            pause();
        } else if (!dialogsOpen() && waitOnDialogs) {
            waitOnDialogs = false;

            enable();
        }
    }

    private void dispatchError(String msg) {
        String str = ColorUtil.wrapWithColorTag(msg, Color.RED);
        client.addChatMessage(ChatMessageType.PRIVATECHAT, this.getClass().getSimpleName(), str, null);
    }
    
    private boolean dialogsOpen()
	{
        if (client.getWidget(WidgetInfo.BANK_PIN_CONTAINER) != null && !client.getWidget(WidgetInfo.BANK_PIN_CONTAINER).isHidden()) {
            return true;
        }

		Widget chatboxParent = client.getWidget(WidgetInfo.CHATBOX_PARENT);
		if (chatboxParent == null || chatboxParent.getOnKeyListener() == null)
		{
			return false;
		}

		// the search box on the world map can be focused, and chat input goes there, even
		// though the chatbox still has its key listener.
		Widget worldMapSearch = client.getWidget(WidgetInfo.WORLD_MAP_SEARCH);
        if (worldMapSearch != null && client.getVar(VarClientInt.WORLD_MAP_SEARCH_FOCUSED) == 1) {
            return true;
        }

        return false;
	}

    void pause() {
        oculusX = client.getOculusOrbFocalPointX(); 
        oculusY = client.getOculusOrbFocalPointY();
        client.setOculusOrbState(0);
    }

    void enable() {
        client.setOculusOrbState(1);
        client.setOculusOrbFocalPointX(oculusX);
        client.setOculusOrbFocalPointY(oculusY);
    }
}

class DetachedCameraListener implements KeyListener {
    @Inject
    private DetachedCameraPlugin plugin;
    @Inject
    private DetachedCameraConfig config;

    @Inject
    private Client client;

    private boolean blockEnter = false;

    @Override
    public void keyTyped(KeyEvent e) {
        // block enter to stop spam
        if (e.getKeyChar() == KeyEvent.VK_ENTER && blockEnter) {
            e.consume();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // TODO check key remapping config setting for enter to chat, instead of checking for explicit string
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            Widget chatbox = client.getWidget(WidgetInfo.CHATBOX_INPUT);
            if (client.getOculusOrbState() != 0 && chatbox != null
                    && chatbox.getText().contains("Press Enter to Chat...")) {

                plugin.chattingState = true;
                plugin.pause();
            } else if (plugin.chattingState && client.getOculusOrbState() == 0) {
                // User is pressing enter to send and exiting chat
                plugin.chattingState = false;

                plugin.enable();
            } else if (plugin.chattingState) {
                // User has somehow turned detached back on
                plugin.chattingState = false;
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE 
            && plugin.chattingState 
            && client.getOculusOrbState() == 0 
            && Strings.isNullOrEmpty(client.getVar(VarClientStr.CHATBOX_TYPED_TEXT))) {
                // User is pressing escape to cancel/clear and exiting chat
                plugin.chattingState = false;

                plugin.enable();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        final char keyChar = e.getKeyChar();

        if (keyChar == KeyEvent.VK_ENTER) {
            blockEnter = false;
        }
    }
}