package com.skillunlock.cache;

import com.skillunlock.data.SkillData;
import net.runelite.api.Skill;
import java.time.Instant;
import java.util.Map;

/**
 * Wrapper class for cached skill data with metadata
 */
public class CacheData
{
	private Instant lastUpdated;
	private Map<Skill, SkillData> skillData;
	
	public Instant getLastUpdated()
	{
		return lastUpdated;
	}
	
	public void setLastUpdated(Instant lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}
	
	public Map<Skill, SkillData> getSkillData()
	{
		return skillData;
	}
	
	public void setSkillData(Map<Skill, SkillData> skillData)
	{
		this.skillData = skillData;
	}
}