package com.cs16;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.Set;

public class Gun extends Item {
	private static final Set<String> SNIPERS = Set.of("scout", "awp", "sg550", "g3sg1");

	public final String id;
	public final float damage;
	public final int cooldown, range, price, magSize, reserve, reloadTicks;

	public Gun(Settings settings, String id, float damage, int cooldown, int range, int price,
			   int magSize, int reserve, int reloadTicks) {
		super(settings);
		this.id = id;
		this.damage = damage;
		this.cooldown = cooldown;
		this.range = range;
		this.price = price;
		this.magSize = magSize;
		this.reserve = reserve;
		this.reloadTicks = reloadTicks;
	}

	public boolean isSniper() { return SNIPERS.contains(id); }

	/** ПКМ: у снайперок зум обрабатывает клиент, сервер ничего не делает. */
	@Override
	public ActionResult use(World world, PlayerEntity player, Hand hand) {
		return ActionResult.PASS;
	}

	/** Выстрел (вызывается, когда клиент держит ЛКМ). */
	public void shoot(ServerWorld sw, ServerPlayerEntity player, ItemStack stack) {
		Ammo.State st = Ammo.get(player.getUuid(), this);
		if (st.reloadEnd >= 0) return;                 // идёт перезарядка
		if (st.mag <= 0) {                             // пусто -> авто-перезарядка
			Ammo.startReload(player, this);
			return;
		}

		Vec3d start = player.getEyePos();
		Vec3d dir = player.getRotationVec(1.0F);
		Vec3d end = start.add(dir.multiply(range));

		BlockHitResult block = sw.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
		if (block.getType() != HitResult.Type.MISS) end = block.getPos();

		Box box = player.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);
		LivingEntity target = null;
		double best = start.squaredDistanceTo(end);
		for (Entity e : sw.getOtherEntities(player, box, en -> en instanceof LivingEntity && en.isAlive())) {
			Optional<Vec3d> hit = e.getBoundingBox().expand(e.getTargetingMargin()).raycast(start, end);
			if (hit.isPresent()) {
				double d = start.squaredDistanceTo(hit.get());
				if (d <= best) { best = d; target = (LivingEntity) e; }
			}
		}

		st.mag--;
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.0F, 1.6F);
		if (target != null) {
			target.damage(sw, sw.getDamageSources().playerAttack(player), damage);
			sw.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.6), target.getZ(), 6, 0.1, 0.1, 0.1, 0.0);
		}
		else if (block.getType() != HitResult.Type.MISS) {
			// пуля ломает «мягкие» блоки: чем мощнее оружие, тем твёрже блок (твёрдость <= урон / 4)
			BlockPos bp = block.getBlockPos();
			BlockState bs = sw.getBlockState(bp);
			float hardness = bs.getHardness(sw, bp);
			if (hardness >= 0 && hardness <= damage / 4.0F) sw.breakBlock(bp, true, player);
		}
		player.getItemCooldownManager().set(stack, cooldown);
		Ammo.show(player, this, st);
	}
}
