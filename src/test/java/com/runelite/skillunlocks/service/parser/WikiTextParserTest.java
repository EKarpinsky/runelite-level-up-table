package com.runelite.skillunlocks.service.parser;

import com.runelite.skillunlocks.domain.model.SkillData;
import com.runelite.skillunlocks.domain.model.SkillUnlock;
import net.runelite.api.Skill;
import org.junit.Test;
import org.junit.Before;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class WikiTextParserTest
{
	private WikiTextParser parser;
	
	@Before
	public void setUp()
	{
		parser = new WikiTextParser();
	}
	
	@Test
	public void testParseAttackPage()
	{
		// Sample Attack wiki text with the actual format
		String wikiText = "{{external|rs}}\n" +
			"{{Level up table\n" +
			"|freeplayall =\n" +
			"* Each level in Attack contributes to a player's [[combat level]]\n" +
			"* Each level in Attack slightly increases the accuracy of a player's melee attacks\n" +
			"|freeplay1 =\n" +
			"* Wield {{plink|Bronze equipment#Weapons|pic=Bronze sword|txt=bronze weapons}}\n" +
			"* Wield {{plink|Iron equipment#Weapons|pic=Iron sword|txt=iron weapons}}\n" +
			"* Wield {{plink|blurite sword}}s (with {{SCP|Quest}} [[The Knight's Sword]] completed)\n" +
			"* Wield {{plink|Silverlight}} (with {{SCP|Quest}} [[Demon Slayer]] completed)\n" +
			"|members1 =\n" +
			"* Wield {{plink|bronze claws}}\n" +
			"* Wield {{plink|bronze felling axe}}s\n" +
			"* Wield {{plink|bronze halberd}}s\n" +
			"|freeplay5 =\n" +
			"* Wield {{plink|Steel equipment#Weapons|pic=Steel sword|txt=steel weapons}}\n" +
			"|members5 =\n" +
			"* Wield {{plink|steel claws}}\n" +
			"* Wield {{plink|steel defender}}s (with {{SCP|Defence|5}})\n" +
			"|freeplay40 =\n" +
			"* Wield {{plink|Rune equipment#Weapons|pic=Rune sword|txt=rune weapons}}\n" +
			"* Wield {{plink|gilded scimitar}}s\n" +
			"|members40 =\n" +
			"* Wield {{plink|rune claws}}\n" +
			"* Wield the {{plink|Ivandis flail}} (with partial completion of {{SCP|Quest}} [[A Taste of Hope]])\n" +
			"}}";
		
		SkillData skillData = parser.parseSkillPage(Skill.ATTACK, wikiText);
		
		assertNotNull(skillData);
		assertEquals(Skill.ATTACK, skillData.getSkill());
		
		List<SkillUnlock> allUnlocks = skillData.getAllUnlocks();
		assertTrue("Should have parsed multiple unlocks", allUnlocks.size() > 10);
		
		// Check level 1 freeplay unlocks
		List<SkillUnlock> level1Unlocks = skillData.getUnlocksForLevel(1);
		assertFalse("Should have level 1 unlocks", level1Unlocks.isEmpty());
		
		// Verify bronze weapons unlock
		boolean foundBronze = false;
		for (SkillUnlock unlock : level1Unlocks)
		{
			if (unlock.getName().contains("bronze weapons"))
			{
				foundBronze = true;
				assertEquals(1, unlock.getLevel());
				assertEquals(SkillUnlock.UnlockType.ITEM, unlock.getType());
			}
		}
		assertTrue("Should find bronze weapons unlock", foundBronze);
		
		// Check level 40 unlocks
		List<SkillUnlock> level40Unlocks = skillData.getUnlocksForLevel(40);
		assertFalse("Should have level 40 unlocks", level40Unlocks.isEmpty());
		
		// Verify rune weapons unlock
		boolean foundRune = false;
		for (SkillUnlock unlock : level40Unlocks)
		{
			if (unlock.getName().contains("rune weapons"))
			{
				foundRune = true;
				assertEquals(40, unlock.getLevel());
			}
		}
		assertTrue("Should find rune weapons unlock", foundRune);
		
		// Check that requirements are parsed
		boolean foundRequirement = false;
		for (SkillUnlock unlock : allUnlocks)
		{
			if (unlock.getRequirements() != null && !unlock.getRequirements().isEmpty())
			{
				foundRequirement = true;
				break;
			}
		}
		assertTrue("Should find at least one unlock with requirements", foundRequirement);
		
		// Check member-only unlocks
		boolean foundMemberUnlock = false;
		for (SkillUnlock unlock : allUnlocks)
		{
			if (unlock.getDescription() != null && unlock.getDescription().contains("Members only"))
			{
				foundMemberUnlock = true;
				break;
			}
		}
		assertTrue("Should find at least one members-only unlock", foundMemberUnlock);
	}
	
	@Test
	public void testPlinkParsing()
	{
		String wikiText = "{{Level up table\n" +
			"|freeplay1 =\n" +
			"* Wield {{plink|Bronze equipment#Weapons|pic=Bronze sword|txt=bronze weapons}}\n" +
			"* Wield {{plink|Silverlight}}\n" +
			"}}";
		
		SkillData skillData = parser.parseSkillPage(Skill.ATTACK, wikiText);
		List<SkillUnlock> unlocks = skillData.getUnlocksForLevel(1);
		
		assertEquals(2, unlocks.size());
		
		// Check that plink with txt parameter uses the txt value
		assertEquals("Wield bronze weapons", unlocks.get(0).getName());
		
		// Check that plink without txt uses the item name
		assertEquals("Wield Silverlight", unlocks.get(1).getName());
	}
	
	@Test
	public void testRequirementsParsing()
	{
		String wikiText = "{{Level up table\n" +
			"|members1 =\n" +
			"* Wield {{plink|bronze hasta}}e (with [[Barbarian Training#Barbarian Smithing|Barbarian Training]] completed)\n" +
			"* Wield {{plink|steel defender}}s (with {{SCP|Defence|5}})\n" +
			"}}";
		
		SkillData skillData = parser.parseSkillPage(Skill.ATTACK, wikiText);
		List<SkillUnlock> unlocks = skillData.getUnlocksForLevel(1);
		
		assertEquals(2, unlocks.size());
		
		// Check barbarian training requirement
		SkillUnlock hastaUnlock = unlocks.get(0);
		assertEquals("Wield bronze hasta", hastaUnlock.getName());
		assertEquals("Barbarian Training completed", hastaUnlock.getRequirements());
		
		// Check Defence level requirement
		SkillUnlock defenderUnlock = unlocks.get(1);
		assertEquals("Wield steel defender", defenderUnlock.getName());
		assertEquals("Defence 5", defenderUnlock.getRequirements());
	}
	
	@Test
	public void testEmptyLevels()
	{
		String wikiText = "{{Level up table\n" +
			"|freeplay1 =\n" +
			"* Wield bronze\n" +
			"|members1 =\n" +
			"|freeplay2 =\n" +
			"|members2 =\n" +
			"|freeplay3 =\n" +
			"* Wield iron\n" +
			"}}";
		
		SkillData skillData = parser.parseSkillPage(Skill.ATTACK, wikiText);
		
		assertEquals(1, skillData.getUnlocksForLevel(1).size());
		assertEquals(0, skillData.getUnlocksForLevel(2).size());
		assertEquals(1, skillData.getUnlocksForLevel(3).size());
	}
}