package com.runelite.skillunlocks.ui.components.controls;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.Serializable;

public class SearchField extends JPanel implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private final IconTextField searchField;
	private final String placeholderText = "Search unlocks...";
	
	public SearchField()
	{
		setLayout(new BorderLayout());
		setBackground(new Color(40, 40, 45));
		
		// Create IconTextField with search icon
		searchField = new IconTextField();
		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setBackground(new Color(40, 40, 45));
		searchField.setHoverBackgroundColor(new Color(45, 45, 50));
		searchField.setMinimumSize(new Dimension(0, 30));
		searchField.setPreferredSize(new Dimension(0, 30));
		
		// IconTextField handles placeholder internally based on empty text
		
		add(searchField, BorderLayout.CENTER);
		
		// Keyboard shortcut
		InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(KeyStroke.getKeyStroke("control F"), "focusSearch");
		getActionMap().put("focusSearch", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				searchField.requestFocusInWindow();
			}
		});
	}
	
	private void clearSearch()
	{
		searchField.setText("");
	}

	public String getSearchText()
	{
		// Sanitize input - remove any control characters and limit length
		String text = searchField.getText();
		if (text == null)
		{
			return "";
		}
		
		// Remove control characters and normalize whitespace
		text = text.replaceAll("\\p{Cntrl}&&[^\\r\\n\\t]", "");
		text = text.replaceAll("\\s+", " ");
		text = text.trim();
		
		// Limit length to prevent excessive memory usage
		if (text.length() > 100)
		{
			text = text.substring(0, 100);
		}
		
		return text;
	}
	
	public void addSearchListener(javax.swing.event.DocumentListener listener)
	{
		searchField.getDocument().addDocumentListener(listener);
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		searchField.setEnabled(enabled);
	}
	
	public void setText(String text)
	{
		searchField.setText(text);
	}
}