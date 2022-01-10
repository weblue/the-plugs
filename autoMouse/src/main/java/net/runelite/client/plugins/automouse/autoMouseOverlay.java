/*
 * Copyright (c) 2019-2020, ganom <https://github.com/Ganom>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.automouse;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class autoMouseOverlay extends Overlay
{
	private static final Color FLASH_COLOR = new Color(255, 0, 0, 70);
	private final Client client;
	private final autoMouse plugin;
	private final autoMouseConfig config;
	private int timeout;

	@Inject
	autoMouseOverlay(Client client, autoMouse plugin, autoMouseConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Point point = client.getMouseCanvasPosition();
		if (point != null && point.getX() >= 0 && point.getY() >= 0) {
			drawBigDot(graphics, Color.red, point.getX(), point.getY());
		}

		if (plugin.isFlash() && config.flash())
		{
			final Color flash = graphics.getColor();
			graphics.setColor(FLASH_COLOR);
			graphics.fill(new Rectangle(client.getCanvas().getSize()));
			graphics.setColor(flash);
			timeout++;
			if (timeout >= 50)
			{
				timeout = 0;
				plugin.setFlash(false);
			}
		}
		return null;
	}

	public static void drawBigDot(Graphics2D graphics, Color color, int x, int y) {
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
		graphics.fillRect(x, y, 4, 4);
	}

}
