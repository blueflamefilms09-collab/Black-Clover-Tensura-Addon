package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.perf.PerfParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: Shishou as a body in the world. One class, three lives of the same one sword: a WILD flying sword that hunts hostile mobs and eats what they leave (and settles into its pedestal after N kills, N days
 * or when its mana is spent), a DORMANT sword embedded in the ground that waits for a wielder (right click claims it), and a friendly MINION thrown by its wielder that fights for them within a leash.
 * It flies like a vex ({@link FlyingMoveControl}, {@link FlyingPathNavigation}), never targets a player, a villager, a tamed creature or a wielder's ally ({@link ShishouTargets}), and cannot be killed: it
 * is invulnerable, and if it is removed any other way (void, a command) the world's record brings it back ({@link ShishouService#onBodyLost}). The ONE-per-world rule is enforced by the entity itself: at
 * its first tick and every five seconds it asks the world's record whether it is the valid body; a copy or an old body dissolves.
 *
 * <p>Animation state is one synced number ({@link ShishouRules#STATE_IDLE} .. {@code STATE_DORMANT}); the client draws the clip of that state ({@code client/shishou}); the Rukh trail at the hilt is server-sent,
 * only while aggro or attacking, through the perf budget, to nearby players only. Cost per tick: the vanilla goal evaluation of five goals; the heavy checks run once a second.</p>
 */
public class ShishouEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(ShishouEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(ShishouEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MINION = SynchedEntityData.defineId(ShishouEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HIDDEN = SynchedEntityData.defineId(ShishouEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerUuid;
    private boolean expectedRemoval, dormantPending;
    private int strikeCooldown, attackTicks;
    private ServerPlayer ownerCache;
    private long ownerCacheTick = Long.MIN_VALUE;
    private BlockPos groundSpot;
    private final ShishouCooldowns ownCooldowns = new ShishouCooldowns();
    private static final UUID SELF_KEY = new UUID(0x5155L, 0x5348L);

    public ShishouEntity(EntityType<? extends ShishouEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.FLYING_SPEED, 1.5).add(Attributes.MOVEMENT_SPEED, 0.8)
                .add(Attributes.ATTACK_DAMAGE, 14.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(STATE, ShishouRules.STATE_IDLE);
        b.define(LEVEL, 1);
        b.define(MINION, false);
        b.define(HIDDEN, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ShishouGoals.DormantPedestalGoal(this));
        this.goalSelector.addGoal(2, new ShishouGoals.TelekineticStrikeGoal(this));
        this.goalSelector.addGoal(3, new ShishouGoals.StayByWielderGoal(this));
        this.goalSelector.addGoal(4, new ShishouGoals.PatrolGoal(this));
        this.goalSelector.addGoal(5, new ShishouGoals.WanderAirGoal(this));
        this.targetSelector.addGoal(1, new ShishouGoals.HuntHostilesGoal(this));
    }

    // ---------------------------------------------------------------- state
    public int state() { return entityData.get(STATE); }

    public int swordLevel() { return entityData.get(LEVEL); }

    public boolean isDormant() { return state() == ShishouRules.STATE_DORMANT; }

    public boolean isMinion() { return entityData.get(MINION); }

    /** True while the wielder's Stealth hides the thrown sword (the client draws it faint). */
    public boolean isVeiled() { return entityData.get(HIDDEN); }

    public void setVeiled(boolean v) { entityData.set(HIDDEN, v); }

    public UUID ownerUuid() { return ownerUuid; }

    public boolean wantsDormant() { return dormantPending; }

    public void setState(int s) {
        if (state() != s) entityData.set(STATE, s);
    }

    /** This body is a thrown minion of {@code owner}. */
    public void makeMinion(UUID owner) {
        this.ownerUuid = owner;
        entityData.set(MINION, true);
        setState(ShishouRules.STATE_IDLE);
    }

    /** This body is dormant (embedded, static, waiting for a wielder). */
    public void makeDormant() {
        setState(ShishouRules.STATE_DORMANT);
        setNoAi(true);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        setTarget(null);
        dormantPending = false;
        refreshDimensions();
    }

    /** A removal the service asked for (a claim, a recall, a ghost): not a loss. */
    public void expectRemoval() { expectedRemoval = true; }

    public ShishouCooldowns ownCooldowns() { return ownCooldowns; }

    public static UUID selfKey() { return SELF_KEY; }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) {
            stateSince = tickCount;
            refreshDimensions();
        }
    }

    private int stateSince;

    /** Ticks since the animation state last changed (the renderer plays the attack clip from its start). */
    public int stateAge() { return tickCount - stateSince; }

    /** The blade is longer than the body's box: the culling box covers it, so the sword does not vanish when only its tip is in view. */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(1.1); }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isDormant() ? EntityDimensions.scalable(0.55f, 1.7f) : EntityDimensions.scalable(0.9f, 0.6f);
    }

    // ---------------------------------------------------------------- the wielder
    /** The wielder while online, found at most once a second (a cached lookup). */
    public ServerPlayer owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel sl)) return null;
        long now = sl.getGameTime();
        if (now - ownerCacheTick >= 20L || ownerCache != null && ownerCache.isRemoved()) {
            ownerCacheTick = now;
            net.minecraft.server.MinecraftServer s = sl.getServer();
            ownerCache = s == null ? null : s.getPlayerList().getPlayer(ownerUuid);
        }
        return ownerCache;
    }

    // ---------------------------------------------------------------- the hunt (2.1.0)
    private long huntNextScan, ignoreUntil;
    private int ignoredId = -1, chargeTicks, stuckTicks;
    private Vec3 lastPos = Vec3.ZERO;

    public long huntNextScan() { return huntNextScan; }

    public void setHuntNextScan(long t) { huntNextScan = t; }

    /** After a kill, or when the target is given up: the next look for prey is immediate. */
    public void scanNow() { huntNextScan = 0L; }

    /** Has this creature been given up on (a charge that could not reach it) and is the pause not over yet? */
    public boolean isIgnored(LivingEntity t) { return t != null && t.getId() == ignoredId && level().getGameTime() < ignoreUntil; }

    private void ignore(LivingEntity t) {
        ignoredId = t.getId();
        ignoreUntil = level().getGameTime() + ShishouHunt.IGNORE_TICKS;
    }

    /** A new charge begins: the speed curve starts over. */
    public void beginCharge() {
        chargeTicks = 0;
        stuckTicks = 0;
        lastPos = position();
    }

    /** One tick of the charge at {@code aim}: accelerating direct steering, with the unstick rule. */
    public void charge(Vec3 aim) {
        chargeTicks++;
        double top = ShishouHunt.topSpeed(swordLevel(), ShishouConfig.chargePercent());
        steer(aim, ShishouHunt.chargeSpeed(chargeTicks, top), true);
    }

    /** One tick of an ordinary flight toward {@code aim} at {@code blocksPerTick} (the patrol). */
    public void fly(Vec3 aim, double blocksPerTick) { steer(aim, blocksPerTick, false); }

    /** In reach of the target: the velocity dies down so he rides along with it instead of shooting past. */
    public void hoverOn(Vec3 aim) {
        stuckTicks = 0;
        setDeltaMovement(getDeltaMovement().scale(0.6));
    }

    private int bounceTicks;

    /** Still on the bounce after a strike: the goal leaves the velocity alone (the charge would blend it away). */
    public boolean bouncing() { return bounceTicks > 0; }

    /** A strike landed: bounce off {@code from} (the target's middle) a little, then the charge begins again. Server config {@code flurryBounce}. */
    public void bounceOff(Vec3 from) {
        if (!ShishouConfig.flurryBounce()) return;
        Vec3 d = position().subtract(from);
        double[] v = ShishouHunt.bounce(d.x, d.y, d.z);
        setDeltaMovement(v[0], v[1], v[2]);
        hasImpulse = true;
        bounceTicks = ShishouHunt.BOUNCE_TICKS;
        beginCharge();
    }

    private void steer(Vec3 aim, double speed, boolean charging) {
        Vec3 to = aim.subtract(position());
        double dist = to.length();
        if (dist < 1.0e-4) return;
        double sp = charging ? ShishouHunt.arrive(speed, dist) : speed;
        Vec3 want = to.scale(sp / dist), v = getDeltaMovement();
        Vec3 nv = new Vec3(ShishouHunt.blend(v.x, want.x), ShishouHunt.blend(v.y, want.y), ShishouHunt.blend(v.z, want.z));
        double movedSqr = position().distanceToSqr(lastPos);
        lastPos = position();
        stuckTicks = ShishouHunt.progressed(movedSqr) ? 0 : stuckTicks + 1;
        int u = ShishouHunt.unstick(stuckTicks);
        if (u == 1) {                                                                         // blocked: slide up and to the side of the way in
            Vec3 side = new Vec3(-to.z, 0.0, to.x).normalize().scale(((tickCount / 10) & 1) == 0 ? 0.25 : -0.25);
            nv = nv.add(0.0, 0.3, 0.0).add(side);
        } else if (u == 2 && charging && getTarget() != null) {                               // cannot get there: another creature first
            ignore(getTarget());
            setTarget(null);
            scanNow();
            stuckTicks = 0;
            return;
        }
        setDeltaMovement(nv);
        hasImpulse = true;
    }

    /** May this creature be hunted by this body? */
    public boolean isValidTarget(LivingEntity t) {
        if (t == this || !ShishouConfig.enabled()) return false;
        if (isMinion()) {
            ServerPlayer o = owner();
            if (o == null || o.level() != level()) return false;
            int leash = ShishouConfig.minionLeash();
            if (t.distanceToSqr(o) > (double) leash * leash) return false;
            return ShishouTargets.hostileFor(o, t);
        }
        return ShishouTargets.hostileFor(null, t);
    }

    // ---------------------------------------------------------------- claiming
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return isDormant() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) ShishouService.claim(sp, this);
        return InteractionResult.CONSUME;
    }

    // ---------------------------------------------------------------- the strike
    public boolean strikeReady() { return strikeCooldown <= 0 && !isDormant(); }

    /** The telekinetic strike: the attack animation, the damage (level scaled, capped), the elements, the kill credit. Returns true when it landed. */
    public boolean strike(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl) || !strikeReady() || target == null || !target.isAlive()) return false;
        if (!ShishouService.payStrike(this, sl)) return false;
        strikeCooldown = ShishouHunt.strikeCooldown(swordLevel());
        attackTicks = ShishouRules.ATTACK_ANIM_TICKS;
        setState(ShishouRules.STATE_ATTACK);
        boolean landed = ShishouService.strike(this, sl, target);
        if (!target.isAlive()) {                                                              // a kill: the next prey is looked for at once
            setTarget(null);
            scanNow();
        }
        return landed;
    }

    // ---------------------------------------------------------------- the tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        serverTick();
    }

    private void serverTick() {
        ServerLevel sl = (ServerLevel) level();
        long now = sl.getGameTime();
        if (strikeCooldown > 0) strikeCooldown--;
        if (bounceTicks > 0) bounceTicks--;
        if (attackTicks > 0 && --attackTicks == 0 && state() == ShishouRules.STATE_ATTACK) setState(getTarget() != null ? ShishouRules.STATE_AGGRO : ShishouRules.STATE_IDLE);
        if (!isDormant()) {
            LivingEntity t = getTarget();
            if (state() == ShishouRules.STATE_IDLE && t != null) setState(ShishouRules.STATE_AGGRO);
            else if (state() == ShishouRules.STATE_AGGRO && t == null) setState(ShishouRules.STATE_IDLE);
            if (t != null) faceTowards(t);
            else if (getDeltaMovement().horizontalDistanceSqr() > 0.0004) {
                float yaw = (float) (Math.toDegrees(Math.atan2(-getDeltaMovement().x, getDeltaMovement().z)));
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
            }
            if ((state() == ShishouRules.STATE_AGGRO || state() == ShishouRules.STATE_ATTACK) && (tickCount % 3) == 0) trail(sl);
        }
        if ((tickCount + getId()) % 20 == 0) ShishouService.bodyUpkeep(this, sl, now);
    }

    private void faceTowards(LivingEntity t) {
        Vec3 d = t.getEyePosition().subtract(position());
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, h));
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        setXRot(pitch);
    }

    /** The Rukh trail at the hilt and the point of the blade: two to three particles every third tick, light for a wielder's sword, dark for the wild one, through the perf budget. */
    private void trail(ServerLevel sl) {
        Vec3 look = calculateViewVector(getXRot(), getYRot());
        Vec3 c = position().add(0, 0.3, 0);
        Vec3 hilt = c.subtract(look.scale(ShishouRules.HILT_BACK));
        Vec3 tip = c.add(look.scale(ShishouRules.BLADE_LENGTH - ShishouRules.HILT_BACK));
        net.minecraft.core.particles.ParticleOptions p = isMinion() ? Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT) : Fx.options(FxThemes.SKILL_SHISHOU, FxTone.DARK);
        PerfParticles.send(sl, p, hilt.x, hilt.y, hilt.z, 2, 0.08, 0.08, 0.08, 0.01);
        if (state() == ShishouRules.STATE_ATTACK) PerfParticles.send(sl, p, tip.x, tip.y, tip.z, 1, 0.05, 0.05, 0.05, 0.02);
    }

    /** Called by the service when the wild sword wants its pedestal (checked once a second). */
    public void setDormantPending(boolean v) { dormantPending = v; }

    /** The ground spot chosen by the pedestal goal. */
    public BlockPos groundSpot() { return groundSpot; }

    public void setGroundSpot(BlockPos p) { groundSpot = p; }

    /** The sword's mirrored level on the synced data (for the client's glow). */
    public void setSwordLevel(int l) { if (swordLevel() != l) entityData.set(LEVEL, Math.max(1, Math.min(ShishouLevels.MAX_LEVEL, l))); }

    // ---------------------------------------------------------------- never killed
    @Override
    public boolean isInvulnerableTo(DamageSource source) { return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY); }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override public boolean isPushable() { return !isDormant(); }
    @Override public boolean fireImmune() { return true; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean isPersistenceRequired() { return true; }

    /** Any removal that destroys the body and that nobody asked for is a loss: the world's record brings the sword back. */
    @Override
    public void remove(RemovalReason reason) {
        boolean lost = !level().isClientSide && !expectedRemoval && reason.shouldDestroy();
        super.remove(reason);
        if (lost) ShishouService.onBodyLost(this, level() instanceof ServerLevel sl ? sl : null, position());
    }

    // ---------------------------------------------------------------- saving
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("State", state());
        tag.putBoolean("Minion", isMinion());
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
        entityData.set(MINION, tag.getBoolean("Minion"));
        int s = tag.getInt("State");
        if (s == ShishouRules.STATE_DORMANT) makeDormant();
        else setState(ShishouRules.STATE_IDLE);
    }
}
