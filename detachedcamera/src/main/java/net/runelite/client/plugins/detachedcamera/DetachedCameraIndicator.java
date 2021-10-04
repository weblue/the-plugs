package net.runelite.client.plugins.detachedcamera;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

public class DetachedCameraIndicator extends InfoBox{

    public DetachedCameraIndicator(BufferedImage image, Plugin plugin) {
        super(image, plugin);
    }

    @Override
    public String getText() {
        return "Det Cam";
    }

    @Override
    public Color getTextColor() {
        return Color.CYAN;
    }
    
}
