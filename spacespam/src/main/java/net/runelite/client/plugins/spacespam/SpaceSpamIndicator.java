package net.runelite.client.plugins.spacespam;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

public class SpaceSpamIndicator extends InfoBox{

    public SpaceSpamIndicator(BufferedImage image, Plugin plugin) {
        super(image, plugin);
    }

    @Override
    public String getText() {
        return "Sp^2";
    }

    @Override
    public Color getTextColor() {
        return Color.WHITE;
    }
    
}
