/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.smartbot.model;

import org.l2jmobius.gameserver.config.custom.SmartBotConfig;

/**
 * SmartBot Presets for Level 20 with Grade D Gear.
 */
public enum SmartBotPreset
{
	ARCHER(
		7, // Human Rogue (Archer)
		20,
		SmartBotRole.FIGHTER,
		new int[][]
		{
			{278, 1},    // Reinforced Long Bow (D Grade)
			{17, 3000},   // Wooden Arrow
			{398, 1},    // Manticore Skin Shirt (D Grade)
			{418, 1},    // Manticore Skin Gaiters (D Grade)
			{2437, 1},   // Manticore Skin Boots (D Grade)
			{41, 1},     // Leather Helmet
			{607, 1},    // Leather Gauntlets
			{916, 1},    // Elven Necklace (D Grade)
			{852, 2},    // Elven Earring (D Grade)
			{884, 2},    // Elven Ring (D Grade)
			{1463, 3000}, // Soulshot: D-grade
			{728, 100},   // Mana Potion
			{1539, 50}    // Greater Healing Potion
		},
		Buffs.FIGHTER_BUFFS
	),
	
	MAGE(
		11, // Human Wizard
		20,
		SmartBotRole.MAGE,
		new int[][]
		{
			{179, 1},    // Staff of Life (D Grade)
			{439, 1},    // Knowledge Tunic (D Grade)
			{471, 1},    // Knowledge Stockings (D Grade)
			{2459, 1},   // Knowledge Gloves (D Grade)
			{2423, 1},   // Leather Boots
			{41, 1},     // Leather Helmet
			{916, 1},    // Elven Necklace (D Grade)
			{852, 2},    // Elven Earring (D Grade)
			{884, 2},    // Elven Ring (D Grade)
			{3948, 3000}, // Blessed Spiritshot: D-Grade
			{728, 100},   // Mana Potion
			{1539, 50}    // Greater Healing Potion
		},
		Buffs.MAGE_BUFFS
	),
	
	HEALER(
		15, // Cleric (Healer)
		20,
		SmartBotRole.HEALER,
		new int[][]
		{
			{179, 1},    // Staff of Life (D Grade)
			{439, 1},    // Knowledge Tunic (D Grade)
			{471, 1},    // Knowledge Stockings (D Grade)
			{2459, 1},   // Knowledge Gloves (D Grade)
			{2423, 1},   // Leather Boots
			{41, 1},     // Leather Helmet
			{916, 1},    // Elven Necklace (D Grade)
			{852, 2},    // Elven Earring (D Grade)
			{884, 2},    // Elven Ring (D Grade)
			{3948, 3000}, // Blessed Spiritshot: D-Grade
			{728, 100},   // Mana Potion
			{1539, 50}    // Greater Healing Potion
		},
		Buffs.MAGE_BUFFS
	),
	
	BUFFER(
		15, // Cleric (Buffer)
		20,
		SmartBotRole.BUFFER,
		new int[][]
		{
			{179, 1},    // Staff of Life (D Grade)
			{439, 1},    // Knowledge Tunic (D Grade)
			{471, 1},    // Knowledge Stockings (D Grade)
			{2459, 1},   // Knowledge Gloves (D Grade)
			{2423, 1},   // Leather Boots
			{41, 1},     // Leather Helmet
			{916, 1},    // Elven Necklace (D Grade)
			{852, 2},    // Elven Earring (D Grade)
			{884, 2},    // Elven Ring (D Grade)
			{3948, 3000}, // Blessed Spiritshot: D-Grade
			{728, 100},   // Mana Potion
			{1539, 50}    // Greater Healing Potion
		},
		Buffs.MAGE_BUFFS
	),
	
	DAGGER(
		7, // Human Rogue (Dagger)
		20,
		SmartBotRole.FIGHTER,
		new int[][]
		{
			{223, 1},    // Cursed Maingauche (D Grade)
			{398, 1},    // Manticore Skin Shirt (D Grade)
			{418, 1},    // Manticore Skin Gaiters (D Grade)
			{2437, 1},   // Manticore Skin Boots (D Grade)
			{41, 1},     // Leather Helmet
			{607, 1},    // Leather Gauntlets
			{916, 1},    // Elven Necklace (D Grade)
			{852, 2},    // Elven Earring (D Grade)
			{884, 2},    // Elven Ring (D Grade)
			{1463, 3000}, // Soulshot: D-grade
			{728, 100},   // Mana Potion
			{1539, 50}    // Greater Healing Potion
		},
		Buffs.FIGHTER_BUFFS
	),
	
	TANK(
		4, // Human Knight (Tank)
		20,
		SmartBotRole.FIGHTER,
		new int[][]
		{
			{70, 1},     // Bastard Sword (D Grade)
			{2494, 1},   // Brigandine Shield (D Grade)
			{355, 1},    // Brigandine Tunic (D Grade)
			{383, 1},    // Brigandine Gaiters (D Grade)
			{45, 1},     // Brigandine Helmet (D Grade)
			{2463, 1},   // Brigandine Gauntlets (D Grade)
			{2439, 1},   // Brigandine Boots (D Grade)
			{916, 1},    // Elven Necklace (D Grade)
			{852, 2},    // Elven Earring (D Grade)
			{884, 2},    // Elven Ring (D Grade)
			{1463, 3000}, // Soulshot: D-grade
			{728, 100},   // Mana Potion
			{1539, 50}    // Greater Healing Potion
		},
		Buffs.FIGHTER_BUFFS
	);
	
	private final int _classId;
	private final int _level;
	private final SmartBotRole _role;
	private final int[][] _items;
	private final int[][] _buffSkillIds;
	
	SmartBotPreset(int classId, int level, SmartBotRole role, int[][] items, int[][] buffSkillIds)
	{
		_classId = classId;
		_level = level;
		_role = role;
		_items = items;
		_buffSkillIds = buffSkillIds;
	}
	
	public int getClassId()
	{
		return _classId;
	}
	
	public int getLevel()
	{
		return SmartBotConfig.BOT_STARTING_LEVEL > 0 ? SmartBotConfig.BOT_STARTING_LEVEL : _level;
	}
	
	public SmartBotRole getRole()
	{
		return _role;
	}
	
	public int[][] getItems()
	{
		return _items;
	}
	
	public int[][] getBuffSkillIds()
	{
		return _buffSkillIds;
	}
	
	public static SmartBotPreset fromName(String name)
	{
		try
		{
			return valueOf(name.toUpperCase());
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public static SmartBotPreset fromClassId(int classId)
	{
		for (SmartBotPreset preset : values())
		{
			if (preset._classId == classId)
			{
				return preset;
			}
		}
		return null;
	}
	
	public static final class Buffs
	{
		public static final int[][] FIGHTER_BUFFS =
		{
			{1204, 2}, // Wind Walk
			{1040, 3}, // Shield
			{1035, 4}, // Mental Shield
			{1068, 3}, // Might
			{1086, 2}, // Haste
			{1077, 3}, // Focus
			{1240, 3}, // Guidance
			{1242, 3}, // Death Whisper
			{1268, 4}, // Vampiric Rage
			{1045, 6}, // Blessed Body
		};
		
		public static final int[][] MAGE_BUFFS =
		{
			{1204, 2}, // Wind Walk
			{1040, 3}, // Shield
			{1035, 4}, // Mental Shield
			{1085, 3}, // Acumen
			{1059, 3}, // Empower
			{1078, 6}, // Concentration
			{1045, 6}, // Blessed Body
			{1048, 6}, // Blessed Soul
			{1303, 2}, // Wild Magic
		};
	}
}
