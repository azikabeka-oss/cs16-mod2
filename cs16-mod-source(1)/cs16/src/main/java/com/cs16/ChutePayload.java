package com.cs16;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Клиент -> сервер: парашют раскрыт (шлётся каждый тик, пока он открыт). */
public record ChutePayload() implements CustomPayload {
	public static final CustomPayload.Id<ChutePayload> ID = new CustomPayload.Id<>(Identifier.of("cs16", "chute"));
	public static final PacketCodec<RegistryByteBuf, ChutePayload> CODEC = PacketCodec.unit(new ChutePayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
