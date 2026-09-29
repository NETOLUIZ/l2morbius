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
package org.l2jmobius.gameserver.smartbot.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.smartbot.model.SmartBotData;

/**
 * Data Access Object for smart_bots database table.
 */
public class SmartBotDao
{
	private static final Logger LOGGER = Logger.getLogger(SmartBotDao.class.getName());
	
	private static final String SELECT_ALL = "SELECT char_obj_id, bot_name, owner_id, preset, template_class_id, spawn_x, spawn_y, spawn_z, heading, follow_target_obj_id, melee_attack_range, active FROM smart_bots";
	private static final String SELECT_BY_OWNER = "SELECT char_obj_id, bot_name, owner_id, preset, template_class_id, spawn_x, spawn_y, spawn_z, heading, follow_target_obj_id, melee_attack_range, active FROM smart_bots WHERE owner_id=?";
	private static final String SELECT_ONE = "SELECT char_obj_id, bot_name, owner_id, preset, template_class_id, spawn_x, spawn_y, spawn_z, heading, follow_target_obj_id, melee_attack_range, active FROM smart_bots WHERE char_obj_id=?";
	private static final String INSERT = "INSERT INTO smart_bots (char_obj_id, bot_name, owner_id, preset, template_class_id, spawn_x, spawn_y, spawn_z, heading, follow_target_obj_id, melee_attack_range, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String UPDATE = "UPDATE smart_bots SET spawn_x=?, spawn_y=?, spawn_z=?, heading=?, follow_target_obj_id=?, melee_attack_range=?, active=?, updated_at=? WHERE char_obj_id=?";
	private static final String DELETE_BOT = "DELETE FROM smart_bots WHERE char_obj_id=?";
	
	public List<SmartBotData> loadAll()
	{
		final List<SmartBotData> result = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_ALL);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				result.add(map(rs));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[SmartBotDao] Failed loading all bots: " + e.getMessage(), e);
		}
		return result;
	}
	
	public List<SmartBotData> loadBotsByOwner(int ownerId)
	{
		final List<SmartBotData> result = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_BY_OWNER))
		{
			ps.setInt(1, ownerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					result.add(map(rs));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[SmartBotDao] Failed loading bots by owner: " + e.getMessage(), e);
		}
		return result;
	}
	
	public SmartBotData findByCharObjId(int charObjId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_ONE))
		{
			ps.setInt(1, charObjId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return map(rs);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[SmartBotDao] Failed finding bot " + charObjId + ": " + e.getMessage(), e);
		}
		return null;
	}
	
	public void insert(SmartBotData data)
	{
		final long now = System.currentTimeMillis();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT))
		{
			ps.setInt(1, data.getCharObjId());
			ps.setString(2, data.getBotName());
			ps.setInt(3, data.getOwnerId());
			ps.setString(4, data.getPresetName());
			ps.setInt(5, data.getTemplateClassId());
			ps.setInt(6, data.getSpawnX());
			ps.setInt(7, data.getSpawnY());
			ps.setInt(8, data.getSpawnZ());
			ps.setInt(9, data.getHeading());
			ps.setInt(10, data.getFollowTargetObjId());
			ps.setInt(11, data.getMeleeAttackRange());
			ps.setInt(12, data.isActive() ? 1 : 0);
			ps.setLong(13, now);
			ps.setLong(14, now);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[SmartBotDao] Failed inserting bot " + data.getBotName() + ": " + e.getMessage(), e);
		}
	}
	
	public void update(SmartBotData data)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE))
		{
			ps.setInt(1, data.getSpawnX());
			ps.setInt(2, data.getSpawnY());
			ps.setInt(3, data.getSpawnZ());
			ps.setInt(4, data.getHeading());
			ps.setInt(5, data.getFollowTargetObjId());
			ps.setInt(6, data.getMeleeAttackRange());
			ps.setInt(7, data.isActive() ? 1 : 0);
			ps.setLong(8, System.currentTimeMillis());
			ps.setInt(9, data.getCharObjId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[SmartBotDao] Failed updating bot " + data.getBotName() + ": " + e.getMessage(), e);
		}
	}
	
	public void delete(int charObjId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_BOT))
		{
			ps.setInt(1, charObjId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "[SmartBotDao] Failed deleting bot " + charObjId + ": " + e.getMessage(), e);
		}
	}
	
	private SmartBotData map(ResultSet rs) throws SQLException
	{
		return new SmartBotData(
			rs.getInt("char_obj_id"),
			rs.getString("bot_name"),
			rs.getInt("owner_id"),
			rs.getString("preset"),
			rs.getInt("template_class_id"),
			rs.getInt("spawn_x"),
			rs.getInt("spawn_y"),
			rs.getInt("spawn_z"),
			rs.getInt("heading"),
			rs.getInt("follow_target_obj_id"),
			rs.getInt("melee_attack_range"),
			rs.getInt("active") == 1
		);
	}
}
