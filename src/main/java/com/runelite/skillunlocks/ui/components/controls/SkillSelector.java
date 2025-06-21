package com.runelite.skillunlocks.ui.components.controls;

import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.Consumer;

public class SkillSelector extends JPanel
{
	private static final int ICON_SIZE = 20;
	private static final Color DROPDOWN_BG = new Color(40, 40, 40);
	private static final Color DROPDOWN_HOVER = new Color(50, 50, 50);
	
	private final SkillIconManager skillIconManager;
	private final Consumer<Skill> onSkillSelected;
	private final Map<Skill, Integer> playerLevels;
	private JComboBox<Skill> skillComboBox;
	private Skill selectedSkill;
	
	public SkillSelector(SkillIconManager skillIconManager,
						 Consumer<Skill> onSkillSelected,
						 Map<Skill, Integer> playerLevels)
	{
		this.skillIconManager = skillIconManager;
		this.onSkillSelected = onSkillSelected;
		this.playerLevels = playerLevels;
		
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		
		createSkillDropdown();
	}
	
	@SuppressWarnings("deprecation")
	private void createSkillDropdown()
	{
		// Create combo box with skills
		skillComboBox = new JComboBox<>();
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				skillComboBox.addItem(skill);
			}
		}
		
		// Custom renderer for the dropdown
		skillComboBox.setRenderer(new SkillListCellRenderer());
		
		// Style the combo box
		skillComboBox.setBackground(DROPDOWN_BG);
		skillComboBox.setForeground(Color.WHITE);
		skillComboBox.setFont(FontManager.getRunescapeFont());
		skillComboBox.setFocusable(false);
		skillComboBox.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(60, 60, 60), 1),
			new EmptyBorder(5, 10, 5, 5)
		));
		
		// Custom UI for modern look
		skillComboBox.setUI(new ModernComboBoxUI());
		
		// Create container panel with vertical layout
		JPanel containerPanel = new JPanel();
		containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
		containerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		
		// Label above dropdown
		JLabel label = new JLabel("Select Skill");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		containerPanel.add(label);
		containerPanel.add(Box.createVerticalStrut(5));
		
		// Ensure dropdown has full width
		skillComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		skillComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
		skillComboBox.setPreferredSize(new Dimension(200, 35));
		containerPanel.add(skillComboBox);
		
		add(containerPanel, BorderLayout.CENTER);
		
		// Add selection listener AFTER UI is built
		skillComboBox.addActionListener(e -> {
			Skill selected = (Skill) skillComboBox.getSelectedItem();
			if (selected != null && selected != selectedSkill)
			{
				selectedSkill = selected;
				onSkillSelected.accept(selected);
			}
		});
	}
	
	private class SkillListCellRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, 
													  int index, boolean isSelected, boolean cellHasFocus)
		{
			JPanel panel = new JPanel(new BorderLayout(10, 0));
			panel.setBorder(new EmptyBorder(5, 10, 5, 10));
			
			if (isSelected)
			{
				panel.setBackground(ColorScheme.BRAND_ORANGE);
			}
			else
			{
				panel.setBackground(index % 2 == 0 ? DROPDOWN_BG : DROPDOWN_HOVER);
			}
			
			if (value instanceof Skill)
			{
				Skill skill = (Skill) value;
				
				// Skill icon
				try
				{
					if (skillIconManager != null)
					{
						BufferedImage icon = skillIconManager.getSkillImage(skill, false);
						if (icon != null)
						{
							Image scaled = icon.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
							JLabel iconLabel = new JLabel(new ImageIcon(scaled));
							panel.add(iconLabel, BorderLayout.WEST);
						}
					}
				}
				catch (Exception e)
				{
					// Log error but continue - icon is not critical
				}
				
				// Skill name
				JLabel nameLabel = new JLabel(formatSkillName(skill));
				nameLabel.setFont(FontManager.getRunescapeFont());
				nameLabel.setForeground(isSelected ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
				panel.add(nameLabel, BorderLayout.CENTER);
				
				// Player level badge
				if (playerLevels != null)
				{
					Integer level = playerLevels.get(skill);
					if (level != null && level > 0)
					{
						JLabel levelLabel = new JLabel(String.valueOf(level));
						levelLabel.setFont(FontManager.getRunescapeSmallFont());
						levelLabel.setForeground(getLevelColor(level));
						levelLabel.setBorder(BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(getLevelColor(level), 1),
							new EmptyBorder(2, 6, 2, 6)
						));
						panel.add(levelLabel, BorderLayout.EAST);
					}
				}
			}
			
			return panel;
		}
		
		private String formatSkillName(Skill skill)
		{
			String name = skill.getName();
			return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
		}
		
		private Color getLevelColor(int level)
		{
			if (level >= 99) return new Color(255, 215, 0); // Gold
			if (level >= 75) return new Color(100, 255, 100); // Green
			if (level >= 50) return new Color(255, 255, 100); // Yellow
			if (level >= 25) return new Color(255, 200, 100); // Orange
			return ColorScheme.LIGHT_GRAY_COLOR; // Gray
		}
	}
	
	private static class ModernComboBoxUI extends BasicComboBoxUI
	{
		@Override
		protected JButton createArrowButton()
		{
			JButton button = new JButton("▼");
			button.setFont(new Font("Dialog", Font.PLAIN, 10));
			button.setBackground(DROPDOWN_BG);
			button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			button.setBorder(new EmptyBorder(0, 5, 0, 5));
			button.setFocusable(false);
			
			button.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					button.setForeground(ColorScheme.BRAND_ORANGE);
				}
				
				@Override
				public void mouseExited(MouseEvent e)
				{
					button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				}
			});
			
			return button;
		}
		
		@Override
		protected ComboPopup createPopup()
		{
			BasicComboPopup popup = (BasicComboPopup) super.createPopup();
			popup.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1));
			popup.getList().setBackground(DROPDOWN_BG);
			popup.getList().setSelectionBackground(ColorScheme.BRAND_ORANGE);
			popup.getList().setSelectionForeground(Color.WHITE);
			return popup;
		}
	}
	
	public void updatePlayerLevels(Map<Skill, Integer> levels)
	{
		if (playerLevels != null)
		{
			playerLevels.clear();
			if (levels != null)
			{
				playerLevels.putAll(levels);
			}
		}
		if (skillComboBox != null)
		{
			skillComboBox.repaint();
		}
	}
	
	public void selectSkill(Skill skill)
	{
		if (skill != null)
		{
			skillComboBox.setSelectedItem(skill);
		}
	}
}