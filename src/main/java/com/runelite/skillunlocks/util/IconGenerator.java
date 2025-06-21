package com.runelite.skillunlocks.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Simple icon generator for the plugin
 */
public class IconGenerator
{
	/**
	 * Draws the level up icon on the provided Graphics2D context
	 * @param g2d The graphics context to draw on
	 */
	public static void drawLevelUpIcon(Graphics2D g2d)
	{
		// Draw background circle
		g2d.setColor(new Color(45, 45, 50));
		g2d.fillOval(2, 2, 28, 28);
		
		// Draw border
		g2d.setColor(new Color(46, 213, 115));
		g2d.setStroke(new BasicStroke(2));
		g2d.drawOval(3, 3, 26, 26);
		
		// Draw level up arrow
		g2d.setColor(new Color(46, 213, 115));
		int[] xPoints = {16, 22, 19, 19, 13, 13, 10};
		int[] yPoints = {6, 14, 14, 24, 24, 14, 14};
		g2d.fillPolygon(xPoints, yPoints, 7);
	}

	public static void main(String[] args) throws IOException
	{
		// Create a 32x32 image
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();
		
		// Enable anti-aliasing
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// Clear background (transparent)
		g2d.setComposite(AlphaComposite.Clear);
		g2d.fillRect(0, 0, 32, 32);
		g2d.setComposite(AlphaComposite.SrcOver);
		
		// Use the shared drawing method
		drawLevelUpIcon(g2d);
		
		// Draw small "1" on bottom left
		g2d.setColor(new Color(255, 215, 0));
		g2d.setFont(new Font("Arial", Font.BOLD, 10));
		g2d.drawString("1", 6, 25);
		
		// Draw small "99" on bottom right
		g2d.drawString("99", 18, 25);
		
		g2d.dispose();
		
		// Save the image
		File outputDir = new File("src/main/resources");
		outputDir.mkdirs();
		File outputFile = new File(outputDir, "icon.png");
		ImageIO.write(image, "PNG", outputFile);
		
		System.out.println("Icon created at: " + outputFile.getAbsolutePath());
	}
}