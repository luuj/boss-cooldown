package net.runelite.client.plugins.tickcd;

import java.awt.Color;
import net.runelite.api.NPC;

public class NpcInfo {
    public final NPC currNPC;
    public int ticks;
    public Color color;
    public int attackTicks;
    public Color attackColor;

    public NpcInfo(NPC currNPC, int ticks, Color color) {
        this.currNPC = currNPC;
        this.ticks = ticks;
        this.color = color;
        this.attackTicks = ticks;
        this.attackColor = color;
    }
}
