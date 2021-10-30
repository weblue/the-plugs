/*
 * Copyright (c) 2019-2020, ganom <https://github.com/Ganom>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.automouse;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("autoMouse")
public interface autoMouseConfig extends Config
{
	@ConfigSection(
			position = 0,
			keyName = "mainConfig",
			name = "Main Toggles",
			description = ""
	)
	String mainConfig = "Main Toggles";

	@ConfigItem(
			keyName = "toggle",
			name = "Toggle",
			description = "Toggles the clicker.",
			position = 0,
			section = mainConfig
	)
	default Keybind toggle()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "start",
			name = "Listen for external clicks",
			description = "Listen for external clicks from other plugins",
			position = 0,
			section = mainConfig
	)
	default boolean start()
	{
		return false;
	}

	@ConfigItem(
			keyName = "click",
			name = "Click",
			description = "Externally accessed single click",
			position = 0,
			section = mainConfig
	)
	default boolean click()
	{
		return false;
	}

	@ConfigSection(
			position = 0,
			keyName = "boundsConfig",
			name = "Boundaries of auto clicke",
			description = ""
	)
	String boundsConfig = "Boundaries";

	@ConfigItem(
			keyName = "miniDelay",
			name = "Absolute Delay Min",
			description = "",
			position = 3,
			section = boundsConfig
	)
	default int min()
	{
		return 120;
	}

	@ConfigItem(
			keyName = "maxiDelay",
			name = "Absolute Delay Max",
			description = "",
			position = 4,
			section = boundsConfig
	)
	default int max()
	{
		return 240;
	}

	@ConfigItem(
			keyName = "target",
			name = "Delay Target",
			description = "",
			position = 5,
			section = boundsConfig
	)
	default int target()
	{
		return 180;
	}

	@ConfigItem(
			keyName = "deviation",
			name = "Delay Deviation",
			description = "",
			position = 6,
			section = boundsConfig
	)
	default int deviation()
	{
		return 10;
	}

	@ConfigItem(
			keyName = "weightedDistribution",
			name = "Weighted Distribution",
			description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
			position = 7,
			section = boundsConfig
	)
	default boolean weightedDistribution()
	{
		return false;
	}

	@ConfigSection(
			position = 1,
			keyName = "helperConfig",
			name = "Helper Config",
			description = ""
	)
	String helperConfig = "Helper Config";

	@ConfigItem(
			keyName = "autoDisable",
			name = "Auto Disable at low HP",
			description = "Automatically disables the clicker when you get to low hp.",
			position = 0,
			section = helperConfig
	)
	default boolean autoDisableHp()
	{
		return false;
	}

	@ConfigItem(
			keyName = "autoDisableInv",
			name = "Disable when Inv is full",
			description = "Automatically disables the clicker when your inventory is full..",
			position = 1,
			section = helperConfig
	)
	default boolean autoDisableInv()
	{
		return false;
	}

	@Range(
			min = 3,
			max = 98
	)
	@ConfigItem(
			keyName = "hpThreshold",
			name = "Hp Threshold",
			description = "The hp in which the plugin will auto disable.",
			position = 2,
			hidden = true,
			unhide = "autoDisable",
			section = helperConfig
	)
	default int hpThreshold()
	{
		return 200;
	}

	@ConfigItem(
			keyName = "flash",
			name = "Flash on Low HP",
			description = "Your Screen flashes when you get to low hp.",
			position = 3,
			hidden = true,
			unhide = "autoDisable",
			section = helperConfig
	)
	default boolean flash()
	{
		return false;
	}
}
