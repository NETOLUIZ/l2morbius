package custom;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Applies native 20% boost to Attack Speed, Cast Speed, and Critical Rate (Physical and Magical).
 */
public class ServerStatBoost
{
	private static final int SKILL_ID = 10004;
	
	public ServerStatBoost()
	{
		Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
	}
	
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(SKILL_ID, 1);
		if ((skill != null) && (player.getSkillLevel(SKILL_ID) <= 0))
		{
			player.addSkill(skill, true);
		}
	}
	
	public static void main(String[] args)
	{
		new ServerStatBoost();
	}
}
