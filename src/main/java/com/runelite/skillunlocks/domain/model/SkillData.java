package com.runelite.skillunlocks.domain.model;

import lombok.Data;
import lombok.Builder;
import net.runelite.api.Skill;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.time.Instant;

@Data
@Builder
public class SkillData
{
	private final Skill skill;
	@Builder.Default
	private final NavigableMap<Integer, SkillLevelData> levelData = new TreeMap<>();
	private Instant lastUpdated;
	
	public void addUnlock(SkillUnlock unlock)
	{
		levelData.computeIfAbsent(unlock.getLevel(), level -> 
			SkillLevelData.builder()
				.level(level)
				.build()
		).addUnlock(unlock);
	}
	
	public List<SkillUnlock> getUnlocksForLevel(int level)
	{
		SkillLevelData data = levelData.get(level);
		return data != null ? data.getUnlocks() : new ArrayList<>();
	}
	
	public List<SkillUnlock> getAllUnlocks()
	{
		List<SkillUnlock> allUnlocks = new ArrayList<>();
		levelData.values().forEach(data -> allUnlocks.addAll(data.getUnlocks()));
		return allUnlocks;
	}
	
	public NavigableMap<Integer, SkillLevelData> getUnlocksUpToLevel(int maxLevel)
	{
		return levelData.headMap(maxLevel, true);
	}
	
	public SkillLevelData getNextUnlock(int currentLevel)
	{
		Integer nextLevel = levelData.higherKey(currentLevel);
		return nextLevel != null ? levelData.get(nextLevel) : null;
	}
}