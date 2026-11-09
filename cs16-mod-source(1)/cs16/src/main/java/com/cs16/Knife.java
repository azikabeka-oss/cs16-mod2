package com.cs16;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/** Нож: ПКМ по блоку — «пробивает» его за несколько ударов (даже обсидиан). */
public class Knife extends Item {
	private static final float MAX_HARDNESS = 50.0F;   // обсидиан = 50, бедрок не ломается
	private static final int RESET_TICKS = 100;        // прогресс сбрасывается, если не бить 5 секунд

	private record Progress(int hits, long lastTick) {}
	private static final Map<String, Progress> HITS = new HashMap<>();

	public static final float BACKSTAB_DAMAGE = 40.0F;   // 20 сердец: убивает почти любого моба

	public Knife(Settings settings) { super(settings); }

	/** Удар в спину: вызывается ДО обычного удара, поэтому итоговый урон = BACKSTAB_DAMAGE. */
	public static void tryBackstab(ServerWorld sw, ServerPlayerEntity attacker, LivingEntity target) {
		Vec3d flat = new Vec3d(target.getX() - attacker.getX(), 0, target.getZ() - attacker.getZ());
		Vec3d look = target.getRotationVec(1.0F);
		Vec3d facing = new Vec3d(look.x, 0, look.z);
		if (flat.lengthSquared() < 1.0E-6 || facing.lengthSquared() < 1.0E-6) return;
		// цель смотрит в ту же сторону, куда от атакующего идёт вектор к ней => атакующий сзади
		if (flat.normalize().dotProduct(facing.normalize()) < 0.4) return;

		target.damage(sw, sw.getDamageSources().playerAttack(attacker), BACKSTAB_DAMAGE);
		sw.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 0.7F);
		sw.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.6), target.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
		attacker.sendMessage(Text.literal("Удар в спину!"), true);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		World world = ctx.getWorld();
		PlayerEntity player = ctx.getPlayer();
		if (!(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp)) return ActionResult.SUCCESS;
		if (Economy.isNormal(sp.getUuid())) return ActionResult.PASS;   // F6: обычное прохождение

		BlockPos pos = ctx.getBlockPos();
		BlockState state = sw.getBlockState(pos);
		float hardness = state.getHardness(sw, pos);
		if (hardness < 0 || hardness > MAX_HARDNESS) {
			sp.sendMessage(Text.literal("Этот блок нож не берёт"), true);
			return ActionResult.FAIL;
		}

		int needed = Math.max(1, (int) Math.ceil(hardness / 10.0F));   // обсидиан = 5 ударов
		String key = sw.getRegistryKey().getValue() + "@" + pos.asLong();
		Progress old = HITS.get(key);
		int hits = (old != null && Ammo.TICK - old.lastTick() <= RESET_TICKS) ? old.hits() + 1 : 1;

		sw.playSound(null, pos, state.getSoundGroup().getHitSound(), SoundCategory.BLOCKS, 1.0F, 0.8F);
		if (hits >= needed) {
			HITS.remove(key);
			sw.breakBlock(pos, true, sp);
		} else {
			HITS.put(key, new Progress(hits, Ammo.TICK));
			sp.sendMessage(Text.literal("Удар " + hits + " / " + needed), true);
		}
		return ActionResult.SUCCESS;
	}
}
