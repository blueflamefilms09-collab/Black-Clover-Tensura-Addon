package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.audio.Cue;
import com.newuniverse.nusmp.audio.MobSoundSet;
import com.newuniverse.nusmp.audio.MobVoice;
import com.newuniverse.nusmp.audio.SoundFamilied;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * A creature of the dungeon's food web (docs/living_dungeon.md section 1, kept as history). Tier 1 are the slimes, tier 2 the Living
 * Armors, tier 3 the apex predators (Direwolf, Basilisk). Each has a <b>hunger</b> (0-100) that falls with time: hungry predators hunt,
 * full ones sleep and ignore everyone, one that starves takes damage. Nothing it dies of drops loot of its own (the economy's drop handler
 * pays the stones and the meat). Until 1.58.0 it also left a Carcass block where it died and fed the chunk's ecosystem; both went with the
 * Living Dungeon.
 */
public abstract class DungeonCreature extends PathfinderMob implements SoundFamilied {
    public static final int HUNGER_MAX = 100, HUNGRY = 40, FULL = 80;

    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(DungeonCreature.class, EntityDataSerializers.BOOLEAN);

    protected int hunger = 70;
    private BlockPos home;
    private int lastAggroTick = -1000;

    protected DungeonCreature(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** 1 = slime, 2 = Living Armor, 3 = Direwolf or Basilisk. */
    public abstract int tier();

    /**
     * 1.99.0 (the bespoke audio engine, docs/audio/mob_audio.md): the family of {@code audio/MobSoundSet} this creature sounds like. The Living Armor and the bosses built on it clank, the
     * Direwolf growls, the Basilisk hisses, the slime squelches; no creature of the dungeon plays a vanilla zombie or skeleton sound.
     */
    @Override public String soundFamily() { return "armor"; }

    @Override protected SoundEvent getAmbientSound() { return asleep() ? null : MobVoice.event(soundFamily(), Cue.AMBIENT); }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return MobVoice.event(soundFamily(), Cue.HURT); }

    @Override protected SoundEvent getDeathSound() { return MobVoice.event(soundFamily(), Cue.DEATH); }

    @Override protected float getSoundVolume() { return MobSoundSet.family(soundFamily()).volume(); }

    @Override public float getVoicePitch() { return MobVoice.pitch(soundFamily(), Cue.AMBIENT, random); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        String f = soundFamily();
        playSound(MobVoice.event(f, Cue.STEP), MobVoice.volume(f, Cue.STEP), MobVoice.pitch(f, Cue.STEP, random));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) MobVoice.play(this, soundFamily(), Cue.ATTACK);
        return hit;
    }

    /** The first time it notices a target (and at most every three seconds) it shouts its aggro cue. */
    @Override
    public void setTarget(LivingEntity target) {
        LivingEntity before = getTarget();
        super.setTarget(target);
        if (target != null && before == null && !level().isClientSide && tickCount - lastAggroTick > 60) {
            lastAggroTick = tickCount;
            MobVoice.play(this, soundFamily(), Cue.AGGRO);
        }
    }

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

    public boolean hungry() { return hunger <= HUNGRY; }

    public boolean full() { return hunger >= FULL; }

    public boolean nearHome(LivingEntity e) { return home != null && home.distSqr(e.blockPosition()) < 36; }

    /** It ate: hunger rises. */
    public void feed(int amount) {
        hunger = Math.min(HUNGER_MAX, hunger + amount);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new DungeonGoals.Sleep(this));
        if (canAttack()) goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(4, new DungeonGoals.HuntForSustenance(this, 1.1));
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

    /** What it does now and then (slimes graze and grow). */
    protected void metabolize() {}

    // standard loot is disabled in the Dungeon: the economy's drop handler pays the stones and the meat
    @Override protected void dropFromLootTable(DamageSource source, boolean hitByPlayer) {}

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {}

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Hunger", hunger);
        if (home != null) tag.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hunger = tag.contains("Hunger") ? tag.getInt("Hunger") : 70;
        if (tag.contains("Home")) home = BlockPos.of(tag.getLong("Home"));
    }
}
