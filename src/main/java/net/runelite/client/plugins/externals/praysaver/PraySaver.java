/*
 * Copyright (c) 2019-2020, ganom <https://github.com/Ganom>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.externals.customswapper;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Prayer;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.vars.InterfaceTab;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.externals.utils.ExtUtils;
import net.runelite.client.plugins.externals.utils.Tab;
import net.runelite.client.util.Clipboard;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;
import net.runelite.api.events.GameTick;
import org.apache.commons.lang3.tuple.Pair;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Pray Saver",
	description = "Save your pray while you slay",
	tags = {"slayer"},
	type = PluginType.UTILITY
)
@Slf4j
@SuppressWarnings("unused")
@PluginDependency(ExtUtils.class)
public class PraySaver extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private KeyManager keyManager;
	@Inject
	private CustomSwapperConfig config;
	@Inject
	private ExtUtils utils;

	private ExecutorService executor;
	private Robot robot;
//TODO implement bum mode
	@Provides
	CustomSwapperConfig getConfig(ConfigManager manager)
	{
		return manager.getConfig(PraySaverConfig.class);
	}

	@Override
	protected void startUp() throws AWTException
	{
		//TODO default running state true?
		executor = Executors.newFixedThreadPool(1);
		robot = new Robot();
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			keyManager.registerKeyListener(hotkey);
		}
	}

	@Override
	protected void shutDown()
	{
		executor.shutdown();
		//TODO disable running state?
		keyManager.unregisterKeyListener(hotkey);
	}

	@Subscribe
	public void onGameTick() {
		final Widget widget = client.getWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);

		//TODO check healthbar not visible
		if (widget.getSpriteId() == 1058 && client.getLocalPlayer().getHealth() == null) {
			disable();
		}// else if (widget.getSpriteId() != 1058) {
		//	log.debug("Quick pray: already disabled");
		//}

	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			//TODO disable running state?
			keyManager.unregisterKeyListener(hotkey);
			return;
		}
		//TODO enable running state?
		keyManager.registerKeyListener(hotkey);
	}

	private void disable() {
		final Widget widget = client.getWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);

		if (widget == null)
		{
			log.debug("Quick pray: Can't find valid widget");
			continue;
		}

		extUtils.click(widget.getBounds());
	}

	private void dispatchError(String error)
	{
		String str = ColorUtil.wrapWithColorTag("Pray Saver", Color.MAGENTA)
			+ " has encountered an "
			+ ColorUtil.wrapWithColorTag("error", Color.RED)
			+ ": "
			+ error;

		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", str, null);
	}

	private long getMillis()
	{
		return (long) (Math.random() * config.randLow() + config.randHigh());
	}

	private final HotkeyListener hotkey = new HotkeyListener(() -> config.hotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			//TODO change this to enable/disable running state
			decode(config.customSwapOne());
		}
	};
}
