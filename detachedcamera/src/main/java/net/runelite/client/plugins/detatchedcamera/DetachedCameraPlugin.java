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
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;

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

    private final HotkeyListener cameraHotkey = new HotkeyListener(() -> config.hotkey()) {
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
            // toggleInfoBox(client.getOculusOrbState() != 0);
            // dispatchError(client.getOculusOrbState() != 0 ? "Detached cam enabled" :
            // "Detached cam disabled");
        }
    };
    // TODO do this functionality when pin interface pops up or choice dialog
    // TODO check key remapping config setting for enter to chat, instead of
    // checking for explicit string
    // TODO fix when user escapes or backspaces out of chat
    /*
     * private final HotkeyListener chatHotkey = new HotkeyListener(() -> new
     * Keybind(10, 0)) {
     * 
     * @Override public void hotkeyPressed() { Widget chatbox =
     * client.getWidget(WidgetInfo.CHATBOX_INPUT);
     * 
     * if (client.getOculusOrbState() != 0 && chatbox != null &&
     * chatbox.getText().contains("Press Enter to Chat...")) { // User is pressing
     * enter and has key remapping enabled chattingState = true;
     * 
     * oculusX = client.getOculusOrbFocalPointX(); oculusY =
     * client.getOculusOrbFocalPointY(); client.setOculusOrbState(0); } else if
     * (chattingState && client.getOculusOrbState() == 0) { // User is pressing
     * enter to send and exiting chat chattingState = false;
     * 
     * client.setOculusOrbState(1); client.setOculusOrbFocalPointX(oculusX);
     * client.setOculusOrbFocalPointY(oculusY); } else if (chattingState) { // User
     * has somehow turned detached back on chattingState = false; } } };
     */

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
        // keyManager.registerKeyListener(chatHotkey);
        keyManager.registerKeyListener(inputListener);
    }

    @Override
    protected void shutDown() {
        infoBoxManager.removeIf(t -> t instanceof DetachedCameraIndicator);
        keyManager.unregisterKeyListener(cameraHotkey);
        // keyManager.unregisterKeyListener(chatHotkey);
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
            client.setOculusOrbState(1);

            client.setOculusOrbFocalPointX(oculusX);
            client.setOculusOrbFocalPointY(oculusY);

            doIt = false;
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (client.getOculusOrbState() != 0) {
            oculusX = client.getOculusOrbFocalPointX();
            oculusY = client.getOculusOrbFocalPointY();
        }
    }

    private void dispatchError(String msg) {
        String str = ColorUtil.wrapWithColorTag(msg, Color.RED);
        client.addChatMessage(ChatMessageType.PRIVATECHAT, this.getClass().getSimpleName(), str, null);
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
        if (e.getKeyChar() == KeyEvent.VK_ENTER && blockEnter /* && plugin.chatboxFocused() */) {
            e.consume();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        /*
         * if (!plugin.chatboxFocused()) { //if not focused on chatbox (or world
         * search), return return; }
         */
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            Widget chatbox = client.getWidget(WidgetInfo.CHATBOX_INPUT);
            if (client.getOculusOrbState() != 0 && chatbox != null
                    && chatbox.getText().contains("Press Enter to Chat...")) {

                plugin.chattingState = true;
                plugin.oculusX = client.getOculusOrbFocalPointX(); 
                plugin.oculusY = client.getOculusOrbFocalPointY();
                client.setOculusOrbState(0);
            } else if (plugin.chattingState && client.getOculusOrbState() == 0) {
                // User is pressing enter to send and exiting chat
                plugin.chattingState = false;

                client.setOculusOrbState(1);
                client.setOculusOrbFocalPointX(plugin.oculusX);
                client.setOculusOrbFocalPointY(plugin.oculusY);
            } else if (plugin.chattingState) {
                // User has somehow turned detached back on
                plugin.chattingState = false;
            }
            // TODO this may be useful to lock oculus back on when attemping to exit chat
            /*
             * case KeyEvent.VK_ESCAPE: // When exiting typing mode, block the escape key //
             * so that it doesn't trigger the in-game hotkeys e.consume();
             * plugin.setTyping(false); clientThread.invoke(() -> {
             * client.setVar(VarClientStr.CHATBOX_TYPED_TEXT, ""); plugin.lockChat(); });
             * break;
             */

            // TODO see above todo
            /*
             * case KeyEvent.VK_BACK_SPACE: // Only lock chat on backspace when the typed
             * text is now empty if
             * (Strings.isNullOrEmpty(client.getVar(VarClientStr.CHATBOX_TYPED_TEXT))) {
             * plugin.setTyping(false); clientThread.invoke(plugin::lockChat); } break;
             */
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        final int keyCode = e.getKeyCode();
        final char keyChar = e.getKeyChar();

        if (keyChar == KeyEvent.VK_ENTER) {
            blockEnter = false;
        }
    }
}