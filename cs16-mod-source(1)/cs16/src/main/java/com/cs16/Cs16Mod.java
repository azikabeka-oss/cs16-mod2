package com.cs16;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class Cs16Mod implements ModInitializer {
	@Override
	public void onInitialize() {
		Weapons.init();
		Shop.init();

		PayloadTypeRegistry.playS2C().register(ModePayload.ID, ModePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ChutePayload.ID, ChutePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ChutePayload.ID, (payload, ctx) -> Movement.chuteOpen(ctx.player()));

		// ЛКМ = выстрел: клиент шлёт пакет, пока кнопка зажата
		PayloadTypeRegistry.playC2S().register(ShootPayload.ID, ShootPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ShootPayload.ID, (payload, ctx) -> {
			ServerPlayerEntity p = ctx.player();
			ItemStack held = p.getMainHandStack();
			if (!(held.getItem() instanceof Gun g) || Economy.isNormal(p.getUuid())) return;
			if (p.getItemCooldownManager().isCoolingDown(held)) return;
			for (ServerWorld w : ctx.server().getWorlds()) {
				if (w.getPlayerByUuid(p.getUuid()) == p) { g.shoot(w, p, held); break; }
			}
		});
		// с оружием в руке ЛКМ не ломает блоки и не бьёт рукой
		AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) ->
			player.getStackInHand(hand).getItem() instanceof Gun ? ActionResult.FAIL : ActionResult.PASS);
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			Item held = player.getStackInHand(hand).getItem();
			if (held instanceof Gun) return ActionResult.FAIL;
			// нож: удар в спину наносит огромный урон (только в режиме CS)
			if (held == Weapons.KNIFE && world instanceof ServerWorld sw
					&& player instanceof ServerPlayerEntity sp && entity instanceof LivingEntity le
					&& !Economy.isNormal(sp.getUuid())) {
				Knife.tryBackstab(sw, sp, le);
			}
			return ActionResult.PASS;
		});

		// счётчик тиков и завершение перезарядки
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Ammo.TICK++;
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) { Ammo.tick(p); Movement.tick(p); }
		});

		// $300 за убийство (только в режиме CS)
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getAttacker() instanceof ServerPlayerEntity p && !Economy.isNormal(p.getUuid())) {
				Economy.add(p.getUuid(), Economy.KILL_REWARD);
				p.sendMessage(Text.literal("+$" + Economy.KILL_REWARD + "  (баланс: $" + Economy.get(p.getUuid()) + ")"), true);
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
			dispatcher.register(CommandManager.literal("money").executes(ctx -> {
				ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
				p.sendMessage(Text.literal("Баланс: $" + Economy.get(p.getUuid())), false);
				return 1;
			}));

			// F6: переключение CS-режима / обычного прохождения
			dispatcher.register(CommandManager.literal("csmode").executes(ctx -> {
				ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
				boolean normal = Economy.toggleNormal(p.getUuid());
				ServerPlayNetworking.send(p, new ModePayload(!normal));
				if (!normal) {
					// включился режим CS: сразу выдаём нож, если его нет
					boolean has = false;
					for (int i = 0; i < p.getInventory().size(); i++) {
						if (p.getInventory().getStack(i).getItem() == Weapons.KNIFE) { has = true; break; }
					}
					if (!has) p.giveItemStack(new ItemStack(Weapons.KNIFE));
				}
				p.sendMessage(Text.literal(normal
					? "Обычное прохождение: оружие и магазин выключены (F6 — включить CS)"
					: "Режим CS включён: нож выдан, B — магазин, F6 — выключить"), true);
				return 1;
			}));

			// R: перезарядка
			dispatcher.register(CommandManager.literal("csreload").executes(ctx -> {
				ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
				if (!Economy.isNormal(p.getUuid()) && p.getMainHandStack().getItem() instanceof Gun g) Ammo.startReload(p, g);
				return 1;
			}));

			dispatcher.register(CommandManager.literal("shop").executes(ctx -> {
				ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
				p.sendMessage(Text.literal("=== Магазин (B или /buy <id>) ==="), false);
				Shop.ENTRIES.forEach((id, e) -> p.sendMessage(Text.literal(id + " — $" + e.price()), false));
				p.sendMessage(Text.literal("Баланс: $" + Economy.get(p.getUuid())), false);
				return 1;
			}));

			dispatcher.register(CommandManager.literal("buy")
				.then(CommandManager.argument("item", StringArgumentType.word())
					.suggests((c, b) -> CommandSource.suggestMatching(Shop.ENTRIES.keySet(), b))
					.executes(ctx -> {
						ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
						if (Economy.isNormal(p.getUuid())) {
							p.sendMessage(Text.literal("Магазин выключен (обычное прохождение). F6 — включить CS."), false);
							return 0;
						}
						Shop.Entry e = Shop.ENTRIES.get(StringArgumentType.getString(ctx, "item").toLowerCase());
						if (e == null) { p.sendMessage(Text.literal("Нет такого товара. См. /shop"), false); return 0; }
						if (!Economy.spend(p.getUuid(), e.price())) {
							p.sendMessage(Text.literal("Недостаточно денег! Нужно $" + e.price()), false);
							return 0;
						}
						for (ItemStack s : e.items().get()) {
							p.giveItemStack(s);
							if (s.getItem() instanceof Gun g) Ammo.refill(p.getUuid(), g);   // полный боезапас
						}
						p.sendMessage(Text.literal("Куплено: " + e.name() + ". Баланс: $" + Economy.get(p.getUuid())), false);
						return 1;
					})));
		});
	}
}
