package com.runelite.skillunlocks;

import com.google.inject.Provides;
import com.runelite.skillunlocks.api.WikiHttpClient;
import com.runelite.skillunlocks.cache.CacheManager;
import com.runelite.skillunlocks.api.WikiApiClient;
import com.runelite.skillunlocks.domain.repository.UnlockRepository;
import com.runelite.skillunlocks.service.parser.WikiTextParser;
import com.runelite.skillunlocks.ui.SkillUnlocksPanel;
import com.runelite.skillunlocks.util.IconGenerator;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

/**
 * Main plugin class for Skill Unlocks
 * This plugin fetches and displays skill unlock information from the OSRS Wiki,
 * showing what items, abilities, and content become available at each level.
 */
@Slf4j
@PluginDescriptor(
	name = "Skill Unlocks",
	description = "Shows what unlocks at every level for all skills",
	tags = {"skill", "level", "unlock", "wiki", "progression", "requirements"}
)
public class SkillUnlocksPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SkillUnlocksConfig config;

	@Inject
	private ClientToolbar clientToolbar;
	
	@Inject
	private OkHttpClient httpClient;
	
	@Inject
	private SkillIconManager skillIconManager;
	
	private SkillUnlocksPanel panel;
	private NavigationButton navButton;
	private UnlockRepository repository;

	@Override
	protected void startUp()
	{
		log.info("Skill Unlocks plugin started!");
		
		// Initialize services
		WikiHttpClient wikiHttpClient = new WikiHttpClient(httpClient);
		WikiTextParser wikiTextParser = new WikiTextParser();
		WikiApiClient wikiApiClient = new WikiApiClient(wikiHttpClient, wikiTextParser);
		CacheManager cacheManager = new CacheManager();
		repository = new UnlockRepository(wikiApiClient, cacheManager);
		
		// Create panel
		panel = new SkillUnlocksPanel(client, config, repository, skillIconManager);
		
		// Create navigation button
		BufferedImage icon = loadPluginIcon();
		
		navButton = NavigationButton.builder()
			.tooltip("Skill Unlocks")
			.icon(icon)
			.panel(panel)
			.build();
		
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		log.info("Skill Unlocks plugin stopped!");
		
		// Remove navigation button first
		if (clientToolbar != null && navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
		
		// Clean up panel resources
		if (panel != null)
		{
			panel.cleanup();
			panel = null;
		}
		
		// Shutdown repository
		if (repository != null)
		{
			repository.shutdown();
			repository = null;
		}
		
		navButton = null;
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (panel != null)
		{
			// Update on Swing thread to ensure thread safety
			SwingUtilities.invokeLater(() -> {
				if (panel != null) // Double-check after context switch
				{
					panel.updatePlayerStats();
				}
			});
		}
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onStatChanged(StatChanged statChanged)
	{
		if (panel != null)
		{
			// Update on Swing thread to ensure thread safety
			SwingUtilities.invokeLater(() -> {
				if (panel != null) // Double-check after context switch
				{
					panel.updatePlayerStats();
				}
			});
		}
	}

	@Provides
	@SuppressWarnings("unused")
	SkillUnlocksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkillUnlocksConfig.class);
	}
	
	private BufferedImage loadPluginIcon()
	{
		try
		{
			// Try to load icon from resources
			BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
			if (icon != null)
			{
				return icon;
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to load plugin icon", e);
		}
		
		// Fallback: create a simple icon programmatically
		return createDefaultIcon();
	}
	
	private BufferedImage createDefaultIcon()
	{
		// Create a simple level-up arrow icon
		BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = icon.createGraphics();
		
		// Enable anti-aliasing
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// Draw icon
		drawIcon(g2d);
		
		g2d.dispose();
		return icon;
	}
	
	private void drawIcon(Graphics2D g2d)
	{
		// Use shared icon drawing logic from IconGenerator
		IconGenerator.drawLevelUpIcon(g2d);
	}
}
