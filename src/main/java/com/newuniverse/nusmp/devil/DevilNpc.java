package com.newuniverse.nusmp.devil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;

import java.util.Optional;
import java.util.UUID;

/**
 * Why: the figure of a devil that stands in the circle in front of the caller while a ritual runs and the offer is open (docs/devil_forms.md). A plain entity: no AI, no health, invulnerable, not pickable,
 * not pushable, no collision, never saved. Three synced values: who called it (the owner's UUID), which devil it is (the {@link WorldDevil} id) and its state ({@link DevilNpcRules#RISING} and so on);
 * the client renderer draws the devil's Blender mesh from them. The server side lives in {@link DevilNpcs}: this class only counts its own life, ends its terminal states and asks {@link DevilNpcs#check} once
 * every {@link DevilNpcRules#CHECK_EVERY} ticks whether it should still be there.
 *
 * <p>Cost: one entity per ritual (at most one per player, at most {@code npc.max} on the server), living at most {@code npc.lifetimeSeconds}; one tick of a counter, one check every ten ticks, no scan, no packet
 * after the first one but the three synced values when they change.
 */
public class DevilNpc extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(DevilNpc.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DEVIL = SynchedEntityData.defineId(DevilNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(DevilNpc.class, EntityDataSerializers.INT);

    /** Ticks since it was made (both sides count it), the tick the current state began (the server counts {@link #stateTicks}, the client reads {@link #clientStateAt}). */
    private int life, stateTicks, clientStateAt;

    public DevilNpc(EntityType<? extends DevilNpc> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(OWNER, Optional.empty());
        b.define(DEVIL, "");
        b.define(STATE, DevilNpcRules.RISING);
    }

    // ---------------------------------------------------------------- what it is
    public UUID owner() { return entityData.get(OWNER).orElse(null); }

    void setOwner(UUID id) { entityData.set(OWNER, Optional.ofNullable(id)); }

    public String devilId() { return entityData.get(DEVIL); }

    /** The devil this is, or null for an id that is not in the roster (a figure from a version that had more). */
    public WorldDevil devil() { return WorldDevil.byId(devilId()); }

    void setDevil(String id) { entityData.set(DEVIL, id == null ? "" : id); }

    public int state() { return DevilNpcRules.clampState(entityData.get(STATE)); }

    void setState(int s) {
        int n = DevilNpcRules.clampState(s);
        if (n == state()) return;
        entityData.set(STATE, n);
        stateTicks = 0;
    }

    /** Ticks it has lived (both sides). */
    public int life() { return life; }

    /** Client: ticks since the state last changed (for the animation). */
    public int stateAge() { return Math.max(0, life - clientStateAt); }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) clientStateAt = life;
    }

    // ---------------------------------------------------------------- the clock
    @Override
    public void tick() {
        super.tick();
        life++;
        if (level().isClientSide) return;
        stateTicks++;
        int st = state();
        if (DevilNpcRules.terminal(st) && stateTicks >= DevilNpcRules.endTicks(st)) {
            discard();
            return;
        }
        if (st == DevilNpcRules.RISING && stateTicks >= DevilNpcRules.RISE_TICKS) setState(DevilNpcRules.WAITING);
        if (life % DevilNpcRules.CHECK_EVERY == 0) DevilNpcs.check(this);
    }

    // ---------------------------------------------------------------- nothing can touch it
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public PushReaction getPistonPushReaction() { return PushReaction.IGNORE; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
