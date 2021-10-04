package net.runelite.client.plugins.externals.praysaver;

import com.google.inject.Provides;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
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
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
        name = "Pray Saver",
        description = "Save your pray while you slay",
        type = PluginType.UTILITY
)
@Slf4j
public class PraySaver extends Plugin {
    @Inject
    private Client client;
    @Inject
    private KeyManager keyManager;
    @Inject
    private PraySaverConfig config;

    private boolean running = false;
    private ExecutorService executor;

    @Provides
    PraySaverConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(PraySaverConfig.class);
    }

    @Override
    protected void startUp() {
        executor = Executors.newFixedThreadPool(1);
        if (client.getGameState() == GameState.LOGGED_IN) {
            keyManager.registerKeyListener(hotkey);
            keyManager.registerKeyListener(qpHotkey);
        }
    }

    @Override
    protected void shutDown() {
        executor.shutdown();
        running = false;
        keyManager.unregisterKeyListener(hotkey);
        keyManager.unregisterKeyListener(qpHotkey);
    }

    @Subscribe
    private void onGameTick(GameTick tick) {
        if (running) {
            if (client.getVar(Varbits.QUICK_PRAYER) == 1 &&
                    client.getLocalPlayer().getHealthRatio() == -1 &&
                    client.getLocalPlayer().getInteracting() == null) {
                toggle();
                log.debug("PraySaver: toggling prayer off");
            } else if (client.getBoostedSkillLevel(Skill.PRAYER) > 0 &&
                    (client.getLocalPlayer().getHealthRatio() > 0 ||
                            client.getLocalPlayer().getInteracting() != null) &&
                    client.getVar(Varbits.QUICK_PRAYER) == 0 &&
                    config.bumMode()) {
                toggle();
                log.debug("PraySaver: toggling prayer on");
            }
        }
    }


    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            keyManager.unregisterKeyListener(hotkey);
            keyManager.unregisterKeyListener(qpHotkey);
            running = false;
        } else {
            keyManager.registerKeyListener(hotkey);
            keyManager.registerKeyListener(qpHotkey);
        }
    }


    private void toggle() {
        final Widget widget = client.getWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);

        if (widget == null) {
            log.debug("PraySaver: Can't find valid widget");
            return;
        }

        click(widget.getBounds());
    }

    private void dispatchError(String msg) {
        String str = ColorUtil.wrapWithColorTag("Pray Saver: ", Color.RED)
                //+ " has encountered an "
                + ColorUtil.wrapWithColorTag(msg, Color.BLACK);
        //+ ": "
        //+ error;

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", str, null);
    }

    /**
     * This method must be called on a new
     * thread, if you try to call it on
     * {@link net.runelite.client.callback.ClientThread}
     * it will result in a crash/desynced thread.
     */
    private void click(Rectangle rectangle) {
        assert !client.isClientThread();
        Point point = getClickPoint(rectangle);
        click(point);
    }

    private void click(Point p) {
        assert !client.isClientThread();

        if (client.isStretchedEnabled()) {
            final Dimension stretched = client.getStretchedDimensions();
            final Dimension real = client.getRealDimensions();
            final double width = (stretched.width / real.getWidth());
            final double height = (stretched.height / real.getHeight());
            final Point point = new Point((int) (p.getX() * width), (int) (p.getY() * height));
            mouseEvent(501, point);
            mouseEvent(502, point);
            mouseEvent(500, point);
            return;
        }
        mouseEvent(501, p);
        mouseEvent(502, p);
        mouseEvent(500, p);
    }

    private Point getClickPoint(@NotNull Rectangle rect) {
        final int x = (int) (rect.getX() + getRandomIntBetweenRange((int) rect.getWidth() / 6 * -1, (int) rect.getWidth() / 6) + rect.getWidth() / 2);
        final int y = (int) (rect.getY() + getRandomIntBetweenRange((int) rect.getHeight() / 6 * -1, (int) rect.getHeight() / 6) + rect.getHeight() / 2);

        return new Point(x, y);
    }

    private int getRandomIntBetweenRange(int min, int max) {
        return (int) ((Math.random() * ((max - min) + 1)) + min);
    }

    private void mouseEvent(int id, @NotNull Point point) {
        MouseEvent e = new MouseEvent(
                client.getCanvas(), id,
                System.currentTimeMillis(),
                0, (int) point.getX(), (int) point.getY(),
                1, false, 1
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

    private final HotkeyListener qpHotkey = new HotkeyListener(() -> config.quickPray()) {
        @Override
        protected void hotkeyPressed() {
            log.debug("PraySaver: Quick pray bind pressed");
            if (client.getVar(Varbits.QUICK_PRAYER) == 0)
                client.setVarbit(Varbits.QUICK_PRAYER, 1);
            else
                client.setVarbit(Varbits.QUICK_PRAYER, 0);
        }
    };
}
