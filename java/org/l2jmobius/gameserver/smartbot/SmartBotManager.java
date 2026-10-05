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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.SmartBotConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.smartbot.dao.SmartBotDao;
import org.l2jmobius.gameserver.smartbot.model.SmartBotData;
import org.l2jmobius.gameserver.smartbot.model.SmartBotKnowledge;
import org.l2jmobius.gameserver.smartbot.model.SmartBotPreset;
import org.l2jmobius.gameserver.smartbot.model.SmartBotRole;

/**
 * Master manager for SmartBot companion players.
 */
public class SmartBotManager
{
	private static final Logger LOGGER = Logger.getLogger(SmartBotManager.class.getName());
	public static final SmartBotManager INSTANCE = new SmartBotManager();
	
	private final SmartBotDao _dao = new SmartBotDao();
	private final Map<Integer, Player> _spawnedBots = new ConcurrentHashMap<>();
	private final Map<Integer, SmartBotController> _controllers = new ConcurrentHashMap<>();
	private final Map<Integer, SmartBotData> _botData = new ConcurrentHashMap<>();
	
	private SmartBotManager()
	{
	}
	
	public static SmartBotManager getInstance()
	{
		return INSTANCE;
	}
	
	public void initialize()
	{
		SmartBotKnowledge.getInstance().load();
		restoreBots();
	}
	
	public void restoreBots()
	{
		if (!SmartBotConfig.ENABLE_SMART_BOT)
		{
			return;
		}
		
		int restored = 0;
		for (SmartBotData data : _dao.loadAll())
		{
			if (!data.isActive())
			{
				continue;
			}
			
			try
			{
				if (spawnExistingBot(data))
				{
					restored++;
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "[SmartBotManager] Failed restoring bot " + data.getBotName() + ": " + e.getMessage(), e);
			}
		}
		LOGGER.info("[SmartBotManager] Restored " + restored + " active smart bots.");
	}
	
	public Player createPlayerBot(Player owner, String botName, SmartBotPreset preset)
	{
		if (owner == null || preset == null)
		{
			return null;
		}
		
		final List<Player> ownedBots = getBotsByOwner(owner);
		if (ownedBots.size() >= SmartBotConfig.MAX_BOTS_PER_PLAYER)
		{
			owner.sendMessage("Voce ja atingiu o limite maximo de " + SmartBotConfig.MAX_BOTS_PER_PLAYER + " bots!");
			return null;
		}
		
		if (SmartBotConfig.BOT_DONATION_COIN_COUNT > 0)
		{
			if (owner.getInventory().getInventoryItemCount(SmartBotConfig.BOT_DONATION_COIN_ID, -1) < SmartBotConfig.BOT_DONATION_COIN_COUNT)
			{
				owner.sendMessage("Voce precisa de " + SmartBotConfig.BOT_DONATION_COIN_COUNT + " moedas para criar este bot!");
				return null;
			}
			owner.destroyItemByItemId(ItemProcessType.BUY, SmartBotConfig.BOT_DONATION_COIN_ID, SmartBotConfig.BOT_DONATION_COIN_COUNT, owner, true);
		}
		
		String finalName = botName != null ? botName.trim() : "";
		if (finalName.isEmpty() || finalName.length() > 16)
		{
			finalName = owner.getName() + "_" + preset.name().charAt(0) + Rnd.get(10, 99);
		}
		
		final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(preset.getClassId());
		if (template == null)
		{
			owner.sendMessage("Erro: template de classe nao encontrado para preset " + preset.name());
			return null;
		}
		
		final PlayerAppearance app = new PlayerAppearance((byte) 0, (byte) 0, (byte) 0, false);
		final Player bot = Player.create(template, "bot_" + owner.getAccountName(), finalName, app);
		if (bot == null)
		{
			owner.sendMessage("Erro: Nao foi possivel criar o personagem bot no banco de dados.");
			return null;
		}
		
		bot.setOnlineStatus(true, false);
		bot.setHeading(owner.getHeading());
		
		// Set level to 20
		final int startLevel = preset.getLevel();
		final long exp = ExperienceData.getInstance().getExpForLevel(startLevel);
		bot.getStat().addExpAndSp(exp - bot.getStat().getExp(), 0, false);
		bot.giveAvailableSkills(true, true, true);
		
		// Equip items and consumables
		for (int[] itemData : preset.getItems())
		{
			final Item item = bot.getInventory().addItem(ItemProcessType.REWARD, itemData[0], itemData[1], bot, null);
			if (item != null && item.isEquipable())
			{
				bot.useEquippableItem(item, false);
			}
		}
		
		// Auto Soulshots / Spiritshots
		if (preset.getRole() == SmartBotRole.MAGE || preset.getRole() == SmartBotRole.HEALER || preset.getRole() == SmartBotRole.BUFFER)
		{
			bot.addAutoSoulShot(3948); // Blessed Spiritshot: D-Grade
		}
		else
		{
			bot.addAutoSoulShot(1463); // Soulshot: D-grade
		}
		
		// Spread bots in a circle around the owner so they don't stack
		final int botIndex = getBotsByOwner(owner).size();
		final double angle = (2 * Math.PI * botIndex) / Math.max(SmartBotConfig.MAX_BOTS_PER_PLAYER, 1);
		final int spreadRadius = 60 + (botIndex * 20);
		final int spawnX = owner.getX() + (int) (Math.cos(angle) * spreadRadius);
		final int spawnY = owner.getY() + (int) (Math.sin(angle) * spreadRadius);
		final int spawnZ = owner.getZ();
		
		final SmartBotData data = new SmartBotData(
			bot.getObjectId(),
			finalName,
			owner.getObjectId(),
			preset.name(),
			preset.getClassId(),
			spawnX,
			spawnY,
			spawnZ,
			owner.getHeading(),
			owner.getObjectId(),
			40,
			true
		);
		
		_dao.insert(data);
		internalSpawn(bot, data);
		
		// Follow owner by default
		setFollowTarget(bot.getObjectId(), owner.getObjectId());
		
		// Automatically join owner's party
		joinOwnerParty(owner, bot);
		
		owner.sendMessage("Bot " + finalName + " (" + preset.name() + ") criado com sucesso!");
		return bot;
	}
	
