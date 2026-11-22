package com.newuniverse.nusmp.skill.summon;

import com.newuniverse.nusmp.skill.rules.SummonSpecies;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Why: the one creature type behind every Hell Mode summon of the Summoner (docs/summoner_creatures.md): which of the twelve it is (Hayate, Cerorin, Rocanel, Mira, Hatch, Tsubame, Solarin,
 * Genbu, Ellie, Okiyosan, Dradra, Merus) is one synced number, so a species adds no entity type and no packet of its own. It is a plain {@link PathfinderMob} with melee and the vanilla navigation
 * (ground or flying by species); the Summoner's upkeep ({@code Summons.tick}) gives it targets, keeps it near its owner and expires it, exactly as it did the vanilla wolf, golem and vex.
 * Its blow carries the species' effect ({@link SummonSpecies.Blow}). A <b>performer</b> is the same creature shown by a cast (the Dragon cast shows Dradra): invulnerable, harmless, no goals, plays its
 * {@code ability} clip and removes itself after its life ({@link SummonSpecies#performerTicks}), or at once when its owner is gone or far away.
 *
 * <p>Cost (rule 11): vanilla AI for one target, no scan of its own, no packet of its own beyond the vanilla entity data (species, performer flag); the owner's cap bounds the creatures
 * ({@code SummonRules.HARD_CAP}) and a performer lives at most four seconds with one per owner. Nothing is saved for a performer. Server decides everything; the client only draws the clip.
 */
public final class SummonBeast extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> SPECIES = SynchedEntityData.defineId(SummonBeast.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PERFORMER = SynchedEntityData.defineId(SummonBeast.class, EntityDataSerializers.BOOLEAN);
    private static final String K_SPECIES = "Species";
    /** The owner key of every summon (SummonerSkill.OWNER_KEY); a literal here because that class is package-private. */
    private static final String OWNER_KEY = "nusmp_owner";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID performerOwner;
    private int performerLife;

    public SummonBeast(EntityType<? extends SummonBeast> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.FLYING_SPEED, 0.6).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    // ---------------------------------------------------------------- species
    public SummonSpecies species() {
        SummonSpecies s = SummonSpecies.byIndex(entityData.get(SPECIES));
        return s == null ? SummonSpecies.HAYATE : s;
    }

    /** Set once by the creator, before the creature is added: the species, its speed and its box. */
    public void setSpecies(SummonSpecies s) {
        entityData.set(SPECIES, s.ordinal());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(s.speed);
        if (s.flying) getAttribute(Attributes.FLYING_SPEED).setBaseValue(s.speed * 2.0);
        refreshDimensions();
    }

    public boolean isPerformer() { return entityData.get(PERFORMER); }

    /** Makes this creature a performer for {@code owner}: harmless, invulnerable, with no goals, gone after {@code ticks}. */
    public void makePerformer(UUID owner, int ticks) {
        entityData.set(PERFORMER, true);
        performerOwner = owner;
        getPersistentData().putUUID(OWNER_KEY, owner);   // marks it as the owner's side: area skills never take it for a foe (it is not tagged as a summon, so it never counts toward the cap)
        performerLife = Math.max(10, Math.min(120, ticks));
        setInvulnerable(true);
        setNoAi(false);
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        setNoGravity(species().flying);
    }

    public UUID performerOwner() { return performerOwner; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(SPECIES, 0);
        b.define(PERFORMER, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SPECIES.equals(key)) refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        SummonSpecies s = species();
        return EntityDimensions.scalable(s.width, s.height);
    }

    // ---------------------------------------------------------------- movement and goals
    @Override
    protected PathNavigation createNavigation(Level level) {
        return species().flying ? new FlyingPathNavigation(this, level) : new GroundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void setSpeed(float speed) { super.setSpeed(speed); }

    /** The flying species hover: the move control changes with the species, set at the first tick. */
    private boolean moveSet;

    private void ensureMove() {
        if (moveSet) return;
        moveSet = true;
        if (species().flying) {
            this.moveControl = new FlyingMoveControl(this, 20, true);
            this.navigation = createNavigation(level());
        } else this.moveControl = new MoveControl(this);
    }

    // ---------------------------------------------------------------- tick, combat
    @Override
    public void tick() {
        ensureMove();
        super.tick();
        if (level().isClientSide) return;
        if (isPerformer()) {
            if (--performerLife <= 0 || (tickCount % 10 == 0 && !performerOwnerNear())) discard();
        } else if (species().flying && !isNoGravity()) setNoGravity(true);
    }

    private boolean performerOwnerNear() {
        if (performerOwner == null) return false;
        ServerPlayer p = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(performerOwner);
        return p != null && p.isAlive() && p.level() == level() && p.distanceToSqr(this) < 48.0 * 48.0;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (isPerformer()) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity t) {
            switch (species().blow) {
                case POISON -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                case FIRE -> t.igniteForSeconds(6);
                case SLOW -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                case WEAKEN -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                case KNOCK -> t.push(getLookAngle().x * 0.8, 0.35, getLookAngle().z * 0.8);
                case BITE -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                default -> { }
            }
        }
        return hit;
    }

    /** A performer can not be hurt; a real summon can not be hurt by its own owner (an area skill of theirs never reaches it either, see SkillTargets). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isPerformer() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (!level().isClientSide && source.getEntity() instanceof ServerPlayer sp && sp.getUUID().equals(ownerOf())) return false;
        return super.hurt(source, amount);
    }

    private UUID ownerOf() {
        var data = getPersistentData();
        return data.hasUUID(OWNER_KEY) ? data.getUUID(OWNER_KEY) : null;
    }

    @Override public boolean isPushable() { return !isPerformer() && super.isPushable(); }
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean shouldBeSaved() { return !isPerformer() && super.shouldBeSaved(); }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean causeFallDamage(float dist, float mult, DamageSource src) { return !species().flying && super.causeFallDamage(dist, mult, src); }
    @Override protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) { if (!species().flying) super.checkFallDamage(y, onGround, state, pos); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(K_SPECIES, entityData.get(SPECIES));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        SummonSpecies s = SummonSpecies.byIndex(tag.getInt(K_SPECIES));
        if (s != null) {
            entityData.set(SPECIES, s.ordinal());
            if (s.flying) getAttribute(Attributes.FLYING_SPEED).setBaseValue(s.speed * 2.0);
        }
    }

    // ---------------------------------------------------------------- animation (GeckoLib): idle / walk / attack from the species' own file, the ability clip for a performer
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SummonBeast>(this, "main", 4, state -> {
            SummonBeast b = state.getAnimatable();
            SummonSpecies s = b.species();
            if (b.isPerformer()) return state.setAndContinue(RawAnimation.begin().thenPlay(clip(s, "ability")).thenLoop(clip(s, "idle")));
            return state.setAndContinue(RawAnimation.begin().thenLoop(clip(s, state.isMoving() ? "walk" : "idle")));
        }));
        controllers.add(new AnimationController<SummonBeast>(this, "attack", 0, state -> {
            SummonBeast b = state.getAnimatable();
            if (b.swinging && state.getController().getAnimationState() == AnimationController.State.STOPPED) {
                state.getController().forceAnimationReset();
                state.setAnimation(RawAnimation.begin().thenPlay(clip(b.species(), "attack")));
            }
            return PlayState.CONTINUE;
        }));
    }

    /** Clip names, made once per species and clip (no string building per frame). */
    private static final String[][] CLIPS = new String[SummonSpecies.values().length][];

    static String clip(SummonSpecies s, String clip) {
        String[] row = CLIPS[s.ordinal()];
        if (row == null) {
            String p = "animation.summon_" + s.id + ".";
            row = CLIPS[s.ordinal()] = new String[] {p + "idle", p + "walk", p + "attack", p + "ability"};
        }
        return switch (clip) { case "idle" -> row[0]; case "walk" -> row[1]; case "attack" -> row[2]; default -> row[3]; };
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
