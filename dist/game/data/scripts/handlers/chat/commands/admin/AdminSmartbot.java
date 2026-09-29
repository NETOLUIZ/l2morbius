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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.smartbot.SmartBotManager;
import org.l2jmobius.gameserver.smartbot.model.SmartBotPreset;

/**
 * Admin command handler for SmartBot management.
 */
public class AdminSmartbot implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_bot_create",
		"admin_bot_follow",
		"admin_bot_unfollow",
		"admin_bot_despawn",
		"admin_bot_delete",
		"admin_bot_attack",
		"admin_bot_stop",
		"admin_bot_buff"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (activeChar == null || !activeChar.isGM())
		{
			return false;
		}
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		switch (actualCommand)
		{
			case "admin_bot_create":
			{
				if (st.countTokens() < 2)
				{
					activeChar.sendMessage("Uso: //bot_create <nome> <ARCHER|MAGE|HEALER|BUFFER|DAGGER|TANK>");
					return true;
				}
				
				final String botName = st.nextToken();
				final String presetName = st.nextToken().toUpperCase();
				final SmartBotPreset preset = SmartBotPreset.fromName(presetName);
				if (preset == null)
				{
					activeChar.sendMessage("Preset invalido! Disponiveis: ARCHER, MAGE, HEALER, BUFFER, DAGGER, TANK");
					return true;
				}
				
				final Player bot = SmartBotManager.getInstance().createGmBot(
					botName,
					preset,
					activeChar.getX() + 30,
					activeChar.getY() + 30,
					activeChar.getZ()
				);
				
				if (bot != null)
				{
					activeChar.sendMessage("SmartBot GM " + botName + " (" + preset.name() + ") criado com sucesso!");
				}
				else
				{
					activeChar.sendMessage("Falha ao criar o SmartBot.");
				}
				return true;
			}
			case "admin_bot_follow":
			{
				final Player targetBot = getTargetBot(activeChar);
				if (targetBot == null)
				{
					activeChar.sendMessage("Selecione um SmartBot primeiro.");
					return true;
				}
				
				SmartBotManager.getInstance().setFollowTarget(targetBot.getObjectId(), activeChar.getObjectId());
				activeChar.sendMessage(targetBot.getName() + " agora esta seguindo voce.");
				return true;
			}
			case "admin_bot_unfollow":
			{
				final Player targetBot = getTargetBot(activeChar);
				if (targetBot == null)
				{
					activeChar.sendMessage("Selecione um SmartBot primeiro.");
					return true;
				}
				
				SmartBotManager.getInstance().clearFollowTarget(targetBot.getObjectId());
				activeChar.sendMessage(targetBot.getName() + " parou de seguir.");
				return true;
			}
			case "admin_bot_despawn":
			{
				final Player targetBot = getTargetBot(activeChar);
				if (targetBot == null)
				{
					activeChar.sendMessage("Selecione um SmartBot primeiro.");
					return true;
				}
				
				SmartBotManager.getInstance().despawnBot(targetBot.getObjectId());
				activeChar.sendMessage(targetBot.getName() + " foi despawnado.");
				return true;
			}
			case "admin_bot_delete":
			{
				final Player targetBot = getTargetBot(activeChar);
				if (targetBot == null)
				{
					activeChar.sendMessage("Selecione um SmartBot primeiro.");
					return true;
				}
				
				SmartBotManager.getInstance().deleteBot(targetBot.getObjectId());
				activeChar.sendMessage(targetBot.getName() + " foi deletado do banco e do jogo.");
				return true;
			}
			case "admin_bot_attack":
			{
				final Player targetBot = getTargetBot(activeChar);
				if (targetBot == null)
				{
					activeChar.sendMessage("Selecione um SmartBot primeiro.");
					return true;
				}
				
				SmartBotManager.getInstance().setAttackMode(targetBot.getObjectId(), true);
				activeChar.sendMessage(targetBot.getName() + " esta em modo de ataque!");
				return true;
			}
			case "admin_bot_stop":
			{
				final Player targetBot = getTargetBot(activeChar);
				if (targetBot == null)
				{
					activeChar.sendMessage("Selecione um SmartBot primeiro.");
					return true;
				}
				
				SmartBotManager.getInstance().stopBot(targetBot.getObjectId());
				activeChar.sendMessage(targetBot.getName() + " parou todas as acoes.");
				return true;
			}
			case "admin_bot_buff":
			{
				final Player targetBot = getTargetBot(activeChar);
				if (targetBot == null)
				{
					activeChar.sendMessage("Selecione um SmartBot primeiro.");
					return true;
				}
				
				SmartBotManager.getInstance().requestBuff(targetBot.getObjectId(), activeChar);
				return true;
			}
		}
		
		return false;
	}
	
	private Player getTargetBot(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		if (target instanceof Player && SmartBotManager.getInstance().isBot((Player) target))
		{
			return (Player) target;
		}
		return null;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
