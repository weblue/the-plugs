package net.runelite.client.plugins.spacespam;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigTitleSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Title;

@ConfigGroup("SpaceSpamConfig")
public interface SpaceSpamConfig extends Config
{

	@ConfigTitleSection(
			position = 1,
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
			name = "Toggle space spam",
			description = "",
			position = 1,
			titleSection = "hotkeys"
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

}