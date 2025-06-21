package com.skillunlock.ui.modern;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class PillFilterBar extends JPanel
{
	public enum FilterType
	{
		ALL("All"),
		NEXT_5("Next 5"),
		UNLOCKED("Unlocked"),
		LOCKED("Locked"),
		ITEMS("Items"),
		QUESTS("Quests"),
		ACTIVITIES("Activities"),
		OTHER("Other");
		
		private final String displayName;
		
		FilterType(String displayName)
		{
			this.displayName = displayName;
		}
		
		public String getDisplayName()
		{
			return displayName;
		}
	}
	
	private static final int PILL_HEIGHT = 28;
	private static final int PILL_PADDING = 16;
	private static final int PILL_SPACING = 6;
	private static final Color PILL_BG = new Color(45, 45, 50);
	private static final Color PILL_SELECTED = ColorScheme.BRAND_ORANGE;
	private static final Color PILL_HOVER = new Color(60, 60, 65);
	
	private final List<PillButton> pills = new ArrayList<>();
	private FilterType selectedFilter = FilterType.ALL;
	private ActionListener filterChangeListener;
	private JLabel resultCountLabel;
	
	public PillFilterBar()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 10, 5, 10));
		
		// Create scrollable pill container
		JPanel pillContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, PILL_SPACING, 0));
		pillContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		
		// Add filter pills
		for (FilterType filter : FilterType.values())
		{
			PillButton pill = new PillButton(filter);
			pills.add(pill);
			pillContainer.add(pill);
			
			if (filter == FilterType.ALL)
			{
				pill.setSelected(true);
			}
		}
		
		// Wrap in scroll pane for horizontal scrolling
		JScrollPane scrollPane = new JScrollPane(pillContainer);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		scrollPane.setPreferredSize(new Dimension(0, PILL_HEIGHT + 4));
		
		// Custom scrollbar styling
		JScrollBar hBar = scrollPane.getHorizontalScrollBar();
		hBar.setPreferredSize(new Dimension(0, 6));
		hBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		hBar.setUI(new ModernScrollBarUI());
		
		add(scrollPane, BorderLayout.CENTER);
		
		// Result count label
		resultCountLabel = new JLabel("");
		resultCountLabel.setFont(FontManager.getRunescapeSmallFont());
		resultCountLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		resultCountLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
		add(resultCountLabel, BorderLayout.EAST);
	}
	
	private class PillButton extends JComponent
	{
		private final FilterType filter;
		private boolean selected = false;
		private boolean hovered = false;
		private float animationProgress = 0f;
		private Timer animationTimer;
		
		public PillButton(FilterType filter)
		{
			this.filter = filter;
			setCursor(new Cursor(Cursor.HAND_CURSOR));
			
			// Calculate size based on text
			FontMetrics fm = getFontMetrics(FontManager.getRunescapeSmallFont());
			int width = fm.stringWidth(filter.getDisplayName()) + PILL_PADDING * 2;
			setPreferredSize(new Dimension(width, PILL_HEIGHT));
			
			// Animation timer
			animationTimer = new Timer(16, e -> {
				if (selected && animationProgress < 1f)
				{
					animationProgress = Math.min(1f, animationProgress + 0.15f);
					repaint();
				}
				else if (!selected && animationProgress > 0f)
				{
					animationProgress = Math.max(0f, animationProgress - 0.15f);
					repaint();
				}
				else
				{
					animationTimer.stop();
				}
			});
			
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					hovered = true;
					repaint();
				}
				
				@Override
				public void mouseExited(MouseEvent e)
				{
					hovered = false;
					repaint();
				}
				
				@Override
				public void mouseClicked(MouseEvent e)
				{
					selectFilter(filter);
				}
			});
		}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			int width = getWidth();
			int height = getHeight();
			
			// Background color
			Color bgColor = PILL_BG;
			if (animationProgress > 0)
			{
				bgColor = blendColors(PILL_BG, PILL_SELECTED, animationProgress);
			}
			else if (hovered)
			{
				bgColor = PILL_HOVER;
			}
			
			// Draw pill background
			g2d.setColor(bgColor);
			g2d.fill(new RoundRectangle2D.Float(0, 0, width, height, height, height));
			
			// Draw text
			g2d.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics fm = g2d.getFontMetrics();
			
			Color textColor = animationProgress > 0.5f ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR;
			g2d.setColor(textColor);
			
			String text = filter.getDisplayName();
			int textX = (width - fm.stringWidth(text)) / 2;
			int textY = (height + fm.getAscent()) / 2 - 2;
			g2d.drawString(text, textX, textY);
			
			g2d.dispose();
		}
		
		public void setSelected(boolean selected)
		{
			if (this.selected != selected)
			{
				this.selected = selected;
				animationTimer.start();
			}
		}
		
		private Color blendColors(Color c1, Color c2, float ratio)
		{
			float r = ratio;
			float ir = 1.0f - r;
			
			return new Color(
				(int)(c1.getRed() * ir + c2.getRed() * r),
				(int)(c1.getGreen() * ir + c2.getGreen() * r),
				(int)(c1.getBlue() * ir + c2.getBlue() * r)
			);
		}
	}
	
	private void selectFilter(FilterType filter)
	{
		if (filter == selectedFilter)
		{
			return;
		}
		
		selectedFilter = filter;
		
		// Update pill states
		for (PillButton pill : pills)
		{
			pill.setSelected(pill.filter == filter);
		}
		
		// Notify listener
		if (filterChangeListener != null)
		{
			filterChangeListener.actionPerformed(null);
		}
	}
	
	public FilterType getSelectedFilter()
	{
		return selectedFilter;
	}
	
	public void setFilterChangeListener(ActionListener listener)
	{
		this.filterChangeListener = listener;
	}
	
	public void updateResultCount(int shown, int total)
	{
		if (shown == total)
		{
			resultCountLabel.setText(total + " unlocks");
		}
		else
		{
			resultCountLabel.setText(shown + "/" + total);
		}
	}
	
	// Custom scrollbar UI for modern look
	private static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI
	{
		@Override
		protected void configureScrollBarColors()
		{
			thumbColor = new Color(80, 80, 80);
			trackColor = ColorScheme.DARKER_GRAY_COLOR;
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
			button.setMinimumSize(new Dimension(0, 0));
			button.setMaximumSize(new Dimension(0, 0));
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
			g2d.fillRoundRect(thumbBounds.x, thumbBounds.y + 1,
				thumbBounds.width, thumbBounds.height - 2, 4, 4);
			
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
		// Clean up all pill button timers
		for (PillButton pill : pills)
		{
			if (pill.animationTimer != null && pill.animationTimer.isRunning())
			{
				pill.animationTimer.stop();
				pill.animationTimer = null;
			}
		}
		pills.clear();
		
		// Clear listeners
		filterChangeListener = null;
	}
}