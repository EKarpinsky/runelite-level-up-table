package com.runelite.skillunlocks.ui.components.cards;

import com.runelite.skillunlocks.domain.model.SkillUnlock;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.LinkBrowser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;

@Slf4j
public class UnlockCard extends JPanel
{
	private static final int CARD_HEIGHT = 75;
	private static final int LEVEL_SIZE = 50;
	private static final int CORNER_RADIUS = 12;
	private static final int SHADOW_SIZE = 3;
	
	// Status colors
	private static final Color UNLOCKED_COLOR = new Color(46, 213, 115);
	private static final Color NEXT_UNLOCK_COLOR = new Color(255, 234, 167);
	private static final Color LOCKED_COLOR = new Color(128, 128, 128);
	
	// Card backgrounds
	private static final Color CARD_BG_UNLOCKED = new Color(46, 213, 115, 20);
	private static final Color CARD_BG_NEXT = new Color(255, 234, 167, 20);
	private static final Color CARD_BG_LOCKED = new Color(45, 45, 50);
	
	@Getter
    private final SkillUnlock unlock;
	private final int playerLevel;
	private final net.runelite.api.Skill skill;
	private boolean isHovered = false;
	private boolean isExpanded = false;
	private boolean isPressed = false;
	private float hoverProgress = 0f;
	private float expandProgress = 0f;
	private Timer hoverTimer;
	private Timer expandTimer;
    private Rectangle wikiButtonBounds = null;
	private Rectangle copyButtonBounds = null;
	private Rectangle xpButtonBounds = null;
	private boolean wikiHovered = false;
	private boolean copyHovered = false;
	private boolean xpHovered = false;
	private MouseAdapter mouseAdapter;
	
	public UnlockCard(SkillUnlock unlock, int playerLevel, net.runelite.api.Skill skill)
	{
		this.unlock = unlock;
		this.playerLevel = playerLevel;
		this.skill = skill;
		
		setLayout(null);
		setOpaque(false);
		setPreferredSize(new Dimension(0, CARD_HEIGHT + SHADOW_SIZE * 2));
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		// Hover animation timer
		hoverTimer = new Timer(16, e -> {
			if (isHovered && hoverProgress < 1f)
			{
				hoverProgress = Math.min(1f, hoverProgress + 0.1f);
				repaint();
			}
			else if (!isHovered && hoverProgress > 0f)
			{
				hoverProgress = Math.max(0f, hoverProgress - 0.1f);
				repaint();
			}
			else
			{
				hoverTimer.stop();
			}
		});
		
		// Expand animation timer
		expandTimer = new Timer(16, e -> {
			if (isExpanded && expandProgress < 1f)
			{
				expandProgress = Math.min(1f, expandProgress + 0.1f);
				updatePreferredSize();
			}
			else if (!isExpanded && expandProgress > 0f)
			{
				expandProgress = Math.max(0f, expandProgress - 0.1f);
				updatePreferredSize();
			}
			else
			{
				expandTimer.stop();
			}
		});
		
		mouseAdapter = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				isHovered = true;
				hoverTimer.start();
			}
			
