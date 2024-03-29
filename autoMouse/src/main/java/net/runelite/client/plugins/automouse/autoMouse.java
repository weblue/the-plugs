/*
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.automouse;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.automouse.support.Flow;
import net.runelite.client.plugins.automouse.support.FlowTemplates;
import net.runelite.client.plugins.automouse.support.Pair;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;
import static net.runelite.client.plugins.automouse.Edge.*;
import static net.runelite.client.plugins.automouse.MovementPattern.*;

@SuppressWarnings("BusyWait")
@Extension
@PluginDescriptor(name = "Auto Mouse", tags = "automation", enabledByDefault = false)
@Slf4j
public class autoMouse extends Plugin {
    @Inject
    private Client client;
    @Inject
    private autoMouseConfig config;
    @Inject
    private autoMouseOverlay overlay;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ConfigManager configManager;
    @Inject
    private KeyManager keyManager;

    private ExecutorService executorService;
    private SecureRandom random = new SecureRandom();
    private boolean run;
    private boolean offScreenNoClick = false;
    private Point startingPos;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private boolean flash;

    @Provides
    autoMouseConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(autoMouseConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        keyManager.registerKeyListener(hotkeyListener);
        executorService = Executors.newSingleThreadExecutor();
        startingPos = new Point(random.nextInt(753), random.nextInt(463));
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(hotkeyListener);
        executorService.shutdown();
    }

    private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            run = !run;
            if (!run) {
                return;
            }

            startingPos = client.getMouseCanvasPosition();
            executorService.submit(() -> clickLoop());
        }
    };

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getKey().equals("click") && Boolean.parseBoolean(event.getNewValue()) && config.start() && run) {
            //External "click" event
            // Look over the timings here
            executorService.submit(() -> {
                if (client.getGameState() != GameState.LOGGED_IN || checkHitpoints() || checkInventory()) {
                    if (config.flash()) {
                        setFlash(true);
                    }
                    return;
                }

                try {
                    //mess with
                    long delay = randomConfiguredDelay(10, 100);
                    log.info("Random single click delay is: " + delay);
                    sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                executorService.submit(this::click);

                configManager.setConfiguration("autoMouse", "click", Boolean.FALSE);
            });
        }
    }

    public void click() {
        assert !client.isClientThread();

        Point curPos = client.getMouseCanvasPosition();

        curPos = getStretchedPoint(curPos);

        mouseEvent(501, curPos);
        mouseEvent(502, curPos);
        mouseEvent(500, curPos);
    }

    //If this is ever in a separate thread, make sure to wait for return
    public Point move(Point origin) {
        assert !client.isClientThread();

        Point destination;

        double rand = Math.random();
        MovementPattern movement = rand > .49 ? NO_MOVE : (rand > .20 ? SMALL_MOVE : (rand > .10 ? BIG_MOVE : MOVE_OFF_SCREEN));
        log.info("Next movement type: {}", movement.name());

        switch (movement) {
            case NO_MOVE:
                long delay = randomConfiguredDelay(config.deviation(), config.target());
//                log.info("No move; sleeping for {}", delay);
                try {
                    sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return origin;
            case MOVE_OFF_SCREEN:
                offScreenNoClick = true;
                rand = Math.random();
                Edge edge = rand > .75 ? NORTH : (rand > .50 ? EAST : (rand > .25 ? SOUTH : WEST));
                log.info("Moving off screen to the: {}", edge.name());

                switch(edge) {
                    case NORTH:
                        destination = new Point( random.nextInt(1506) - 653, -1 - random.nextInt(284));
                        break;
                    case EAST:
                        destination = new Point( random.nextInt(300) + 754, random.nextInt(926) - 263);
                        break;
                    case SOUTH:
                        destination = new Point( random.nextInt(1506) - 653, random.nextInt(284) + 464);
                        break;
                    case WEST:
                    default:
                        destination = new Point( -1 - random.nextInt(284), random.nextInt(926) - 263);
                        break;
                }

                break;
            case BIG_MOVE:
                //30 pixels less than the max on the x axis to reduce inv + minimap clicks TODO figure out real number
                destination = new Point(random.nextInt(723), random.nextInt(463));
                break;
            case SMALL_MOVE:
            default:
                destination = new Point(origin.getX() + (int) Math.floor(random.nextGaussian() * 7) - 3, origin.getY() + (int) Math.floor(random.nextGaussian() * 7) - 3);
                break;
        }

        List<Point> points = genPoints(origin, getStretchedPoint(destination));

        points.forEach((point1) -> {
            long delta = (long) Math.floor(Math.abs(random.nextGaussian()) * 20);
            long time = System.currentTimeMillis() + delta;
            if ((point1.getX() <= 753 && point1.getX() >= 0) && (point1.getY() <= 463 && point1.getY() >= 0)) {
                if ((client.getMouseCanvasPosition().getX() == 0 && client.getMouseCanvasPosition().getY() == 0)) {
                    client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_ENTERED, time, 0, point1.getX(), point1.getY(), 0, false, 0));
                } else {
                    client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_MOVED, time, 0, point1.getX(), point1.getY(), 0, false, 0));
                }
            } else {
                client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_EXITED, time, 0, point1.getX(), point1.getY(), 0, false, 0));
            }

            try {
                sleep(delta);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        if(!(client.getMouseCanvasPosition().getX() == 0 && client.getMouseCanvasPosition().getY() == 0)) {
            offScreenNoClick = false;
        } else {
            //TODO this isn't going off
            log.info("Mouse off screen, offScreenNoClick: {}", offScreenNoClick);
        }

        //TODO this is currently broken because of timing
        log.info("current mouse position: {}, {} last destination: {}, {}", client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY(), destination.getX(), destination.getY());
        return getStretchedPoint(destination);
    }

    private Point getStretchedPoint(Point destination) {
        if (client.isStretchedEnabled()) {
            final Dimension stretched = client.getStretchedDimensions();
            final Dimension real = client.getRealDimensions();
            final double width = (stretched.width / real.getWidth());
            final double height = (stretched.height / real.getHeight());
//            log.info("stretched width: {}, stretched height: {}, real width: {}, real height: {}", stretched.width, stretched.height, real.width, real.height);
            destination = new Point((int)(destination.getX() * width), (int)(destination.getY() * height));
        }
        return destination;
    }

    private void clickLoop() {
        assert !client.isClientThread();

        Point origin = startingPos;
//        Point origin = new Point(random.nextInt(753), random.nextInt(463));

        while (run) {
            if (client.getGameState() == GameState.LOADING) {
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            GameState butt = client.getGameState();
            if (butt != GameState.LOGGED_IN || checkHitpoints() || checkInventory()) {
                run = false;
                if (config.flash()) {
                    setFlash(true);
                }
                break;
            }

            if (config.move()) {
                origin = move(origin);
//                origin = getStretchedPoint(client.getMouseCanvasPosition());
            }
            if (!offScreenNoClick)
                click();

            try {
                long delay = randomConfiguredDelay(config.deviation(), config.target());
                log.info("Random loop delay is: " + delay);
                sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void mouseEvent(int id, @NotNull Point point) {
        MouseEvent e = new MouseEvent(client.getCanvas(), id, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, 1);

        client.getCanvas().dispatchEvent(e);
    }

//    private long randomDelay() {
//        /* generate a normal even distribution random */
//        return (long) Math.abs(random.nextGaussian());
//    }

    /**
     * Generate a gaussian random (average at 0.0, std dev of 1.0)
     * take the absolute value of it (if we don't, every negative value will be clamped at the minimum value)
     * get the log base e of it to make it shifted towards the right side
     * invert it to shift the distribution to the other end
     * clamp it to min max, any values outside of range are set to min or max
     */
    //Target is pretty useless
    //TODO change this to generate more randomly, while still normalizing towards target
    private long randomConfiguredDelay(int deviation, int target) {
        return (long) clamp((-Math.log(Math.abs(random.nextGaussian()))) * deviation + target);
    }

    private double clamp(double val) {
        return Math.max(config.min(), Math.min(config.max(), val));
    }

    private boolean checkHitpoints() {
        if (!config.autoDisableHp()) {
            return false;
        }
        return client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.hpThreshold();
    }

    private boolean checkInventory() {
        if (!config.autoDisableInv()) {
            return false;
        }
        final Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        return inventoryWidget.getWidgetItems().size() == 28;
    }

    private List<Point> genPoints(Point curPoint, Point nextPoint) {
        int lastMousePositionX = curPoint.getX();
        int lastMousePositionY = curPoint.getY();
        int xDistance = nextPoint.getX() - lastMousePositionX;
        int yDistance = nextPoint.getY() - lastMousePositionY;

        int minSteps = 5;

        List<Point> points = new ArrayList<>();

        double distance = Math.hypot(xDistance, yDistance);
        //TODO change this
        Pair<Flow, Long> flowTime = getFlowWithTime(distance, 500);
        Flow flow = flowTime.x;
        long mouseMovementMs = flowTime.y;

        log.debug("Movement arc length computed to {} and time predicted to {} ms", distance, mouseMovementMs);

      /* Number of steps is calculated from the movement time and limited by minimal amount of steps
         (should have at least MIN_STEPS) and distance (shouldn't have more steps than pixels travelled) */
        int steps = (int) Math.ceil(Math.min(distance, Math.max(mouseMovementMs / 8, minSteps)));

        double deviationMultiplierX = (random.nextDouble() - 0.5) * 2;
        double deviationMultiplierY = (random.nextDouble() - 0.5) * 2;

        double completedXDistance = 0;
        double completedYDistance = 0;

        double simulatedMouseX = curPoint.getX();
        double simulatedMouseY = curPoint.getY();

        for (int i = 0; i < steps; i++) {
            // All steps take equal amount of time. This is a value from 0...1 describing how far along the process is.
            double timeCompletion = i / (double) steps;

            int effectFadeSteps = 15;

            double effectFadeStep = Math.max(i - (steps - effectFadeSteps) + 1, 0);
            // value from 0 to 1, when effectFadeSteps remaining steps, starts to decrease to 0 linearly
            // This is here so noise and deviation wouldn't add offset to mouse final position, when we need accuracy.
            double effectFadeMultiplier = (effectFadeSteps - effectFadeStep) / effectFadeSteps;

            double xStepSize = flow.getStepSize(xDistance, steps, timeCompletion);
            double yStepSize = flow.getStepSize(yDistance, steps, timeCompletion);

            completedXDistance += xStepSize;
            completedYDistance += yStepSize;
            double completedDistance = Math.hypot(completedXDistance, completedYDistance);
            double completion = Math.min(1, completedDistance / distance);
            log.trace("Step: x: {} y: {} tc: {} c: {}", xStepSize, yStepSize, timeCompletion, completion);

            Point deviation = getDeviation(distance, completion);

            simulatedMouseX += xStepSize;
            simulatedMouseY += yStepSize;

            int mousePosX = roundTowards(simulatedMouseX + deviation.getX() * deviationMultiplierX * effectFadeMultiplier, nextPoint.getX());

            int mousePosY = roundTowards(simulatedMouseY + deviation.getY() * deviationMultiplierY * effectFadeMultiplier, nextPoint.getY());

            points.add(new Point(mousePosX, mousePosY));

        }
        return points;
    }

    private static int roundTowards(double value, int target) {
        if (target > value) {
            return (int) Math.ceil(value);
        } else {
            return (int) Math.floor(value);
        }
    }

    private Point getDeviation(double totalDistanceInPixels, double completionFraction) {
        double deviationFunctionResult = (1 - Math.cos(completionFraction * Math.PI * 2)) / 2;

        double deviationX = totalDistanceInPixels / 10;
        double deviationY = totalDistanceInPixels / 10;

        return new Point((int) (deviationFunctionResult * deviationX), (int) (deviationFunctionResult * deviationY));
    }

    private Pair<Flow, Long> getFlowWithTime(double distance, long mouseMovementTimeMs) {
        List<Flow> flows = (Arrays.asList(new Flow(FlowTemplates.constantSpeed()), new Flow(FlowTemplates.variatingFlow()), new Flow(FlowTemplates.interruptedFlow()), new Flow(FlowTemplates.interruptedFlow2()), new Flow(FlowTemplates.slowStartupFlow()), new Flow(FlowTemplates.slowStartup2Flow()), new Flow(FlowTemplates.adjustingFlow()), new Flow(FlowTemplates.jaggedFlow()), new Flow(FlowTemplates.stoppingFlow())));

        double time = mouseMovementTimeMs + (long) (Math.random() * mouseMovementTimeMs);
        Flow flow = flows.get((int) (Math.random() * flows.size()));

        // Let's ignore waiting time, e.g 0's in flow, by increasing the total time
        // by the amount of 0's there are in the flow multiplied by the time each bucket represents.
        double timePerBucket = time / (double) flow.getFlowCharacteristics().length;
        for (double bucket : flow.getFlowCharacteristics()) {
            if (Math.abs(bucket - 0) < 10e-6) {
                time += timePerBucket;
            }
        }

        return new Pair<>(flow, (long) time);
    }
}

enum MovementPattern {
    NO_MOVE,
    SMALL_MOVE,
    BIG_MOVE,
    MOVE_OFF_SCREEN
}

enum Edge {
    NORTH,
    EAST,
    SOUTH,
    WEST
}