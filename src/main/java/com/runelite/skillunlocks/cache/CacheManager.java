package com.runelite.skillunlocks.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.runelite.skillunlocks.cache.model.CacheData;
import com.runelite.skillunlocks.cache.serialization.InstantTypeAdapter;
import com.runelite.skillunlocks.domain.model.SkillData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.RuneLite;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;

@Slf4j
public class CacheManager
{
	private static final String CACHE_DIR = "level-up-table";
	private static final String CACHE_FILE = "skill-data-cache.json";
	
	// Cache throttling configuration
	private static final long SAVE_DEBOUNCE_MS = 5000; // Wait 5 seconds before saving
	private static final long MIN_SAVE_INTERVAL_MS = 10000; // Minimum 10 seconds between saves
	
	private final Gson gson;
    private final Path cacheFile;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // Throttling state
    private ScheduledFuture<?> pendingSave;
    private Map<Skill, SkillData> pendingData;
    private long lastSaveTime = 0;
	
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
	
	public synchronized void saveSkillData(Map<Skill, SkillData> skillDataMap)
	{
		// Store pending data
		pendingData = new HashMap<>(skillDataMap);
		
		// Cancel any existing pending save
		if (pendingSave != null && !pendingSave.isDone())
		{
			pendingSave.cancel(false);
		}
		
		// Calculate delay based on last save time
		long timeSinceLastSave = System.currentTimeMillis() - lastSaveTime;
		long delay = Math.max(SAVE_DEBOUNCE_MS, MIN_SAVE_INTERVAL_MS - timeSinceLastSave);
		
		// Schedule the save
		pendingSave = scheduler.schedule(this::performSave, delay, TimeUnit.MILLISECONDS);
		log.debug("Scheduled cache save in {}ms", delay);
	}
	
	private void performSave()
	{
		if (pendingData == null || pendingData.isEmpty())
		{
			return;
		}
		
		try
		{
			// Create cache data
			CacheData cacheData = new CacheData();
			cacheData.setLastUpdated(Instant.now());
			cacheData.setSkillData(pendingData);
			
			String json = gson.toJson(cacheData);
			
			// Write with file locking
			writeWithLock(json);
			
			lastSaveTime = System.currentTimeMillis();
			log.info("Saved skill data cache to {}", cacheFile);
		}
		catch (IOException e)
		{
			log.error("Failed to save skill data cache", e);
		}
		finally
		{
			pendingData = null;
		}
	}
	
	private void writeWithLock(String content) throws IOException
	{
		// Use file channel for atomic write with locking
		try (FileChannel channel = FileChannel.open(cacheFile, 
			StandardOpenOption.CREATE, 
			StandardOpenOption.WRITE, 
			StandardOpenOption.TRUNCATE_EXISTING))
		{
			// Try to acquire exclusive lock
			try (FileLock lock = channel.tryLock())
			{
				if (lock != null)
				{
					java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(content.getBytes());
					while (buffer.hasRemaining())
					{
						int written = channel.write(buffer);
						if (written < 0)
						{
							throw new IOException("Failed to write to file");
						}
					}
				}
				else
				{
					log.warn("Could not acquire file lock for cache write");
					// Fall back to regular write
					Files.write(cacheFile, content.getBytes());
				}
			}
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
			// Read with file locking to prevent concurrent access issues
			String json = readWithLock();
			if (json.isEmpty())
			{
				return new HashMap<>();
			}
			
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
	
	private String readWithLock() throws IOException
	{
		try (FileChannel channel = FileChannel.open(cacheFile, StandardOpenOption.READ))
		{
			// Try to acquire shared lock for reading
			try (FileLock lock = channel.tryLock(0, Long.MAX_VALUE, true))
			{
				if (lock != null)
				{
					java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate((int) channel.size());
					channel.read(buffer);
					buffer.flip();
					return new String(buffer.array());
				}
				else
				{
					log.warn("Could not acquire file lock for cache read");
					// Fall back to regular read
					return new String(Files.readAllBytes(cacheFile));
				}
			}
		}
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
	
	public synchronized void clearCache()
	{
		// Cancel any pending saves
		if (pendingSave != null && !pendingSave.isDone())
		{
			pendingSave.cancel(false);
		}
		pendingData = null;
		
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
	
	/**
	 * Shutdown the cache manager and save any pending data
	 */
	public void shutdown()
	{
		try
		{
			// Cancel pending save and perform immediate save if needed
			if (pendingSave != null && !pendingSave.isDone())
			{
				pendingSave.cancel(false);
				if (pendingData != null)
				{
					performSave();
				}
			}
			
			// Shutdown scheduler
			scheduler.shutdown();
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS))
			{
				scheduler.shutdownNow();
			}
		}
		catch (InterruptedException e)
		{
			log.error("Error during cache manager shutdown", e);
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}