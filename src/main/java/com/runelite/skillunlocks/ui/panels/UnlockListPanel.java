package com.runelite.skillunlocks.ui.panels;

import com.runelite.skillunlocks.domain.model.SkillUnlock;
import com.runelite.skillunlocks.ui.components.cards.MilestoneCard;
import com.runelite.skillunlocks.ui.components.cards.UnlockCard;
import com.runelite.skillunlocks.ui.components.controls.PillFilterBar;
import com.runelite.skillunlocks.util.UnlockFilterUtil;
import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Specialized panel for displaying the list of skill unlocks
 * Manages milestone cards and filtering
 */
public class UnlockListPanel extends JPanel implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	@Getter
	private final JPanel contentPanel;
	
	@Getter
	private final JLabel statusLabel;
	
	private final List<MilestoneCard> milestoneCards = new ArrayList<>();
	
	public UnlockListPanel()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		// Content panel for unlock cards
		contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		
		// Status label for empty states
		statusLabel = new JLabel("Select a skill to view unlocks", SwingConstants.CENTER);
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setFont(FontManager.getRunescapeFont());
		
		// Initially show status
		showStatus("Select a skill to view unlocks");
		
		// Create scroll pane
		JScrollPane scrollPane = createScrollPane();
		add(scrollPane, BorderLayout.CENTER);
	}
	
	private JScrollPane createScrollPane()
	{
		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		
		// Modern scrollbar
		JScrollBar vBar = scrollPane.getVerticalScrollBar();
		vBar.setPreferredSize(new Dimension(8, 0));
		vBar.setUI(new ModernScrollBarUI());
		
		return scrollPane;
	}
	
	/**
	 * Updates the content with grouped unlocks
	 */
	public void updateContent(Map<String, List<SkillUnlock>> groupedUnlocks, 
							  int playerLevel, Skill skill)
	{
		clearContent();
		
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
				UnlockCard unlockCard = new UnlockCard(unlock, playerLevel, skill);
				card.addContent(unlockCard);
			}
			
			milestoneCards.add(card);
			contentPanel.add(card);
			contentPanel.add(Box.createVerticalStrut(5));
		}
		
		contentPanel.revalidate();
		contentPanel.repaint();
	}
	
	/**
	 * Filters the displayed content based on search and filter criteria
	 */
	public void filterContent(String searchText, PillFilterBar.FilterType filterType, 
							  int playerLevel)
	{
		if (milestoneCards.isEmpty())
		{
			return;
		}
		
		for (MilestoneCard card : milestoneCards)
		{
			boolean hasVisibleContent = false;
			
			for (Component component : card.getContentPanel().getComponents())
			{
				if (component instanceof UnlockCard)
				{
					UnlockCard unlockCard = (UnlockCard) component;
					boolean visible = UnlockFilterUtil.shouldShowUnlock(
						unlockCard.getUnlock(), searchText, filterType, playerLevel);
					unlockCard.setVisible(visible);
					
					if (visible)
					{
						hasVisibleContent = true;
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
		
		contentPanel.revalidate();
		contentPanel.repaint();
	}
	
	/**
	 * Shows a status message in the content area
	 */
	public void showStatus(String message)
	{
		contentPanel.removeAll();
		statusLabel.setText(message);
		contentPanel.add(Box.createVerticalGlue());
		contentPanel.add(statusLabel);
		contentPanel.add(Box.createVerticalGlue());
		contentPanel.revalidate();
		contentPanel.repaint();
	}
	
	/**
	 * Clears all content from the panel
	 */
	public void clearContent()
	{
		// Clean up milestone cards
		for (MilestoneCard card : milestoneCards)
		{
			card.cleanup();
		}
		milestoneCards.clear();
		contentPanel.removeAll();
	}
	
	
	/**
	 * Counts visible unlocks
	 */
	public int[] countVisibleUnlocks()
	{
		int totalCount = 0;
		int visibleCount = 0;
		
		for (MilestoneCard card : milestoneCards)
		{
			for (Component component : card.getContentPanel().getComponents())
			{
				if (component instanceof UnlockCard)
				{
					totalCount++;
					if (component.isVisible())
					{
						visibleCount++;
					}
				}
			}
		}
		
		return new int[] { visibleCount, totalCount };
	}
	
	/**
	 * Modern scrollbar UI implementation
	 */
	private static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI
	{
		@Override
		protected void configureScrollBarColors()
		{
			thumbColor = new Color(80, 80, 80, 85);
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
	 * Cleans up resources
	 */
	public void cleanup()
	{
		clearContent();
	}
}