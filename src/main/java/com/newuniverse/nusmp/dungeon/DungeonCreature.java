package com.newuniverse.nusmp.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A creature of the Living Dungeon's food web (docs/living_dungeon.md section 1). Tier 1 are the slimes (scavengers), tier 2 the Living
 * Armors, tier 3 the apex predators (Direwolf, Basilisk). Each has a <b>hunger</b> (0-100) that falls with time: hungry predators hunt,
 * full ones sleep and ignore everyone, one that starves takes damage. Nothing it dies of drops loot: it leaves a Carcass.
 */
public abstract class DungeonCreature extends PathfinderMob {
    public static final int HUNGER_MAX = 100, HUNGRY = 40, FULL = 80;

    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(DungeonCreature.class, EntityDataSerializers.BOOLEAN);

    protected int hunger = 70;
    /** Marked by a Butcher Cleaver's Anatomical Cut: its carcass carries an extra portion of meat. */
    private boolean dressed;
    private BlockPos home;

    protected DungeonCreature(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** 1 = slime, 2 = Living Armor, 3 = Direwolf or Basilisk. */
    public abstract int tier();

    /** What it weighs in the chunk's biomass. */
    public abstract float weight();

    /** Which Carcass it leaves (CarcassBlock.KIND: 0 slime, 1 armor, 2 basilisk, 3 direwolf, 4 anything else). */
    public abstract int carcassKind();

    /** Is this something it hunts? */
    public boolean isPrey(LivingEntity e) { return false; }

    /** Does it hunt players (when hungry, or when one comes into its den)? */
    public boolean huntsPlayers() { return false; }

    protected boolean canAttack() { return tier() >= 2; }

    /** A full apex predator is asleep (the client draws it curled up). */
    public boolean asleep() { return entityData.get(SLEEPING); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SLEEPING, false);
    }

    public int hunger() { return hunger; }

    public void dress() { dressed = true; }

    public boolean hungry() { return hunger <= HUNGRY; }

    public boolean full() { return hunger >= FULL; }

    public boolean nearHome(LivingEntity e) { return home != null && home.distSqr(e.blockPosition()) < 36; }

    /** It ate: hunger rises, and the chunk gets some of the magicules back. */
    public void feed(int amount, float magicule) {
        hunger = Math.min(HUNGER_MAX, hunger + amount);
        if (level() instanceof ServerLevel sl) {
            EcosystemData eco = LivingDungeon.eco(sl, blockPosition());
            if (eco != null) {
                eco.magicule += magicule;
                eco.clamp();
                LivingDungeon.touch(sl, blockPosition());
            }
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new DungeonGoals.Sleep(this));
        if (canAttack()) goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(4, new DungeonGoals.HuntForSustenance(this, 1.1));
        goalSelector.addGoal(5, new DungeonGoals.Scavenge(this, 1.0));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        if (huntsPlayers()) targetSelector.addGoal(2, new DungeonGoals.HostileToPlayers(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (home == null) home = blockPosition();
        if (tickCount % 100 == 0) hunger = Math.max(0, hunger - 1);
        if (hunger == 0 && tickCount % 40 == 0) hurt(damageSources().starve(), 1.0f);
        if (tickCount % 200 == 7) metabolize();
        if (tickCount % 10 == 0) entityData.set(SLEEPING, tier() == 3 && full() && getTarget() == null && getNavigation().isDone());
    }

    /** What it does with the chunk around it now and then (slimes graze magicules). */
    protected void metabolize() {}

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && level() instanceof ServerLevel sl) CarcassBlock.leave(sl, blockPosition(), carcassKind(), weight() + (dressed ? 10f : 0f));
    }

    // standard loot is disabled in the Dungeon: a kill leaves a carcass to harvest, not items
    @Override protected void dropFromLootTable(DamageSource source, boolean hitByPlayer) {}

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {}

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Hunger", hunger);
        tag.putBoolean("Dressed", dressed);
        if (home != null) tag.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hunger = tag.contains("Hunger") ? tag.getInt("Hunger") : 70;
        dressed = tag.getBoolean("Dressed");
        if (tag.contains("Home")) home = BlockPos.of(tag.getLong("Home"));
    }
}
