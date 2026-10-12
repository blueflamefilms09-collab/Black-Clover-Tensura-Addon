package com.newuniverse.nusmp.dungeon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * A Dungeon slime: the base of the food web (tier 1). Born of dense magicules, it grazes the air for them and the Dungeon Mycelium and
 * the carcasses for the rest, grows (three sizes) when well fed, flees when hurt and is eaten by everything above it.
 */
public class DungeonSlimeEntity extends DungeonCreature {
    private static final EntityDataAccessor<Integer> GROWTH = SynchedEntityData.defineId(DungeonSlimeEntity.class, EntityDataSerializers.INT);

    public DungeonSlimeEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 6).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override public int tier() { return 1; }

    @Override public float weight() { return 2f + 3f * growth(); }

    @Override public int carcassKind() { return 0; }

    public int growth() { return entityData.get(GROWTH); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (GROWTH.equals(key)) refreshDimensions();
        super.onSyncedDataUpdated(key);
    }

    @Override public EntityDimensions getDefaultDimensions(Pose pose) { return super.getDefaultDimensions(pose).scale(1f + 0.6f * growth()); }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new PanicGoal(this, 1.4));
    }

    /** Grazes the magicules of the air; well fed, it grows. */
    @Override
    protected void metabolize() {
        if (!(level() instanceof ServerLevel sl)) return;
        EcosystemData eco = LivingDungeon.eco(sl, blockPosition());
        if (eco != null && eco.magicule >= 40f && hunger < 95) {
            eco.magicule -= 12f;
            eco.clamp();
            LivingDungeon.touch(sl, blockPosition());
            hunger = Math.min(HUNGER_MAX, hunger + 18);
        }
        if (hunger >= 85 && growth() < 2 && random.nextInt(3) == 0) {
            entityData.set(GROWTH, growth() + 1);
            hunger -= 25;
            setHealth(getMaxHealth());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Growth", growth());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(GROWTH, Math.max(0, Math.min(2, tag.getInt("Growth"))));
    }
}
