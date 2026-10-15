package net.runelite.client.plugins.tickcd;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;

@ConfigGroup("tickcd")
public interface TickcdConfig extends Config
{
	@ConfigSection(
		name = "Animations",
		description = "Which NPC animations to count",
		position = 0
	)
	String animSettings = "animSettings";

	@ConfigSection(
		name = "Appearance",
		description = "How the overlay is drawn",
		position = 1
	)
	String appearanceSection = "appearanceSection";

	@ConfigSection(
		name = "Extra bosses",
		description = "Bosses that can't be tracked by animation",
		position = 2
	)
	String extraSettings = "extraSettings";

	@ConfigItem(
		keyName = "allNPC",
		name = "Animation list",
		description = "One entry per line: NPC name, then one or more groups of animation ID, ticks, color number (1-5). "
			+ "Example: Vorkath, 1234, 5, 1, 5678, 5, 2",
		position = 0,
		section = animSettings
	)
	default String allNPC()
	{
		return "";
	}

	@Alpha
	@ConfigItem(
		keyName = "npcColor",
		name = "Tick color 1",
		description = "Text color for entries with color number 1",
		position = 0,
		section = appearanceSection
	)
	default Color npcColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		keyName = "npcColor2",
		name = "Tick color 2",
		description = "Text color for entries with color number 2",
		position = 1,
		section = appearanceSection
	)
	default Color npcColor2()
	{
		return Color.GREEN;
	}

	@Alpha
	@ConfigItem(
		keyName = "npcColor3",
		name = "Tick color 3",
		description = "Text color for entries with color number 3",
		position = 2,
		section = appearanceSection
	)
	default Color npcColor3()
	{
		return Color.RED;
	}

	@Alpha
	@ConfigItem(
		keyName = "npcColor4",
		name = "Tick color 4",
		description = "Text color for entries with color number 4",
		position = 3,
		section = appearanceSection
	)
	default Color npcColor4()
	{
		return Color.MAGENTA;
	}

	@Alpha
	@ConfigItem(
		keyName = "npcColor5",
		name = "Tick color 5",
		description = "Text color for entries with color number 5",
		position = 4,
		section = appearanceSection
	)
	default Color npcColor5()
	{
		return Color.ORANGE;
	}

	@ConfigItem(
		keyName = "font",
		name = "Font",
		description = "Font used for the overlay text",
		position = 5,
		section = appearanceSection
	)
	default FontType font()
	{
		return FontType.BOLD.withSize(30);
	}

	@Range(min = 0, max = 300)
	@ConfigItem(
		keyName = "textZ",
		name = "Text height",
		description = "How high above the NPC the counter is drawn",
		position = 6,
		section = appearanceSection
	)
	default int textZ()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "enableOlm",
		name = "Olm counter",
		description = "Show a tick counter for the Great Olm",
		position = 0,
		section = extraSettings
	)
	default boolean enableOlm()
	{
		return false;
	}
}
