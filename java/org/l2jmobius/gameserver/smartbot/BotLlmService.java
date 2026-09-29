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
package org.l2jmobius.gameserver.smartbot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.SmartBotConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.smartbot.model.BotMemory;
import org.l2jmobius.gameserver.smartbot.model.SmartBotData;
import org.l2jmobius.gameserver.smartbot.model.SmartBotKnowledge;
import org.l2jmobius.gameserver.smartbot.model.SmartBotRole;

/**
 * Service for local AI text generation (Ollama) and player chat command recognition.
 */
public class BotLlmService
{
	private static final Logger LOGGER = Logger.getLogger(BotLlmService.class.getName());
	public static final BotLlmService INSTANCE = new BotLlmService();
	
	private static final Pattern RESPONSE_PATTERN = Pattern.compile("\"response\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
	
	private final HttpClient _httpClient;
	private final Map<Integer, Long> _lastReplyAt = new ConcurrentHashMap<>();
	private final Map<Integer, BotMemory> _memories = new ConcurrentHashMap<>();
	
	private BotLlmService()
	{
		_httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(2))
			.build();
	}
	
	public static BotLlmService getInstance()
	{
		return INSTANCE;
	}
	
	public void handlePlayerAllChat(Player speaker, String rawMessage)
	{
		if ((speaker == null) || (rawMessage == null) || !SmartBotConfig.ENABLE_SMART_BOT)
		{
			return;
		}
		
		final String message = rawMessage.trim();
		if (message.isEmpty())
		{
			return;
		}
		
		final ChatCommand command = parseCommand(message);
		final boolean allCommand = isAllCommand(message);
		
		if (allCommand && (command != ChatCommand.NONE))
		{
			handleAllBotsCommand(speaker, message, command);
			return;
		}
		
		final Player bot = (command == ChatCommand.BUFF) ? selectBufferForMessage(speaker, message) : selectBotForMessage(speaker, message);
		if (bot == null)
		{
			return;
		}
		
		// Ownership check: if bot belongs to an owner, only the owner, party member, or GM can command it
		final SmartBotData data = SmartBotManager.getInstance().getBotData(bot.getObjectId());
		if (data != null && data.getOwnerId() > 0 && !speaker.isGM())
		{
			if (data.getOwnerId() != speaker.getObjectId())
			{
				final Player owner = World.getPlayer(data.getOwnerId());
				if (owner == null || owner.getParty() == null || !owner.getParty().getMembers().contains(speaker))
				{
					return; // Ignore command from non-owner
				}
			}
		}
		
		final BotMemory memory = getMemory(bot);
		memory.add(speaker.getName() + ": " + message);
		
		switch (command)
		{
			case FOLLOW:
			{
				SmartBotManager.getInstance().setFollowTarget(bot.getObjectId(), speaker.getObjectId());
				final String reply = generateCommandReply(bot, speaker, "follow");
				memory.add(bot.getName() + ": " + reply);
				sayWithDelay(bot, reply);
				return;
			}
			case ASSIST:
			{
				SmartBotManager.getInstance().setAssistTarget(bot.getObjectId(), speaker.getObjectId());
				final String reply = generateCommandReply(bot, speaker, "assist");
				memory.add(bot.getName() + ": " + reply);
				sayWithDelay(bot, reply);
				return;
			}
			case ATTACK:
			{
				SmartBotManager.getInstance().setAttackMode(bot.getObjectId(), true);
				final String reply = generateCommandReply(bot, speaker, "attack");
				memory.add(bot.getName() + ": " + reply);
				sayWithDelay(bot, reply);
				return;
			}
			case STOP:
			{
				SmartBotManager.getInstance().stopBot(bot.getObjectId());
				final String reply = generateCommandReply(bot, speaker, "stop");
				memory.add(bot.getName() + ": " + reply);
				sayWithDelay(bot, reply);
				return;
			}
			case BUFF:
			{
				SmartBotManager.getInstance().requestBuff(bot.getObjectId(), speaker);
				return;
			}
			case RECALL:
			{
				bot.teleToLocation(speaker.getX() + 30, speaker.getY() + 30, speaker.getZ());
				sayWithDelay(bot, "Right beside you, " + speaker.getName() + "!");
				return;
			}
			case NONE:
			default:
			{
				// Social conversational chat with Ollama / fallback
				if (isAddressedTo(bot, message) || speaker.getTarget() == bot)
				{
					requestLlmReply(bot, speaker, message);
				}
			}
		}
	}
	
