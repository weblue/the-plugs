package net.runelite.client.plugins.spacespam;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("spacespam")
public interface SpaceSpamConfig extends Config
{
	@ConfigItem(
			keyName = "hotkey",
			name = "Toggle space spam",
			description = "",
			position = 1
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = ""
	)

}