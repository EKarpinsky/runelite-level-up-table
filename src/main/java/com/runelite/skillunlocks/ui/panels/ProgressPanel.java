package com.runelite.skillunlocks.ui.panels;

import com.runelite.skillunlocks.ui.components.indicators.CircularProgressGauge;
import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.Serializable;

/**
 * Specialized panel for displaying skill progress
 * Contains the circular progress gauge and related information
 */
public class ProgressPanel extends JPanel implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	@Getter
	private final CircularProgressGauge progressGauge;
	
	public ProgressPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setOpaque(false);
		
		// Create progress gauge
		progressGauge = new CircularProgressGauge();
		progressGauge.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		// Add spacing and gauge
		add(Box.createVerticalGlue());
		add(progressGauge);
		add(Box.createVerticalGlue());
	}
	
	/**
	 * Updates the progress display with current skill data
	 */
	public void updateProgress(Skill skill, int playerLevel, int unlockedCount, 
							   int totalCount, Integer nextUnlockLevel)
	{
		progressGauge.updateProgress(skill, playerLevel, unlockedCount, totalCount, nextUnlockLevel);
		revalidate();
		repaint();
	}
	
	/**
	 * Resets the progress display
	 */
	public void reset()
	{
		progressGauge.reset();
		revalidate();
		repaint();
	}
	
	/**
	 * Cleans up any resources used by this panel
	 */
	public void cleanup()
	{
		progressGauge.cleanup();
	}
	
	@Override
	public Dimension getPreferredSize()
	{
		Dimension gaugeSize = progressGauge.getPreferredSize();
		return new Dimension(gaugeSize.width + 20, gaugeSize.height + 20);
	}
	
	@Override
	public Dimension getMaximumSize()
	{
		return getPreferredSize();
	}
}