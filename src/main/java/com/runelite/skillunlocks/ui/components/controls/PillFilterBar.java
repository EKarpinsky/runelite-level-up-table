package com.runelite.skillunlocks.ui.components.controls;

import com.runelite.skillunlocks.ui.animation.ColorAnimator;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PillFilterBar extends JPanel implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	@Getter
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

    }
	
	private static final int PILL_HEIGHT = 28;
	private static final int PILL_PADDING = 8;
	private static final int PILL_SPACING = 6;
	private static final Color PILL_BG = new Color(45, 45, 50);
	private static final Color PILL_SELECTED = ColorScheme.BRAND_ORANGE;
	private static final Color PILL_HOVER = new Color(60, 60, 65);
	
	private final List<PillButton> pills = new ArrayList<>();
	@Getter
    private FilterType selectedFilter = FilterType.ALL;
	@Setter
    private ActionListener filterChangeListener;
	private final JLabel resultCountLabel;
	
	public PillFilterBar()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 10, 5, 10));
		
		// Keep every filter visible at the default sidebar width.
		JPanel pillContainer = new JPanel(new GridLayout(0, 3, PILL_SPACING, PILL_SPACING));
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
		
		add(pillContainer, BorderLayout.CENTER);
		
		// Result count label
		resultCountLabel = new JLabel("", SwingConstants.RIGHT);
		resultCountLabel.setFont(FontManager.getRunescapeSmallFont());
		resultCountLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		resultCountLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
		add(resultCountLabel, BorderLayout.SOUTH);
	}
	
	private class PillButton extends JComponent
	{
		private final FilterType filter;
		private boolean selected = false;
		private boolean hovered = false;
		private ColorAnimator colorAnimator;
		private Color currentBgColor = PILL_BG;
		
		public PillButton(FilterType filter)
		{
			this.filter = filter;
			setCursor(new Cursor(Cursor.HAND_CURSOR));
			
			// Calculate size based on text
			FontMetrics fm = getFontMetrics(FontManager.getRunescapeSmallFont());
			int width = fm.stringWidth(filter.getDisplayName()) + PILL_PADDING * 2;
			setPreferredSize(new Dimension(width, PILL_HEIGHT));
			
			// Initialize color animator
			colorAnimator = new ColorAnimator(PILL_BG, PILL_SELECTED, 150, color -> {
				currentBgColor = color;
				repaint();
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
			Color bgColor = currentBgColor;
			if (!selected && colorAnimator.getProgress() == 0 && hovered)
			{
				bgColor = PILL_HOVER;
			}
			
			// Draw pill background
			g2d.setColor(bgColor);
			g2d.fill(new RoundRectangle2D.Float(0, 0, width, height, height, height));
			
			// Draw text
			g2d.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics fm = g2d.getFontMetrics();
			
			Color textColor = colorAnimator.getProgress() > 0.5f ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR;
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
				if (selected)
				{
					colorAnimator.animateTo();
				}
				else
				{
					colorAnimator.animateFrom();
				}
			}
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
	
	/**
	 * Cleanup method to stop all timers and release resources
	 */
	public void cleanup()
	{
		// Clean up all pill button color animators
		for (PillButton pill : pills)
		{
			if (pill.colorAnimator != null)
			{
				pill.colorAnimator.cleanup();
				pill.colorAnimator = null;
			}
		}
		pills.clear();
		
		// Clear listeners
		filterChangeListener = null;
	}
}