	public Player createGmBot(String botName, SmartBotPreset preset, int x, int y, int z)
	{
		final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(preset.getClassId());
		if (template == null)
		{
			return null;
		}
		
		final PlayerAppearance app = new PlayerAppearance((byte) 0, (byte) 0, (byte) 0, false);
		final Player bot = Player.create(template, "bot_gm", botName, app);
		if (bot == null)
		{
			return null;
		}
		
		bot.setOnlineStatus(true, false);
		bot.setHeading(0);
		
		final long exp = ExperienceData.getInstance().getExpForLevel(preset.getLevel());
		bot.getStat().addExpAndSp(exp - bot.getStat().getExp(), 0, false);
		bot.giveAvailableSkills(true, true, true);
		
		for (int[] itemData : preset.getItems())
		{
			final Item item = bot.getInventory().addItem(ItemProcessType.REWARD, itemData[0], itemData[1], bot, null);
			if (item != null && item.isEquipable())
			{
				bot.useEquippableItem(item, false);
			}
		}
		
		if (preset.getRole() == SmartBotRole.MAGE || preset.getRole() == SmartBotRole.HEALER || preset.getRole() == SmartBotRole.BUFFER)
		{
			bot.addAutoSoulShot(3948);
		}
		else
		{
			bot.addAutoSoulShot(1463);
		}
		
		final SmartBotData data = new SmartBotData(
			bot.getObjectId(),
			botName,
			0,
			preset.name(),
			preset.getClassId(),
			x,
			y,
			z,
			0,
			0,
			40,
			true
		);
		
		_dao.insert(data);
		internalSpawn(bot, data);
		return bot;
	}
	
	public boolean spawnExistingBot(SmartBotData data)
	{
		final Player bot = Player.load(data.getCharObjId());
		if (bot == null)
		{
			return false;
		}
		
		bot.setOnlineStatus(true, false);
		internalSpawn(bot, data);
		return true;
	}
	
