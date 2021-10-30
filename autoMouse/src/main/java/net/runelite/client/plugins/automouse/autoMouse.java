/*
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.automouse;

import com.google.inject.Provides;

import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.ClientUI;
import org.jetbrains.annotations.NotNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Auto Mouse",
	tags = "automation",
	enabledByDefault = false
)
@Slf4j
public class autoMouse extends Plugin
{
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
	@Inject
	private ClientUI clientUi;

	private ExecutorService executorService;
	private Point point;
	private SecureRandom random;
	private boolean run;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private boolean flash;

	@Provides
	autoMouseConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(autoMouseConfig.class);
	}

	@Override
	protected void startUp() {
		overlayManager.add(overlay);
		keyManager.registerKeyListener(hotkeyListener);
		executorService = Executors.newSingleThreadExecutor();
		random = new SecureRandom();
	}

	@Override
	protected void shutDown() {
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(hotkeyListener);
		executorService.shutdown();
		random = null;
	}

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.toggle()) {
		@Override
		public void hotkeyPressed() {
			run = !run;
			if (!run) {
				return;
			}
			point = client.getMouseCanvasPosition();

			executorService.submit(() -> clickLoop());
		}
	};

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getKey().equals("click") && Boolean.parseBoolean(event.getNewValue()) && config.start()) {
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
					long delay = randomConfiguredDelay();
					log.info("Random single click delay is: " + delay);
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Point p = new Point(random.nextInt(463), random.nextInt(753));
				log.info(p.toString());
				executorService.submit(() -> click(p));

				configManager.setConfiguration("autoMouse", "click", Boolean.FALSE);
			});
		}
	}

	public void click(Point p)
	{
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

	private void clickLoop() {
		assert !client.isClientThread();

		while (run) {
			if (client.getGameState() != GameState.LOGGED_IN || checkHitpoints() || checkInventory()) {
				run = false;
				if (config.flash())
				{
					setFlash(true);
				}
				break;
			}

			click(point);

			try {
				long delay = randomConfiguredDelay();
				log.info("Random loop delay is: " + delay);
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void mouseEvent(int id, @NotNull Point point) {
		MouseEvent e = new MouseEvent(
				client.getCanvas(), id,
				System.currentTimeMillis(),
				0, point.getX(), point.getY(),
				1, false, 1
		);

		client.getCanvas().dispatchEvent(e);
	}

	private long randomDelay() {
		/* generate a normal even distribution random */
		return (long) Math.abs(random.nextGaussian());
	}

	/**
	 * Generate a gaussian random (average at 0.0, std dev of 1.0)
	 * take the absolute value of it (if we don't, every negative value will be clamped at the minimum value)
	 * get the log base e of it to make it shifted towards the right side
	 * invert it to shift the distribution to the other end
	 * clamp it to min max, any values outside of range are set to min or max
	 */
	private long randomConfiguredDelay() {
		return (long) clamp((-Math.log(Math.abs(random.nextGaussian()))) * config.deviation() + config.target());
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
}
