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

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.SmartBotConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.smartbot.model.SmartBotData;
import org.l2jmobius.gameserver.smartbot.model.SmartBotPreset;
import org.l2jmobius.gameserver.smartbot.model.SmartBotRole;

/**
 * Controller driving real-time SmartBot AI (follow, combat, assist, healing, buffs, potions).
 */
public class SmartBotController implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(SmartBotController.class.getName());
	
	private static final int[] HEAL_SKILLS =
	{
		1217, // Greater Heal
		1218, // Greater Battle Heal
		1015, // Battle Heal
		1011  // Heal
	};
	
	private static final int[] RESURRECT_SKILLS =
	{
		1016, // Resurrection
		1254  // Mass Resurrection
	};
	
	private static final int[] MAGE_ATTACK_SKILLS =
	{
		1239, // Hurricane
		1230, // Prominence
		1235, // Hydro Blast
		1220, // Blaze
		1177  // Wind Strike
	};
	
	private static final int[] TANK_AGGRO_SKILLS =
	{
		28, // Aggression
		18  // Aura of Hate
	};
	
	private static final int[] DAGGER_ATTACK_SKILLS =
	{
		263, // Deadly Blow
		344  // Lethal Blow
	};
	
	private static final int[] ARCHER_STUN_SKILLS =
	{
		101 // Stun Shot
	};
	
	private final Player _bot;
	private final SmartBotData _data;
	
	private ScheduledFuture<?> _task;
	private long _lastTankAggroAt = 0L;
	private long _lastDaggerSkillAt = 0L;
	private long _lastBuffCheckAt = 0L;
	private long _lastArcherStunAt = 0L;
	private int _requestedBuffTargetObjId = 0;
	
	private static final long BUFFER_RECHECK_INTERVAL_MS = 3000L;
	private static final long DAGGER_SKILL_COOLDOWN_MS = 3000L;
	private static final long TANK_AGGRO_COOLDOWN_MS = 4000L;
	private static final long ARCHER_STUN_COOLDOWN_MS = 5000L;
	
	public SmartBotController(Player bot, SmartBotData data)
	{
		_bot = bot;
		_data = data;
	}
	
	public void start()
	{
		if (_task == null)
		{
			_task = ThreadPool.schedulePriorityTaskAtFixedRate(this, 1000L, 1000L);
		}
	}
	
	public void stop()
	{
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
		}
	}
	
	@Override
	public void run()
	{
		if ((_bot == null) || _bot.isDead() || _bot.isTeleporting())
		{
			return;
		}
		
		try
		{
			useManaPotionIfNeeded();
			
			final SmartBotRole role = getRole();
			
			// Priority 1: Requested manual buff
			if (role == SmartBotRole.BUFFER)
			{
				final Player buffTarget = getRequestedBuffTarget();
				if (buffTarget != null)
				{
					handleBufferBehavior(buffTarget);
					return;
				}
			}
			
			// Priority 2: Auto buffing party members
			if (role == SmartBotRole.BUFFER)
			{
				final Player buffTarget = findBuffTarget();
				if (buffTarget != null)
				{
					handleBufferBehavior(buffTarget);
					return;
				}
			}
			
			// Priority 3: Follow mode
			final Player followTarget = getFollowTarget();
			if (followTarget != null)
			{
				if (role == SmartBotRole.HEALER)
				{
					final Player healTarget = findHealTarget();
					if (healTarget != null)
					{
						handleHealerBehavior(healTarget);
						return;
					}
				}
				
				handleFollow(followTarget);
				return;
			}
			
			// Priority 4: Resurrection for dead follow target
			if (role == SmartBotRole.HEALER)
			{
				final Player deadFollowTarget = getFollowTargetAllowDead();
				if ((deadFollowTarget != null) && deadFollowTarget.isDead())
				{
					handleResurrectionBehavior(deadFollowTarget);
					if (_bot.isCastingNow())
					{
						return;
					}
				}
			}
			
			// Priority 5: Assist mode
			final Player assistTarget = getAssistTarget();
			if (assistTarget != null)
			{
				if (role == SmartBotRole.HEALER)
				{
					final Player healTarget = findHealTarget();
					if (healTarget != null)
					{
						handleHealerBehavior(healTarget);
						return;
					}
					handleFollow(assistTarget);
					return;
				}
				
				if (role == SmartBotRole.BUFFER)
				{
					final Player buffTarget = findBuffTarget();
					if (buffTarget != null)
					{
						handleBufferBehavior(buffTarget);
						return;
					}
					handleFollow(assistTarget);
					return;
				}
				
				final Monster assistedMob = getAssistMob(assistTarget);
				if (assistedMob != null)
				{
					handleCombat(assistedMob);
					return;
				}
				
				handleFollow(assistTarget);
				return;
			}
			
			// Priority 6: Autonomous combat mode
			if (_data.isAttackMode())
			{
				final Monster targetMob = findNearestAttackableMonster();
				if (targetMob != null)
				{
					handleCombat(targetMob);
					return;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "[SmartBotController] Error in bot " + _bot.getName() + " loop: " + e.getMessage(), e);
		}
	}
	
	private void handleFollow(Player target)
	{
		if (target == null || target.isDead())
		{
			return;
		}
		
		final double dist = _bot.calculateDistance2D(target);
		if (dist > 1500)
		{
			// Teleport near target if too far, with per-bot spread
			final int[] offset = getBotSpreadOffset(target, 60);
			_bot.teleToLocation(target.getX() + offset[0], target.getY() + offset[1], target.getZ());
			return;
		}
		
		// Use a wider follow distance (minimum 150) so bots don't stack on top of owner
		final int followDist = Math.max(SmartBotConfig.BOT_FOLLOW_DISTANCE, 150);
		if (dist > followDist)
		{
			if (_bot.getAI().getIntention() != Intention.FOLLOW)
			{
				_bot.getAI().setIntentionFollow(target);
			}
		}
	}
	
	/**
	 * Calculates a unique X/Y offset for this bot relative to its owner so multiple bots
	 * spread out in a circle rather than stacking on the same point.
	 * @param owner the target player (owner)
	 * @param radius distance from the owner center
	 * @return int[2] with {offsetX, offsetY}
	 */
	private int[] getBotSpreadOffset(Player owner, int radius)
	{
		final java.util.List<Player> siblings = SmartBotManager.getInstance().getBotsByOwner(owner);
		int index = 0;
		for (int i = 0; i < siblings.size(); i++)
		{
			if (siblings.get(i).getObjectId() == _bot.getObjectId())
			{
				index = i;
				break;
			}
		}
		final int total = Math.max(siblings.size(), 1);
		final double angle = (2 * Math.PI * index) / total;
		return new int[]
		{
			(int) (Math.cos(angle) * radius),
			(int) (Math.sin(angle) * radius)
		};
	}
	
	private void handleCombat(Monster mob)
	{
		if ((mob == null) || mob.isDead() || mob.isAlikeDead())
		{
			_bot.setTarget(null);
			_bot.getAI().setIntentionIdle();
			return;
		}
		
		if (_bot.getTarget() != mob)
		{
			_bot.setTarget(mob);
		}
		
		final SmartBotRole role = getRole();
		final long now = System.currentTimeMillis();
		
		// Mage combat
		if (role == SmartBotRole.MAGE)
		{
			if (_bot.isCastingNow())
			{
				return;
			}
			
			final Skill attackSkill = selectKnownSkill(MAGE_ATTACK_SKILLS);
			if ((attackSkill != null) && (_bot.getCurrentMp() >= attackSkill.getMpConsume()))
			{
				_bot.useMagic(attackSkill, true, false);
				return;
			}
			
			_bot.getAI().setIntentionAttack(mob);
			return;
		}
		
		// Tank combat
		if (role == SmartBotRole.FIGHTER && isTankPreset())
		{
			if ((now - _lastTankAggroAt > TANK_AGGRO_COOLDOWN_MS) && !_bot.isCastingNow())
			{
				final Skill aggroSkill = selectKnownSkill(TANK_AGGRO_SKILLS);
				if (aggroSkill != null && _bot.getCurrentMp() >= aggroSkill.getMpConsume())
				{
					_bot.useMagic(aggroSkill, true, false);
					_lastTankAggroAt = now;
					return;
				}
			}
		}
		
		// Dagger combat
		if (role == SmartBotRole.FIGHTER && isDaggerPreset())
		{
			if ((now - _lastDaggerSkillAt > DAGGER_SKILL_COOLDOWN_MS) && !_bot.isCastingNow())
			{
				final Skill blowSkill = selectKnownSkill(DAGGER_ATTACK_SKILLS);
				if (blowSkill != null && _bot.getCurrentMp() >= blowSkill.getMpConsume())
				{
					_bot.useMagic(blowSkill, true, false);
					_lastDaggerSkillAt = now;
					return;
				}
			}
		}
		
		// Archer combat
		if (role == SmartBotRole.FIGHTER && isArcherPreset())
		{
			if ((now - _lastArcherStunAt > ARCHER_STUN_COOLDOWN_MS) && !_bot.isCastingNow())
			{
				final Skill stunSkill = selectKnownSkill(ARCHER_STUN_SKILLS);
				if (stunSkill != null && _bot.getCurrentMp() >= stunSkill.getMpConsume())
				{
					_bot.useMagic(stunSkill, true, false);
					_lastArcherStunAt = now;
					return;
				}
			}
		}
		
		if (_bot.getAI().getIntention() != Intention.ATTACK)
		{
			_bot.getAI().setIntentionAttack(mob);
		}
	}
	
	private void handleHealerBehavior(Player target)
	{
		if (target == null || target.isDead() || _bot.isCastingNow())
		{
			return;
		}
		
		_bot.setTarget(target);
		final Skill healSkill = selectKnownSkill(HEAL_SKILLS);
		if (healSkill != null && _bot.getCurrentMp() >= healSkill.getMpConsume())
		{
			_bot.useMagic(healSkill, false, false);
		}
	}
	
	private void handleResurrectionBehavior(Player deadTarget)
	{
		if (deadTarget == null || !deadTarget.isDead() || _bot.isCastingNow())
		{
			return;
		}
		
		_bot.setTarget(deadTarget);
		final Skill resSkill = selectKnownSkill(RESURRECT_SKILLS);
		if (resSkill != null && _bot.getCurrentMp() >= resSkill.getMpConsume())
		{
			_bot.useMagic(resSkill, false, false);
		}
	}
	
	private void handleBufferBehavior(Player target)
	{
		if (target == null || target.isDead() || _bot.isCastingNow())
		{
			return;
		}
		
		final long now = System.currentTimeMillis();
		if (now - _lastBuffCheckAt < BUFFER_RECHECK_INTERVAL_MS)
		{
			return;
		}
		_lastBuffCheckAt = now;
		
		final int[][] buffList = target.isMageClass() ? SmartBotPreset.Buffs.MAGE_BUFFS : SmartBotPreset.Buffs.FIGHTER_BUFFS;
		for (int[] buffEntry : buffList)
		{
			final int skillId = buffEntry[0];
			final int skillLevel = buffEntry[1];
			
			if (!target.isAffectedBySkill(skillId))
			{
				Skill buffSkill = _bot.getKnownSkill(skillId);
				if (buffSkill == null)
				{
					buffSkill = SkillData.getInstance().getSkill(skillId, skillLevel);
				}
				
				if (buffSkill != null && _bot.getCurrentMp() >= buffSkill.getMpConsume())
				{
					_bot.setTarget(target);
					_bot.useMagic(buffSkill, false, false);
					return;
				}
			}
		}
		
		// If all buffs are applied, clear requested buff target
		if (_requestedBuffTargetObjId == target.getObjectId())
		{
			_requestedBuffTargetObjId = 0;
			say("All buffs applied, " + target.getName() + "!");
		}
	}
	
	private Player findHealTarget()
	{
		final Party party = _bot.getParty();
		if (party != null)
		{
			for (Player member : party.getMembers())
			{
				if (member != null && !member.isDead() && _bot.calculateDistance2D(member) <= SmartBotConfig.BOT_HEAL_RANGE)
				{
					if (member.getCurrentHpPercent() < SmartBotConfig.BOT_HEAL_HP_PERCENT)
					{
						return member;
					}
				}
			}
		}
		
		final Player followTarget = getFollowTarget();
		if (followTarget != null && !followTarget.isDead() && _bot.calculateDistance2D(followTarget) <= SmartBotConfig.BOT_HEAL_RANGE)
		{
			if (followTarget.getCurrentHpPercent() < SmartBotConfig.BOT_HEAL_HP_PERCENT)
			{
				return followTarget;
			}
		}
		
		return null;
	}
	
	private Player findBuffTarget()
	{
		final Party party = _bot.getParty();
		if (party != null)
		{
			for (Player member : party.getMembers())
			{
				if (member != null && !member.isDead() && _bot.calculateDistance2D(member) <= 800)
				{
					if (hasMissingBuffs(member))
					{
						return member;
					}
				}
			}
		}
		
		final Player followTarget = getFollowTarget();
		if (followTarget != null && !followTarget.isDead() && _bot.calculateDistance2D(followTarget) <= 800)
		{
			if (hasMissingBuffs(followTarget))
			{
				return followTarget;
			}
		}
		
		return null;
	}
	
	private boolean hasMissingBuffs(Player target)
	{
		final int[][] buffList = target.isMageClass() ? SmartBotPreset.Buffs.MAGE_BUFFS : SmartBotPreset.Buffs.FIGHTER_BUFFS;
		for (int[] buffEntry : buffList)
		{
			if (!target.isAffectedBySkill(buffEntry[0]))
			{
				return true;
			}
		}
		return false;
	}
	
	private void useManaPotionIfNeeded()
	{
		if (_bot.getCurrentMpPercent() < SmartBotConfig.BOT_MANA_POTION_MP_PERCENT)
		{
			final Item potion = _bot.getInventory().getItemByItemId(SmartBotConfig.BOT_MANA_POTION_ITEM_ID);
			if (potion != null && potion.getCount() > 0)
			{
				final IItemHandler handler = ItemHandler.getInstance().getHandler(potion.getEtcItem());
				if (handler != null)
				{
					handler.onItemUse(_bot, potion, false);
				}
			}
		}
	}
	
	private Monster findNearestAttackableMonster()
	{
		Monster nearest = null;
		double bestDistance = SmartBotConfig.BOT_ATTACK_SEARCH_RADIUS;
		
		for (Monster monster : World.getVisibleObjectsInRange(_bot, Monster.class, SmartBotConfig.BOT_ATTACK_SEARCH_RADIUS))
		{
			if (monster != null && !monster.isDead() && monster.isAutoAttackable(_bot))
			{
				final double dist = _bot.calculateDistance2D(monster);
				if (dist < bestDistance)
				{
					bestDistance = dist;
					nearest = monster;
				}
			}
		}
		return nearest;
	}
	
	private Monster getAssistMob(Player assistTarget)
	{
		if (assistTarget == null)
		{
			return null;
		}
		
		final Creature target = assistTarget.getTarget() instanceof Creature ? (Creature) assistTarget.getTarget() : null;
		if (target instanceof Monster && !target.isDead() && _bot.calculateDistance2D(target) <= SmartBotConfig.BOT_ATTACK_SEARCH_RADIUS)
		{
			return (Monster) target;
		}
		return null;
	}
	
	private Skill selectKnownSkill(int[] skillIds)
	{
		for (int skillId : skillIds)
		{
			final Skill skill = _bot.getKnownSkill(skillId);
			if (skill != null)
			{
				return skill;
			}
		}
		// Fallback: search highest level of skill from SkillData
		for (int skillId : skillIds)
		{
			final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
			if (skill != null)
			{
				return skill;
			}
		}
		return null;
	}
	
	public void requestBuff(Player target)
	{
		if (target != null)
		{
			_requestedBuffTargetObjId = target.getObjectId();
			say("Buffing you now, " + target.getName() + "!");
		}
	}
	
	public Player getRequestedBuffTarget()
	{
		if (_requestedBuffTargetObjId <= 0)
		{
			return null;
		}
		return World.getPlayer(_requestedBuffTargetObjId);
	}
	
	public Player getFollowTarget()
	{
		if (_data.getFollowTargetObjId() <= 0)
		{
			return null;
		}
		final Player target = World.getPlayer(_data.getFollowTargetObjId());
		return (target != null && !target.isDead()) ? target : null;
	}
	
	public Player getFollowTargetAllowDead()
	{
		if (_data.getFollowTargetObjId() <= 0)
		{
			return null;
		}
		return World.getPlayer(_data.getFollowTargetObjId());
	}
	
	public Player getAssistTarget()
	{
		if (_data.getAssistTargetObjId() <= 0)
		{
			return null;
		}
		final Player target = World.getPlayer(_data.getAssistTargetObjId());
		return (target != null && !target.isDead()) ? target : null;
	}
	
	public SmartBotRole getRole()
	{
		final SmartBotPreset preset = SmartBotPreset.fromName(_data.getPresetName());
		return preset != null ? preset.getRole() : SmartBotRole.FIGHTER;
	}
	
	private boolean isTankPreset()
	{
		return "TANK".equalsIgnoreCase(_data.getPresetName());
	}
	
	private boolean isDaggerPreset()
	{
		return "DAGGER".equalsIgnoreCase(_data.getPresetName());
	}
	
	private boolean isArcherPreset()
	{
		return "ARCHER".equalsIgnoreCase(_data.getPresetName());
	}
	
	public void say(String message)
	{
		if (_bot != null && message != null)
		{
			_bot.broadcastPacket(new CreatureSay(_bot, ChatType.GENERAL, _bot.getName(), message));
		}
	}
}
