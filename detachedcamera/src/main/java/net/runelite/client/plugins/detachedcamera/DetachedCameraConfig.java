package net.runelite.client.plugins.detachedcamera;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("detachedcamera")
public interface DetachedCameraConfig extends Config
{
	@ConfigItem(
			keyName = "hotkey",
			name = "Toggle camera mode",
			description = "",
			position = 1
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

}