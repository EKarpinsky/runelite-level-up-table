package com.runelite.skillunlocks;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("skillunlocks")
public interface SkillUnlocksConfig extends Config
{
	@ConfigItem(
		keyName = "refreshOnStartup",
		name = "Refresh on startup",
		description = "Force refresh skill data from wiki on plugin startup"
	)
	default boolean refreshOnStartup()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOnlyUnlocked",
		name = "Show only unlocked",
		description = "Show only items you have already unlocked"
	)
	default boolean showOnlyUnlocked()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightNext",
		name = "Highlight next unlock",
		description = "Highlight the next upcoming unlock for each skill"
	)
	default boolean highlightNext()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cacheExpiry",
		name = "Cache expiry (hours)",
		description = "How long to cache wiki data before refreshing (in hours)"
	)
	default int cacheExpiry()
	{
		return 24;
	}
}