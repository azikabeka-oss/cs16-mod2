package com.cs16;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Клиент -> сервер: игрок держит ЛКМ с оружием в руке. */
public record ShootPayload() implements CustomPayload {
	public static final CustomPayload.Id<ShootPayload> ID = new CustomPayload.Id<>(Identifier.of("cs16", "shoot"));
	public static final PacketCodec<RegistryByteBuf, ShootPayload> CODEC = PacketCodec.unit(new ShootPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
