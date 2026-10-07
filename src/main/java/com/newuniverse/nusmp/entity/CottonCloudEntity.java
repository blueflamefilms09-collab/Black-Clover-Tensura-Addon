package com.newuniverse.nusmp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.UUID;

/**
 * 0.52: Charmy's cotton cloud, the one prop most of Cotton Magic's spells use (it replaces the old billboard sheep and puffs).
 * <ul>
 *   <li>{@link #PLATFORM}: a flat cumulus under someone's feet (Sheep Cook's kitchen, the place a sheep leaps out of).</li>
 *   <li>{@link #RIDE}: Cotton Cloud. It rises, then carries up to four riders where the first one steers (walk forward to
 *       fly where you look; sneak to step off) and sets them down gently when it fades.</li>
 *   <li>{@link #WRAP}: Sheep Bondage's cocoon, hugging a foe for the length of the hold.</li>
 *   <li>{@link #BURST}: a quick puff that swells and fades where a strike lands.</li>
 * </ul>
 * Drawn by {@code CottonCloudRenderer} (a heap of cotton puffs).
 */
public class CottonCloudEntity extends Entity {
    public static final int PLATFORM = 0, RIDE = 1, WRAP = 2, BURST = 3;
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(CottonCloudEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(CottonCloudEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE_MAX = SynchedEntityData.defineId(CottonCloudEntity.class, EntityDataSerializers.INT);

    private int life;
    private UUID follow;

    public CottonCloudEntity(EntityType<? extends CottonCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(MODE, PLATFORM);
        b.define(SCALE, 1f);
        b.define(LIFE_MAX, 100);
    }

    public int mode() { return entityData.get(MODE); }
    public float cloudScale() { return entityData.get(SCALE); }
    public int maxLife() { return entityData.get(LIFE_MAX); }
    public int life() { return life; }

    /** Spawns a cloud. 'scale' is relative to a 2.6-block cumulus. */
    public static CottonCloudEntity spawn(ServerLevel sl, int mode, Vec3 at, float scale, int lifeTicks) {
        CottonCloudEntity c = NUEntities.COTTON_CLOUD.get().create(sl);
        if (c == null) return null;
        c.entityData.set(MODE, mode);
        c.entityData.set(SCALE, scale);
        c.entityData.set(LIFE_MAX, lifeTicks);
        c.setPos(at.x, at.y, at.z);
        c.refreshDimensions();
        sl.addFreshEntity(c);
        return c;
    }

    /** A cocoon that stays on 'who' (Sheep Bondage). */
    public static CottonCloudEntity wrap(ServerLevel sl, LivingEntity who, int lifeTicks) {
        CottonCloudEntity c = spawn(sl, WRAP, who.position(), Math.max(0.6f, who.getBbHeight() / 1.8f), lifeTicks);
        if (c != null) c.follow = who.getUUID();
        return c;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key) || MODE.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float s = entityData == null ? 1f : cloudScale();
        return switch (entityData == null ? PLATFORM : mode()) {
            case WRAP -> EntityDimensions.scalable(1.1f * s, 1.9f * s);
            case BURST -> EntityDimensions.scalable(1.0f, 0.5f);
            default -> EntityDimensions.scalable(2.6f * s, 0.6f * s);
        };
    }

    // ---------------------------------------------------------------- behaviour
    @Override
    public void tick() {
        super.tick();
        life++;
        if (level().isClientSide) { fx(); return; }
        if (!(level() instanceof ServerLevel sl)) return;
        if (life >= maxLife()) { end(sl); return; }
        switch (mode()) {
            case RIDE -> ride();
            case WRAP -> {
                if (follow != null && sl.getEntity(follow) instanceof LivingEntity t && t.isAlive()) setPos(t.getX(), t.getY(), t.getZ());
                else if (life > 4) end(sl);
            }
            default -> { }
        }
    }

    /** Cotton Cloud: up for 30 ticks, then wherever the first rider walks toward. */
    private void ride() {
        Vec3 v = Vec3.ZERO;
        if (life < 30) v = new Vec3(0, 0.12, 0);
        else if (getControllingPassenger() instanceof LivingEntity c) {
            float fwd = c.zza;
            if (Math.abs(fwd) > 0.01f) v = c.getLookAngle().scale(fwd > 0 ? 0.32 : -0.15);
        } else if (getPassengers().isEmpty() && life > 40) v = new Vec3(0, -0.04, 0);                // an empty cloud settles
        setDeltaMovement(v);
        move(MoverType.SELF, v);
    }

    private void end(ServerLevel sl) {
        if (mode() == RIDE) {                                                                       // a gentle landing for everyone aboard
            for (Entity p : new ArrayList<>(getPassengers())) {
                p.stopRiding();
                if (p instanceof LivingEntity l) l.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
            }
        }
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 16, 0.8 * cloudScale(), 0.3, 0.8 * cloudScale(), 0.03);
        discard();
    }

    private void fx() {
        if (tickCount % 3 != 0) return;
        float s = cloudScale();
        level().addParticle(mode() == BURST ? ParticleTypes.POOF : ParticleTypes.CLOUD, getX() + (random.nextDouble() - 0.5) * 2.2 * s, getY() + 0.2 + random.nextDouble() * 0.6 * s,
                getZ() + (random.nextDouble() - 0.5) * 2.2 * s, 0, 0.01, 0);
    }

    // ---------------------------------------------------------------- solidity and riding
    @Override public boolean canBeCollidedWith() { return false; }                                  // cotton is soft: you walk through it
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isControlledByLocalInstance() { return false; }                     // the server moves it

    @Override
    protected boolean canAddPassenger(Entity passenger) { return mode() == RIDE && getPassengers().size() < 4; }

    @Override
    public LivingEntity getControllingPassenger() {
        return getPassengers().isEmpty() || !(getPassengers().get(0) instanceof LivingEntity l) ? null : l;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float partial) {
        int i = Math.max(0, getPassengers().indexOf(passenger));
        double[][] seat = {{0, 0}, {0.9, 0.5}, {-0.9, 0.5}, {0, -0.9}};
        return new Vec3(seat[i][0] * cloudScale(), dims.height() * 0.9, seat[i][1] * cloudScale());
    }

    // ---------------------------------------------------------------- save (these are spell effects: they do not persist)
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
