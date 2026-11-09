package com.cs16;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class Cs16Client implements ClientModInitializer {
	private static boolean zoomed = false, prevUse = false;
	private static int oldFov = 70;
	private static boolean csOn = false, hopApplied = false;
	private static double hopSpeed = 0;
	private static int groundTicks = 0;

	@Override
	public void onInitializeClient() {
		KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("cs16", "main"));
		KeyBinding shopKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.cs16.shop", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, cat));
		KeyBinding modeKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.cs16.mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, cat));
		KeyBinding reloadKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.cs16.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, cat));

		ClientPlayNetworking.registerGlobalReceiver(ModePayload.ID, (payload, ctx) -> csOn = payload.on());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> csOn = false);

		// в режиме CS инвентарь не открывается (E — кнопка парашюта); в обычном режиме работает как всегда
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (csOn && client.player != null && client.currentScreen == null) {
				while (client.options.inventoryKey.wasPressed()) { }
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				if (zoomed) setZoom(client, false);
				return;
			}
			var held = client.player.getMainHandStack().getItem();
			boolean isGun = held instanceof Gun;
			boolean sniper = isGun && ((Gun) held).isSniper();
			boolean noScreen = client.currentScreen == null;

			var pl = client.player;
			boolean jumpHeld = client.options.jumpKey.isPressed();
			if (csOn && noScreen && !pl.getAbilities().flying && !pl.hasVehicle() && !pl.isTouchingWater()) {
				// авто-бхоп: держишь пробел — прыгаешь сразу при приземлении, скорость копится от прыжка к прыжку
				if (pl.isOnGround()) {
					if (jumpHeld && !hopApplied) {
						Vec3d v = pl.getVelocity();
						double sp = Math.hypot(v.x, v.z);
						if (sp > 0.02) {
							double target = Math.min(0.6, Math.max(hopSpeed * 1.05, 0.32));
							pl.setVelocity(v.x / sp * target, v.y, v.z / sp * target);
							hopSpeed = target;
						}
						hopApplied = true;
						groundTicks = 0;
					} else if (!jumpHeld && ++groundTicks > 3) {
						hopSpeed = 0;
					}
				} else {
					hopApplied = false;
					// парашют: E (клавиша инвентаря) в падении — медленное планирование без урона
					Vec3d v = pl.getVelocity();
					boolean chute = client.options.inventoryKey.isPressed() && v.y < -0.1 && !pl.isClimbing()
						&& !pl.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
					if (chute) {
						if (v.y < -0.12) pl.setVelocity(v.x, -0.12, v.z);
						ClientPlayNetworking.send(new ChutePayload());
					}
				}
			}

			// ЛКМ — стрельба (пока зажата)
			if (isGun && noScreen && client.options.attackKey.isPressed()) {
				ClientPlayNetworking.send(new ShootPayload());
			}

			// ПКМ — зум, только у снайперок
			boolean use = client.options.useKey.isPressed();
			if (use && !prevUse && sniper && noScreen) setZoom(client, !zoomed);
			prevUse = use;
			if (zoomed && (!sniper || !noScreen)) setZoom(client, false);

			while (shopKey.wasPressed()) {
				if (noScreen) client.setScreen(new ShopScreen());
			}
			while (reloadKey.wasPressed()) {
				client.player.networkHandler.sendChatCommand("csreload");
			}
			while (modeKey.wasPressed()) {
				client.player.networkHandler.sendChatCommand("csmode");
			}
		});
	}

	private static void setZoom(MinecraftClient client, boolean on) {
		if (on == zoomed) return;
		if (on) {
			oldFov = client.options.getFov().getValue();
			client.options.getFov().setValue(30);
		} else {
			client.options.getFov().setValue(oldFov);
		}
		zoomed = on;
	}
}
