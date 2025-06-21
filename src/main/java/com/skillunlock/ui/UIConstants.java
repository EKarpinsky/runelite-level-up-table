package com.skillunlock.ui;

import java.awt.Color;
import java.awt.Dimension;

/**
 * UI Constants for the Skill Unlock plugin
 * Centralizes all colors, dimensions, and spacing values
 */
public final class UIConstants
{
	// Prevent instantiation
	private UIConstants() {}
	
	// Base colors
	public static final Color BACKGROUND_COLOR = new Color(40, 40, 45);
	public static final Color DARKER_BACKGROUND_COLOR = new Color(30, 30, 35);
	public static final Color PANEL_BACKGROUND_COLOR = new Color(45, 45, 50);
	public static final Color HOVER_COLOR = new Color(50, 50, 55);
	public static final Color BORDER_COLOR = new Color(60, 60, 65);
	public static final Color DARKER_BORDER_COLOR = new Color(70, 70, 75);
	
	// Text colors
	public static final Color TEXT_COLOR = new Color(200, 200, 200);
	public static final Color MUTED_TEXT_COLOR = new Color(150, 150, 150);
	public static final Color DISABLED_TEXT_COLOR = new Color(128, 128, 128);
	
	// Status colors
	public static final Color SUCCESS_COLOR = new Color(46, 213, 115);
	public static final Color WARNING_COLOR = new Color(255, 234, 167);
	public static final Color ERROR_COLOR = new Color(255, 100, 100);
	public static final Color INFO_COLOR = new Color(100, 200, 255);
	
	// Level milestone colors
	public static final Color MILESTONE_25_COLOR = new Color(40, 167, 69);
	public static final Color MILESTONE_50_COLOR = new Color(0, 123, 255);
	public static final Color MILESTONE_75_COLOR = new Color(148, 0, 211);
	public static final Color MILESTONE_99_COLOR = new Color(255, 215, 0);
	
	// Unlock type colors
	public static final Color TYPE_ITEM_COLOR = new Color(100, 200, 255);
	public static final Color TYPE_QUEST_COLOR = new Color(255, 200, 100);
	public static final Color TYPE_ACTIVITY_COLOR = new Color(100, 255, 100);
	public static final Color TYPE_LOCATION_COLOR = new Color(100, 255, 200);
	public static final Color TYPE_ABILITY_COLOR = new Color(255, 255, 100);
	public static final Color TYPE_SPELL_COLOR = new Color(200, 100, 255);
	public static final Color TYPE_PRAYER_COLOR = new Color(255, 100, 255);
	public static final Color TYPE_OTHER_COLOR = new Color(108, 117, 125);
	
	// Glass effect colors
	public static final Color GLASS_TOP_COLOR = new Color(50, 50, 55, 200);
	public static final Color GLASS_BOTTOM_COLOR = new Color(40, 40, 45, 200);
	public static final Color GLASS_BORDER_COLOR = new Color(70, 70, 75, 100);
	
	// Member color
	public static final Color MEMBER_COLOR = new Color(255, 152, 31);
	
	// Component dimensions
	public static final Dimension BUTTON_SIZE = new Dimension(120, 30);
	public static final Dimension ICON_SIZE = new Dimension(20, 20);
	public static final Dimension SKILL_ICON_SIZE = new Dimension(25, 25);
	public static final int SCROLLBAR_WIDTH = 8;
	
	// Spacing constants
	public static final int SMALL_SPACING = 5;
	public static final int MEDIUM_SPACING = 10;
	public static final int LARGE_SPACING = 15;
	public static final int COMPONENT_GAP = 8;
	
	// Border radius
	public static final int BORDER_RADIUS = 10;
	public static final int SMALL_BORDER_RADIUS = 4;
	
	// Animation durations (milliseconds)
	public static final int HOVER_ANIMATION_DURATION = 150;
	public static final int EXPAND_ANIMATION_DURATION = 200;
	
	// Font sizes
	public static final int LARGE_FONT_SIZE = 14;
	public static final int NORMAL_FONT_SIZE = 12;
	public static final int SMALL_FONT_SIZE = 10;
	
	/**
	 * Get color for skill level milestones
	 */
	public static Color getMilestoneColor(int level)
	{
		if (level >= 99) return MILESTONE_99_COLOR;
		if (level >= 75) return MILESTONE_75_COLOR;
		if (level >= 50) return MILESTONE_50_COLOR;
		if (level >= 25) return MILESTONE_25_COLOR;
		return TEXT_COLOR;
	}
	
	/**
	 * Create a translucent version of a color
	 */
	public static Color withAlpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
}