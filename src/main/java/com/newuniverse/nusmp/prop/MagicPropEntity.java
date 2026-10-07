package com.newuniverse.nusmp.prop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 0.53: one real 3D thing a spell has put in the world (see {@link MagicProps}). All its state that a painter needs is synced:
 * kind, scale, life, a free {@code param}, a {@code seed}, a target entity, a size (hitbox) and flags (solid, hittable, gravity).
 * It is never saved. Behaviours drive it on the server; painters draw it on the client.
 */
public class MagicPropEntity extends Entity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE_MAX = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PARAM = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> W = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> H = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(MagicPropEntity.class, EntityDataSerializers.BYTE);
    private static final int SOLID = 1, HITTABLE = 2, GRAVITY = 4;

    private int life;
    private UUID owner;

    public MagicPropEntity(EntityType<? extends MagicPropEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, 0);
        b.define(LIFE_MAX, 100);
        b.define(PARAM, 0);
        b.define(SEED, 0);
        b.define(TARGET, -1);
        b.define(SCALE, 1f);
        b.define(W, 1f);
        b.define(H, 1f);
        b.define(HEALTH, 0f);
        b.define(FLAGS, (byte) 0);
    }

    void setup(PropKind kind, float scale, int lifeTicks, int param, Entity owner) {
        entityData.set(KIND, kind.ordinal());
        entityData.set(SCALE, scale);
        entityData.set(LIFE_MAX, lifeTicks);
        entityData.set(PARAM, param);
        entityData.set(SEED, random.nextInt());
        entityData.set(W, scale);
        entityData.set(H, scale);
        if (owner != null) this.owner = owner.getUUID();
        refreshDimensions();
    }

    // ---------------------------------------------------------------- state a behaviour or painter can read and set
    public PropKind kind() {
        PropKind[] all = PropKind.values();
        int k = entityData == null ? 0 : entityData.get(KIND);
        return all[Math.max(0, Math.min(all.length - 1, k))];
    }
    public float scale() { return entityData.get(SCALE); }
    public int life() { return life; }
    public int maxLife() { return entityData.get(LIFE_MAX); }
    /** 0..1 of its life used. */
    public float lifeFrac() { return Math.min(1f, life / (float) Math.max(1, maxLife())); }
    public int param() { return entityData.get(PARAM); }
    public void setParam(int v) { entityData.set(PARAM, v); }
    public int seed() { return entityData.get(SEED); }
    public float health() { return entityData.get(HEALTH); }
    public UUID ownerId() { return owner; }

    public LivingEntity owner() {
        return owner != null && level() instanceof ServerLevel sl && sl.getEntity(owner) instanceof LivingEntity l && l.isAlive() ? l : null;
    }

    public void setTarget(Entity t) { entityData.set(TARGET, t == null ? -1 : t.getId()); }
    public Entity target() { int id = entityData.get(TARGET); return id < 0 ? null : level().getEntity(id); }

    /** Hitbox size in blocks (default: the scale). */
    public void setSize(float w, float h) { entityData.set(W, w); entityData.set(H, h); refreshDimensions(); }

    /** Other things collide with it (a wall, a platform, a fortress block). */
    public void setSolid(boolean v) { flag(SOLID, v); }
    /** It can be hit and destroyed; health is how much it takes. */
    public void setHittable(float health) { flag(HITTABLE, health > 0); entityData.set(HEALTH, health); }
    /** It falls (default: it floats). */
    public void setGravity(boolean v) { setNoGravity(!v); flag(GRAVITY, v); }

    private void flag(int bit, boolean on) {
        byte f = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (on ? f | bit : f & ~bit));
    }

    private boolean has(int bit) { return (entityData.get(FLAGS) & bit) != 0; }

    // ---------------------------------------------------------------- the clock
    @Override
    public void tick() {
        super.tick();
        life++;
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        MagicProps.Behavior b = MagicProps.behavior(kind());
        if (life >= maxLife()) { finish(sl, b); return; }
        if (!has(GRAVITY)) setNoGravity(true);
        if (b != null) b.tick(this, sl);
    }

    private void finish(ServerLevel sl, MagicProps.Behavior b) {
        if (b != null) b.end(this, sl);
        discard();
    }

    /** Ends it now (its behaviour's end() runs). */
    public void expire() {
        if (level() instanceof ServerLevel sl && !isRemoved()) finish(sl, MagicProps.behavior(kind()));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !has(HITTABLE) || isRemoved()) return false;
        MagicProps.Behavior b = MagicProps.behavior(kind());
        if (b != null && !b.hurt(this, source, amount)) return false;
        float h = entityData.get(HEALTH) - amount;
        entityData.set(HEALTH, h);
        if (h <= 0 && level() instanceof ServerLevel sl) finish(sl, b);
        return true;
    }

    // ---------------------------------------------------------------- shape
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return entityData == null ? EntityDimensions.scalable(1f, 1f) : EntityDimensions.scalable(entityData.get(W), entityData.get(H));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (W.equals(key) || H.equals(key)) refreshDimensions();
    }

    @Override public boolean canBeCollidedWith() { return has(SOLID); }
    @Override public boolean isPickable() { return has(HITTABLE); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