	private void internalSpawn(Player bot, SmartBotData data)
	{
		_spawnedBots.put(bot.getObjectId(), bot);
		_botData.put(bot.getObjectId(), data);
		
		bot.setRunning();
		bot.spawnMe(data.getSpawnX(), data.getSpawnY(), data.getSpawnZ());
		bot.setHeading(data.getHeading());
		
		// Apply Wind Walk buff (skill 1204) to boost bot run speed
		final org.l2jmobius.gameserver.mechanics.skill.Skill windWalk = SkillData.getInstance().getSkill(1204, 2);
		if (windWalk != null)
		{
			windWalk.applyEffects(bot, bot);
		}
		
		final SmartBotController controller = new SmartBotController(bot, data);
		_controllers.put(bot.getObjectId(), controller);
		controller.start();
	}
	
	public void despawnBot(int objectId)
	{
		final Player bot = _spawnedBots.remove(objectId);
		final SmartBotController controller = _controllers.remove(objectId);
		final SmartBotData data = _botData.remove(objectId);
		
		if (controller != null)
		{
			controller.stop();
		}
		
		if (bot != null)
		{
			if (data != null)
			{
				data.setSpawnX(bot.getX());
				data.setSpawnY(bot.getY());
				data.setSpawnZ(bot.getZ());
				data.setHeading(bot.getHeading());
				_dao.update(data);
			}
			
			bot.autoSave();
			bot.deleteMe();
		}
	}
	
	public void deleteBot(int objectId)
	{
		despawnBot(objectId);
		_dao.delete(objectId);
		GameClient.deleteCharByObjId(objectId);
	}
	
	public void saveAll()
	{
		for (Player bot : _spawnedBots.values())
		{
			if (bot != null && bot.isOnline())
			{
				final SmartBotData data = _botData.get(bot.getObjectId());
				if (data != null)
				{
					data.setSpawnX(bot.getX());
					data.setSpawnY(bot.getY());
					data.setSpawnZ(bot.getZ());
					data.setHeading(bot.getHeading());
					_dao.update(data);
				}
				bot.autoSave();
			}
		}
	}
	
	public void setFollowTarget(int botObjectId, int targetObjectId)
	{
		final SmartBotData data = _botData.get(botObjectId);
		final Player bot = _spawnedBots.get(botObjectId);
		if (data == null)
		{
			return;
		}
		
		data.setFollowTargetObjId(targetObjectId);
		data.setAssistTargetObjId(0);
		data.setAttackMode(false);
		_dao.update(data);
		
		if (bot != null)
		{
			bot.abortAttack();
			bot.abortCast();
			bot.setTarget(null);
			bot.getAI().setIntentionIdle();
		}
	}
	
	public void clearFollowTarget(int botObjectId)
	{
		final SmartBotData data = _botData.get(botObjectId);
		if (data != null)
		{
			data.setFollowTargetObjId(0);
			_dao.update(data);
		}
	}
	
	public void setAssistTarget(int botObjectId, int targetObjectId)
	{
		final SmartBotData data = _botData.get(botObjectId);
		final Player bot = _spawnedBots.get(botObjectId);
		if (data == null)
		{
			return;
		}
		
		data.setAssistTargetObjId(targetObjectId);
		data.setFollowTargetObjId(0);
		data.setAttackMode(false);
		_dao.update(data);
		
		if (bot != null)
		{
			bot.abortAttack();
			bot.abortCast();
			bot.setTarget(null);
			bot.getAI().setIntentionIdle();
		}
	}
	
	public void setAttackMode(int botObjectId, boolean attackMode)
	{
		final SmartBotData data = _botData.get(botObjectId);
		final Player bot = _spawnedBots.get(botObjectId);
		if (data == null)
		{
			return;
		}
		
		data.setAttackMode(attackMode);
		data.setFollowTargetObjId(0);
		data.setAssistTargetObjId(0);
		_dao.update(data);
		
		if (bot != null && !attackMode)
		{
			bot.abortAttack();
			bot.abortCast();
			bot.setTarget(null);
			bot.getAI().setIntentionIdle();
		}
	}
	
	public void stopBot(int botObjectId)
	{
		final SmartBotData data = _botData.get(botObjectId);
		final Player bot = _spawnedBots.get(botObjectId);
		if (data != null)
		{
			data.setAttackMode(false);
			data.setFollowTargetObjId(0);
			data.setAssistTargetObjId(0);
			_dao.update(data);
		}
		
		if (bot != null)
		{
			bot.abortAttack();
			bot.abortCast();
			bot.setTarget(null);
			bot.getAI().setIntentionIdle();
		}
	}
	
