package com.skillunlock.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skillunlock.data.SkillData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Client for fetching skill data from the OSRS Wiki API
 * 
 * Handles all communication with the wiki, including fetching page content
 * and delegating parsing to the WikiTextParser.
 */
@Slf4j
@Singleton
public class WikiClient
{
	private static final String WIKI_API_URL = "https://oldschool.runescape.wiki/api.php";
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
	
	private final OkHttpClient httpClient;
	private final Gson gson;
	private final WikiTextParser parser;
	
	// Rate limiting - allow 1 request per second
	private long lastRequestTime = 0;
	private static final long MIN_REQUEST_INTERVAL_MS = 1000;
	
	@Inject
	public WikiClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient.newBuilder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.build();
		this.gson = gson;
		this.parser = new WikiTextParser();
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
	
	private synchronized String fetchWikiText(String pageName) throws IOException
	{
		// Rate limiting
		long currentTime = System.currentTimeMillis();
		long timeSinceLastRequest = currentTime - lastRequestTime;
		if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS)
		{
			try
			{
				Thread.sleep(MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new IOException("Rate limiting interrupted", e);
			}
		}
		lastRequestTime = System.currentTimeMillis();
		
		// Always go directly to the Level_up_table subpage
		String levelUpPageName = pageName + "/Level_up_table";

		String encodedPageName = URLEncoder.encode(levelUpPageName, StandardCharsets.UTF_8.toString());
		String url = String.format("%s?action=query&prop=revisions&titles=%s&rvslots=*&rvprop=content&format=json",
			WIKI_API_URL, encodedPageName);
		
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", "RuneLite Level up table Plugin")
			.build();
		
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				log.error("Failed to fetch wiki page: {} - {}", levelUpPageName, response.code());
				return null;
			}
			
			String body = response.body().string();

			String wikiText = extractWikiText(body);
			return wikiText;
		}
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
			log.error("Failed to parse wiki response: " + e.getMessage());
			return null;
		}
	}
}