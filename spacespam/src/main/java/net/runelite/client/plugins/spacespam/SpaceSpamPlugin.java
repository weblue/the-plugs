package net.runelite.client.plugins.spacespam;

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
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
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
        name = "Space Spam",
        description = "Everybody get up, it's time to spam now",
        enabledByDefault = false,
        tags = {"automation, hotkey, skilling"}
)
@Slf4j
public class SpaceSpamPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private KeyManager keyManager;
    @Inject
    private SpaceSpamConfig config;
	@Inject
	private InfoBoxManager infoBoxManager;

    private boolean running = false;
    private ExecutorService executor;

    private final HotkeyListener hotkey = new HotkeyListener(() -> config.hotkey()) {
        @Override
        public void hotkeyPressed() {
            running = !running;
            toggleInfoBox(running);
            log.info(running ? "Space spam enabled" : "Space spam disabled");
            //dispatchError(running ? "Space spam enabled" : "Space spam disabled");
        }
    };

    void toggleInfoBox(boolean show) {
        if (show) {
            infoBoxManager.addInfoBox(
                new SpaceSpamIndicator(ImageUtil.loadImageResource(getClass(), "buffed.png"), this)
            );
        } else {
            infoBoxManager.removeIf(t -> t instanceof SpaceSpamIndicator);
        }
    }

    @Provides
    SpaceSpamConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(SpaceSpamConfig.class);
    }

    @Override
    protected void startUp() {
        executor = Executors.newSingleThreadExecutor();
        keyManager.registerKeyListener(hotkey);
    }

    @Override
    protected void shutDown() {
        executor.shutdown();
        infoBoxManager.removeIf(t -> t instanceof SpaceSpamIndicator);
        running = false;
        keyManager.unregisterKeyListener(hotkey);
    }

    @Subscribe
    private void onWidgetLoaded(WidgetLoaded event)
    {
        if (running && event.getGroupId() == WidgetID.MULTISKILL_MENU_GROUP_ID) {
            executor.submit(this::press);
        }
    }

    private void dispatchError(String msg) {
        String str = ColorUtil.wrapWithColorTag(msg, Color.RED);
        client.addChatMessage(ChatMessageType.PRIVATECHAT, "Space Spam", str, null);
    }

    private void press() {
        assert !client.isClientThread();

        keyEvent(KeyEvent.KEY_PRESSED, KeyEvent.VK_SPACE, ' ');
        keyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, ' ');
        keyEvent(KeyEvent.KEY_RELEASED, KeyEvent.VK_SPACE, ' ');
    }

    private void keyEvent(int id, int key, char c) {
        KeyEvent e = 
            new KeyEvent(client.getCanvas(), id, System.currentTimeMillis(), 0, key, c);

        client.getCanvas().dispatchEvent(e);
    }
}