	private void handleAllBotsCommand(Player speaker, String message, ChatCommand command)
	{
		final Collection<Player> nearbyBots = SmartBotManager.getInstance().getNearbyBots(speaker, SmartBotConfig.BOT_CHAT_RADIUS);
		for (Player bot : nearbyBots)
		{
			final SmartBotData data = SmartBotManager.getInstance().getBotData(bot.getObjectId());
			if (data != null && data.getOwnerId() > 0 && data.getOwnerId() != speaker.getObjectId() && !speaker.isGM())
			{
				continue;
			}
			
			switch (command)
			{
				case FOLLOW:
					SmartBotManager.getInstance().setFollowTarget(bot.getObjectId(), speaker.getObjectId());
					break;
				case ASSIST:
					SmartBotManager.getInstance().setAssistTarget(bot.getObjectId(), speaker.getObjectId());
					break;
				case ATTACK:
					SmartBotManager.getInstance().setAttackMode(bot.getObjectId(), true);
					break;
				case STOP:
					SmartBotManager.getInstance().stopBot(bot.getObjectId());
					break;
				case BUFF:
					SmartBotManager.getInstance().requestBuff(bot.getObjectId(), speaker);
					break;
				case RECALL:
					bot.teleToLocation(speaker.getX() + Rnd.get(-40, 40), speaker.getY() + Rnd.get(-40, 40), speaker.getZ());
					break;
				default:
					break;
			}
		}
	}
	
	private Player selectBotForMessage(Player speaker, String message)
	{
		final Collection<Player> nearbyBots = SmartBotManager.getInstance().getNearbyBots(speaker, SmartBotConfig.BOT_CHAT_RADIUS);
		if (nearbyBots.isEmpty())
		{
			return null;
		}
		
		// 1. Check if a bot name is explicitly mentioned
		for (Player bot : nearbyBots)
		{
			if (isAddressedTo(bot, message))
			{
				return bot;
			}
		}
		
		// 2. Check if current target is a bot
		if (speaker.getTarget() instanceof Player && SmartBotManager.getInstance().isBot((Player) speaker.getTarget()))
		{
			return (Player) speaker.getTarget();
		}
		
		// 3. Check if speaker has an owned bot nearby
		for (Player bot : nearbyBots)
		{
			final SmartBotData data = SmartBotManager.getInstance().getBotData(bot.getObjectId());
			if (data != null && data.getOwnerId() == speaker.getObjectId())
			{
				return bot;
			}
		}
		
		// 4. Return closest bot
		Player closest = null;
		double bestDistance = Double.MAX_VALUE;
		for (Player bot : nearbyBots)
		{
			final double dist = speaker.calculateDistance2D(bot);
			if (dist < bestDistance)
			{
				bestDistance = dist;
				closest = bot;
			}
		}
		return closest;
	}
	
	private Player selectBufferForMessage(Player speaker, String message)
	{
		final Collection<Player> nearbyBots = SmartBotManager.getInstance().getNearbyBots(speaker, SmartBotConfig.BOT_CHAT_RADIUS);
		for (Player bot : nearbyBots)
		{
			final SmartBotController controller = SmartBotManager.getInstance().getController(bot.getObjectId());
			if (controller != null && controller.getRole() == SmartBotRole.BUFFER)
			{
				final SmartBotData data = SmartBotManager.getInstance().getBotData(bot.getObjectId());
				if (data == null || data.getOwnerId() == 0 || data.getOwnerId() == speaker.getObjectId() || speaker.isGM())
				{
					return bot;
				}
			}
		}
		return selectBotForMessage(speaker, message);
	}
	
	private boolean isAddressedTo(Player bot, String message)
	{
		return message.toLowerCase(Locale.ROOT).contains(bot.getName().toLowerCase(Locale.ROOT));
	}
	
	private boolean isAllCommand(String message)
	{
		final String lower = message.toLowerCase(Locale.ROOT);
		return lower.contains("all") || lower.contains("todos");
	}
	
	private ChatCommand parseCommand(String message)
	{
		final String lower = message.toLowerCase(Locale.ROOT);
		if (lower.contains("follow") || lower.contains("segue") || lower.contains("seguir"))
		{
			return ChatCommand.FOLLOW;
		}
		if (lower.contains("assist") || lower.contains("ajuda") || lower.contains("ajudar"))
		{
			return ChatCommand.ASSIST;
		}
		if (lower.contains("attack") || lower.contains("ataca") || lower.contains("atacar"))
		{
			return ChatCommand.ATTACK;
		}
		if (lower.contains("stop") || lower.contains("idle") || lower.contains("para") || lower.contains("parar"))
		{
			return ChatCommand.STOP;
		}
		if (lower.contains("buff") || lower.contains("buffs") || lower.contains("buffy"))
		{
			return ChatCommand.BUFF;
		}
		if (lower.contains("recall") || lower.contains("vem") || lower.contains("aqui") || lower.contains("tele"))
		{
			return ChatCommand.RECALL;
		}
		return ChatCommand.NONE;
	}
	
