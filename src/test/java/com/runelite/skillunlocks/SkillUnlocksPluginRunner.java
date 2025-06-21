package com.runelite.skillunlocks;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SkillUnlocksPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SkillUnlocksPlugin.class);
		RuneLite.main(args);
	}
}