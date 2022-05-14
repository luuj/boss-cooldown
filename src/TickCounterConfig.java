package net.runelite.client.plugins.tickcounter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

import java.awt.*;

@ConfigGroup("Inferno")
public interface TickCounterConfig extends Config {
    @ConfigItem(
            name = "NPC List",
            keyName = "allNPC",
            description = "NPCid, NPCanimation, NPCticks",
            position = 0
    )
    default String allNPC() {
        return "";
    }

    @ConfigItem(
            keyName = "npcColor",
            name = "Highlight Color",
            description = "Color of tick counter",
            position = 1,
            section = "overlay"
    )
    default Color npcColor()
    {
        return Color.CYAN;
    }

    @Range(
            min = 5,
            max = 50
    )
    @ConfigItem(
            keyName = "textSize",
            name = "Text Size",
            description = "Sets the text size of the ticks overlay",
            position = 2,
            section = "overlay"
    )
    default int textSize() {
        return 30;
    }

    @Range(
            min = 0,
            max = 300
    )
    @ConfigItem(
            name = "Z Offset",
            keyName = "textZ",
            description = "",
            position = 3,
            section = "overlay"
    )
    default int textZ() {
        return 50;
    }
}