	private String generateCommandReply(Player bot, Player speaker, String command)
	{
		switch (command)
		{
			case "follow":
				return "Following you, " + speaker.getName() + "!";
			case "assist":
				return "Assisting your targets!";
			case "attack":
				return "Engaging in combat!";
			case "stop":
				return "Standing by.";
			default:
				return "Understood!";
		}
	}
	
	private void requestLlmReply(Player bot, Player speaker, String userText)
	{
		final long now = System.currentTimeMillis();
		final Long lastReply = _lastReplyAt.get(bot.getObjectId());
		if (lastReply != null && (now - lastReply < SmartBotConfig.BOT_REPLY_COOLDOWN_MS))
		{
			return;
		}
		_lastReplyAt.put(bot.getObjectId(), now);
		
		ThreadPool.execute(() ->
		{
			try
			{
				final String prompt = buildPrompt(bot, speaker, userText);
				final String jsonBody = "{\"model\":\"" + SmartBotConfig.BOT_OLLAMA_MODEL + "\",\"prompt\":\"" + escapeJson(prompt) + "\",\"stream\":false}";
				
				final HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(SmartBotConfig.BOT_OLLAMA_URL))
					.header("Content-Type", "application/json")
					.timeout(Duration.ofSeconds(3))
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
					.build();
				
				final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
				if (response.statusCode() == 200)
				{
					final String reply = extractResponse(response.body());
					if (reply != null && !reply.isBlank())
					{
						final BotMemory memory = getMemory(bot);
						memory.add(bot.getName() + ": " + reply);
						sayWithDelay(bot, reply);
						return;
					}
				}
			}
			catch (Exception ignored)
			{
				// Ollama offline or timed out: fallback to predefined short reply
			}
			
			// Fallback reply
			final String fallback = getFallbackReply(userText);
			if (fallback != null)
			{
				sayWithDelay(bot, fallback);
			}
		});
	}
	
	private String buildPrompt(Player bot, Player speaker, String userText)
	{
		final String knowledge = SmartBotKnowledge.getInstance().findRelevant(userText, SmartBotConfig.BOT_KNOWLEDGE_FACTS);
		final BotMemory memory = getMemory(bot);
		
		return "You are " + bot.getName() + ", a friendly Lineage 2 player companion. "
			+ "Max words: " + SmartBotConfig.BOT_MAX_REPLY_WORDS + ". "
			+ "Facts: " + knowledge + "\n"
			+ "History:\n" + memory.render() + "\n"
			+ speaker.getName() + ": " + userText + "\n"
			+ bot.getName() + ":";
	}
	
	private String extractResponse(String json)
	{
		final Matcher matcher = RESPONSE_PATTERN.matcher(json);
		if (matcher.find())
		{
			return matcher.group(1).replace("\\n", " ").replace("\\\"", "\"").trim();
		}
		return null;
	}
	
	private String getFallbackReply(String text)
	{
		final String lower = text.toLowerCase(Locale.ROOT);
		if (lower.contains("oi") || lower.contains("ola") || lower.contains("hi") || lower.contains("hello"))
		{
			return "Hello there! Ready for battle.";
		}
		if (lower.contains("obrigado") || lower.contains("thanks") || lower.contains("ty"))
		{
			return "You're welcome!";
		}
		if (lower.contains("ajuda") || lower.contains("help"))
		{
			return "Say 'follow', 'assist', 'buff' or 'attack' to command me.";
		}
		return "I am with you!";
	}
	
	private String escapeJson(String raw)
	{
		return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
	}
	
	private void sayWithDelay(Player bot, String text)
	{
		ThreadPool.schedule(() ->
		{
			if (bot != null && bot.isOnline())
			{
				bot.broadcastPacket(new CreatureSay(bot, ChatType.GENERAL, bot.getName(), text));
			}
		}, 800L);
	}
	
	public BotMemory getMemory(Player bot)
	{
		return _memories.computeIfAbsent(bot.getObjectId(), k -> new BotMemory(SmartBotConfig.BOT_MEMORY_LINES));
	}
	
	public enum ChatCommand
	{
		NONE,
		FOLLOW,
		ASSIST,
		ATTACK,
		STOP,
		BUFF,
		RECALL
	}
}
