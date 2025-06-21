package com.runelite.skillunlocks.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.runelite.skillunlocks.cache.model.CacheData;
import com.runelite.skillunlocks.cache.serialization.InstantTypeAdapter;
import com.runelite.skillunlocks.domain.model.SkillData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.RuneLite;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class CacheManager
{
	private static final String CACHE_DIR = "level-up-table";
	private static final String CACHE_FILE = "skill-data-cache.json";
	private final Gson gson;
    private final Path cacheFile;
	
	@Inject
	public CacheManager()
	{
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(Instant.class, new InstantTypeAdapter())
			.create();

        Path cacheDirectory = Paths.get(RuneLite.RUNELITE_DIR.toString(), CACHE_DIR);
		this.cacheFile = cacheDirectory.resolve(CACHE_FILE);
		
		try
		{
			Files.createDirectories(cacheDirectory);
		}
		catch (IOException e)
		{
			log.error("Failed to create cache directory", e);
		}
	}
	
	public void saveSkillData(Map<Skill, SkillData> skillDataMap)
	{
		try
		{
			CacheData cacheData = new CacheData();
			cacheData.setLastUpdated(Instant.now());
			cacheData.setSkillData(skillDataMap);
			
			String json = gson.toJson(cacheData);
			Files.write(cacheFile, json.getBytes());
			
			log.info("Saved skill data cache to {}", cacheFile);
		}
		catch (IOException e)
		{
			log.error("Failed to save skill data cache", e);
		}
	}
	
	public Map<Skill, SkillData> loadSkillData()
	{
		if (!Files.exists(cacheFile))
		{
			log.info("No cache file found");
			return new HashMap<>();
		}
		
		try
		{
			String json = new String(Files.readAllBytes(cacheFile));
			CacheData cacheData = gson.fromJson(json, CacheData.class);
			
			if (cacheData != null && cacheData.getSkillData() != null)
			{
				log.info("Loaded skill data cache from {}", cacheFile);
				return cacheData.getSkillData();
			}
		}
		catch (IOException e)
		{
			log.error("Failed to load skill data cache", e);
		}
		
		return new HashMap<>();
	}
	
	public boolean isCacheExpired(int expiryHours)
	{
		if (!Files.exists(cacheFile))
		{
			return true;
		}
		
		try
		{
			Instant lastModified = Files.getLastModifiedTime(cacheFile).toInstant();
			Instant expiryTime = lastModified.plus(expiryHours, ChronoUnit.HOURS);
			return Instant.now().isAfter(expiryTime);
		}
		catch (IOException e)
		{
			log.error("Failed to check cache expiry", e);
			return true;
		}
	}
	
	public void clearCache()
	{
		try
		{
			if (Files.exists(cacheFile))
			{
				Files.delete(cacheFile);
				log.info("Cleared skill data cache");
			}
		}
		catch (IOException e)
		{
			log.error("Failed to clear cache", e);
		}
	}
}