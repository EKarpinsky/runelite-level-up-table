package com.runelite.skillunlocks.ui.panels;

import com.runelite.skillunlocks.ui.components.controls.SearchField;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.Serializable;

/**
 * Specialized panel for search functionality
 * Combines search field with view mode toggle
 */
public class SearchBarPanel extends JPanel implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	@Getter
	private final SearchField searchField;
	
	@Getter
	private final JButton viewModeButton;
	
	private boolean compactMode = false;
	
	public SearchBarPanel()
	{
		setLayout(new BorderLayout(5, 0));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 5, 10));
		
		// Enhanced search field
		searchField = new SearchField();
		add(searchField, BorderLayout.CENTER);
		
		// View mode toggle button
		viewModeButton = createViewModeButton();
		add(viewModeButton, BorderLayout.EAST);
	}
	
	private JButton createViewModeButton()
	{
		JButton button = new JButton("⊞");
		button.setToolTipText("Toggle compact mode");
		button.setBackground(new Color(60, 60, 65));
		button.setForeground(Color.WHITE);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(70, 70, 75), 1),
			new EmptyBorder(5, 10, 5, 10)
		));
		button.setFocusPainted(false);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		// Hover effect
		button.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseEntered(java.awt.event.MouseEvent evt)
			{
				button.setBackground(ColorScheme.BRAND_ORANGE);
			}
			
			public void mouseExited(java.awt.event.MouseEvent evt)
			{
				button.setBackground(new Color(60, 60, 65));
			}
		});
		
		button.addActionListener(e -> toggleViewMode());
		
		return button;
	}
	
	private void toggleViewMode()
	{
		compactMode = !compactMode;
		viewModeButton.setText(compactMode ? "⊡" : "⊞");
		viewModeButton.setToolTipText(compactMode ? "Switch to normal view" : "Toggle compact mode");
	}
	
	/**
	 * Adds a document listener to the search field
	 */
	public void addSearchListener(DocumentListener listener)
	{
		searchField.addSearchListener(listener);
	}
	
	/**
	 * Gets the current search text
	 */
	public String getSearchText()
	{
		return searchField.getSearchText();
	}
	
	/**
	 * Adds an action listener for view mode changes
	 */
	public void addViewModeListener(ActionListener listener)
	{
		viewModeButton.addActionListener(listener);
	}
	
	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		searchField.setEnabled(enabled);
		viewModeButton.setEnabled(enabled);
	}
}