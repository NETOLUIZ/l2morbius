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
package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * SmartBot system configuration reader.
 */
public class SmartBotConfig
{
	private static final String SMART_BOT_CONFIG_FILE = "./config/Custom/SmartBot.ini";
	
	public static boolean ENABLE_SMART_BOT;
	public static int SMART_BOT_NPC_ID;
	public static int MAX_BOTS_PER_PLAYER;
	public static int BOT_DONATION_COIN_ID;
	public static int BOT_DONATION_COIN_COUNT;
	public static int BOT_STARTING_LEVEL;
	public static int BOT_FOLLOW_DISTANCE;
	public static int BOT_ATTACK_SEARCH_RADIUS;
	public static int BOT_HEAL_RANGE;
	public static double BOT_HEAL_HP_PERCENT;
	public static int BOT_MANA_POTION_ITEM_ID;
	public static int BOT_MANA_POTION_COUNT;
	public static double BOT_MANA_POTION_MP_PERCENT;
	public static int BOT_CHAT_RADIUS;
	public static int BOT_MAX_REPLY_CHARS;
	public static int BOT_MAX_REPLY_WORDS;
	public static long BOT_REPLY_COOLDOWN_MS;
	public static int BOT_MEMORY_LINES;
	public static int BOT_KNOWLEDGE_FACTS;
	public static String BOT_OLLAMA_URL;
	public static String BOT_OLLAMA_MODEL;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(SMART_BOT_CONFIG_FILE);
		ENABLE_SMART_BOT = config.getBoolean("EnableSmartBot", true);
		SMART_BOT_NPC_ID = config.getInt("SmartBotNpcId", 50020);
		MAX_BOTS_PER_PLAYER = config.getInt("MaxBotsPerPlayer", 3);
		BOT_DONATION_COIN_ID = config.getInt("BotDonationCoinId", 4037);
		BOT_DONATION_COIN_COUNT = config.getInt("BotDonationCoinCount", 0);
		BOT_STARTING_LEVEL = config.getInt("BotStartingLevel", 20);
		BOT_FOLLOW_DISTANCE = config.getInt("BotFollowDistance", 80);
		BOT_ATTACK_SEARCH_RADIUS = config.getInt("BotAttackSearchRadius", 900);
		BOT_HEAL_RANGE = config.getInt("BotHealRange", 900);
		BOT_HEAL_HP_PERCENT = config.getDouble("BotHealHpPercent", 85.0);
		BOT_MANA_POTION_ITEM_ID = config.getInt("BotManaPotionItemId", 728);
		BOT_MANA_POTION_COUNT = config.getInt("BotManaPotionCount", 100);
		BOT_MANA_POTION_MP_PERCENT = config.getDouble("BotManaPotionMpPercent", 35.0);
		BOT_CHAT_RADIUS = config.getInt("BotChatRadius", 1250);
		BOT_MAX_REPLY_CHARS = config.getInt("BotMaxReplyChars", 100);
		BOT_MAX_REPLY_WORDS = config.getInt("BotMaxReplyWords", 15);
		BOT_REPLY_COOLDOWN_MS = config.getLong("BotReplyCooldownMs", 2500L);
		BOT_MEMORY_LINES = config.getInt("BotMemoryLines", 10);
		BOT_KNOWLEDGE_FACTS = config.getInt("BotKnowledgeFacts", 4);
		BOT_OLLAMA_URL = config.getString("BotOllamaUrl", "http://127.0.0.1:11434/api/generate");
		BOT_OLLAMA_MODEL = config.getString("BotOllamaModel", "gemma3");
	}
}