	public void requestBuff(int botObjectId, Player target)
	{
		final SmartBotController controller = _controllers.get(botObjectId);
		if (controller != null)
		{
			controller.requestBuff(target);
		}
	}
	
	public void recallBot(Player owner, int botObjectId)
	{
		final Player bot = _spawnedBots.get(botObjectId);
		if (bot != null && owner != null)
		{
			// Spread recalled bots around the owner to avoid stacking
			final List<Player> siblings = getBotsByOwner(owner);
			int index = 0;
			for (int i = 0; i < siblings.size(); i++)
			{
				if (siblings.get(i).getObjectId() == botObjectId)
				{
					index = i;
					break;
				}
			}
			final int total = Math.max(siblings.size(), 1);
			final double angle = (2 * Math.PI * index) / total;
			final int radius = 60;
			bot.teleToLocation(owner.getX() + (int) (Math.cos(angle) * radius), owner.getY() + (int) (Math.sin(angle) * radius), owner.getZ());
			setFollowTarget(bot.getObjectId(), owner.getObjectId());
		}
	}
	
	public void handlePartyInvite(Player requestor, Player bot, int partyDistributionTypeId)
	{
		if (requestor == null || bot == null)
		{
			return;
		}
		
		final SmartBotData data = _botData.get(bot.getObjectId());
		if (data != null && data.getOwnerId() > 0 && data.getOwnerId() != requestor.getObjectId() && !requestor.isGM())
		{
			requestor.sendMessage(bot.getName() + " pertence a outro jogador e nao pode entrar no seu grupo.");
			return;
		}
		
		if (requestor.getParty() == null)
		{
			PartyDistributionType type = PartyDistributionType.findById(partyDistributionTypeId);
			if (type == null)
			{
				type = PartyDistributionType.FINDERS_KEEPERS;
			}
			final Party party = new Party(requestor, type);
			requestor.setParty(party);
			party.addPartyMember(bot);
		}
		else if (requestor.getParty().getMemberCount() < 9)
		{
			requestor.getParty().addPartyMember(bot);
		}
		
		setFollowTarget(bot.getObjectId(), requestor.getObjectId());
	}
	
	private void joinOwnerParty(Player owner, Player bot)
	{
		if (owner.getParty() == null)
		{
			final Party party = new Party(owner, PartyDistributionType.FINDERS_KEEPERS);
			owner.setParty(party);
			party.addPartyMember(bot);
		}
		else if (owner.getParty().getMemberCount() < 9)
		{
			owner.getParty().addPartyMember(bot);
		}
	}
	
	public List<Player> getBotsByOwner(Player owner)
	{
		final List<Player> list = new ArrayList<>();
		if (owner == null)
		{
			return list;
		}
		
		for (SmartBotData data : _botData.values())
		{
			if (data.getOwnerId() == owner.getObjectId())
			{
				final Player bot = _spawnedBots.get(data.getCharObjId());
				if (bot != null && bot.isOnline())
				{
					list.add(bot);
				}
			}
		}
		return list;
	}
	
	public Collection<Player> getNearbyBots(Player speaker, int radius)
	{
		final List<Player> list = new ArrayList<>();
		for (Player bot : _spawnedBots.values())
		{
			if (bot != null && bot.isOnline() && bot.calculateDistance2D(speaker) <= radius)
			{
				list.add(bot);
			}
		}
		return list;
	}
	
	public boolean isBot(int objectId)
	{
		return _spawnedBots.containsKey(objectId);
	}
	
	public boolean isBot(Player player)
	{
		return (player != null) && _spawnedBots.containsKey(player.getObjectId());
	}
	
	public Player getBot(int objectId)
	{
		return _spawnedBots.get(objectId);
	}
	
	public SmartBotData getBotData(int objectId)
	{
		return _botData.get(objectId);
	}
	
	public SmartBotController getController(int objectId)
	{
		return _controllers.get(objectId);
	}
	
	public Collection<Player> getSpawnedBots()
	{
		return _spawnedBots.values();
	}
}
