package com.runelite.skillunlocks.ui.components.indicators;

import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

public class CircularProgressGauge extends JPanel
{
	private static final int GAUGE_SIZE = 120;
	private static final int STROKE_WIDTH = 8;
	private static final Color TRACK_COLOR = new Color(45, 45, 50);
	private static final Color PROGRESS_COLOR = new Color(46, 213, 115);
	private static final Color MILESTONE_COLOR = new Color(255, 215, 0);
	
	private Skill skill;
	private int playerLevel = 1;
	private int unlockedCount = 0;
	private int totalCount = 0;
	private Integer nextUnlockLevel = null;
	private float animatedProgress = 0f;
	private float targetProgress = 0f;
	private Timer animationTimer;
	
	public CircularProgressGauge()
	{
		setPreferredSize(new Dimension(GAUGE_SIZE + 20, GAUGE_SIZE + 40));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setOpaque(false);
		
		// Animation timer for smooth progress updates
		animationTimer = new Timer(16, e -> {
			if (Math.abs(animatedProgress - targetProgress) > 0.01f)
			{
				float diff = targetProgress - animatedProgress;
				animatedProgress += diff * 0.1f;
				repaint();
			}
			else
			{
				animatedProgress = targetProgress;
				animationTimer.stop();
				repaint();
			}
		});
	}
	
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		int centerX = getWidth() / 2;
		int centerY = (getHeight() - 20) / 2;
		int radius = GAUGE_SIZE / 2;
		
		// Draw outer glow for high completion
		if (animatedProgress > 0.75f)
		{
			float glowAlpha = (animatedProgress - 0.75f) * 4f; // 0 to 1
			g2d.setColor(new Color(46, 213, 115, (int)(30 * glowAlpha)));
			for (int i = 3; i > 0; i--)
			{
				g2d.fillOval(
					centerX - radius - i * 4,
					centerY - radius - i * 4,
					GAUGE_SIZE + i * 8,
					GAUGE_SIZE + i * 8
				);
			}
		}
		
		// Draw track
		g2d.setStroke(new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(TRACK_COLOR);
		g2d.draw(new Ellipse2D.Float(
			centerX - radius + (float) STROKE_WIDTH / 2,
			centerY - radius + (float) STROKE_WIDTH / 2,
			GAUGE_SIZE - STROKE_WIDTH,
			GAUGE_SIZE - STROKE_WIDTH
		));
		
		// Draw progress arc
		if (animatedProgress > 0)
		{
            g2d.setColor(PROGRESS_COLOR);
			Arc2D progressArc = new Arc2D.Float(
				centerX - radius + (float) STROKE_WIDTH / 2,
				centerY - radius + (float) STROKE_WIDTH / 2,
				GAUGE_SIZE - STROKE_WIDTH,
				GAUGE_SIZE - STROKE_WIDTH,
				90, // Start at top
				-animatedProgress * 360, // Clockwise
				Arc2D.OPEN
			);
			g2d.draw(progressArc);
			
			// Draw milestone markers
			drawMilestoneMarkers(g2d, centerX, centerY, radius);
		}
		
		// Draw center content
		drawCenterContent(g2d, centerX, centerY);
		
		// Draw skill name and next unlock below gauge
		drawBottomInfo(g2d, centerY + radius + 15);
		
		g2d.dispose();
	}
	
	private void drawMilestoneMarkers(Graphics2D g2d, int centerX, int centerY, int radius)
	{
		float[] milestones = {0.25f, 0.5f, 0.75f, 1f};
		int markerRadius = 4;
		
		for (float milestone : milestones)
		{
			if (animatedProgress >= milestone)
			{
				double angle = Math.toRadians(90 - milestone * 360);
				int markerX = centerX + (int)(Math.cos(angle) * (radius - (double) STROKE_WIDTH / 2));
				int markerY = centerY - (int)(Math.sin(angle) * (radius - (double) STROKE_WIDTH / 2));
				
				// Outer glow
				g2d.setColor(new Color(255, 215, 0, 50));
				g2d.fillOval(markerX - markerRadius - 2, markerY - markerRadius - 2,
					markerRadius * 2 + 4, markerRadius * 2 + 4);
				
				// Marker dot
				g2d.setColor(MILESTONE_COLOR);
				g2d.fillOval(markerX - markerRadius, markerY - markerRadius,
					markerRadius * 2, markerRadius * 2);
			}
		}
	}
	
