package com.cs16;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Сервер -> клиент: включён ли режим CS (нужен клиенту для бхопа и парашюта). */
public record ModePayload(boolean on) implements CustomPayload {
	public static final CustomPayload.Id<ModePayload> ID = new CustomPayload.Id<>(Identifier.of("cs16", "mode"));
	public static final PacketCodec<RegistryByteBuf, ModePayload> CODEC =
		PacketCodec.tuple(PacketCodecs.BOOLEAN, ModePayload::on, ModePayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
