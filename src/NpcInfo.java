package net.runelite.client.plugins.tickcd;

import java.awt.Color;
import net.runelite.api.NPC;

public class NpcInfo {
    public NPC currNPC;
    public int ticks;
    public Color color;

    public NpcInfo(NPC currNPC, int ticks, Color color) {
        this.currNPC = currNPC;
        this.ticks = ticks;
        this.color = color;
    }
}
