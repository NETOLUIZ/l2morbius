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

/**
 * Data container for persistent SmartBot records.
 */
public class SmartBotData
{
	private final int _charObjId;
	private final String _botName;
	private final int _ownerId;
	private final String _presetName;
	private final int _templateClassId;
	
	private int _spawnX;
	private int _spawnY;
	private int _spawnZ;
	private int _heading;
	private int _followTargetObjId;
	private int _meleeAttackRange;
	private boolean _active;
	private boolean _attackMode;
	private int _assistTargetObjId;
	
	public SmartBotData(int charObjId, String botName, int ownerId, String presetName, int templateClassId, int spawnX, int spawnY, int spawnZ, int heading, int followTargetObjId, int meleeAttackRange, boolean active)
	{
		_charObjId = charObjId;
		_botName = botName;
		_ownerId = ownerId;
		_presetName = presetName;
		_templateClassId = templateClassId;
		_spawnX = spawnX;
		_spawnY = spawnY;
		_spawnZ = spawnZ;
		_heading = heading;
		_followTargetObjId = followTargetObjId;
		_meleeAttackRange = meleeAttackRange;
		_active = active;
	}
	
	public int getCharObjId()
	{
		return _charObjId;
	}
	
	public String getBotName()
	{
		return _botName;
	}
	
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	public String getPresetName()
	{
		return _presetName;
	}
	
	public int getTemplateClassId()
	{
		return _templateClassId;
	}
	
	public int getSpawnX()
	{
		return _spawnX;
	}
	
	public void setSpawnX(int spawnX)
	{
		_spawnX = spawnX;
	}
	
	public int getSpawnY()
	{
		return _spawnY;
	}
	
	public void setSpawnY(int spawnY)
	{
		_spawnY = spawnY;
	}
	
	public int getSpawnZ()
	{
		return _spawnZ;
	}
	
	public void setSpawnZ(int spawnZ)
	{
		_spawnZ = spawnZ;
	}
	
	public int getHeading()
	{
		return _heading;
	}
	
	public void setHeading(int heading)
	{
		_heading = heading;
	}
	
	public int getFollowTargetObjId()
	{
		return _followTargetObjId;
	}
	
	public void setFollowTargetObjId(int followTargetObjId)
	{
		_followTargetObjId = followTargetObjId;
	}
	
	public int getMeleeAttackRange()
	{
		return _meleeAttackRange;
	}
	
	public void setMeleeAttackRange(int meleeAttackRange)
	{
		_meleeAttackRange = meleeAttackRange;
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	public void setActive(boolean active)
	{
		_active = active;
	}
	
	public boolean isAttackMode()
	{
		return _attackMode;
	}
	
	public void setAttackMode(boolean attackMode)
	{
		_attackMode = attackMode;
	}
	
	public int getAssistTargetObjId()
	{
		return _assistTargetObjId;
	}
	
	public void setAssistTargetObjId(int assistTargetObjId)
	{
		_assistTargetObjId = assistTargetObjId;
	}
}
