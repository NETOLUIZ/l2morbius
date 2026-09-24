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
package custom.events.ArenaBossEvent;

import java.util.logging.Logger;

import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Event;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Daily Arena Boss Event
 * Spawns Baium (Arena 1), Valakas (Arena 2), and Zaken (Arena 3)
 * Every day at 21:00 (server time)
 */
public class ArenaBossEvent extends Event
{
	private static final Logger LOGGER = Logger.getLogger(ArenaBossEvent.class.getName());
	
	// Boss NPC IDs
	private static final int BAIUM_ID = 60002;
	private static final int VALAKAS_ID = 60003;
	private static final int ZAKEN_ID = 60004;
	
	// Coliseum Arenas Coordinates
	private static final int ARENA1_X = 149500;
	private static final int ARENA1_Y = 46045;
	private static final int ARENA1_Z = -3410;
	
	private static final int ARENA2_X = 149500;
	private static final int ARENA2_Y = 47420;
	private static final int ARENA2_Z = -3410;
	
	private static final int ARENA3_X = 149500;
	private static final int ARENA3_Y = 46665;
	private static final int ARENA3_Z = -3410;
	
	private static final String CRON_SCHEDULE = "0 21 * * *"; // Daily at 21:00
	
	// Active Boss references
	private Npc _baium = null;
	private Npc _valakas = null;
	private Npc _zaken = null;
	
	private ArenaBossEvent()
	{
		addKillId(BAIUM_ID, VALAKAS_ID, ZAKEN_ID);
		
		// Initial spawn so bosses are present immediately
		spawnBosses();
		
		// Schedule daily 21:00 respawn
		scheduleDailySpawn();
	}
	
	private void scheduleDailySpawn()
	{
		final SchedulingPattern schedulingPattern = new SchedulingPattern(CRON_SCHEDULE);
		final long delay = schedulingPattern.getDelayToNextFromNow();
		final StatSet params = new StatSet();
		params.set("SchedulingPattern", CRON_SCHEDULE);
		
		getTimers().addTimer("ScheduleDaily", params, delay + 5000, null, null);
		LOGGER.info("[ArenaBossEvent] Next daily respawn scheduled for " + TimeUtil.getDateTimeString(System.currentTimeMillis() + delay));
	}
	
	private synchronized void spawnBosses()
	{
		// Despawn previous instances if any still exist
		if ((_baium != null) && !_baium.isDead())
		{
			_baium.deleteMe();
		}
		if ((_valakas != null) && !_valakas.isDead())
		{
			_valakas.deleteMe();
		}
		if ((_zaken != null) && !_zaken.isDead())
		{
			_zaken.deleteMe();
		}
		
		_baium = addSpawn(BAIUM_ID, ARENA1_X, ARENA1_Y, ARENA1_Z, 0, false, 0);
		_valakas = addSpawn(VALAKAS_ID, ARENA2_X, ARENA2_Y, ARENA2_Z, 0, false, 0);
		_zaken = addSpawn(ZAKEN_ID, ARENA3_X, ARENA3_Y, ARENA3_Z, 0, false, 0);
		
		World.broadcastToAllOnlinePlayers("==================================================");
		World.broadcastToAllOnlinePlayers("[Boss Event] Os Grandes Chefes estao no Coliseu!");
		World.broadcastToAllOnlinePlayers("-> Baium na Arena 1");
		World.broadcastToAllOnlinePlayers("-> Valakas na Arena 2");
		World.broadcastToAllOnlinePlayers("-> Zaken na Arena 3");
		World.broadcastToAllOnlinePlayers("Teleporte disponivel no Gatekeeper (Arena Zone)!");
		World.broadcastToAllOnlinePlayers("==================================================");
		
		LOGGER.info("[ArenaBossEvent] Bosses Baium, Valakas, and Zaken spawned in Coliseum Arenas 1, 2, 3.");
	}
	
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if ("ScheduleDaily".equals(event))
		{
			spawnBosses();
			scheduleDailySpawn();
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		final String killerName = (killer != null) ? killer.getName() : "Desconhecido";
		final int npcId = npc.getId();
		
		if (npcId == BAIUM_ID)
		{
			_baium = null;
			World.broadcastToAllOnlinePlayers("[Boss Event] Baium (Arena 1) foi derrotado por " + killerName + "! Respawn amanha as 21:00!");
		}
		else if (npcId == VALAKAS_ID)
		{
			_valakas = null;
			World.broadcastToAllOnlinePlayers("[Boss Event] Valakas (Arena 2) foi derrotado por " + killerName + "! Respawn amanha as 21:00!");
		}
		else if (npcId == ZAKEN_ID)
		{
			_zaken = null;
			World.broadcastToAllOnlinePlayers("[Boss Event] Zaken (Arena 3) foi derrotado por " + killerName + "! Respawn amanha as 21:00!");
		}
	}
	
	@Override
	public boolean eventStart(Player eventMaker)
	{
		spawnBosses();
		if (eventMaker != null)
		{
			eventMaker.sendMessage("Arena Bosses respawned manually!");
		}
		return true;
	}
	
	@Override
	public boolean eventStop()
	{
		if ((_baium != null) && !_baium.isDead())
		{
			_baium.deleteMe();
			_baium = null;
		}
		if ((_valakas != null) && !_valakas.isDead())
		{
			_valakas.deleteMe();
			_valakas = null;
		}
		if ((_zaken != null) && !_zaken.isDead())
		{
			_zaken.deleteMe();
			_zaken = null;
		}
		World.broadcastToAllOnlinePlayers("[Boss Event] Arena Bosses foram encerrados.");
		return true;
	}
	
	@Override
	public boolean eventBypass(Player player, String bypass)
	{
		return false;
	}
	
	public static void main(String[] args)
	{
		new ArenaBossEvent();
	}
}