			@Override
			public void mouseExited(MouseEvent e)
			{
				isHovered = false;
				isPressed = false;
				hoverTimer.start();
			}
			
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					isPressed = true;
					repaint();
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && isPressed)
				{
					isPressed = false;
					toggleExpanded();
				}
			}
			
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isRightMouseButton(e))
				{
					showContextMenu(e);
				}
				else if (SwingUtilities.isLeftMouseButton(e) && isExpanded)
				{
					// Check if click is on any action button
					if (wikiButtonBounds != null && wikiButtonBounds.contains(e.getPoint()))
					{
						openWikiPage();
					}
					else if (copyButtonBounds != null && copyButtonBounds.contains(e.getPoint()))
					{
						copyUnlockName();
					}
					else if (xpButtonBounds != null && xpButtonBounds.contains(e.getPoint()))
					{
						// Could open XP calculator or show detailed XP info
						log.info("XP needed: " + calculateXPNeeded());
					}
				}
			}
		};
		addMouseListener(mouseAdapter);
		
		addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				if (isExpanded)
				{
					boolean needsRepaint = false;
					
					// Check wiki button hover
					boolean wasWikiHovered = wikiHovered;
					wikiHovered = wikiButtonBounds != null && wikiButtonBounds.contains(e.getPoint());
					if (wasWikiHovered != wikiHovered) needsRepaint = true;
					
					// Check copy button hover
					boolean wasCopyHovered = copyHovered;
					copyHovered = copyButtonBounds != null && copyButtonBounds.contains(e.getPoint());
					if (wasCopyHovered != copyHovered) needsRepaint = true;
					
					// Check XP button hover
					boolean wasXpHovered = xpHovered;
					xpHovered = xpButtonBounds != null && xpButtonBounds.contains(e.getPoint());
					if (wasXpHovered != xpHovered) needsRepaint = true;
					
					// Update cursor - keep hand cursor for entire card
					// Just update visual feedback through hover states
					
					if (needsRepaint)
					{
						repaint();
					}
				}
			}
		});
		
		// Add tooltip with full information
		setToolTipText(createTooltipText());
	}
	
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		int cardY = SHADOW_SIZE;
		int cardHeight = getHeight() - SHADOW_SIZE * 2;
		
		// Pressed state offset
		int pressOffset = isPressed ? 2 : 0;
		
		// Draw shadow (less shadow when pressed)
		if (hoverProgress > 0 && !isPressed)
		{
			g2d.setColor(new Color(0, 0, 0, (int)(50 * hoverProgress)));
			g2d.fill(new RoundRectangle2D.Float(
				2, cardY + 2, getWidth() - 4, cardHeight,
				CORNER_RADIUS, CORNER_RADIUS
			));
		}
		
		// Apply pressed transform
		if (isPressed)
		{
			g2d.translate(pressOffset, pressOffset);
		}
		
		// Draw card background
		Color bgColor = getCardBackgroundColor();
		if (hoverProgress > 0)
		{
			// Lighten on hover
			bgColor = blendColors(bgColor, ColorScheme.DARK_GRAY_HOVER_COLOR, hoverProgress * 0.3f);
		}
		if (isPressed)
		{
			// Darken when pressed
			bgColor = bgColor.darker();
		}
		g2d.setColor(bgColor);
		g2d.fill(new RoundRectangle2D.Float(
			0, cardY, getWidth(), cardHeight,
			CORNER_RADIUS, CORNER_RADIUS
		));
		
		// Draw border
		g2d.setColor(isExpanded ? ColorScheme.BRAND_ORANGE : new Color(60, 60, 60, 100));
		g2d.setStroke(new BasicStroke(isExpanded ? 2 : 1));
		g2d.draw(new RoundRectangle2D.Float(
			0.5f, cardY + 0.5f, getWidth() - 1, cardHeight - 1,
			CORNER_RADIUS, CORNER_RADIUS
		));
		
		// Draw level circle with progress ring
		drawLevelIndicator(g2d, cardY + 10);
		
		// Draw content
		drawContent(g2d, LEVEL_SIZE + 20, cardY, getWidth() - LEVEL_SIZE - 30, CARD_HEIGHT);
		
		// Draw expanded details if expanded
		if (expandProgress > 0)
		{
			drawExpandedContent(g2d, 10, cardY + CARD_HEIGHT, getWidth() - 20, expandProgress);
		}
		
		g2d.dispose();
	}
	
	private void drawLevelIndicator(Graphics2D g2d, int y)
	{
		int x = 10;
		int centerX = x + LEVEL_SIZE / 2;
		int centerY = y + LEVEL_SIZE / 2;
		
		// Background circle
		g2d.setColor(new Color(30, 30, 35));
		g2d.fillOval(x, y, LEVEL_SIZE, LEVEL_SIZE);
		
		// Progress ring
		Color statusColor = getStatusColor();
		if (unlock.getLevel() <= playerLevel)
		{
			// Full ring for unlocked
			g2d.setColor(statusColor);
			g2d.setStroke(new BasicStroke(3));
			g2d.drawOval(x + 2, y + 2, LEVEL_SIZE - 4, LEVEL_SIZE - 4);
		}
		else
		{
			// Partial ring showing progress to unlock
			float progress = (float) playerLevel / unlock.getLevel();
			int angle = (int) (360 * progress);
			
			g2d.setColor(new Color(60, 60, 60));
			g2d.setStroke(new BasicStroke(3));
			g2d.drawOval(x + 2, y + 2, LEVEL_SIZE - 4, LEVEL_SIZE - 4);
			
			g2d.setColor(statusColor);
			Arc2D arc = new Arc2D.Float(x + 2, y + 2, LEVEL_SIZE - 4, LEVEL_SIZE - 4,
				90, -angle, Arc2D.OPEN);
			g2d.draw(arc);
		}
		
		// Level number
		g2d.setColor(statusColor);
		g2d.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		String levelText = String.valueOf(unlock.getLevel());
		FontMetrics fm = g2d.getFontMetrics();
		int textX = centerX - fm.stringWidth(levelText) / 2;
		int textY = centerY + fm.getAscent() / 2 - 2;
		g2d.drawString(levelText, textX, textY);
	}
	
	private void drawContent(Graphics2D g2d, int x, int y, int width, int height)
	{
		int contentY = y + 10;
		
		// Unlock name
		g2d.setColor(Color.WHITE);
		g2d.setFont(FontManager.getRunescapeFont().deriveFont(Font.BOLD));
		String name = truncateText(g2d, unlock.getName(), width - 10);
		g2d.drawString(name, x, contentY + 15);
		
		// Type badge
		drawTypeBadge(g2d, x, contentY + 25, unlock.getType());
		
		// Members indicator
		if ("Members only".equals(unlock.getDescription()))
		{
			drawMembersBadge(g2d, x + getTypeBadgeWidth(g2d, unlock.getType()) + 8, contentY + 25);
		}
		
		// Requirements
		if (!unlock.getRequirements().isEmpty())
		{
			g2d.setColor(new Color(180, 180, 180));
			g2d.setFont(FontManager.getRunescapeSmallFont());
			String reqText = "Requires: " + truncateText(g2d, unlock.getRequirements(), width - 10);
			g2d.drawString(reqText, x, contentY + 48);
		}
	}
	
	private void drawTypeBadge(Graphics2D g2d, int x, int y, SkillUnlock.UnlockType type)
	{
		// Get type color
		Color typeColor = getTypeColor(type);
		Color bgColor = new Color(typeColor.getRed(), typeColor.getGreen(), typeColor.getBlue(), 30);
		
		// Calculate badge size
		g2d.setFont(FontManager.getRunescapeSmallFont());
		String typeText = formatType(type);
		FontMetrics fm = g2d.getFontMetrics();
		int badgeWidth = fm.stringWidth(typeText) + 16;
		int badgeHeight = 18;
		
		// Draw badge background
		g2d.setColor(bgColor);
		g2d.fill(new RoundRectangle2D.Float(x, y, badgeWidth, badgeHeight, 9, 9));
		
		// Draw badge border
		g2d.setColor(typeColor);
		g2d.setStroke(new BasicStroke(1));
		g2d.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, badgeWidth - 1, badgeHeight - 1, 9, 9));
		
		// Draw text
		g2d.setColor(typeColor);
		g2d.drawString(typeText, x + 8, y + 13);
	}
	
	private void drawMembersBadge(Graphics2D g2d, int x, int y)
	{
		Color memberColor = new Color(255, 152, 31);
		Color bgColor = new Color(255, 152, 31, 30);
		
		g2d.setFont(FontManager.getRunescapeSmallFont());
		String text = "Members";
		FontMetrics fm = g2d.getFontMetrics();
		int badgeWidth = fm.stringWidth(text) + 16;
		int badgeHeight = 18;
		
		// Draw badge
		g2d.setColor(bgColor);
		g2d.fill(new RoundRectangle2D.Float(x, y, badgeWidth, badgeHeight, 9, 9));
		
		g2d.setColor(memberColor);
		g2d.setStroke(new BasicStroke(1));
		g2d.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, badgeWidth - 1, badgeHeight - 1, 9, 9));
		
		g2d.drawString(text, x + 8, y + 13);
	}
	
	private int getTypeBadgeWidth(Graphics2D g2d, SkillUnlock.UnlockType type)
	{
		g2d.setFont(FontManager.getRunescapeSmallFont());
		return g2d.getFontMetrics().stringWidth(formatType(type)) + 16;
	}
	
	private void drawExpandedContent(Graphics2D g2d, int x, int y, int width, float progress)
	{
		// Fade in the expanded content
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, progress));
		
		// Calculate expanded height based on content
		int expandedHeight = calculateExpandedHeight();
		
		// Background for expanded section
		g2d.setColor(new Color(35, 35, 40));
		g2d.fillRect(x, y, width, (int)(expandedHeight * progress));
		
		// Separator line
		g2d.setColor(new Color(60, 60, 65));
		g2d.drawLine(x + 20, y + 5, x + width - 20, y + 5);
		
		int contentY = y + 20;
		
		// Full description
		g2d.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g2d.setFont(FontManager.getRunescapeSmallFont());
		
		// Action buttons section
		drawActionButtons(g2d, x + 10, contentY, width - 20);
		contentY += 35;
		
		// Requirements section with better formatting
		if (!unlock.getRequirements().isEmpty())
		{
			g2d.setColor(new Color(255, 200, 100));
			g2d.drawString("Requirements:", x + 10, contentY);
			contentY += 15;
			
			g2d.setColor(new Color(200, 200, 200));
			String[] reqLines = unlock.getRequirements().split(",");
			for (String req : reqLines)
			{
				String trimmedReq = req.trim();
				// Check if requirement is met (simplified check)
				boolean reqMet = checkRequirementMet(trimmedReq);
				g2d.setColor(reqMet ? new Color(100, 255, 100) : new Color(200, 200, 200));
				String prefix = reqMet ? "✓ " : "• ";
				g2d.drawString(prefix + trimmedReq, x + 20, contentY);
				contentY += 15;
			}
			contentY += 5;
		}
		
		// Additional useful information
		drawAdditionalInfo(g2d, x + 10, contentY, width - 20);
		
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
	}
	
	private Color getCardBackgroundColor()
	{
		if (unlock.getLevel() <= playerLevel)
		{
			return CARD_BG_UNLOCKED;
		}
		else if (unlock.getLevel() <= playerLevel + 5)
		{
			return CARD_BG_NEXT;
		}
		else
		{
			return CARD_BG_LOCKED;
		}
	}
	
	private Color getStatusColor()
	{
		if (unlock.getLevel() <= playerLevel)
		{
			return UNLOCKED_COLOR;
		}
		else if (unlock.getLevel() == playerLevel + 1)
		{
			return NEXT_UNLOCK_COLOR;
		}
		else
		{
			return LOCKED_COLOR;
		}
	}
	
	private Color getTypeColor(SkillUnlock.UnlockType type)
	{
		switch (type)
		{
			case ITEM:
				return new Color(100, 200, 255); // Light blue
			case SPELL:
				return new Color(255, 100, 255); // Magenta
			case PRAYER:
				return new Color(255, 255, 100); // Yellow
			case QUEST:
				return new Color(100, 255, 200); // Mint
			case LOCATION:
				return new Color(255, 200, 100); // Orange
			case ACTIVITY:
				return new Color(200, 100, 255); // Purple
			case ABILITY:
				return new Color(100, 255, 100); // Green
			default:
				return ColorScheme.LIGHT_GRAY_COLOR;
		}
	}
	
	private String formatType(SkillUnlock.UnlockType type)
	{
		String typeStr = type.toString();
		return typeStr.substring(0, 1).toUpperCase() + typeStr.substring(1).toLowerCase();
	}
	
	private String truncateText(Graphics2D g2d, String text, int maxWidth)
	{
		FontMetrics fm = g2d.getFontMetrics();
		if (fm.stringWidth(text) <= maxWidth)
		{
			return text;
		}
		
		String ellipsis = "...";
		int ellipsisWidth = fm.stringWidth(ellipsis);
		int availableWidth = maxWidth - ellipsisWidth;
		
		for (int i = text.length() - 1; i > 0; i--)
		{
			String truncated = text.substring(0, i);
			if (fm.stringWidth(truncated) <= availableWidth)
			{
				return truncated + ellipsis;
			}
		}
		
		return ellipsis;
	}
	
	private Color blendColors(Color c1, Color c2, float ratio)
	{
        float ir = 1.0f - ratio;
		
		return new Color(
			(int)(c1.getRed() * ir + c2.getRed() * ratio),
			(int)(c1.getGreen() * ir + c2.getGreen() * ratio),
			(int)(c1.getBlue() * ir + c2.getBlue() * ratio),
			(int)(c1.getAlpha() * ir + c2.getAlpha() * ratio)
		);
	}
	
	private String createTooltipText()
	{
		StringBuilder sb = new StringBuilder("<html>");
		sb.append("<b>").append(unlock.getName()).append("</b><br>");
		sb.append("Level ").append(unlock.getLevel());
		
		if ("Members only".equals(unlock.getDescription()))
		{
			sb.append(" (Members)");
		}
		
		if (!unlock.getRequirements().isEmpty())
		{
			sb.append("<br><i>").append(unlock.getRequirements()).append("</i>");
		}
		
		sb.append("</html>");
		return sb.toString();
	}

    private void toggleExpanded()
	{
		isExpanded = !isExpanded;
		expandTimer.start();
		repaint();
	}
	
	private void updatePreferredSize()
	{
		int baseHeight = CARD_HEIGHT + SHADOW_SIZE * 2;
		int expandedHeight = baseHeight + calculateExpandedHeight(); // Dynamic height based on content
		int currentHeight = baseHeight + (int)((expandedHeight - baseHeight) * expandProgress);
		setPreferredSize(new Dimension(getPreferredSize().width, currentHeight));
		revalidate();
		repaint();
	}
	
	private void showContextMenu(MouseEvent e)
	{
		JPopupMenu popup = new JPopupMenu();
		popup.setBackground(new Color(40, 40, 45));
		popup.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65)));
		
		// View on Wiki
		JMenuItem wikiItem = createMenuItem("View on OSRS Wiki", this::openWikiPage);
		popup.add(wikiItem);
		
		// Copy name
		JMenuItem copyItem = createMenuItem("Copy unlock name", this::copyUnlockName);
		popup.add(copyItem);
		
		popup.show(this, e.getX(), e.getY());
	}
	
	private JMenuItem createMenuItem(String text, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.setBackground(new Color(40, 40, 45));
		item.setForeground(Color.WHITE);
		item.setFont(FontManager.getRunescapeSmallFont());
		item.setBorder(new EmptyBorder(5, 10, 5, 10));
		
		item.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				item.setBackground(ColorScheme.BRAND_ORANGE);
			}
			
			@Override
			public void mouseExited(MouseEvent e)
			{
				item.setBackground(new Color(40, 40, 45));
			}
		});
		
		item.addActionListener(e -> action.run());
		return item;
	}
	
	private void openWikiPage()
	{
		// Build wiki URL for the specific unlock
		String pageName = getWikiPageName(unlock.getName());
		String wikiUrl = "https://oldschool.runescape.wiki/w/" + pageName.replace(" ", "_");
		
		try
		{
			LinkBrowser.browse(wikiUrl);
			log.info("Opened wiki page: " + wikiUrl);
		}
		catch (Exception e)
		{
			log.error("Failed to open wiki page", e);
		}
	}
	
	private String getWikiPageName(String unlockName)
	{
		// Clean up common patterns
		String pageName = unlockName;
		
		// Remove common prefixes
		pageName = pageName.replaceAll("^(Make|Craft|Smith|Cook|Catch|Mine|Cut|Fletch|Brew|Create|Build|Grow|Wield|Wear|Equip|Cast|Use|Enter|Access) ", "");
		
		// Handle specific patterns
		if (unlockName.toLowerCase().contains("teleport"))
		{
			// For spells, try to extract the spell name
			pageName = pageName.replace(" teleport", "");
			if (!pageName.contains("Teleport"))
			{
				pageName = pageName + " Teleport";
			}
		}
		else if (unlockName.toLowerCase().contains("prayer"))
		{
			// Extract prayer name
			pageName = pageName.replace(" prayer", "");
		}
		else if (unlockName.toLowerCase().contains("cape"))
		{
			// Cape of Accomplishment pages
			String skillName = skill.getName();
			pageName = skillName.substring(0, 1).toUpperCase() + skillName.substring(1).toLowerCase() + " cape";
		}
		
		// Handle equipment/items
		if (unlockName.toLowerCase().startsWith("wield") || unlockName.toLowerCase().startsWith("wear"))
		{
			// Extract item name
			pageName = unlockName.substring(unlockName.indexOf(" ") + 1);
		}
		
		return pageName;
	}
	
	private int calculateExpandedHeight()
	{
		int height = 60; // Base height for action buttons and padding
		
		if (!unlock.getRequirements().isEmpty())
		{
			String[] reqs = unlock.getRequirements().split(",");
			height += 20 + (reqs.length * 15); // Header + lines
		}
		
		// Additional info section
		height += 60; // Space for additional info
		
		return height;
	}
	
	private void drawActionButtons(Graphics2D g2d, int x, int y, int width)
	{
		// Quick action buttons
		int buttonWidth = 80;
		int buttonHeight = 25;
		int spacing = 10;
		
		// Wiki button
		wikiButtonBounds = new Rectangle(x, y, buttonWidth, buttonHeight);
		drawActionButton(g2d, x, y, buttonWidth, buttonHeight, "Wiki", 
			new Color(100, 150, 255), wikiHovered);
		
		// Copy button
		copyButtonBounds = new Rectangle(x + buttonWidth + spacing, y, buttonWidth, buttonHeight);
		drawActionButton(g2d, x + buttonWidth + spacing, y, buttonWidth, buttonHeight, "Copy", 
			new Color(150, 150, 150), copyHovered);
		
		// Level-specific action
		if (unlock.getLevel() > playerLevel)
		{
			int xpNeeded = calculateXPNeeded();
			String xpText = formatXP(xpNeeded) + " XP";
			int xpButtonX = x + (buttonWidth + spacing) * 2;
			xpButtonBounds = new Rectangle(xpButtonX, y, buttonWidth + 20, buttonHeight);
			drawActionButton(g2d, xpButtonX, y, buttonWidth + 20, buttonHeight, xpText, 
				new Color(255, 200, 100), xpHovered);
		}
		else
		{
			xpButtonBounds = null;
		}
	}
	
	private void drawActionButton(Graphics2D g2d, int x, int y, int width, int height, String text, Color color, boolean hovered)
	{
		// Button background
		if (hovered)
		{
			g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
		}
		else
		{
			g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
		}
		g2d.fillRoundRect(x, y, width, height, 6, 6);
		
		// Button border
		g2d.setColor(hovered ? color.brighter() : color);
		g2d.setStroke(new BasicStroke(hovered ? 2 : 1));
		g2d.drawRoundRect(x, y, width, height, 6, 6);
		
		// Button text
		g2d.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = g2d.getFontMetrics();
		int textX = x + (width - fm.stringWidth(text)) / 2;
		int textY = y + (height + fm.getAscent()) / 2 - 2;
		g2d.drawString(text, textX, textY);
	}
	
	private void drawAdditionalInfo(Graphics2D g2d, int x, int y, int width)
	{
		g2d.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g2d.setFont(FontManager.getRunescapeSmallFont());
		
		// Show type-specific information
		switch (unlock.getType())
		{
			case ITEM:
				if (unlock.getName().toLowerCase().contains("weapon") || 
					unlock.getName().toLowerCase().contains("armour") ||
					unlock.getName().toLowerCase().contains("shield"))
				{
					g2d.drawString("Combat equipment - check stats on wiki", x, y);
				}
				else if (unlock.getName().toLowerCase().contains("pickaxe") ||
						 unlock.getName().toLowerCase().contains("hatchet") ||
						 unlock.getName().toLowerCase().contains("rod"))
				{
					g2d.drawString("Skilling tool - improves gathering speed", x, y);
				}
				break;
				
			case SPELL:
				g2d.drawString("Magic spell - requires runes to cast", x, y);
				break;
				
			case PRAYER:
				g2d.drawString("Prayer - drains prayer points when active", x, y);
				break;
				
			case QUEST:
				g2d.drawString("Quest requirement - check quest guide", x, y);
				break;
				
			case LOCATION:
				g2d.drawString("New area unlocked - explore for content", x, y);
				break;
				
			case ACTIVITY:
				g2d.drawString("Training method - check efficiency guides", x, y);
				break;
				
			default:
				// Show level progress
				if (unlock.getLevel() > playerLevel)
				{
					float progress = (float) playerLevel / unlock.getLevel() * 100;
					g2d.drawString(String.format("Progress: %.1f%%", progress), x, y);
				}
				break;
		}
	}
	
	private boolean checkRequirementMet(String requirement)
	{
		// Simple check - in a real implementation this would check actual player stats
		// For now, just check if it contains a number and compare to player level
		String[] parts = requirement.split(" ");
		for (String part : parts)
		{
			try
			{
				int reqLevel = Integer.parseInt(part);
				// Assume it's a level requirement
				return playerLevel >= reqLevel;
			}
			catch (NumberFormatException e)
			{
				// Not a number, continue
			}
		}
		return false; // Can't determine, assume not met
	}
	
	private int calculateXPNeeded()
	{
		// Calculate XP needed to reach the unlock level
		// Using OSRS XP formula
		int currentXP = getXPForLevel(playerLevel);
		int targetXP = getXPForLevel(unlock.getLevel());
		return targetXP - currentXP;
	}
	
	private int getXPForLevel(int level)
	{
		if (level <= 1) return 0;
		if (level > 99) level = 99;
		
		double total = 0;
		for (int i = 1; i < level; i++)
		{
			total += Math.floor(i + 300 * Math.pow(2, i / 7.0));
		}
		return (int) Math.floor(total / 4);
	}
	
	private String formatXP(int xp)
	{
		if (xp >= 1000000)
		{
			return String.format("%.1fM", xp / 1000000.0);
		}
		else if (xp >= 1000)
		{
			return String.format("%.1fK", xp / 1000.0);
		}
		return String.valueOf(xp);
	}
	
	private void copyUnlockName()
	{
		Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new java.awt.datatransfer.StringSelection(unlock.getName()), null);
		log.info("Copied to clipboard: " + unlock.getName());
	}
	
	/**
	 * Cleanup method to stop timers and release resources
	 */
	public void cleanup()
	{
		if (hoverTimer != null && hoverTimer.isRunning())
		{
			hoverTimer.stop();
			hoverTimer = null;
		}
		
		if (expandTimer != null && expandTimer.isRunning())
		{
			expandTimer.stop();
			expandTimer = null;
		}
		
		// Remove mouse listeners to prevent memory leaks
		if (mouseAdapter != null)
		{
			removeMouseListener(mouseAdapter);
			mouseAdapter = null;
		}
		
		// Clear references
		wikiButtonBounds = null;
		copyButtonBounds = null;
		xpButtonBounds = null;
	}
}