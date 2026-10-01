package com.runelite.skillunlocks;

import com.runelite.skillunlocks.ui.SkillUnlocksPanel;
import com.runelite.skillunlocks.ui.components.cards.UnlockCard;
import com.runelite.skillunlocks.ui.components.controls.SearchField;
import com.runelite.skillunlocks.ui.panels.UnlockListPanel;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;

import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;

/** Captures the real, logged-out client. No fixture data or account is used. */
public final class ClientCapture
{
	public static void main(String[] args)
	{
		try
		{
			capture(Path.of(args[0]));
			System.exit(0);
		}
		catch (Throwable error)
		{
			error.printStackTrace();
			System.exit(1);
		}
	}

	private static void capture(Path output) throws Exception
	{
		Files.createDirectories(output);
		SkillUnlocksPluginRunner.main(new String[0]);
		Client client = RuneLite.getInjector().getInstance(Client.class);
		JFrame frame = await(() -> client.getCanvas() == null ? null
			: (JFrame) SwingUtilities.getWindowAncestor(client.getCanvas()));
		await(() -> {
			for (Component component : descendants(frame))
			{
				if (component instanceof JTabbedPane)
				{
					JTabbedPane tabs = (JTabbedPane) component;
					for (int index = 0; index < tabs.getTabCount(); index++)
					{
						if ("Skill Unlocks".equals(tabs.getToolTipTextAt(index)))
						{
							tabs.setSelectedIndex(index);
							return true;
						}
					}
				}
			}
			return null;
		});
		SkillUnlocksPanel panel = await(() -> find(frame, SkillUnlocksPanel.class, Component::isShowing));
		RuneLite.getInjector().getInstance(ConfigManager.class)
			.setConfiguration("runelite", "gameSize", new Dimension(1167, 960));
		await(() -> frame.getWidth() == 1440 && frame.getHeight() == 960 ? true : null);
		onEdt(() -> {
			frame.setLocation(0, 0);
			find(frame, SearchField.class, Component::isShowing).setText("rune");
			return null;
		});

		int[] counts = await(() -> {
			int[] result = find(frame, UnlockListPanel.class, Component::isShowing).countVisibleUnlocks();
			return result[0] > 0 && result[0] < result[1] ? result : null;
		});
		onEdt(() -> {
			for (Component component : descendants(panel))
			{
				if (component instanceof UnlockCard && component.isVisible())
				{
					UnlockCard card = (UnlockCard) component;
					String text = card.getUnlock().getName() + " " + card.getUnlock().getDescription()
						+ " " + card.getUnlock().getRequirements();
					if (!text.toLowerCase(Locale.ROOT).contains("rune"))
					{
						throw new IllegalStateException("Search left an unrelated unlock visible");
					}
				}
			}
			return null;
		});
		await(() -> client.getGameState() == GameState.LOGIN_SCREEN ? true : null);

		// Allow search expansion and progress animations to finish before taking pixels.
		Thread.sleep(1500);
		Robot robot = new Robot();
		robot.mouseMove(1430, 990);
		robot.waitForIdle();
		onEdt(() -> {
			if (find(panel, UnlockCard.class, card -> card.isShowing() && card.getVisibleRect().height > 30) == null)
			{
				throw new IllegalStateException("No unlock card is visible in the screenshot");
			}
			return null;
		});
		Rectangle panelBounds = onEdt(() -> new Rectangle(panel.getLocationOnScreen(), panel.getSize()));
		Rectangle clientBounds = onEdt(frame::getBounds);
		if (!clientBounds.contains(panelBounds) || panelBounds.width < 200 || panelBounds.height < 500)
		{
			throw new IllegalStateException("Panel is clipped or too small: " + panelBounds);
		}
		write(robot, panelBounds, output.resolve("panel.png"));
		write(robot, clientBounds, output.resolve("client.png"));
		System.out.printf("PASS: real client, state=%s, search=rune, visible=%d/%d unlocks%n",
			client.getGameState(), counts[0], counts[1]);
	}

	private static void write(Robot robot, Rectangle bounds, Path path) throws Exception
	{
		ImageIO.write(robot.createScreenCapture(bounds), "png", path.toFile());
		long bytes = Files.size(path);
		if (bytes >= 1_000_000)
		{
			throw new IllegalStateException("Image exceeds 1 MB: " + path);
		}
		System.out.printf("%s: %dx%d, %d bytes%n", path, bounds.width, bounds.height, bytes);
	}

	private static <T> T await(Callable<T> query) throws Exception
	{
		long deadline = System.nanoTime() + 90_000_000_000L;
		do
		{
			T value = onEdt(query);
			if (value != null)
			{
				return value;
			}
			Thread.sleep(200);
		}
		while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the loaded Skill Unlocks panel");
	}

	private static <T> T onEdt(Callable<T> action) throws Exception
	{
		FutureTask<T> task = new FutureTask<>(action);
		SwingUtilities.invokeAndWait(task);
		return task.get();
	}

	private static <T extends Component> T find(Component root, Class<T> type, Predicate<T> matches)
	{
		for (Component component : descendants(root))
		{
			if (type.isInstance(component) && matches.test(type.cast(component)))
			{
				return type.cast(component);
			}
		}
		return null;
	}

	private static List<Component> descendants(Component root)
	{
		List<Component> components = new ArrayList<>();
		components.add(root);
		if (root instanceof Container)
		{
			for (Component child : ((Container) root).getComponents())
			{
				components.addAll(descendants(child));
			}
		}
		return components;
	}
}
