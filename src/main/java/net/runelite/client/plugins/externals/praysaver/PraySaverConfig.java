/*
 * Copyright (c) 2019-2020, ganom <https://github.com/Ganom>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.externals.praysaver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigTitleSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Title;

@ConfigGroup("praySaver")
public interface PraySaverConfig extends Config
{

	@ConfigTitleSection(
		keyName = "config",
		name = "Config",
		description = "",
		position = 0
	)
	default Title config()
	{
		return new Title();
	}

	@ConfigItem(
		keyName = "swapBack",
		name = "Swap back to inventory",
		description = "Once finished with a swap, should it swap back to inventory?",
		titleSection = "config",
		position = 1
	)
	default boolean swapBack()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enablePrayCheck",
		name = "Active Prayer Check",
		description = "Enabling this will make it so you can't toggle prayers if they're on.",
		titleSection = "config",
		position = 2
	)
	default boolean enablePrayCheck()
	{
		return true;
	}

	@ConfigItem(
		keyName = "randLow",
		name = "Minimum MS Delay",
		description = "Dont set this too high.",
		titleSection = "config",
		position = 3
	)
	default int randLow()
	{
		return 70;
	}

	@ConfigItem(
		keyName = "randLower",
		name = "Maximum MS Delay",
		description = "Dont set this too high.",
		titleSection = "config",
		position = 4
	)
	default int randHigh()
	{
		return 80;
	}

	@ConfigTitleSection(
		position = 1,
		keyName = "mainConfig",
		name = "Main Config",
		description = ""
	)
	default Title mainConfig()
	{
		return new Title();
	}

	@ConfigItem(
		keyName = "bumMode",
		name = "Enable pray on enter combat",
		description = "Makes it even lazier",
		position = 1,
		titleSection = "mainConfig"
	)
	default boolean bumMode()
	{
		return false;
	}

	@ConfigTitleSection(
		position = 2,
		keyName = "hotkeys",
		name = "Hotkeys",
		description = ""
	)
	default Title hotkeys()
	{
		return new Title();
	}

	@ConfigItem(
		keyName = "hotkey",
		name = "Toggle pray saver",
		description = "",
		position = 1,
		titleSection = "hotkeys"
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}
}
