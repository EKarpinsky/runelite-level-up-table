package com.runelite.skillunlocks.domain.repository;

import com.runelite.skillunlocks.cache.CacheManager;
import com.runelite.skillunlocks.api.WikiApiClient;
import com.runelite.skillunlocks.domain.model.SkillData;
import com.runelite.skillunlocks.domain.model.SkillUnlock;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for managing skill unlock data
 * 
 * Handles fetching skill data from the wiki, caching, and providing
 * methods to query unlock information. Uses async loading to prevent
 * blocking the UI while fetching data.
 */
@Slf4j
@Singleton
public class UnlockRepository
{
	private final WikiApiClient wikiApiClient;
	private final CacheManager cacheManager;
	private final Map<Skill, SkillData> skillDataMap = new ConcurrentHashMap<>();
	private final ExecutorService executorService = Executors.newFixedThreadPool(4);
	private boolean initialized = false;
	
	@Inject
	public UnlockRepository(WikiApiClient wikiApiClient, CacheManager cacheManager)
	{
		this.wikiApiClient = wikiApiClient;
		this.cacheManager = cacheManager;
	}
	
	/**
	 * Initialize the repository by loading skill data
	 * 
	 * @param forceRefresh If true, forces a refresh from wiki ignoring cache
	 * @param cacheExpiryHours Number of hours before cache is considered expired
	 */
	public void initialize(boolean forceRefresh, int cacheExpiryHours)
	{
		if (initialized && !forceRefresh)
		{
			return;
		}
		
		if (!forceRefresh && !cacheManager.isCacheExpired(cacheExpiryHours))
		{
			log.info("Loading skill data from cache");
			Map<Skill, SkillData> cachedData = cacheManager.loadSkillData();
			if (!cachedData.isEmpty())
			{
				skillDataMap.putAll(cachedData);
				initialized = true;
				return;
			}
		}
		
		log.info("Fetching skill data from wiki");
		fetchAllSkillData();
	}
	
	public void refreshData()
	{
		log.info("Forcing refresh of all skill data");
		cacheManager.clearCache();
		skillDataMap.clear();
		fetchAllSkillData();
	}
	
	@SuppressWarnings("deprecation")
	private void fetchAllSkillData()
	{
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				continue; // Skip overall as it doesn't have unlocks
			}
			
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try
				{
					SkillData data = wikiApiClient.fetchSkillData(skill);
					if (data != null && !data.getAllUnlocks().isEmpty())
					{
						skillDataMap.put(skill, data);
						log.info("Fetched {} unlocks for {}", data.getAllUnlocks().size(), skill);
					}
				}
				catch (IOException e)
				{
					log.error("Failed to fetch data for skill: {}", skill, e);
				}
			}, executorService);
			
			futures.add(future);
		}
		
		// Wait for all fetches to complete
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		
		// Save to cache
		if (!skillDataMap.isEmpty())
		{
			cacheManager.saveSkillData(skillDataMap);
			initialized = true;
		}
	}
	
	public SkillData getSkillData(Skill skill)
	{
		return skillDataMap.get(skill);
	}
	
	
	
	
	
	public void shutdown()
	{
		executorService.shutdown();
	}
}