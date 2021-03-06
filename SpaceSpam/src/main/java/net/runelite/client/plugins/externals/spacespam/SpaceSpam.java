package net.runelite.client.plugins.externals.spacespam;

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
import net.runelite.client.plugins.PluginType;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
        name = "Space Spam",
        description = "Everybody get up, it's time to spam now",
        type = PluginType.SKILLING
)
@Slf4j
public class SpaceSpam extends Plugin {
    @Inject
    private Client client;
    @Inject
    private KeyManager keyManager;
    @Inject
    private SpaceSpamConfig config;

    private boolean running = false;
    private ExecutorService executor;

    @Provides
    SpaceSpamConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(SpaceSpamConfig.class);
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

/*    @Subscribe
    private void onGameTick(GameTick tick) {
        if (running &&
                client.getWidget(WidgetInfo.MULTI_SKILL_MENU) != null) {
                //!client.getWidget(WidgetInfo.MULTI_SKILL_MENU).isHidden()) {
            press();
        }
    }*/

    @Subscribe
    private void onWidgetLoaded(WidgetLoaded event)
    {
        if (running &&
                event.getGroupId() == WidgetID.MULTISKILL_MENU_GROUP_ID) {
            press();
        }
    }


//    @Subscribe
//    public void onGameStateChanged(GameStateChanged event) {
//        if (event.getGameState() != GameState.LOGGED_IN) {
//            keyManager.unregisterKeyListener(hotkey);
//            dispatchError("Game state change causing run to stop");
//            running = false;
//        } else {
//            keyManager.registerKeyListener(hotkey);
//        }
//    }

    private void dispatchError(String msg) {
        String str = ColorUtil.wrapWithColorTag("Space Spam: ", Color.RED)
                //+ " has encountered an "
                + ColorUtil.wrapWithColorTag(msg, Color.BLACK);
        //+ ": "
        // + error;

        client.addChatMessage(ChatMessageType.PRIVATECHAT, "", str, null);
    }

    private void press() {
        assert !client.isClientThread();

        keyEvent(KeyEvent.KEY_PRESSED, KeyEvent.VK_SPACE, ' ');
        keyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, ' ');
        keyEvent(KeyEvent.KEY_RELEASED, KeyEvent.VK_SPACE, ' ');
    }

    private void keyEvent(int id, int key, char c) {
        KeyEvent e = new KeyEvent(
                client.getCanvas(),
                id,
                System.currentTimeMillis(),
                0,
                key,
                c
        );

        client.getCanvas().dispatchEvent(e);
    }

    private final HotkeyListener hotkey = new HotkeyListener(() -> config.hotkey()) {
        @Override
        public void hotkeyPressed() {
            log.debug("PraySaver: hotkey pressed");
            running = !running;
            dispatchError(Boolean.toString(running));
        }
    };
}
