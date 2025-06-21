package com.runelite.skillunlocks.service.parser;

import com.runelite.skillunlocks.domain.model.SkillData;
import com.runelite.skillunlocks.domain.model.SkillUnlock;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class WikiTextParser
{
	// Pattern to find the Level up table template - match until the closing }} at the start of a line
	// This ensures we capture the entire template even with nested templates inside
	private static final Pattern LEVEL_UP_TABLE_PATTERN = Pattern.compile("\\{\\{Level up table\\s*\\n([\\s\\S]*?)\\n\\}\\}", Pattern.MULTILINE);
	
	// Pattern to extract content from plink templates
	// More comprehensive plink pattern that handles all parameters
	private static final Pattern PLINK_PATTERN = Pattern.compile("\\{\\{plink\\|([^|}]+)(?:\\|[^}]*)*\\}\\}");
	
	// Pattern to clean wiki formatting
	private static final Pattern WIKI_LINK_PATTERN = Pattern.compile("\\[\\[([^\\|\\]]+)(?:\\|([^\\]]+))?\\]\\]");
	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{[^}]+\\}\\}");
	private static final Pattern SKILL_COMPARE_PATTERN = Pattern.compile("\\{\\{SCP\\|([^}|]+)(?:\\|([^}]+))?\\}\\}");
	
	public SkillData parseSkillPage(Skill skill, String wikiText)
	{
		SkillData skillData = SkillData.builder()
			.skill(skill)
			.lastUpdated(Instant.now())
			.build();
		
		// Find the Level up table template
		Matcher tableMatch = LEVEL_UP_TABLE_PATTERN.matcher(wikiText);
		if (tableMatch.find())
		{
			String tableContent = tableMatch.group(1);
			parseTableContent(tableContent, skillData);
		}
		else
		{
			// If no Level up table template found, log first 500 chars
			log.warn("No Level up table template found for skill: {}. Wiki text preview: {}", 
				skill, wikiText.length() > 500 ? wikiText.substring(0, 500) + "..." : wikiText);
		}
		return skillData;
	}
	
	private void parseTableContent(String tableContent, SkillData skillData)
	{
		// Split by | to get all parameters
		String[] parts = tableContent.split("\\|");
		
		int totalParams = 0;
		int nonEmptyParams = 0;
		
		for (int i = 0; i < parts.length; i++)
		{
			String part = parts[i];
			
			// Skip empty parts
			if (part.trim().isEmpty())
			{
				continue;
			}
			
			// Check if this part contains a parameter definition
			// Allow for whitespace/newlines at the start
			if (part.matches("(?s)^\\s*(freeplay|members)(\\d+|all)\\s*=.*"))
			{
				totalParams++;
				// Extract the parameter name and content
				int equalsIndex = part.indexOf('=');
				if (equalsIndex != -1)
				{
					String paramName = part.substring(0, equalsIndex).trim();
					StringBuilder content = new StringBuilder(part.substring(equalsIndex + 1));
					
					// Continue collecting content until we hit the next parameter
					int j = i + 1;
					while (j < parts.length && !parts[j].matches("(?s)^\\s*(freeplay|members)(\\d+|all)\\s*=.*"))
					{
						content.append("|").append(parts[j]);
						j++;
					}
					
					// Parse the parameter - handle both numbered params and "all" params
					Matcher paramMatcher = Pattern.compile("(freeplay|members)(\\d+|all)").matcher(paramName);
					if (paramMatcher.matches())
					{
						String memberType = paramMatcher.group(1);
						String levelStr = paramMatcher.group(2);
						
						if (!content.toString().trim().isEmpty())
						{
							nonEmptyParams++;
							
							// Handle "all" parameters - apply to all levels
							if ("all".equals(levelStr))
							{
								// For now, we'll add these to level 1 with a special marker
								// In the future, we might want to handle these differently
								parseUnlocksFromContent(content.toString(), 1, memberType, skillData);
							}
							else
							{
								int level = Integer.parseInt(levelStr);
								if (level == 1 || level == 10 || level == 50 || level == 99) {
									log.debug("Content for {} level {}: [{}]", skillData.getSkill(), level, 
										content.length() > 150 ? content.substring(0, 150) + "..." : content.toString());
								}
								parseUnlocksFromContent(content.toString(), level, memberType, skillData);
							}
						}
					}
					
					// Skip the parts we've already processed
					i = j - 1;
				}
			}
		}
		
		log.debug("Found {} total params, {} non-empty for {}", totalParams, nonEmptyParams, skillData.getSkill());
	}
	
	private void parseUnlocksFromContent(String content, int level, String memberType, SkillData skillData)
	{
		// Split by newlines and look for lines starting with *
		String[] lines = content.split("\n");
		
		for (String line : lines)
		{
			String trimmedLine = line.trim();
			if (trimmedLine.startsWith("*"))
			{
				// Remove the * and any following whitespace
				String unlockText = trimmedLine.substring(1).trim();
				
				if (!unlockText.isEmpty())
				{
					processUnlockLine(unlockText, level, memberType, skillData);
				}
			}
		}
	}
	
	private void processUnlockLine(String unlockLine, int level, String memberType, SkillData skillData)
	{
		String cleanedUnlock = cleanUnlockText(unlockLine);
		
		if (cleanedUnlock.length() > 2)
		{
			// Extract requirements if present (text in parentheses)
			String requirements = "";
			Pattern reqPattern = Pattern.compile("\\((?:with\\s+)?([^)]+)\\)");
			Matcher reqMatcher = reqPattern.matcher(cleanedUnlock);
			if (reqMatcher.find())
			{
				requirements = reqMatcher.group(1);
				cleanedUnlock = cleanedUnlock.substring(0, reqMatcher.start()).trim();
			}
			
			SkillUnlock unlock = SkillUnlock.builder()
				.level(level)
				.name(cleanedUnlock)
				.description(memberType.equals("members") ? "Members only" : "")
				.requirements(requirements)
				.type(determineUnlockType(cleanedUnlock, skillData.getSkill()))
				.build();
			
			skillData.addUnlock(unlock);
		}
	}
	
	private String cleanUnlockText(String text)
	{
		// Handle plink templates - extract txt parameter if present, otherwise use item name
		// Also consume trailing pluralization suffixes (e/s) after the template
		String cleaned = text;
		Pattern plinkWithTxtPattern = Pattern.compile("\\{\\{plink\\|[^|}]+(?:\\|[^|}]*)*\\|txt=([^|}]+)(?:\\|[^}]*)*\\}\\}[es]?");
		Matcher plinkTxtMatcher = plinkWithTxtPattern.matcher(cleaned);
		StringBuilder plinkTxtResult = new StringBuilder();
		while (plinkTxtMatcher.find())
		{
			String displayText = plinkTxtMatcher.group(1);
			plinkTxtMatcher.appendReplacement(plinkTxtResult, displayText);
		}
		plinkTxtMatcher.appendTail(plinkTxtResult);
		cleaned = plinkTxtResult.toString();
		
		// Now handle regular plink templates without txt parameter
		// Also consume trailing pluralization suffixes (e/s) after the template
		Pattern simplePlinkPattern = Pattern.compile("\\{\\{plink\\|([^|}]+)(?:\\|[^}]*)*\\}\\}[es]?");
		Matcher simplePlinkMatcher = simplePlinkPattern.matcher(cleaned);
		StringBuilder simplePlinkResult = new StringBuilder();
		while (simplePlinkMatcher.find())
		{
			String itemName = simplePlinkMatcher.group(1);
			simplePlinkMatcher.appendReplacement(simplePlinkResult, itemName);
		}
		simplePlinkMatcher.appendTail(simplePlinkResult);
		cleaned = simplePlinkResult.toString();
		
		// Handle SCP templates (e.g., {{SCP|Quest|Level}} becomes "Quest Level")
		Matcher scpMatcher = SKILL_COMPARE_PATTERN.matcher(cleaned);
		StringBuilder scpResult = new StringBuilder();
		while (scpMatcher.find())
		{
			String skill = scpMatcher.group(1);
			String level = scpMatcher.group(2);
			String replacement = level != null ? skill + " " + level : skill;
			scpMatcher.appendReplacement(scpResult, replacement);
		}
		scpMatcher.appendTail(scpResult);
		cleaned = scpResult.toString();
		
		// Convert wiki links to plain text
		Matcher linkMatcher = WIKI_LINK_PATTERN.matcher(cleaned);
		StringBuilder linkResult = new StringBuilder();
		while (linkMatcher.find())
		{
			String replacement = linkMatcher.group(2) != null ? linkMatcher.group(2) : linkMatcher.group(1);
			linkMatcher.appendReplacement(linkResult, replacement);
		}
		linkMatcher.appendTail(linkResult);
		cleaned = linkResult.toString();
		
		// Remove any remaining templates
		cleaned = TEMPLATE_PATTERN.matcher(cleaned).replaceAll("");
		
		// Clean up formatting and fix any broken parentheses
		cleaned = cleaned.replaceAll("'''", "")
				.replaceAll("''", "")
				.replaceAll("\\s+", " ")
				.replaceAll("\\(with\\s*$", "") // Remove incomplete "(with" at the end
				.trim();
		
		return cleaned;
	}
	
	private SkillUnlock.UnlockType determineUnlockType(String unlockText, Skill skill)
	{
		String lower = unlockText.toLowerCase();
		
		if (skill == Skill.MAGIC && (lower.contains("spell") || lower.contains("teleport") || lower.contains("enchant")))
		{
			return SkillUnlock.UnlockType.SPELL;
		}
		else if (skill == Skill.PRAYER && lower.contains("prayer"))
		{
			return SkillUnlock.UnlockType.PRAYER;
		}
		else if (lower.contains("quest") || lower.contains("miniquest"))
		{
			return SkillUnlock.UnlockType.QUEST;
		}
		else if (lower.contains("area") || lower.contains("location") || lower.contains("access"))
		{
			return SkillUnlock.UnlockType.LOCATION;
		}
		else if (lower.contains("ability") || lower.contains("can "))
		{
			return SkillUnlock.UnlockType.ABILITY;
		}
		else if (lower.contains("activity") || lower.contains("minigame"))
		{
			return SkillUnlock.UnlockType.ACTIVITY;
		}
		else if (isItemUnlock(lower, skill))
		{
			return SkillUnlock.UnlockType.ITEM;
		}
		
		return SkillUnlock.UnlockType.OTHER;
	}
	
	private boolean isItemUnlock(String text, Skill skill)
	{
		return text.contains("make") || text.contains("craft") || text.contains("smith") ||
			   text.contains("cook") || text.contains("catch") || text.contains("mine") ||
			   text.contains("cut") || text.contains("fletch") || text.contains("brew") ||
			   text.contains("create") || text.contains("build") || text.contains("grow") ||
			   text.contains("wield") || text.contains("wear") || text.contains("equip");
	}
}