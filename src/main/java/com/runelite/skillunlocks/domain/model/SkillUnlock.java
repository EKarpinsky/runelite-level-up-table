package com.runelite.skillunlocks.domain.model;

import lombok.Data;
import lombok.Builder;

/**
 * Represents a single skill unlock at a specific level
 * 
 * Contains information about what becomes available at a certain skill level,
 * including the name, description, requirements, and type of unlock.
 */
@Data
@Builder
public class SkillUnlock
{
	private final int level;
	private final String name;
	private final String description;
	@Builder.Default
	private final String requirements = "";
	private final UnlockType type;
	
	public enum UnlockType
	{
		ITEM,
		ACTIVITY,
		SPELL,
		PRAYER,
		ABILITY,
		LOCATION,
		QUEST,
		OTHER
	}
}