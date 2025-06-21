package com.runelite.skillunlocks;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.runelite.skillunlocks.cache.CacheManager;
import com.runelite.skillunlocks.api.WikiApiClient;
import com.runelite.skillunlocks.domain.repository.UnlockRepository;
import com.runelite.skillunlocks.ui.SkillUnlocksPanel;
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
 * 
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
	private Gson gson;
	
	@Inject
	private SkillIconManager skillIconManager;

	private SkillUnlocksPanel panel;
	private NavigationButton navButton;
	private UnlockRepository repository;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Skill Unlocks plugin started!");
		
		// Initialize services
		WikiApiClient wikiApiClient = new WikiApiClient(httpClient, gson);
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
	protected void shutDown() throws Exception
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
		
		// Draw background circle
		g2d.setColor(new Color(45, 45, 50));
		g2d.fillOval(2, 2, 28, 28);
		
		// Draw border
		g2d.setColor(new Color(46, 213, 115));
		g2d.setStroke(new BasicStroke(2));
		g2d.drawOval(3, 3, 26, 26);
		
		// Draw level up arrow
		g2d.setColor(new Color(46, 213, 115));
		int[] xPoints = {16, 22, 19, 19, 13, 13, 10};
		int[] yPoints = {6, 14, 14, 24, 24, 14, 14};
		g2d.fillPolygon(xPoints, yPoints, 7);
		
		g2d.dispose();
		return icon;
	}
}