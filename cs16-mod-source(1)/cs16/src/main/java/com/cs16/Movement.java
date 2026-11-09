package com.cs16;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Серверная часть парашюта: пока он раскрыт — урон от падения не копится. */
public class Movement {
	private static final Map<UUID, Long> CHUTE = new HashMap<>();

	public static void chuteOpen(ServerPlayerEntity p) {
		if (!Economy.isNormal(p.getUuid())) CHUTE.put(p.getUuid(), Ammo.TICK);
	}

	public static void tick(ServerPlayerEntity p) {
		Long last = CHUTE.get(p.getUuid());
		if (last == null) return;
		if (Ammo.TICK - last <= 3) p.onLanding();   // сбрасывает накопленную высоту падения
		else CHUTE.remove(p.getUuid());
	}
}
