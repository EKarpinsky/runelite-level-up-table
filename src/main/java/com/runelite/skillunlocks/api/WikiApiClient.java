package com.runelite.skillunlocks.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.runelite.skillunlocks.domain.model.SkillData;
import com.runelite.skillunlocks.service.parser.WikiTextParser;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for fetching skill data from the OSRS Wiki API
 * Handles all communication with the wiki, including fetching page content
 * and delegating parsing to the WikiTextParser.
 */
@Slf4j
public class WikiApiClient
{
	private static final Map<Skill, String> SKILL_PAGE_NAMES = new HashMap<>();
	
	static {
		// Map skills to their wiki page names
		SKILL_PAGE_NAMES.put(Skill.ATTACK, "Attack");
		SKILL_PAGE_NAMES.put(Skill.STRENGTH, "Strength");
		SKILL_PAGE_NAMES.put(Skill.DEFENCE, "Defence");
		SKILL_PAGE_NAMES.put(Skill.RANGED, "Ranged");
		SKILL_PAGE_NAMES.put(Skill.PRAYER, "Prayer");
		SKILL_PAGE_NAMES.put(Skill.MAGIC, "Magic");
		SKILL_PAGE_NAMES.put(Skill.RUNECRAFT, "Runecraft");
		SKILL_PAGE_NAMES.put(Skill.CONSTRUCTION, "Construction");
		SKILL_PAGE_NAMES.put(Skill.HITPOINTS, "Hitpoints");
		SKILL_PAGE_NAMES.put(Skill.AGILITY, "Agility");
		SKILL_PAGE_NAMES.put(Skill.HERBLORE, "Herblore");
		SKILL_PAGE_NAMES.put(Skill.THIEVING, "Thieving");
		SKILL_PAGE_NAMES.put(Skill.CRAFTING, "Crafting");
		SKILL_PAGE_NAMES.put(Skill.FLETCHING, "Fletching");
		SKILL_PAGE_NAMES.put(Skill.SLAYER, "Slayer");
		SKILL_PAGE_NAMES.put(Skill.HUNTER, "Hunter");
		SKILL_PAGE_NAMES.put(Skill.MINING, "Mining");
		SKILL_PAGE_NAMES.put(Skill.SMITHING, "Smithing");
		SKILL_PAGE_NAMES.put(Skill.FISHING, "Fishing");
		SKILL_PAGE_NAMES.put(Skill.COOKING, "Cooking");
		SKILL_PAGE_NAMES.put(Skill.FIREMAKING, "Firemaking");
		SKILL_PAGE_NAMES.put(Skill.WOODCUTTING, "Woodcutting");
		SKILL_PAGE_NAMES.put(Skill.FARMING, "Farming");
	}
	
	private final WikiHttpClient wikiHttpClient;
	private final WikiTextParser parser;
	
	public WikiApiClient(WikiHttpClient wikiHttpClient, WikiTextParser parser)
	{
		this.wikiHttpClient = wikiHttpClient;
		this.parser = parser;
	}
	
	public SkillData fetchSkillData(Skill skill) throws IOException
	{
		String pageName = SKILL_PAGE_NAMES.get(skill);
		if (pageName == null)
		{
			log.warn("No wiki page mapping for skill: {}", skill);
			return SkillData.builder().skill(skill).build();
		}
		
		String wikiText = fetchWikiText(pageName);
		if (wikiText == null || wikiText.isEmpty())
		{
			log.warn("No wiki text found for skill: {} (tried {}/Level up table)", skill, pageName);
			return SkillData.builder().skill(skill).build();
		}
		
		return parser.parseSkillPage(skill, wikiText);
	}
	
	private String fetchWikiText(String pageName) throws IOException
	{
		// Always go directly to the Level_up_table subpage
		String levelUpPageName = pageName + "/Level_up_table";
		
		String jsonResponse = wikiHttpClient.fetchWikiPage(levelUpPageName);
		if (jsonResponse == null)
		{
			log.warn("Failed to fetch wiki page: {}", levelUpPageName);
			return null;
		}
		
		return extractWikiText(jsonResponse);
	}
	
	private String extractWikiText(String jsonResponse)
	{
		try
		{
			JsonParser parser = new JsonParser();
			JsonObject root = parser.parse(jsonResponse).getAsJsonObject();
			JsonObject query = root.getAsJsonObject("query");
			JsonObject pages = query.getAsJsonObject("pages");
			
			// Get the first (and only) page
			String pageId = pages.keySet().iterator().next();
			JsonObject page = pages.getAsJsonObject(pageId);
			
			if (page.has("missing"))
			{
				log.warn("Page marked as missing in API response");
				return null;
			}
			
			if (!page.has("revisions") || page.getAsJsonArray("revisions").size() == 0)
			{
				log.warn("No revisions found in page");
				return null;
			}
			
			JsonObject revision = page.getAsJsonArray("revisions").get(0).getAsJsonObject();
			JsonObject slots = revision.getAsJsonObject("slots");
			JsonObject main = slots.getAsJsonObject("main");
			
			return main.get("*").getAsString();
		}
		catch (Exception e)
		{
			log.error("Failed to parse wiki response: {}", e.getMessage());
			return null;
		}
	}
}