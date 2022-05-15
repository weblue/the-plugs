package net.runelite.client.plugins.clickForwarding;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ContainableFrame;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.logging.Logger;

@Extension
@PluginDescriptor(name = "Click Forwarding", description = "Forwards external clicks to the game client", enabledByDefault = false, tags = {
        "hotkey, skilling, bossing, utility" })
@Slf4j
public class ClickForwardingPlugin extends Plugin implements MouseListener, NativeMouseInputListener {

    @Inject
    private Client client;

//    @Inject
//    private ClientThread clientThread;

    @Inject
    private MouseManager mouseManager;

    @Getter
    private int lastClickX = -1;
    @Getter
    private int lastClickY = -1;
    @Getter
    private int lastMoveX = -1;
    @Getter
    private int lastMoveY = -1;


    @Override
    protected void startUp() throws NativeHookException {
        mouseManager.registerMouseListener(this);
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeMouseListener(this);
        GlobalScreen.addNativeMouseMotionListener(this);
        Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(java.util.logging.Level.OFF);
        log.info("Click forwarding started");
    }

    @Override
    protected void shutDown() throws NativeHookException {
        mouseManager.unregisterMouseListener(this);
        GlobalScreen.unregisterNativeHook();
        GlobalScreen.removeNativeMouseListener(this);
        GlobalScreen.removeNativeMouseMotionListener(this);
        log.info("Click forwarding stopped");
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent mouseEvent)
    {
        setLastClick(mouseEvent.getX(), mouseEvent.getY());
        return mouseEvent;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent mouseEvent)
    {
        setLastClick(mouseEvent.getX(), mouseEvent.getY());
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent mouseEvent)
    {
        setLastClick(mouseEvent.getX(), mouseEvent.getY());
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent mouseEvent)
    {
        setLastMove(mouseEvent.getX(), mouseEvent.getY());
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent mouseEvent)
    {
        setLastMove(mouseEvent.getX(), mouseEvent.getY());
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent mouseEvent)
    {
        setLastMove(mouseEvent.getX(), mouseEvent.getY());
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent mouseEvent)
    {
        setLastMove(mouseEvent.getX(), mouseEvent.getY());
        return mouseEvent;
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent nativeEvent)
    {
        log.info("nativeMouseClicked");
        nativeMouseInput(new NativeMouseInput(
                nativeEvent.getX(),
                nativeEvent.getY(),
                nativeEvent.getButton(),
                NativeMouseInput.Type.CLICK
        ));
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent nativeEvent)
    {
        nativeMouseInput(new NativeMouseInput(
                nativeEvent.getX(),
                nativeEvent.getY(),
                nativeEvent.getButton(),
                NativeMouseInput.Type.PRESS
        ));
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent nativeEvent)
    {
        nativeMouseInput(new NativeMouseInput(
                nativeEvent.getX(),
                nativeEvent.getY(),
                nativeEvent.getButton(),
                NativeMouseInput.Type.RELEASE
        ));
    }

    @Override
    public void nativeMouseMoved(NativeMouseEvent nativeEvent)
    {
        nativeMouseInput(new NativeMouseInput(
                nativeEvent.getX(),
                nativeEvent.getY(),
                nativeEvent.getButton(),
                NativeMouseInput.Type.MOVEMENT
        ));
    }

    private void setLastClick(int x, int y)
    {
        lastClickX = x;
        lastClickY = y;
    }

    private void setLastMove(int x, int y)
    {
        lastMoveX = x;
        lastMoveY = y;
    }

    public void nativeMouseInput(NativeMouseInput event)
    {

        double eventX = event.getX();
        double eventY = event.getY();

        ContainableFrame frame = ClientUI.getFrame();

        if (frame.getBounds().contains(eventX, eventY))
        {
            return;
        }

        GraphicsDevice screen = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
                .filter(device -> device.getDefaultConfiguration().getBounds().contains(event.getX(), event.getY()))
                .findFirst()
                .orElse(null);
        if (screen == null)
        {
            log.debug("Screen not found to forward mouse event");
            return;
        }

        double screenWidth = screen.getDisplayMode().getWidth();
        double screenHeight = screen.getDisplayMode().getHeight();

        if (eventX < 0)
        {
            eventX = screenWidth + eventX;
        }
        else if (eventX > screenWidth)
        {
            eventX = eventX - screenWidth;
        }

        if (eventY < 0)
        {
            eventY = screenHeight + eventY;
        }
        else if (eventY > screenHeight)
        {
            eventY = eventY - screenHeight;
        }

        int finalEventX = (int) eventX;
        int finalEventY = (int) eventY;

        int x = (int) (finalEventX * ((double) client.getCanvasWidth() / screenWidth));
        int y = (int) (finalEventY * ((double) client.getCanvasHeight() / screenHeight));

        switch (event.getType())
        {
            case PRESS:
                int button = event.getButton();

//                clientThread.invoke(() ->
//                {

                    log.info(
                            "Forwarding mouse press [{}] from [{}, {}, {}] to canvas [{}, {}]",
                            button,
                            screen.getIDstring(),
                            finalEventX,
                            finalEventY,
                            x,
                            y
                    );

                    mouseEvent(MouseEvent.MOUSE_PRESSED, new Point(x, y));
//                });

                break;

            case RELEASE:
                mouseEvent(MouseEvent.MOUSE_RELEASED, new Point(lastClickX, lastClickY));
                mouseEvent(MouseEvent.MOUSE_CLICKED, new Point(lastClickX, lastClickY));
                break;

            case MOVEMENT:
                mouseEvent(MouseEvent.MOUSE_MOVED, new Point(x, y));
                break;
        }
    }

    private void mouseEvent(int id, @NotNull Point point) {
        MouseEvent e = new MouseEvent(client.getCanvas(), id, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, 1);

        client.getCanvas().dispatchEvent(e);
    }
}

@Value
class NativeMouseInput {
    int x;
    int y;
    int button;
    Type type;

    public enum Type
    {
        MOVEMENT, CLICK, PRESS, RELEASE
    }
}