package com.skillunlock.ui.modern;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class EnhancedSearchField extends JPanel
{
	private final IconTextField searchField;
	private final JLabel clearButton;
	private final String placeholderText = "Search unlocks...";
	private boolean showingPlaceholder = true;
	
	public EnhancedSearchField()
	{
		setLayout(new BorderLayout());
		setBackground(new Color(40, 40, 45));
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(60, 60, 65), 1),
			new EmptyBorder(0, 5, 0, 5)
		));
		
		// Search field
		searchField = new IconTextField();
		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setBackground(new Color(40, 40, 45));
		searchField.setHoverBackgroundColor(new Color(50, 50, 55));
		searchField.setBorder(null);
		
		// Set placeholder
		searchField.setText(placeholderText);
		searchField.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		
		// Clear button
		clearButton = new JLabel("✕");
		clearButton.setFont(new Font("Dialog", Font.BOLD, 14));
		clearButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		clearButton.setVisible(false);
		clearButton.setBorder(new EmptyBorder(0, 5, 0, 5));
		
		clearButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				clearSearch();
			}
			
			@Override
			public void mouseEntered(MouseEvent e)
			{
				clearButton.setForeground(ColorScheme.BRAND_ORANGE);
			}
			
			@Override
			public void mouseExited(MouseEvent e)
			{
				clearButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
		});
		
		// Focus handling for placeholder
		searchField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				if (showingPlaceholder)
				{
					searchField.setText("");
					searchField.setForeground(Color.WHITE);
					showingPlaceholder = false;
				}
			}
			
			@Override
			public void focusLost(FocusEvent e)
			{
				if (searchField.getText().isEmpty())
				{
					searchField.setText(placeholderText);
					searchField.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
					showingPlaceholder = true;
					clearButton.setVisible(false);
				}
			}
		});
		
		// Document listener to show/hide clear button
		searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
		{
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e) { 
				if (!showingPlaceholder) {
					updateClearButton();
				}
			}
			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e) { 
				if (!showingPlaceholder) {
					updateClearButton();
				}
			}
			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e) { 
				if (!showingPlaceholder) {
					updateClearButton();
				}
			}
		});
		
		add(searchField, BorderLayout.CENTER);
		add(clearButton, BorderLayout.EAST);
		
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
	
	private void updateClearButton()
	{
		boolean hasText = !searchField.getText().isEmpty() && !showingPlaceholder;
		clearButton.setVisible(hasText);
	}
	
	private void clearSearch()
	{
		searchField.setText("");
		if (!searchField.hasFocus())
		{
			searchField.setText(placeholderText);
			searchField.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			showingPlaceholder = true;
		}
		clearButton.setVisible(false);
	}

	public String getSearchText()
	{
		if (showingPlaceholder)
		{
			return "";
		}
		
		// Sanitize input - remove any control characters and limit length
		String text = searchField.getText();
		if (text == null)
		{
			return "";
		}
		
		// Remove control characters and normalize whitespace
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
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
		// Wrap the listener to only fire when not showing placeholder
		searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
		{
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e)
			{
				if (!showingPlaceholder)
				{
					listener.insertUpdate(e);
				}
			}
			
			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e)
			{
				if (!showingPlaceholder)
				{
					listener.removeUpdate(e);
				}
			}
			
			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e)
			{
				if (!showingPlaceholder)
				{
					listener.changedUpdate(e);
				}
			}
		});
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		searchField.setEnabled(enabled);
		clearButton.setVisible(enabled && !searchField.getText().isEmpty() && !showingPlaceholder);
	}
}