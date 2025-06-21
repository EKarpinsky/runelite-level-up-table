package com.runelite.skillunlocks.ui.animation;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Manages smooth color transitions with animation timing
 */
public class ColorAnimator
{
	private final Color startColor;
	private final Color endColor;
	private final int duration;
	private final Consumer<Color> colorUpdateCallback;
	
	private Timer animationTimer;
	private float progress = 0f;
	private boolean animating = false;
	private boolean reverse = false;
	
	public ColorAnimator(Color startColor, Color endColor, int duration, Consumer<Color> colorUpdateCallback)
	{
		this.startColor = startColor;
		this.endColor = endColor;
		this.duration = duration;
		this.colorUpdateCallback = colorUpdateCallback;
		
		initializeTimer();
	}
	
	private void initializeTimer()
	{
		final int frameDelay = 16; // ~60 FPS
		final float increment = frameDelay / (float) duration;
		
		animationTimer = new Timer(frameDelay, e -> {
			if (!reverse && progress < 1f)
			{
				progress = Math.min(1f, progress + increment);
				updateColor();
			}
			else if (reverse && progress > 0f)
			{
				progress = Math.max(0f, progress - increment);
				updateColor();
			}
			else
			{
				animating = false;
				animationTimer.stop();
			}
		});
	}
	
	private void updateColor()
	{
		Color currentColor = interpolateColor(startColor, endColor, progress);
		colorUpdateCallback.accept(currentColor);
	}
	
	private Color interpolateColor(Color c1, Color c2, float ratio)
	{
		float ir = 1.0f - ratio;
		
		return new Color(
			(int)(c1.getRed() * ir + c2.getRed() * ratio),
			(int)(c1.getGreen() * ir + c2.getGreen() * ratio),
			(int)(c1.getBlue() * ir + c2.getBlue() * ratio),
			(int)(c1.getAlpha() * ir + c2.getAlpha() * ratio)
		);
	}
	
	public void animateTo()
	{
		if (!animating || reverse)
		{
			reverse = false;
			animating = true;
			animationTimer.start();
		}
	}
	
	public void animateFrom()
	{
		if (!animating || !reverse)
		{
			reverse = true;
			animating = true;
			animationTimer.start();
		}
	}
	
	public void stop()
	{
		if (animationTimer != null && animationTimer.isRunning())
		{
			animationTimer.stop();
			animating = false;
		}
	}
	
	public void reset()
	{
		stop();
		progress = 0f;
		updateColor();
	}
	
	public float getProgress()
	{
		return progress;
	}
	
	public boolean isAnimating()
	{
		return animating;
	}
	
	public void cleanup()
	{
		if (animationTimer != null)
		{
			animationTimer.stop();
			animationTimer = null;
		}
	}
}