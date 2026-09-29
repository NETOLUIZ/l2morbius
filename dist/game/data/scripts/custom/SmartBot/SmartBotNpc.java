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
package custom.SmartBot;

import java.util.List;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.config.custom.SmartBotConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.smartbot.SmartBotManager;
import org.l2jmobius.gameserver.smartbot.model.SmartBotData;
import org.l2jmobius.gameserver.smartbot.model.SmartBotPreset;

/**
 * Script controlling NPC 50020 and voiced commands (.bot, .smartbot) for companion management.
 */
public class SmartBotNpc extends Script implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"bot",
		"smartbot",
		"mybots"
	};
	
	public SmartBotNpc()
	{
		if (SmartBotConfig.ENABLE_SMART_BOT)
		{
			addStartNpc(SmartBotConfig.SMART_BOT_NPC_ID);
			addTalkId(SmartBotConfig.SMART_BOT_NPC_ID);
			addFirstTalkId(SmartBotConfig.SMART_BOT_NPC_ID);
			
			VoicedCommandHandler.getInstance().registerHandler(this);
		}
	}
	
	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (player == null || !SmartBotConfig.ENABLE_SMART_BOT)
		{
			return false;
		}
		
		showMainHtml(player, null);
		return true;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainHtml(player, npc);
		return null;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (player == null || !SmartBotConfig.ENABLE_SMART_BOT)
		{
			return null;
		}
		
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String action = st.nextToken();
		
		switch (action)
		{
			case "main":
			{
				showMainHtml(player, npc);
				break;
			}
			case "create":
			{
				if (st.hasMoreTokens())
				{
					final String presetName = st.nextToken();
					final SmartBotPreset preset = SmartBotPreset.fromName(presetName);
					if (preset != null)
					{
						String botName = "";
						if (st.hasMoreTokens())
						{
							botName = st.nextToken();
						}
						SmartBotManager.getInstance().createPlayerBot(player, botName, preset);
					}
				}
				showMainHtml(player, npc);
				break;
			}
			case "follow":
			{
				if (st.hasMoreTokens())
				{
					final int botId = Integer.parseInt(st.nextToken());
					SmartBotManager.getInstance().setFollowTarget(botId, player.getObjectId());
					player.sendMessage("Comando: Seguir ativado!");
				}
				showMainHtml(player, npc);
				break;
			}
			case "assist":
			{
				if (st.hasMoreTokens())
				{
					final int botId = Integer.parseInt(st.nextToken());
					SmartBotManager.getInstance().setAssistTarget(botId, player.getObjectId());
					player.sendMessage("Comando: Assist ativado!");
				}
				showMainHtml(player, npc);
				break;
			}
			case "attack":
			{
				if (st.hasMoreTokens())
				{
					final int botId = Integer.parseInt(st.nextToken());
					SmartBotManager.getInstance().setAttackMode(botId, true);
					player.sendMessage("Comando: Modo de Ataque ativado!");
				}
				showMainHtml(player, npc);
				break;
			}
			case "stop":
			{
				if (st.hasMoreTokens())
				{
					final int botId = Integer.parseInt(st.nextToken());
					SmartBotManager.getInstance().stopBot(botId);
					player.sendMessage("Comando: Parar acao ativado.");
				}
				showMainHtml(player, npc);
				break;
			}
			case "buff":
			{
				if (st.hasMoreTokens())
				{
					final int botId = Integer.parseInt(st.nextToken());
					SmartBotManager.getInstance().requestBuff(botId, player);
				}
				showMainHtml(player, npc);
				break;
			}
			case "recall":
			{
				if (st.hasMoreTokens())
				{
					final int botId = Integer.parseInt(st.nextToken());
					SmartBotManager.getInstance().recallBot(player, botId);
					player.sendMessage("Bot teleportado para junto de voce!");
				}
				showMainHtml(player, npc);
				break;
			}
			case "despawn":
			{
				if (st.hasMoreTokens())
				{
					final int botId = Integer.parseInt(st.nextToken());
					SmartBotManager.getInstance().despawnBot(botId);
					player.sendMessage("Bot recolhido!");
				}
				showMainHtml(player, npc);
				break;
			}
			case "delete":
			{
				if (st.hasMoreTokens())
				{
					final int botId = Integer.parseInt(st.nextToken());
					SmartBotManager.getInstance().deleteBot(botId);
					player.sendMessage("Bot dispensado definitivamente.");
				}
				showMainHtml(player, npc);
				break;
			}
			case "all_follow":
			{
				for (Player bot : SmartBotManager.getInstance().getBotsByOwner(player))
				{
					SmartBotManager.getInstance().setFollowTarget(bot.getObjectId(), player.getObjectId());
				}
				player.sendMessage("Todos os seus bots agora estao te seguindo!");
				showMainHtml(player, npc);
				break;
			}
			case "all_assist":
			{
				for (Player bot : SmartBotManager.getInstance().getBotsByOwner(player))
				{
					SmartBotManager.getInstance().setAssistTarget(bot.getObjectId(), player.getObjectId());
				}
				player.sendMessage("Todos os seus bots agora estao em assist!");
				showMainHtml(player, npc);
				break;
			}
			case "all_recall":
			{
				for (Player bot : SmartBotManager.getInstance().getBotsByOwner(player))
				{
					SmartBotManager.getInstance().recallBot(player, bot.getObjectId());
				}
				player.sendMessage("Todos os seus bots foram teleportados para voce!");
				showMainHtml(player, npc);
				break;
			}
		}
		
		return null;
	}
	
	private void showMainHtml(Player player, Npc npc)
	{
		final List<Player> ownedBots = SmartBotManager.getInstance().getBotsByOwner(player);
		final int botCount = ownedBots.size();
		final int maxBots = SmartBotConfig.MAX_BOTS_PER_PLAYER;
		final int npcObjId = (npc != null) ? npc.getObjectId() : 0;
		final String bypassPrefix = (npc != null) ? "bypass -h npc_" + npcObjId + "_quest_SmartBotNpc " : "bypass voiced_bot ";
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><title>SmartBot Manager</title><body><center>");
		sb.append("<table width=270><tr><td align=center><font color=\"LEVEL\">SmartBot Companion Master</font></td></tr></table>");
		sb.append("<br>");
		sb.append("<font color=\"AAAAAA\">Bots Ativos: </font><font color=\"00FF00\">").append(botCount).append("</font> / ").append(maxBots).append("<br>");
		
		if (SmartBotConfig.BOT_DONATION_COIN_COUNT > 0)
		{
			sb.append("<font color=\"FF9900\">Custo por Bot: ").append(SmartBotConfig.BOT_DONATION_COIN_COUNT).append(" Moeda(s)</font><br>");
		}
		else
		{
			sb.append("<font color=\"66FF66\">[Modo de Teste Local - Gratuito]</font><br>");
		}
		sb.append("<br>");
		
		// If player can create more bots
		if (botCount < maxBots)
		{
			sb.append("<table width=260 bgcolor=222222>");
			sb.append("<tr><td align=center colspan=2><font color=\"LEVEL\">-- Criar Novo Bot (Lv 20 Set D) --</font></td></tr>");
			sb.append("<tr>");
			sb.append("<td align=center><button value=\"Archer\" action=\"").append(bypassPrefix).append("create ARCHER\" width=110 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("<td align=center><button value=\"Mage\" action=\"").append(bypassPrefix).append("create MAGE\" width=110 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("</tr><tr>");
			sb.append("<td align=center><button value=\"Healer\" action=\"").append(bypassPrefix).append("create HEALER\" width=110 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("<td align=center><button value=\"Buffer\" action=\"").append(bypassPrefix).append("create BUFFER\" width=110 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("</tr><tr>");
			sb.append("<td align=center><button value=\"Dagger\" action=\"").append(bypassPrefix).append("create DAGGER\" width=110 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("<td align=center><button value=\"Tank\" action=\"").append(bypassPrefix).append("create TANK\" width=110 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("</tr></table><br>");
		}
		
		// List of existing bots
		if (!ownedBots.isEmpty())
		{
			sb.append("<table width=260 bgcolor=111111>");
			sb.append("<tr><td align=center colspan=3><font color=\"LEVEL\">-- Seus Bots Atuais --</font></td></tr>");
			
			for (Player bot : ownedBots)
			{
				final SmartBotData data = SmartBotManager.getInstance().getBotData(bot.getObjectId());
				final String preset = (data != null) ? data.getPresetName() : "BOT";
				final int bId = bot.getObjectId();
				
				sb.append("<tr><td colspan=3><font color=\"FFFFFF\"><b>").append(bot.getName()).append("</b> (").append(preset).append(" - Lv ").append(bot.getLevel()).append(")</font></td></tr>");
				sb.append("<tr>");
				sb.append("<td><button value=\"Seguir\" action=\"").append(bypassPrefix).append("follow ").append(bId).append("\" width=75 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
				sb.append("<td><button value=\"Assist\" action=\"").append(bypassPrefix).append("assist ").append(bId).append("\" width=75 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
				sb.append("<td><button value=\"Buff\" action=\"").append(bypassPrefix).append("buff ").append(bId).append("\" width=75 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
				sb.append("</tr><tr>");
				sb.append("<td><button value=\"Puxar\" action=\"").append(bypassPrefix).append("recall ").append(bId).append("\" width=75 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
				sb.append("<td><button value=\"Guardar\" action=\"").append(bypassPrefix).append("despawn ").append(bId).append("\" width=75 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
				sb.append("<td><button value=\"Deletar\" action=\"").append(bypassPrefix).append("delete ").append(bId).append("\" width=75 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
				sb.append("</tr><tr><td colspan=3><font color=\"444444\">----------------------------------</font></td></tr>");
			}
			sb.append("</table><br>");
			
			// Group controls
			sb.append("<table width=260><tr>");
			sb.append("<td align=center><button value=\"Todos Seguir\" action=\"").append(bypassPrefix).append("all_follow\" width=85 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
			sb.append("<td align=center><button value=\"Todos Assist\" action=\"").append(bypassPrefix).append("all_assist\" width=85 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
			sb.append("<td align=center><button value=\"Todos Puxar\" action=\"").append(bypassPrefix).append("all_recall\" width=85 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
			sb.append("</tr></table>");
		}
		
		sb.append("</center></body></html>");
		
		final NpcHtmlMessage msg = new NpcHtmlMessage(npcObjId);
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
	
	public static void main(String[] args)
	{
		new SmartBotNpc();
	}
}
