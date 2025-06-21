package com.runelite.skillunlocks.ui;

import com.runelite.skillunlocks.SkillUnlocksConfig;
import com.runelite.skillunlocks.domain.model.SkillData;
import com.runelite.skillunlocks.domain.model.SkillUnlock;
import com.runelite.skillunlocks.domain.repository.UnlockRepository;
import com.runelite.skillunlocks.constants.UIConstants;
import com.runelite.skillunlocks.ui.components.controls.PillFilterBar;
import com.runelite.skillunlocks.ui.panels.SkillSelectorPanel;
import com.runelite.skillunlocks.ui.panels.ProgressPanel;
import com.runelite.skillunlocks.ui.panels.SearchBarPanel;
import com.runelite.skillunlocks.ui.panels.UnlockListPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.io.Serializable;

@Slf4j
public class SkillUnlocksPanel extends PluginPanel implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private final Client client;
	private final SkillUnlocksConfig config;
	private final UnlockRepository repository;
	private final SkillIconManager skillIconManager;
	
	// UI Panels
	private SkillSelectorPanel skillSelectorPanel;
	private ProgressPanel progressPanel;
	private SearchBarPanel searchBarPanel;
	private UnlockListPanel unlockListPanel;
	private PillFilterBar filterBar;
	private JButton refreshButton;
	
	// State
	private Skill selectedSkill = null;
	private final Map<Skill, Integer> playerLevels = new HashMap<>();
	private javax.swing.Timer refreshButtonResetTimer;
	
	public SkillUnlocksPanel(Client client, SkillUnlocksConfig config, UnlockRepository repository,
								  SkillIconManager skillIconManager)
	{
		super(false);
		this.client = client;
		this.config = config;
		this.repository = repository;
		this.skillIconManager = skillIconManager;
		
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		init();
		SwingUtilities.invokeLater(this::loadData);
	}
	
	private void init()
	{
		// Main container with modern styling
		JPanel mainContainer = new JPanel();
		mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
		mainContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		// Header panel with glass effect
		JPanel headerPanel = createGlassPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		// Skill selector panel
		skillSelectorPanel = new SkillSelectorPanel(skillIconManager, this::onSkillSelected, playerLevels);
		headerPanel.add(skillSelectorPanel);
		
		// Progress panel
		progressPanel = new ProgressPanel();
		headerPanel.add(Box.createVerticalStrut(10));
		headerPanel.add(progressPanel);
		
		mainContainer.add(headerPanel);
		mainContainer.add(createSeparator());
		
		// Search bar panel
		searchBarPanel = new SearchBarPanel();
		searchBarPanel.addSearchListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e) { filterContent(); }
			@Override
			public void removeUpdate(DocumentEvent e) { filterContent(); }
			@Override
			public void changedUpdate(DocumentEvent e) { filterContent(); }
		});
		searchBarPanel.addViewModeListener(event -> toggleViewMode());
		mainContainer.add(searchBarPanel);
		
		// Filter bar
		filterBar = new PillFilterBar();
		filterBar.setFilterChangeListener(event -> filterContent());
		mainContainer.add(filterBar);
		
		// Action buttons panel
		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		actionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		
		refreshButton = createModernButton("Refresh Data", "Fetch latest data from wiki");
		refreshButton.addActionListener(this::onRefreshClicked);
		actionPanel.add(refreshButton);
		
		mainContainer.add(actionPanel);
		
		add(mainContainer, BorderLayout.NORTH);
		
		// Unlock list panel
		unlockListPanel = new UnlockListPanel();
		add(unlockListPanel, BorderLayout.CENTER);
	}
	
	private JPanel createGlassPanel()
	{
		return new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				
				// Glass effect with gradient
				GradientPaint gradient = new GradientPaint(
					0, 0, UIConstants.GLASS_TOP_COLOR,
					0, getHeight(), UIConstants.GLASS_BOTTOM_COLOR
				);
				g2d.setPaint(gradient);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), UIConstants.BORDER_RADIUS, UIConstants.BORDER_RADIUS);
				
				// Border
				g2d.setColor(UIConstants.GLASS_BORDER_COLOR);
				g2d.setStroke(new BasicStroke(1));
				g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UIConstants.BORDER_RADIUS, UIConstants.BORDER_RADIUS);
				
				g2d.dispose();
			}
			
			@Override
			public boolean isOpaque()
			{
				return false;
			}
		};
	}
	
	private JButton createModernButton(String text, String tooltip)
	{
		JButton button = new JButton(text);
		button.setToolTipText(tooltip);
		SwingUtil.removeButtonDecorations(button);
		button.setBackground(UIConstants.HOVER_COLOR);
		button.setForeground(Color.WHITE);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(UIConstants.DARKER_BORDER_COLOR, 1),
			new EmptyBorder(UIConstants.SMALL_SPACING, UIConstants.MEDIUM_SPACING, UIConstants.SMALL_SPACING, UIConstants.MEDIUM_SPACING)
		));
		
		button.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseEntered(java.awt.event.MouseEvent evt)
			{
				button.setBackground(ColorScheme.BRAND_ORANGE);
			}
			
			public void mouseExited(java.awt.event.MouseEvent evt)
			{
				button.setBackground(UIConstants.HOVER_COLOR);
			}
		});
		
		return button;
	}
	
	private JSeparator createSeparator()
	{
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		separator.setForeground(UIConstants.withAlpha(UIConstants.BORDER_COLOR, 50));
		return separator;
	}
	
	private void loadData()
	{
		repository.initialize(config.refreshOnStartup(), config.cacheExpiry());
		updatePlayerStats();
		
		// Select Attack skill by default after initialization
		SwingUtilities.invokeLater(() -> skillSelectorPanel.selectSkill(Skill.ATTACK));
	}
	
	private void onSkillSelected(Skill skill)
	{
		selectedSkill = skill;
		updateContentForSkill(skill);
	}
	
	private void updateContentForSkill(Skill skill)
	{
		if (skill == null)
		{
			unlockListPanel.showStatus("Select a skill to view unlocks");
			progressPanel.reset();
			return;
		}
		
		SkillData skillData = repository.getSkillData(skill);
		if (skillData == null || skillData.getAllUnlocks().isEmpty())
		{
			unlockListPanel.showStatus("Loading data for " + skill.getName() + "...");
			progressPanel.reset();
			
			// Trigger async load
			SwingUtilities.invokeLater(() -> {
				// Data should be loaded by repository
				updateContentForSkill(skill);
			});
			return;
		}
		
		// Group unlocks by level ranges
		Map<String, List<SkillUnlock>> groupedUnlocks = groupUnlocksByLevelRange(skillData.getAllUnlocks());
		int playerLevel = playerLevels.getOrDefault(skill, 1);
		
		// Update unlock list
		unlockListPanel.updateContent(groupedUnlocks, playerLevel, skill);
		
		// Update progress gauge
		updateProgress(skill, skillData);
		
		// Apply current filter
		filterContent();
	}
	
	private Map<String, List<SkillUnlock>> groupUnlocksByLevelRange(List<SkillUnlock> unlocks)
	{
		Map<String, List<SkillUnlock>> grouped = new LinkedHashMap<>();
		
		// Sort unlocks by level
		List<SkillUnlock> sorted = new ArrayList<>(unlocks);
		sorted.sort(Comparator.comparingInt(SkillUnlock::getLevel));
		
		// Define milestone ranges
		int[] milestones = {1, 10, 25, 50, 75, 99};
		
		for (int i = 0; i < milestones.length; i++)
		{
			int start = milestones[i];
			int end = (i < milestones.length - 1) ? milestones[i + 1] - 1 : 99;
			
			String range;
			if (start == 1)
			{
				range = "Levels 1-9";
			}
			else if (start == end)
			{
				range = "Level " + start;
			}
			else
			{
				range = "Levels " + start + "-" + end;
			}
			
			List<SkillUnlock> rangeUnlocks = sorted.stream()
				.filter(u -> u.getLevel() >= start && u.getLevel() <= end)
				.collect(Collectors.toList());
			
			if (!rangeUnlocks.isEmpty())
			{
				grouped.put(range, rangeUnlocks);
			}
		}
		
		return grouped;
	}
	
	
	private void updateProgress(Skill skill, SkillData skillData)
	{
		int playerLevel = playerLevels.getOrDefault(skill, 1);
		int unlockedCount = 0;
		Integer nextUnlockLevel = null;
		
		List<SkillUnlock> allUnlocks = skillData.getAllUnlocks();
		for (SkillUnlock unlock : allUnlocks)
		{
			if (unlock.getLevel() <= playerLevel)
			{
				unlockedCount++;
			}
			else if (nextUnlockLevel == null || unlock.getLevel() < nextUnlockLevel)
			{
				nextUnlockLevel = unlock.getLevel();
			}
		}
		
		progressPanel.updateProgress(skill, playerLevel, unlockedCount, allUnlocks.size(), nextUnlockLevel);
	}
	
	private void filterContent()
	{
		String searchText = searchBarPanel.getSearchText().toLowerCase().trim();
		PillFilterBar.FilterType filterType = filterBar.getSelectedFilter();
		int playerLevel = playerLevels.getOrDefault(selectedSkill, 1);
		
		// Filter content in unlock list
		unlockListPanel.filterContent(searchText, filterType, playerLevel);
		
		// Update result count
		int[] counts = unlockListPanel.countVisibleUnlocks();
		filterBar.updateResultCount(counts[0], counts[1]);
	}
	
	
	private void toggleViewMode()
	{
		// TODO: Implement compact mode view
		// The SearchBarPanel handles the button state internally
	}
	
	private void onRefreshClicked(ActionEvent event)
	{
		// Visual feedback
		refreshButton.setText("⟳ Refreshing...");
		refreshButton.setEnabled(false);
		refreshButton.setBackground(UIConstants.DISABLED_TEXT_COLOR);
		
		// Show loading in content area
		unlockListPanel.showStatus("Refreshing data from wiki...");
		
		SwingUtilities.invokeLater(() -> {
			boolean success = false;
			try
			{
				repository.refreshData();
				success = true;
			}
			catch (Exception ex)
			{
				log.error("Failed to refresh data", ex);
			}
			
			// Reset button
			refreshButton.setText(success ? "✓ Refresh Data" : "✗ Refresh Data");
			refreshButton.setEnabled(true);
			refreshButton.setBackground(success ? UIConstants.SUCCESS_COLOR : UIConstants.ERROR_COLOR);
			
			// Restore button color after delay
			// Stop any existing timer first
			if (refreshButtonResetTimer != null && refreshButtonResetTimer.isRunning())
			{
				refreshButtonResetTimer.stop();
			}
			
			refreshButtonResetTimer = new javax.swing.Timer(2000, evt -> {
				refreshButton.setText("Refresh Data");
				refreshButton.setBackground(UIConstants.HOVER_COLOR);
			});
			refreshButtonResetTimer.setRepeats(false);
			refreshButtonResetTimer.start();
			
			if (selectedSkill != null)
			{
				updateContentForSkill(selectedSkill);
			}
		});
	}
	
	
	@SuppressWarnings("deprecation")
	public void updatePlayerStats()
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				playerLevels.put(skill, client.getRealSkillLevel(skill));
			}
		}
		
		skillSelectorPanel.updatePlayerLevels(playerLevels);
		
		if (selectedSkill != null)
		{
			updateContentForSkill(selectedSkill);
		}
	}
	
	
	/**
	 * Cleanup method to stop all timers and release resources
	 */
	public void cleanup()
	{
		// Stop refresh button timer
		if (refreshButtonResetTimer != null && refreshButtonResetTimer.isRunning())
		{
			refreshButtonResetTimer.stop();
			refreshButtonResetTimer = null;
		}
		
		// Clean up panels
		if (unlockListPanel != null)
		{
			unlockListPanel.cleanup();
		}
		
		if (progressPanel != null)
		{
			progressPanel.cleanup();
		}
		
		if (filterBar != null)
		{
			filterBar.cleanup();
		}
		
		// Clear references
		selectedSkill = null;
		playerLevels.clear();
	}
}