package com.cs16;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Economy {
	public static final int START = 800, MAX = 16000, KILL_REWARD = 300;
	private static final Map<UUID, Integer> MONEY = new HashMap<>();
	// игроки, у которых включён режим CS (F6). По умолчанию — обычное прохождение
	private static final Set<UUID> CS_ON = new HashSet<>();

	public static int get(UUID id) { return MONEY.getOrDefault(id, START); }
	public static void add(UUID id, int amount) { MONEY.put(id, Math.min(MAX, get(id) + amount)); }
	public static boolean spend(UUID id, int amount) {
		int m = get(id);
		if (m < amount) return false;
		MONEY.put(id, m - amount);
		return true;
	}

	public static boolean isNormal(UUID id) { return !CS_ON.contains(id); }
	/** @return true, если теперь обычный режим; false, если включился CS */
	public static boolean toggleNormal(UUID id) {
		if (CS_ON.remove(id)) return true;
		CS_ON.add(id);
		return false;
	}
}
