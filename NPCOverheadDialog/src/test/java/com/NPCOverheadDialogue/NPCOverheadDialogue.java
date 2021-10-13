package com.NPCOverheadDialogue;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class NPCOverheadDialogue
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(NPCOverheadDialoguePlugin.class);
		RuneLite.main(args);
	}
}