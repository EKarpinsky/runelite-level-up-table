package com.skillunlock.ui.modern;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public class MilestoneCard extends JPanel
{
	private static final int HEADER_HEIGHT = 45;
	private static final int CORNER_RADIUS = 10;
	private static final Color MILESTONE_99_COLOR = new Color(255, 215, 0); // Gold
	private static final Color MILESTONE_75_COLOR = new Color(148, 0, 211); // Purple
	private static final Color MILESTONE_50_COLOR = new Color(0, 123, 255); // Blue
	private static final Color MILESTONE_25_COLOR = new Color(40, 167, 69); // Green
	private static final Color DEFAULT_COLOR = new Color(108, 117, 125); // Gray
	
	private final String levelRange;
	private final int itemCount;
	private final int startLevel;
	private final int endLevel;
	private final JPanel contentPanel;
	private boolean expanded;
	private boolean autoExpand;
	private float expansionProgress = 0f;
	private Timer expansionTimer;
	private Color milestoneColor;
	
	public MilestoneCard(String levelRange, int itemCount, boolean startExpanded)
	{
		this.levelRange = levelRange;
		this.itemCount = itemCount;
		this.expanded = startExpanded;
		this.autoExpand = startExpanded;
		this.expansionProgress = expanded ? 1f : 0f;
		
		// Parse level range
		int[] levels = parseLevelRange(levelRange);
		this.startLevel = levels[0];
		this.endLevel = levels[1];
		this.milestoneColor = getMilestoneColor();
		
		setLayout(new BorderLayout());
		setOpaque(false);
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		// Content panel for unlock cards
		contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(new Color(40, 40, 45));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		// Expansion animation timer
		expansionTimer = new Timer(16, e -> {
			if (expanded && expansionProgress < 1f)
			{
				expansionProgress = Math.min(1f, expansionProgress + 0.1f);
			}
			else if (!expanded && expansionProgress > 0f)
			{
				expansionProgress = Math.max(0f, expansionProgress - 0.1f);
			}
			else
			{
				expansionTimer.stop();
			}
			revalidate();
			repaint();
		});
		
		// Click handler for header
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getY() <= HEADER_HEIGHT)
				{
					toggleExpanded();
				}
			}
		});
		
		add(contentPanel, BorderLayout.CENTER);
	}
	
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		int width = getWidth();
		int contentHeight = contentPanel.getPreferredSize().height;
		int expandedHeight = HEADER_HEIGHT + contentHeight + 10;
		int currentHeight = HEADER_HEIGHT + (int)((expandedHeight - HEADER_HEIGHT) * expansionProgress);
		
		// Draw card background
		g2d.setColor(new Color(45, 45, 50));
		g2d.fill(new RoundRectangle2D.Float(0, 0, width, currentHeight, CORNER_RADIUS, CORNER_RADIUS));
		
		// Draw header gradient
		GradientPaint headerGradient = new GradientPaint(
			0, 0, new Color(50, 50, 55),
			0, HEADER_HEIGHT, new Color(45, 45, 50)
		);
		g2d.setPaint(headerGradient);
		
		Path2D headerPath = new Path2D.Float();
		headerPath.moveTo(0, CORNER_RADIUS);
		headerPath.quadTo(0, 0, CORNER_RADIUS, 0);
		headerPath.lineTo(width - CORNER_RADIUS, 0);
		headerPath.quadTo(width, 0, width, CORNER_RADIUS);
		headerPath.lineTo(width, HEADER_HEIGHT);
		headerPath.lineTo(0, HEADER_HEIGHT);
		headerPath.closePath();
		g2d.fill(headerPath);
		
		// Draw milestone accent line
		g2d.setColor(milestoneColor);
		g2d.fillRect(0, 0, 4, currentHeight);
		
		// Draw header content
		drawHeaderContent(g2d, width);
		
		// Draw border
		g2d.setColor(new Color(60, 60, 65));
		g2d.setStroke(new BasicStroke(1));
		g2d.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, currentHeight - 1, CORNER_RADIUS, CORNER_RADIUS));
		
		g2d.dispose();
		
		// Update component size
		setPreferredSize(new Dimension(width, currentHeight));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, currentHeight));
	}
	
	private void drawHeaderContent(Graphics2D g2d, int width)
	{
		int y = HEADER_HEIGHT / 2 + 5;
		
		// Expansion arrow
		g2d.setColor(milestoneColor);
		g2d.setFont(new Font("Dialog", Font.BOLD, 12));
		
		// Rotate arrow based on expansion
		Graphics2D g2dRotated = (Graphics2D) g2d.create();
		g2dRotated.translate(20, y - 5);
		g2dRotated.rotate(Math.toRadians(expanded ? 90 : 0), 0, 0);
		g2dRotated.drawString("▶", -5, 5);
		g2dRotated.dispose();
		
		// Level range with icon
		g2d.setColor(Color.WHITE);
		g2d.setFont(FontManager.getRunescapeBoldFont());
		
		// Draw level icon
		drawLevelIcon(g2d, 40, y - 8);
		
		g2d.drawString(levelRange, 65, y);
		
		// Progress bar
		if (startLevel != endLevel)
		{
			drawProgressBar(g2d, 180, y - 8, 80);
		}
		
		// Item count
		g2d.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g2d.setFont(FontManager.getRunescapeSmallFont());
		String countText = itemCount + " unlocks";
		FontMetrics fm = g2d.getFontMetrics();
		g2d.drawString(countText, width - fm.stringWidth(countText) - 15, y);
		
		// Completion indicator
		if (isCompleted())
		{
			g2d.setColor(MILESTONE_99_COLOR);
			g2d.drawString("✓", width - 35, y);
		}
	}
	
	private void drawLevelIcon(Graphics2D g2d, int x, int y)
	{
		// Draw a small icon representing the level milestone
		g2d.setColor(milestoneColor);
		
		if (endLevel >= 99)
		{
			// Star for max level
			drawStar(g2d, x + 8, y + 8, 8);
		}
		else if (endLevel >= 75)
		{
			// Diamond for high level
			drawDiamond(g2d, x + 8, y + 8, 7);
		}
		else if (endLevel >= 50)
		{
			// Square for mid level
			g2d.fillRect(x + 3, y + 3, 10, 10);
		}
		else
		{
			// Circle for low level
			g2d.fillOval(x + 2, y + 2, 12, 12);
		}
	}
	
	private void drawStar(Graphics2D g2d, int cx, int cy, int radius)
	{
		Path2D star = new Path2D.Float();
		for (int i = 0; i < 10; i++)
		{
			double angle = Math.PI * i / 5;
			double r = (i % 2 == 0) ? radius : radius * 0.5;
			double x = cx + r * Math.cos(angle - Math.PI / 2);
			double y = cy + r * Math.sin(angle - Math.PI / 2);
			
			if (i == 0)
			{
				star.moveTo(x, y);
			}
			else
			{
				star.lineTo(x, y);
			}
		}
		star.closePath();
		g2d.fill(star);
	}
	
	private void drawDiamond(Graphics2D g2d, int cx, int cy, int radius)
	{
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(cx, cy - radius);
		diamond.lineTo(cx + radius, cy);
		diamond.lineTo(cx, cy + radius);
		diamond.lineTo(cx - radius, cy);
		diamond.closePath();
		g2d.fill(diamond);
	}
	
	private void drawProgressBar(Graphics2D g2d, int x, int y, int width)
	{
		// Background
		g2d.setColor(new Color(30, 30, 35));
		g2d.fillRoundRect(x, y, width, 16, 8, 8);
		
		// Calculate progress (example - would be based on actual player progress)
		float progress = 0.6f; // Example: 60% through this level range
		
		// Progress fill
		if (progress > 0)
		{
			g2d.setColor(milestoneColor);
			g2d.fillRoundRect(x, y, (int)(width * progress), 16, 8, 8);
		}
		
		// Border
		g2d.setColor(new Color(60, 60, 65));
		g2d.setStroke(new BasicStroke(1));
		g2d.drawRoundRect(x, y, width, 16, 8, 8);
	}
	
	private int[] parseLevelRange(String range)
	{
		if (range.equals("Level 1"))
		{
			return new int[]{1, 1};
		}
		else if (range.equals("Level 99"))
		{
			return new int[]{99, 99};
		}
		else if (range.startsWith("Levels "))
		{
			String[] parts = range.substring(7).split("-");
			if (parts.length == 2)
			{
				try
				{
					return new int[]{
						Integer.parseInt(parts[0].trim()),
						Integer.parseInt(parts[1].trim())
					};
				}
				catch (NumberFormatException e)
				{
					// Fallback
				}
			}
		}
		return new int[]{1, 99};
	}
	
	private Color getMilestoneColor()
	{
		if (endLevel >= 99) return MILESTONE_99_COLOR;
		if (endLevel >= 75) return MILESTONE_75_COLOR;
		if (endLevel >= 50) return MILESTONE_50_COLOR;
		if (endLevel >= 25) return MILESTONE_25_COLOR;
		return DEFAULT_COLOR;
	}
	
	private boolean isCompleted()
	{
		// This would check if all unlocks in this range are obtained
		return false; // Placeholder
	}
	
	public void toggleExpanded()
	{
		expanded = !expanded;
		expansionTimer.start();
		
		// Update content visibility
		contentPanel.setVisible(expanded);
	}
	
	public void setExpanded(boolean expanded)
	{
		if (this.expanded != expanded)
		{
			toggleExpanded();
		}
	}
	
	public boolean isExpanded()
	{
		return expanded;
	}
	
	public void addContent(Component component)
	{
		contentPanel.add(component);
		if (contentPanel.getComponentCount() > 1)
		{
			contentPanel.add(Box.createVerticalStrut(3));
		}
	}
	
	public void clearContent()
	{
		contentPanel.removeAll();
	}
	
	public JPanel getContentPanel()
	{
		return contentPanel;
	}
	
	public boolean shouldAutoExpand()
	{
		return autoExpand;
	}
	
	/**
	 * Cleanup method to stop timers and release resources
	 */
	public void cleanup()
	{
		if (expansionTimer != null && expansionTimer.isRunning())
		{
			expansionTimer.stop();
			expansionTimer = null;
		}
		
		// Clean up content cards
		for (Component component : contentPanel.getComponents())
		{
			if (component instanceof EnhancedUnlockCard)
			{
				((EnhancedUnlockCard) component).cleanup();
			}
		}
		clearContent();
	}
}