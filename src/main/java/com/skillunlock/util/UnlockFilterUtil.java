package com.skillunlock.util;

import com.skillunlock.data.SkillUnlock;
import com.skillunlock.ui.modern.PillFilterBar;

/**
 * Utility class for filtering skill unlocks
 */
public final class UnlockFilterUtil
{
	// Prevent instantiation
	private UnlockFilterUtil() {}
	
	/**
	 * Check if an unlock should be shown based on search text and filter criteria
	 * 
	 * @param unlock The skill unlock to check
	 * @param searchText The search text (already lowercase)
	 * @param filterType The filter type to apply
	 * @param playerLevel The player's current level in the skill
	 * @return true if the unlock should be shown, false otherwise
	 */
	public static boolean shouldShowUnlock(SkillUnlock unlock, String searchText, 
										   PillFilterBar.FilterType filterType, int playerLevel)
	{
		// Apply text search filter
		if (!matchesSearchText(unlock, searchText))
		{
			return false;
		}
		
		// Apply type filter
		return matchesFilterType(unlock, filterType, playerLevel);
	}
	
	/**
	 * Check if an unlock matches the search text
	 */
	private static boolean matchesSearchText(SkillUnlock unlock, String searchText)
	{
		if (searchText == null || searchText.isEmpty())
		{
			return true;
		}
		
		String combined = (unlock.getName() + " " + 
						   unlock.getDescription() + " " + 
						   unlock.getRequirements()).toLowerCase();
		return combined.contains(searchText);
	}
	
	/**
	 * Check if an unlock matches the filter type
	 */
	private static boolean matchesFilterType(SkillUnlock unlock, PillFilterBar.FilterType filterType, int playerLevel)
	{
		switch (filterType)
		{
			case ALL:
				return true;
				
			case NEXT_5:
				return unlock.getLevel() > playerLevel && unlock.getLevel() <= playerLevel + 5;
				
			case UNLOCKED:
				return unlock.getLevel() <= playerLevel;
				
			case LOCKED:
				return unlock.getLevel() > playerLevel;
				
			case ITEMS:
				return unlock.getType() == SkillUnlock.UnlockType.ITEM;
				
			case QUESTS:
				return unlock.getType() == SkillUnlock.UnlockType.QUEST;
				
			case ACTIVITIES:
				return unlock.getType() == SkillUnlock.UnlockType.ACTIVITY || 
					   unlock.getType() == SkillUnlock.UnlockType.LOCATION;
					   
			case OTHER:
				return unlock.getType() == SkillUnlock.UnlockType.ABILITY ||
					   unlock.getType() == SkillUnlock.UnlockType.SPELL ||
					   unlock.getType() == SkillUnlock.UnlockType.PRAYER ||
					   unlock.getType() == SkillUnlock.UnlockType.OTHER;
					   
			default:
				return true;
		}
	}
	
	/**
	 * Determine if a level group should be expanded based on player level
	 * 
	 * @param range The level range string (e.g., "Levels 10-20", "Level 99")
	 * @param playerLevel The player's current level
	 * @return true if the group should be expanded
	 */
	public static boolean shouldExpandLevelGroup(String range, int playerLevel)
	{
		// Extract level numbers from range
		if (range.equals("Level 1"))
		{
			return playerLevel <= 10;
		}
		else if (range.equals("Level 99"))
		{
			return playerLevel >= 90;
		}
		else if (range.startsWith("Levels "))
		{
			String[] parts = range.substring(7).split("-");
			if (parts.length == 2)
			{
				try
				{
					int start = Integer.parseInt(parts[0]);
					int end = Integer.parseInt(parts[1]);
					// Expand if player level is within or near this range
					return playerLevel >= start - 5 && playerLevel <= end + 5;
				}
				catch (NumberFormatException e)
				{
					return false;
				}
			}
		}
		return false;
	}
}