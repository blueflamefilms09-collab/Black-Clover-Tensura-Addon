package com.newuniverse.nusmp.dragonballs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The Eternal Dragon, summoned for one ritual: Shenron (Earth set) or Super Shenron (Super set), one entity per running ritual (so at most two on a server). It is only a body to look at: no AI, no
 * physics, no gravity, cannot be hurt, hit, pushed or saved ({@code noSave}: a restart never leaves a dragon behind, the ritual's own bookkeeping scatters the balls again). The server places it, the
 * ritual session removes it. Drawn by the client renderer from the geo model (tools/gen_dragon_balls.py); its few ambient particles are spawned on the client in {@link #tick} (3 every 6 ticks).
 */
public class ShenronEntity extends Entity {
    private static final EntityDataAccessor<Integer> SET = SynchedEntityData.defineId(ShenronEntity.class, EntityDataSerializers.INT);

    public ShenronEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public BallSet set() { BallSet s = BallSet.byOrdinal(this.entityData.get(SET)); return s == null ? BallSet.EARTH : s; }

    public void setSet(BallSet s) { this.entityData.set(SET, s.ordinal()); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(SET, 0); }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(14.0, 6.0, 14.0); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && tickCount % 6 == 0) {
            for (int i = 0; i < 3; i++)
                level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 8.0, getY() + random.nextDouble() * 16.0, getZ() + (random.nextDouble() - 0.5) * 8.0, 0.0, 0.02, 0.0);
        }
    }
}