	private void drawCenterContent(Graphics2D g2d, int centerX, int centerY)
	{
		// Large percentage
		int percentage = totalCount > 0 ? (int)(animatedProgress * 100) : 0;
		g2d.setColor(Color.WHITE);
		g2d.setFont(FontManager.getRunescapeBoldFont().deriveFont(28f));
		String percentText = percentage + "%";
		FontMetrics fm = g2d.getFontMetrics();
		int textX = centerX - fm.stringWidth(percentText) / 2;
		g2d.drawString(percentText, textX, centerY + 5);
		
		// Level below percentage
		if (playerLevel > 0)
		{
			g2d.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			g2d.setFont(FontManager.getRunescapeSmallFont());
			String levelText = "Level " + playerLevel;
			fm = g2d.getFontMetrics();
			textX = centerX - fm.stringWidth(levelText) / 2;
			g2d.drawString(levelText, textX, centerY + 20);
		}
		
		// Unlock count
		g2d.setColor(new Color(150, 150, 150));
		g2d.setFont(FontManager.getRunescapeSmallFont());
		String countText = unlockedCount + "/" + totalCount;
		fm = g2d.getFontMetrics();
		textX = centerX - fm.stringWidth(countText) / 2;
		g2d.drawString(countText, textX, centerY - 25);
	}
	
	private void drawBottomInfo(Graphics2D g2d, int y)
	{
		if (skill == null) return;
		
		// Skill name
		g2d.setColor(Color.WHITE);
		g2d.setFont(FontManager.getRunescapeBoldFont());
		String skillName = formatSkillName(skill);
		FontMetrics fm = g2d.getFontMetrics();
		int textX = getWidth() / 2 - fm.stringWidth(skillName) / 2;
		g2d.drawString(skillName, textX, y);
		
		// Next unlock info
		if (nextUnlockLevel != null && nextUnlockLevel <= 99 && nextUnlockLevel > playerLevel)
		{
			g2d.setColor(new Color(255, 234, 167));
			g2d.setFont(FontManager.getRunescapeSmallFont());
			String nextText = "Next unlock at level " + nextUnlockLevel;
			fm = g2d.getFontMetrics();
			textX = getWidth() / 2 - fm.stringWidth(nextText) / 2;
			g2d.drawString(nextText, textX, y + 15);
		}
		else if (animatedProgress >= 1f)
		{
			// All unlocked!
			drawCompletionSparkles(g2d, getWidth() / 2, y - 5);
			
			g2d.setColor(MILESTONE_COLOR);
			g2d.setFont(FontManager.getRunescapeSmallFont());
			String completeText = "✨ All unlocked! ✨";
			fm = g2d.getFontMetrics();
			textX = getWidth() / 2 - fm.stringWidth(completeText) / 2;
			g2d.drawString(completeText, textX, y + 15);
		}
	}
	
	private void drawCompletionSparkles(Graphics2D g2d, int centerX, int centerY)
	{
		// Simple sparkle effect for completed skills
		long time = System.currentTimeMillis();
		g2d.setColor(new Color(255, 215, 0, 100));
		
		for (int i = 0; i < 3; i++)
		{
			double angle = (time / 1000.0 + i * 120) % 360;
			double radians = Math.toRadians(angle);
			int x = centerX + (int)(Math.cos(radians) * 30);
			int y = centerY + (int)(Math.sin(radians) * 15);
			
			g2d.fillOval(x - 2, y - 2, 4, 4);
		}
	}
	
	public void updateProgress(Skill skill, int playerLevel, int unlockedCount, int totalCount, Integer nextUnlockLevel)
	{
		this.skill = skill;
		this.playerLevel = playerLevel;
		this.unlockedCount = unlockedCount;
		this.totalCount = totalCount;
		this.nextUnlockLevel = nextUnlockLevel;
		
		float newProgress = totalCount > 0 ? (float) unlockedCount / totalCount : 0f;
		if (newProgress != targetProgress)
		{
			targetProgress = newProgress;
			animationTimer.start();
		}
		
		repaint();
	}
	
	public void reset()
	{
		skill = null;
		playerLevel = 1;
		unlockedCount = 0;
		totalCount = 0;
		nextUnlockLevel = null;
		targetProgress = 0f;
		animatedProgress = 0f;
		repaint();
	}
	
	private String formatSkillName(Skill skill)
	{
		String name = skill.getName();
		return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
	}
	
	/**
	 * Cleanup method to stop timers and release resources
	 */
	public void cleanup()
	{
		if (animationTimer != null && animationTimer.isRunning())
		{
			animationTimer.stop();
			animationTimer = null;
		}
		
		// Reset state
		reset();
	}
}