package net.runelite.client.plugins.tickcd;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("tickcd")
public interface TickcdConfig extends Config {
    @ConfigSection(
            name = "Animations",
            description = "Animation counter settings",
            position = 0
    )
    String animSettings = "animSettings";
    @ConfigItem(
            name = "NPC Animation List",
            keyName = "allNPC",
            description = "NPC name, animation id, ticks, color number",
            position = 1,
            section = "animSettings"
    )
    default String allNPC() {
        return "";
    }

    @ConfigItem(
            keyName = "npcColor",
            name = "Tick Number Color 1",
            description = "Color of tick counter 1",
            position = 2,
            section = "animSettings"
    )
    default Color npcColor()
    {
        return Color.CYAN;
    }

    @ConfigItem(
            keyName = "npcColor2",
            name = "Tick Number Color 2",
            description = "Color of tick counter 2",
            position = 3,
            section = "animSettings"
    )
    default Color npcColor2()
    {
        return Color.GREEN;
    }

    @ConfigItem(
            keyName = "npcColor3",
            name = "Tick Number Color 3",
            description = "Color of tick counter 3",
            position = 4,
            section = "animSettings"
    )
    default Color npcColor3()
    {
        return Color.RED;
    }

    @ConfigItem(
            keyName = "npcColor4",
            name = "Tick Number Color 4",
            description = "Color of tick counter 4",
            position = 5,
            section = "animSettings"
    )
    default Color npcColor4()
    {
        return Color.MAGENTA;
    }

    @ConfigItem(
            keyName = "npcColor5",
            name = "Tick Number Color 5",
            description = "Color of tick counter 5",
            position = 6,
            section = "animSettings"
    )
    default Color npcColor5()
    {
        return Color.ORANGE;
    }

    @Range(
            min = 5,
            max = 50
    )
    @ConfigItem(
            keyName = "textSize",
            name = "Text Size",
            description = "Sets the text size of the ticks overlay",
            position = 7,
            section = "animSettings"
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
            position = 8,
            section = "animSettings"
    )
    default int textZ() {
        return 50;
    }

    @ConfigSection(
            name = "Extra bosses",
            description = "Extra settings for bosses that cannot be tracked",
            position = 9
    )
    String extraSettings = "extraSettings";

    @ConfigItem(
            keyName = "enableOlm",
            name = "Enable Olm Counter",
            description = "Turn on tick counter for Olm",
            section = extraSettings,
            position = 10
    )
    default boolean enableOlm(){return false;}

}
