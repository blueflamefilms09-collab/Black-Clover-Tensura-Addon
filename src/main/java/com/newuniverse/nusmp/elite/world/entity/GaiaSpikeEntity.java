package com.newuniverse.nusmp.elite.world.entity;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.world.MobTouchHandler;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Why: the earth spike of the calamities, a ground hazard the Gravebound Colossus raises in rings, lines and waves (and any later boss or skill can raise). It has two parts a player can read:
 * first a WARNING (for {@code delay} ticks the spot glows with Light Rukh, the tell), then the spike bursts out of the ground, throws up whoever stands there and damages them once, stays
 * a moment as a solid-looking spike, and is gone. It never persists (not saved), never hurts the calamity family, and its owner decides the damage: a {@link CalamityBoss} owner scales it
 * with its phase and the mod's hit cap, any other owner is limited by {@code BalanceLaw.cap}. The client draws it from the same {@code delay} (a synced number) with a rise animation.
 * It is an Entity, not a block, so it needs no world edit and cannot be left behind by a crash.
 */
public class GaiaSpikeEntity extends Entity {
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(GaiaSpikeEntity.class, EntityDataSerializers.INT);
    /** How long a spike stands after it emerged. */
    public static final int UP_TICKS = 24;

    private float damage = 6f;
    private UUID ownerId;
    private boolean struck;

    public GaiaSpikeEntity(EntityType<? extends GaiaSpikeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Raises a spike at the ground position {@code at}, bursting out after {@code delay} ticks, hitting for {@code damage} (base damage, see the class comment). */
    public static GaiaSpikeEntity raise(ServerLevel level, Vec3 at, LivingEntity owner, int delay, float damage) {
        GaiaSpikeEntity s = new GaiaSpikeEntity(com.newuniverse.nusmp.elite.world.WorldEntities.GAIA_SPIKE.get(), level);
        s.setPos(at.x, at.y, at.z);
        s.ownerId = owner == null ? null : owner.getUUID();
        s.damage = damage;
        s.entityData.set(DELAY, Math.max(1, delay));
        level.addFreshEntity(s);
        return s;
    }

    public int delay() { return entityData.get(DELAY); }

    public int upTicks() { return UP_TICKS; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(DELAY, 10); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        int delay = delay();
        if (tickCount < delay) {
            if (tickCount % 4 == 0) RukhFx.spray(sl, RukhFx.Alignment.LIGHT, getX(), getY() + 0.1, getZ(), 2, 0.35, 0.05, 0.35, 0.01);
            return;
        }
        if (!struck) {
            struck = true;
            burst(sl);
        }
        if (tickCount > delay + UP_TICKS) discard();
    }

    private LivingEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getEntity(ownerId) instanceof LivingEntity le ? le : null;
    }

    private void burst(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.HOSTILE, 1.2f, 0.7f);
        RukhFx.burst(sl, new Vec3(getX(), getY() + 0.4, getZ()), RukhFx.Alignment.LIGHT, 8, 0.4);
        LivingEntity owner = owner();
        AABB box = new AABB(getX() - 0.9, getY() - 0.2, getZ() - 0.9, getX() + 0.9, getY() + 2.2, getZ() + 0.9);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == owner || e instanceof CalamityBoss || MobTouchHandler.isHelper(e) || e instanceof PhylacteryEntity) continue;
            if (owner instanceof CalamityBoss boss) {
                if (!boss.isFoe(e)) continue;
                boss.strike(e, damage, 0.3);
            } else {
                if (owner != null && (e == owner || (e instanceof OwnableEntity o && owner.getUUID().equals(o.getOwnerUUID())))) continue;
                e.hurt(damageSources().indirectMagic(this, owner), BalanceLaw.cap(e, damage));
            }
            e.setDeltaMovement(e.getDeltaMovement().x, 0.55, e.getDeltaMovement().z);
            e.hurtMarked = true;
        }
    }

    @Override public boolean isPickable() { return false; }

    @Override public boolean isAttackable() { return false; }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
