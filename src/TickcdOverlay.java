package net.runelite.client.plugins.tickcd;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Iterator;
import javax.inject.Inject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class TickcdOverlay extends OverlayPanel {
    private final TickcdPlugin plugin;
    private final TickcdConfig config;

    @Inject
    private TickcdOverlay(TickcdPlugin plugin, TickcdConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.setPosition(OverlayPosition.DYNAMIC);
        this.setPriority(Overlay.PRIORITY_HIGH);
        this.setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Iterator<NpcInfo> npcIterator = this.plugin.npcList.iterator();

        while(npcIterator.hasNext()) {
            NpcInfo npcInfo = npcIterator.next();
            String textOverlay = Integer.toString(npcInfo.ticks);
            if (npcInfo.ticks < 0){
                npcIterator.remove();
                continue;
            }

            Point textLoc = npcInfo.currNPC.getCanvasTextLocation(graphics, textOverlay, this.config.textZ());
            if (textLoc != null) {
                Point pointShadow = new Point(textLoc.getX() + 1, textLoc.getY() + 1);
                Font oldFont = graphics.getFont();
                graphics.setFont(new Font("Arial", Font.PLAIN, this.config.textSize()));
                OverlayUtil.renderTextLocation(graphics, pointShadow, textOverlay, Color.BLACK);
                OverlayUtil.renderTextLocation(graphics, textLoc, textOverlay, npcInfo.color);
                graphics.setFont(oldFont);
            }
        }

        return super.render(graphics);
    }
}
