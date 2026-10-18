package com.newuniverse.nusmp.elite.world.entity;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.world.MobTouchHandler;
import com.newuniverse.nusmp.elite.world.WorldEntities;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: the void projectile of the mod, one entity for every user: the Lich's bolts, volleys and rain, and the Void skills of players (they call {@link #launch}). It flies straight with no
 * gravity, leaves a trail of Dark Rukh (the only particles it makes), can pierce a number of targets, and ends on the first block. The {@code damage} it carries is a BASE damage: when the
 * owner is a {@link CalamityBoss} the boss scales it (phase, multipliers, the mod's hit cap and the records report, see {@link CalamityBoss#strike}); for any other owner the damage is
 * limited by {@code BalanceLaw.cap} (a player's hit never takes more than the mod's share of a player's health, mobs get the config multiplier), so a skill should pass the damage it
 * already scaled with {@code BalanceLaw.damage}. It never hurts its owner, the owner's pets, the calamity family or a calamity helper.
 */
public class VoidBoltProjectile extends ThrowableProjectile {
    private float damage = 6f;
    private int pierce;
    /** A bolt that has not hit anything after this many ticks is gone. */
    public static final int LIFE_TICKS = 90;
    private final Set<Integer> hit = new HashSet<>();

    public VoidBoltProjectile(EntityType<? extends VoidBoltProjectile> type, Level level) { super(type, level); }

    /**
     * Launches a bolt from {@code from} along {@code dir} at {@code speed} blocks per tick (a boss uses about 1.1, a skill 1.5), carrying {@code damage}, passing through {@code pierce}
     * targets before it stops (0 = ends on the first). Returns the bolt (already added to the level).
     */
    public static VoidBoltProjectile launch(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 dir, float speed, float damage, int pierce) {
        VoidBoltProjectile b = new VoidBoltProjectile(WorldEntities.VOID_BOLT.get(), level);
        b.setOwner(owner);
        b.setPos(from.x, from.y, from.z);
        b.damage = damage;
        b.pierce = Math.max(0, pierce);
        Vec3 d = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
        b.shoot(d.x, d.y, d.z, speed, 0f);
        level.addFreshEntity(b);
        level.playSound(null, b.blockPosition(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.HOSTILE, 0.8f, 0.5f);
        return b;
    }

    public float damage() { return damage; }

    @Override protected double getDefaultGravity() { return 0.0; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        if (tickCount > LIFE_TICKS) {
            discard();
            return;
        }
        RukhFx.spray(sl, RukhFx.Alignment.DARK, getX(), getY(), getZ(), 1, 0.06, 0.06, 0.06, 0.0);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target) || hit.contains(target.getId())) return false;
        Entity o = getOwner();
        if (target == o || target instanceof CalamityBoss || MobTouchHandler.isHelper(target) || target instanceof PhylacteryEntity || target instanceof GaiaSpikeEntity) return false;
        return !(o != null && target instanceof OwnableEntity pet && o.getUUID().equals(pet.getOwnerUUID()));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide || !(result.getEntity() instanceof LivingEntity victim)) return;
        hit.add(victim.getId());
        Entity o = getOwner();
        if (o instanceof CalamityBoss boss) {
            if (!boss.isFoe(victim)) return;
            boss.strike(victim, damage, 0.4);
        } else {
            victim.hurt(damageSources().indirectMagic(this, o), BalanceLaw.cap(victim, damage));
        }
        if (level() instanceof ServerLevel sl) RukhFx.burst(sl, result.getLocation(), RukhFx.Alignment.DARK, 8);
        if (pierce > 0) pierce--;
        else discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel sl) {
            RukhFx.burst(sl, result.getLocation(), RukhFx.Alignment.DARK, 8);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 0.8f, 0.6f);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Pierce", pierce);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.contains("Damage") ? tag.getFloat("Damage") : damage;
        pierce = tag.getInt("Pierce");
    }
}
