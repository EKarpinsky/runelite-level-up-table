package com.skillunlock.ui.modern;

import com.skillunlock.SkillUnlockConfig;
import com.skillunlock.data.SkillData;
import com.skillunlock.data.SkillUnlock;
import com.skillunlock.repository.UnlockRepository;
import com.skillunlock.ui.UIConstants;
import com.skillunlock.util.UnlockFilterUtil;
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

@Slf4j
public class ModernSkillUnlockPanel extends PluginPanel
{
	private final Client client;
	private final SkillUnlockConfig config;
	private final UnlockRepository repository;
	private final SkillIconManager skillIconManager;
	
	// UI Components
	private ModernSkillSelector skillSelector;
	private CircularProgressGauge progressGauge;
	private PillFilterBar filterBar;
	private EnhancedSearchField searchField;
	private JButton refreshButton;
	private JButton viewModeButton;
    private JPanel contentPanel;
	private JLabel statusLabel;
	
	// State
	private Skill selectedSkill = null;
	private final Map<Skill, Integer> playerLevels = new HashMap<>();
	private final List<MilestoneCard> milestoneCards = new ArrayList<>();
	private boolean compactMode = false;
	private javax.swing.Timer refreshButtonResetTimer;
	
	public ModernSkillUnlockPanel(Client client, SkillUnlockConfig config, UnlockRepository repository,
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
		
		// Skill selector
		skillSelector = new ModernSkillSelector(skillIconManager, this::onSkillSelected, playerLevels);
		headerPanel.add(skillSelector);
		
		// Progress gauge
		progressGauge = new CircularProgressGauge();
		progressGauge.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerPanel.add(Box.createVerticalStrut(10));
		headerPanel.add(progressGauge);
		
		mainContainer.add(headerPanel);
		mainContainer.add(createSeparator());
		
		// Search and controls panel
		JPanel controlsPanel = new JPanel(new BorderLayout(5, 0));
		controlsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		controlsPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
		
		// Enhanced search field
		searchField = new EnhancedSearchField();
		searchField.addSearchListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e) { filterContent(); }
			@Override
			public void removeUpdate(DocumentEvent e) { filterContent(); }
			@Override
			public void changedUpdate(DocumentEvent e) { filterContent(); }
		});
		controlsPanel.add(searchField, BorderLayout.CENTER);
		
		// View mode toggle
		viewModeButton = createModernButton("⊞", "Toggle compact mode");
		viewModeButton.addActionListener(event -> toggleViewMode());
		controlsPanel.add(viewModeButton, BorderLayout.EAST);
		
		mainContainer.add(controlsPanel);
		
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
		
		// Content area with modern scroll
		contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		
		// Modern scrollbar
		JScrollBar vBar = scrollPane.getVerticalScrollBar();
		vBar.setPreferredSize(new Dimension(UIConstants.SCROLLBAR_WIDTH, 0));
		vBar.setUI(new ModernScrollBarUI());
		
		// Status label
		statusLabel = new JLabel("Select a skill to view unlocks", SwingConstants.CENTER);
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setFont(FontManager.getRunescapeFont());
		contentPanel.add(Box.createVerticalGlue());
		contentPanel.add(statusLabel);
		contentPanel.add(Box.createVerticalGlue());
		
		add(scrollPane, BorderLayout.CENTER);
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
		SwingUtilities.invokeLater(() -> skillSelector.selectSkill(Skill.ATTACK));
	}
	
	private void onSkillSelected(Skill skill)
	{
		selectedSkill = skill;
		updateContentForSkill(skill);
	}
	
	private void updateContentForSkill(Skill skill)
	{
		contentPanel.removeAll();
		milestoneCards.clear();
		
		if (skill == null)
		{
			showStatus("Select a skill to view unlocks");
			progressGauge.reset();
			return;
		}
		
		SkillData skillData = repository.getSkillData(skill);
		if (skillData == null || skillData.getAllUnlocks().isEmpty())
		{
			showStatus("Loading data for " + skill.getName() + "...");
			progressGauge.reset();
			
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
		
		// Add spacing at top
		contentPanel.add(Box.createVerticalStrut(10));
		
		// Create milestone cards
		for (Map.Entry<String, List<SkillUnlock>> entry : groupedUnlocks.entrySet())
		{
			String range = entry.getKey();
			List<SkillUnlock> unlocks = entry.getValue();
			
			// Determine if this group should start expanded
			boolean shouldExpand = UnlockFilterUtil.shouldExpandLevelGroup(range, playerLevel);
			
			MilestoneCard card = new MilestoneCard(range, unlocks.size(), shouldExpand);
			
			// Add unlock cards to the milestone
			for (SkillUnlock unlock : unlocks)
			{
				EnhancedUnlockCard unlockCard = new EnhancedUnlockCard(unlock, playerLevel, skill);
				card.addContent(unlockCard);
			}
			
			milestoneCards.add(card);
			contentPanel.add(card);
			contentPanel.add(Box.createVerticalStrut(UIConstants.COMPONENT_GAP));
		}
		
		// Update progress gauge
		updateProgress(skill, skillData);
		
		// Apply current filter
		filterContent();
		
		contentPanel.revalidate();
		contentPanel.repaint();
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
		
		progressGauge.updateProgress(skill, playerLevel, unlockedCount, allUnlocks.size(), nextUnlockLevel);
	}
	
	private void filterContent()
	{
		if (milestoneCards.isEmpty())
		{
			return;
		}
		
		String searchText = searchField.getSearchText().toLowerCase().trim();
		PillFilterBar.FilterType filterType = filterBar.getSelectedFilter();
		int playerLevel = playerLevels.getOrDefault(selectedSkill, 1);
		
		int totalCount = 0;
		int shownCount = 0;
		
		for (MilestoneCard card : milestoneCards)
		{
			boolean hasVisibleContent = false;
			
			for (Component component : card.getContentPanel().getComponents())
			{
				if (component instanceof EnhancedUnlockCard)
				{
					EnhancedUnlockCard unlockCard = (EnhancedUnlockCard) component;
					boolean visible = UnlockFilterUtil.shouldShowUnlock(unlockCard.getUnlock(), searchText, filterType, playerLevel);
					unlockCard.setVisible(visible);
					
					totalCount++;
					if (visible)
					{
						hasVisibleContent = true;
						shownCount++;
					}
				}
			}
			
			// Hide entire card if no visible content
			card.setVisible(hasVisibleContent);
			
			// Auto-expand cards with search results
			if (hasVisibleContent && !searchText.isEmpty())
			{
				card.setExpanded(true);
			}
		}
		
		// Update result count
		filterBar.updateResultCount(shownCount, totalCount);
		
		contentPanel.revalidate();
		contentPanel.repaint();
	}
	
	
	private void toggleViewMode()
	{
		compactMode = !compactMode;
		viewModeButton.setText(compactMode ? "⊡" : "⊞");
		// TODO: Implement compact mode view
	}
	
	private void onRefreshClicked(ActionEvent event)
	{
		// Visual feedback
		refreshButton.setText("⟳ Refreshing...");
		refreshButton.setEnabled(false);
		refreshButton.setBackground(UIConstants.DISABLED_TEXT_COLOR);
		
		// Show loading in content area
		showStatus("Refreshing data from wiki...");
		
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
	
	private void showStatus(String message)
	{
		contentPanel.removeAll();
		statusLabel.setText(message);
		contentPanel.add(Box.createVerticalGlue());
		contentPanel.add(statusLabel);
		contentPanel.add(Box.createVerticalGlue());
		contentPanel.revalidate();
		contentPanel.repaint();
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
		
		skillSelector.updatePlayerLevels(playerLevels);
		
		if (selectedSkill != null)
		{
			updateContentForSkill(selectedSkill);
		}
	}
	
	// Modern scrollbar UI
	private static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI
	{
		@Override
		protected void configureScrollBarColors()
		{
			thumbColor = UIConstants.withAlpha(UIConstants.MUTED_TEXT_COLOR, 85);
			trackColor = ColorScheme.DARK_GRAY_COLOR;
		}
		
		@Override
		protected JButton createDecreaseButton(int orientation)
		{
			return createInvisibleButton();
		}
		
		@Override
		protected JButton createIncreaseButton(int orientation)
		{
			return createInvisibleButton();
		}
		
		private JButton createInvisibleButton()
		{
			JButton button = new JButton();
			button.setPreferredSize(new Dimension(0, 0));
			return button;
		}
		
		@Override
		protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds)
		{
			if (thumbBounds.isEmpty() || !scrollbar.isEnabled())
			{
				return;
			}
			
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			g2d.setColor(thumbColor);
			g2d.fillRoundRect(thumbBounds.x + 2, thumbBounds.y,
				thumbBounds.width - 4, thumbBounds.height, 4, 4);
			
			g2d.dispose();
		}
		
		@Override
		protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds)
		{
			// Don't paint track
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
		
		// Clean up milestone cards and their timers
		for (MilestoneCard card : milestoneCards)
		{
			card.cleanup();
		}
		milestoneCards.clear();
		
		// Clean up other components that might have timers
		if (progressGauge != null)
		{
			progressGauge.cleanup();
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