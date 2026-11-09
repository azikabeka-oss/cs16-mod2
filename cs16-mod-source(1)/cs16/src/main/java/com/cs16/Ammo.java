package com.cs16;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Ammo {
	public static class State {
		public int mag, reserve;
		public long reloadEnd = -1;
		State(Gun g) { mag = g.magSize; reserve = g.reserve; }
	}

	public static long TICK = 0;
	private static final Map<UUID, Map<Gun, State>> STATES = new HashMap<>();

	public static State get(UUID id, Gun g) {
		return STATES.computeIfAbsent(id, k -> new HashMap<>()).computeIfAbsent(g, State::new);
	}

	public static void refill(UUID id, Gun g) {
		STATES.computeIfAbsent(id, k -> new HashMap<>()).put(g, new State(g));
	}

	public static void show(ServerPlayerEntity p, Gun g, State st) {
		p.sendMessage(Text.literal(g.id.toUpperCase() + "  " + st.mag + " / " + st.reserve), true);
	}

	public static void startReload(ServerPlayerEntity p, Gun g) {
		State st = get(p.getUuid(), g);
		if (st.reloadEnd >= 0 || st.mag >= g.magSize) return;
		if (st.reserve <= 0) {
			p.sendMessage(Text.literal("Нет патронов!"), true);
			return;
		}
		st.reloadEnd = TICK + g.reloadTicks;
		p.sendMessage(Text.literal("Перезарядка..."), true);
	}

	/** Вызывается каждый серверный тик для каждого игрока. */
	public static void tick(ServerPlayerEntity p) {
		Map<Gun, State> map = STATES.get(p.getUuid());
		if (map == null) return;
		for (Map.Entry<Gun, State> en : map.entrySet()) {
			State st = en.getValue();
			if (st.reloadEnd < 0) continue;
			Gun g = en.getKey();
			if (p.getMainHandStack().getItem() != g) {
				st.reloadEnd = -1;
				p.sendMessage(Text.literal("Перезарядка отменена"), true);
			} else if (TICK >= st.reloadEnd) {
				int take = Math.min(g.magSize - st.mag, st.reserve);
				st.mag += take;
				st.reserve -= take;
				st.reloadEnd = -1;
				show(p, g, st);
			}
		}
	}
}
