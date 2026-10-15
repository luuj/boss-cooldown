package net.runelite.client.plugins.tickcd;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class TickcdOverlay extends Overlay {
    private final TickcdPlugin plugin;
    private final TickcdConfig config;
    private Font cachedFont;
    private int cachedFontSize;

    @Inject
    private TickcdOverlay(TickcdPlugin plugin, TickcdConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_HIGH);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (plugin.npcInfos.isEmpty()) {
            return null;
        }

        // Font and z-offset are the same for every NPC, so set them once per frame.
        Font oldFont = graphics.getFont();
        graphics.setFont(getFont());
        int zOffset = config.textZ();

        // Expired entries are removed on the game tick; render only reads the map.
        for (NpcInfo npcInfo : plugin.npcInfos.values()) {
            if (npcInfo.ticks <= 0) {
                continue;
            }

            String text = Integer.toString(npcInfo.ticks);
            Point textLoc = npcInfo.currNPC.getCanvasTextLocation(graphics, text, zOffset);
            if (textLoc != null) {
                // OverlayUtil already draws a black 1px shadow under the text.
                OverlayUtil.renderTextLocation(graphics, textLoc, text, npcInfo.color);
            }
        }

        graphics.setFont(oldFont);
        return null;
    }

    private Font getFont() {
        int textSize = config.textSize();
        if (cachedFont == null || cachedFontSize != textSize) {
            cachedFont = new Font("Arial", Font.PLAIN, textSize);
            cachedFontSize = textSize;
        }

        return cachedFont;
    }
}
