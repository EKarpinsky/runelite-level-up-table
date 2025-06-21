package com.skillunlock.data;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
public class SkillLevelData
{
	private final int level;
	@Builder.Default
	private final List<SkillUnlock> unlocks = new ArrayList<>();
	
	public void addUnlock(SkillUnlock unlock)
	{
		unlocks.add(unlock);
	}
	
	public boolean hasUnlocks()
	{
		return !unlocks.isEmpty();
	}
}