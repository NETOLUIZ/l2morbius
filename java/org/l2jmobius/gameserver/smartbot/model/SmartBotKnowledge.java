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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Loads text knowledge facts to inject into AI bot prompts.
 */
public class SmartBotKnowledge
{
	private static final Logger LOGGER = Logger.getLogger(SmartBotKnowledge.class.getName());
	public static final SmartBotKnowledge INSTANCE = new SmartBotKnowledge();
	
	private static final File[] KNOWLEDGE_DIRS =
	{
		new File("config/Custom/smartbot/knowledge"),
		new File("config/smartbot/knowledge")
	};
	
	private final List<KnowledgeFact> _facts = new ArrayList<>();
	
	private SmartBotKnowledge()
	{
	}
	
	public static SmartBotKnowledge getInstance()
	{
		return INSTANCE;
	}
	
	public void load()
	{
		_facts.clear();
		
		File targetDir = null;
		for (File dir : KNOWLEDGE_DIRS)
		{
			if (dir.exists() && dir.isDirectory())
			{
				targetDir = dir;
				break;
			}
		}
		
		if (targetDir == null)
		{
			LOGGER.info("[SmartBotKnowledge] No knowledge folder found.");
			return;
		}
		
		final File[] files = targetDir.listFiles();
		if (files == null)
		{
			return;
		}
		
		for (File file : files)
		{
			if ((file == null) || !file.isFile())
			{
				continue;
			}
			
			if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".txt"))
			{
				continue;
			}
			
			loadFile(file);
		}
		
		LOGGER.info("[SmartBotKnowledge] Loaded " + _facts.size() + " knowledge facts.");
	}
	
	private void loadFile(File file)
	{
		try (BufferedReader reader = new BufferedReader(new FileReader(file)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				
				final KnowledgeFact fact = parseFact(line, file.getName());
				if (fact != null)
				{
					_facts.add(fact);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("[SmartBotKnowledge] Failed loading " + file.getName() + ": " + e.getMessage());
		}
	}
	
	private KnowledgeFact parseFact(String line, String source)
	{
		String tags = "";
		String text = line;
		
		if (line.startsWith("["))
		{
			final int end = line.indexOf(']');
			if (end > 0)
			{
				tags = line.substring(1, end).trim();
				text = line.substring(end + 1).trim();
			}
		}
		
		if (text.isEmpty())
		{
			return null;
		}
		
		return new KnowledgeFact(tags, text, source);
	}
	
	public String findRelevant(String message, int limit)
	{
		if ((message == null) || message.trim().isEmpty() || _facts.isEmpty())
		{
			return "";
		}
		
		final String query = normalize(message);
		final List<ScoredFact> scored = new ArrayList<>();
		
		for (KnowledgeFact fact : _facts)
		{
			final int score = score(query, fact);
			if (score > 0)
			{
				scored.add(new ScoredFact(fact, score));
			}
		}
		
		scored.sort(Comparator.comparingInt(ScoredFact::getScore).reversed());
		
		final StringBuilder sb = new StringBuilder();
		int count = 0;
		for (ScoredFact scoredFact : scored)
		{
			if (count >= limit)
			{
				break;
			}
			
			if (sb.length() > 0)
			{
				sb.append('\n');
			}
			
			sb.append("- ").append(scoredFact.getFact().getText());
			count++;
		}
		
		return sb.toString();
	}
	
	private int score(String query, KnowledgeFact fact)
	{
		int score = 0;
		final String normalizedTags = normalize(fact.getTags());
		final String normalizedText = normalize(fact.getText());
		final String[] words = query.split("\\s+");
		
		for (String word : words)
		{
			if (word.length() < 3)
			{
				continue;
			}
			
			if (normalizedTags.contains(word))
			{
				score += 5;
			}
			
			if (normalizedText.contains(word))
			{
				score += 2;
			}
		}
		
		if (containsAny(query, "where", "location", "farm", "level", "xp", "hunt", "zone", "onde", "upar"))
		{
			if (containsAny(normalizedTags, "location", "level", "farm"))
			{
				score += 6;
			}
		}
		
		if (containsAny(query, "buff", "buffs", "song", "songs", "dance", "dances"))
		{
			if (containsAny(normalizedTags, "buff", "song", "dance"))
			{
				score += 6;
			}
		}
		
		if (containsAny(query, "assist", "attack", "fight", "kill", "mob", "ataca", "ajuda"))
		{
			if (containsAny(normalizedTags, "assist", "attack", "combat", "party", "role"))
			{
				score += 5;
			}
		}
		
		if (containsAny(query, "heal", "healer", "hp", "res", "ress", "resurrect", "revive", "cura", "cura-me"))
		{
			if (containsAny(normalizedTags, "healer", "heal", "resurrect", "revive"))
			{
				score += 7;
			}
		}
		
		return score;
	}
	
	private boolean containsAny(String text, String... values)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		for (String value : values)
		{
			if (text.contains(value))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private String normalize(String text)
	{
		if (text == null)
		{
			return "";
		}
		
		return text.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9\\s]", " ")
			.replaceAll("\\s+", " ")
			.trim();
	}
	
	private static class KnowledgeFact
	{
		private final String _tags;
		private final String _text;
		private final String _source;
		
		private KnowledgeFact(String tags, String text, String source)
		{
			_tags = tags;
			_text = text;
			_source = source;
		}
		
		public String getTags()
		{
			return _tags;
		}
		
		public String getText()
		{
			return _text;
		}
		
		@SuppressWarnings("unused")
		public String getSource()
		{
			return _source;
		}
	}
	
	private static class ScoredFact
	{
		private final KnowledgeFact _fact;
		private final int _score;
		
		private ScoredFact(KnowledgeFact fact, int score)
		{
			_fact = fact;
			_score = score;
		}
		
		public KnowledgeFact getFact()
		{
			return _fact;
		}
		
		public int getScore()
		{
			return _score;
		}
	}
}
