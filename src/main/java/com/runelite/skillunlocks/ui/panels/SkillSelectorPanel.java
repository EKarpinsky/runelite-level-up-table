
package com.runelite.skillunlocks.ui.panels;

import com.runelite.skillunlocks.ui.components.controls.SkillSelector;
import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Specialized panel for skill selection
 * Follows RuneLite pattern of specialized UI components
 */
public class SkillSelectorPanel extends JPanel implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	@Getter
	private final SkillSelector skillSelector;
	
	public SkillSelectorPanel(SkillIconManager skillIconManager, 
							  Consumer<Skill> onSkillSelected,
							  Map<Skill, Integer> playerLevels)
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		
		// Create and add skill selector
		skillSelector = new SkillSelector(skillIconManager, onSkillSelected, playerLevels);
		add(skillSelector, BorderLayout.CENTER);
	}
	
	/**
	 * Updates the player levels displayed in the selector
	 */
	public void updatePlayerLevels(Map<Skill, Integer> playerLevels)
	{
		skillSelector.updatePlayerLevels(playerLevels);
	}
	
	/**
	 * Programmatically selects a skill
	 */
	public void selectSkill(Skill skill)
	{
		skillSelector.selectSkill(skill);
	}
	
	
	@Override
	public Dimension getPreferredSize()
	{
		Dimension preferred = super.getPreferredSize();
		preferred.height = skillSelector.getPreferredSize().height + 20;
		return preferred;
	}
}