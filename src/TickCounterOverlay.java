package net.runelite.client.plugins.tickcd;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Iterator;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public class TickCounterOverlay extends OverlayPanel {
    private final Client client;
    private final TickCounterPlugin plugin;
    private final TickCounterConfig config;

    @Inject
    private TickCounterOverlay(Client client, TickCounterPlugin plugin, TickCounterConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.setPosition(OverlayPosition.DYNAMIC);
        this.setPriority(OverlayPriority.HIGH);
        this.setLayer(OverlayLayer.ABOVE_SCENE);
    }

    public Dimension render(Graphics2D graphics) {
        Iterator var2 = this.plugin.npcList.iterator();

        while(var2.hasNext()) {
            NpcInfo npcInfo = (NpcInfo)var2.next();
            String textOverlay = Integer.toString(npcInfo.ticks);

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
