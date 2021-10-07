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

@ConfigGroup("SlaySaverConfig")
public interface SlaySaverConfig extends Config
{
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
	@ConfigItem(
			keyName = "quickPray",
			name = "Toggle quick pray",
			description = "",
			position = 2,
			titleSection = "hotkeys"
	)
	default Keybind quickPray()
	{
		return Keybind.NOT_SET;
	}
}
