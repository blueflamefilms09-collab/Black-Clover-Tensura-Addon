package com.newuniverse.nusmp.elite.world.entity;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.bar.BossBarSync;
import com.newuniverse.nusmp.boss.budget.BossBudget;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.elite.api.CalamityDamageEvent;
import com.newuniverse.nusmp.elite.world.MobTouchHandler;
import com.newuniverse.nusmp.elite.world.WorldConfig;
import com.newuniverse.nusmp.elite.world.WorldData;
import com.newuniverse.nusmp.elite.world.calamity.CalamityManager;
import com.newuniverse.nusmp.elite.world.rules.CalamityDef;
import com.newuniverse.nusmp.elite.world.rules.CalamityPhases;
import com.newuniverse.nusmp.elite.world.rules.MoveScheduler;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Why: the shared body of the two calamity bosses (the Gravebound Colossus and the Lich), so that both fight by the same rules and the rules are written once. It is deliberately NOT built on the
 * Zogratis framework: that one is made for the devil family (power percentages, the Gates, Charter Grimoires, arenas that reset), a calamity is something else, a world event in the open. What a
 * calamity boss shares with the rest of the mod it takes from the mod: the per-second damage budget (it is a {@link BudgetedBoss}, so the boss rules of the mod apply to it automatically: no
 * regeneration in combat, no Unique or Ultimate skills), the animated boss bar ({@code BossBarSync}), {@code BalanceLaw} for the damage it deals to players (a hit never takes more than the
 * mod's share of a player's health), and the Rukh particles for every effect.
 *
 * <p>The fight: a list of MOVES ({@link MoveScheduler}), each a telegraph (a wind-up where the boss stands still and marks the danger with Rukh), the blow, and a recovery. PHASES come from
 * the health thresholds of the calamity's data file ({@link CalamityPhases}): a blow can never skip a phase, crossing a threshold is a short invulnerable transformation, and every phase
 * tightens the cooldowns and hardens the hits. Players are never held in place: only damage, short slows and knockback.
 */
public abstract class CalamityBoss extends Monster implements BudgetedBoss {
    protected static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(CalamityBoss.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(CalamityBoss.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Long> ANIM_START = SynchedEntityData.defineId(CalamityBoss.class, EntityDataSerializers.LONG);
    protected static final EntityDataAccessor<Boolean> SHIELD = SynchedEntityData.defineId(CalamityBoss.class, EntityDataSerializers.BOOLEAN);

    public static final int ANIM_IDLE = 0, ANIM_WINDUP = 1, ANIM_STRIKE = 2, ANIM_TRANSFORM = 3, ANIM_CAST = 4;
    private static final int ST_IDLE = 0, ST_WINDUP = 1, ST_ACTIVE = 2, ST_RECOVER = 3, ST_TRANSFORM = 4;
    /** How long a phase change lasts (the boss is invulnerable and shows the transformation). */
    public static final int TRANSFORM_TICKS = 50;

    private final BossBudget dpsBudget = new BossBudget();
    private final ServerBossEvent bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_6);
    private final Map<String, Long> readyAt = new HashMap<>();
    private final Map<UUID, Float> fighters = new HashMap<>();
    private final List<UUID> helpers = new ArrayList<>();
    private final Random rng = new Random();

    private String calamityId = "";
    private CalamityDef def;
    private int state = ST_IDLE, stateTicks, activeTotal;
    private MoveScheduler.Move move;
    private LivingEntity moveTarget;
    private Vec3 aim = Vec3.ZERO;
    private String lastMove;
    private long riseTick, nextMoveAt, meleeReadyAt, lastNear;
    /** A calamity nobody is near for this long (ticks) leaves, so one that was fled from cannot block the director for ever. */
    public static final long ABANDON_TICKS = 12000L;
    private boolean fightEnded, tookDamage, barPrimed;
    private int barPhase = -1;
    private float baseHealth = -1f;

    protected CalamityBoss(EntityType<? extends CalamityBoss> type, Level level) {
        super(type, level);
        this.xpReward = 600;
    }

    public static AttributeSupplier.Builder bossAttributes(double health, double attack, double speed, double armor) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.ATTACK_DAMAGE, attack).add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ARMOR, armor).add(Attributes.ARMOR_TOUGHNESS, 4.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    // ================================================================ what a concrete boss says

    /** The id of the calamity's data file this boss belongs to. */
    public abstract String defaultCalamityId();

    /** The moves, in a fixed list. */
    protected abstract List<MoveScheduler.Move> moves();

    /** Called every tick of a wind-up: mark the danger. {@code left} counts down to 0, {@code total} is the wind-up length. */
    protected abstract void telegraph(MoveScheduler.Move m, LivingEntity target, Vec3 aim, int left, int total);

    /** The blow. Returns the number of ticks the move stays ACTIVE afterwards (a dash), or 0 for an instant move. */
    protected abstract int execute(MoveScheduler.Move m, LivingEntity target, Vec3 aim);

    /** Called each tick of an active move. */
    protected void tickActive(MoveScheduler.Move m, LivingEntity target, Vec3 aim, int elapsed, int total) {}

    /** A new phase began (1 or 2); the boss is transforming. */
    protected void onPhaseStart(int phase) {}

    /** Every server tick, after the move machine. */
    protected void bossTick(ServerLevel level, LivingEntity target) {}

    /** The damage a blow is cut to while the boss is shielded or otherwise protected (1 = unchanged). */
    protected float damageTakenFactor(DamageSource source) { return 1f; }

    /** The side of the Rukh the boss's effects use: DARK for death magic, LIGHT for earth and elements. */
    public abstract RukhFx.Alignment rukh();

    /** Reach of the plain melee blow in blocks (measured from the edge of the body). */
    protected double meleeReach() { return 3.0; }

    // ================================================================ data

    protected final CalamityDef def() {
        if (def == null || !def.id().equals(calamityId)) bindDef();
        return def;
    }

    private void bindDef() {
        if (calamityId.isEmpty()) calamityId = defaultCalamityId();
        def = WorldData.calamity(calamityId);
        if (def == null && !calamityId.equals(defaultCalamityId())) {
            calamityId = defaultCalamityId();
            def = WorldData.calamity(calamityId);
        }
    }

    public String calamityId() { return calamityId.isEmpty() ? defaultCalamityId() : calamityId; }

    /** Binds the boss to its data file and scales its health and damage by the file's and the config's multipliers. Call once, right after creating it. */
    public void setup(String id) {
        this.calamityId = id;
        bindDef();
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
        if (baseHealth < 0f && hp != null) baseHealth = (float) hp.getBaseValue();
        double hm = (def == null ? 1.0 : def.healthMult()) * WorldConfig.calamityHealth();
        if (hp != null) hp.setBaseValue(baseHealth * hm);
        setHealth(getMaxHealth());
        bar.setName(getDisplayName());
    }

    public List<Double> thresholds() {
        CalamityDef d = def();
        return d == null || d.phases().isEmpty() ? List.of(0.66, 0.33) : d.phases();
    }

    public int phase() { return entityData.get(PHASE); }

    public int phaseCount() { return thresholds().size() + 1; }

    public boolean shielded() { return entityData.get(SHIELD); }

    protected void setShield(boolean on) { entityData.set(SHIELD, on); }

    public int anim() { return entityData.get(ANIM); }

    public long animStart() { return entityData.get(ANIM_START); }

    public Map<UUID, Float> fighters() { return fighters; }

    public boolean tookDamageFromBoss() { return tookDamage; }

    public long fightTicks() { return riseTick == 0L ? 0L : Math.max(0L, level().getGameTime() - riseTick); }

    public CalamityPhases.Tuning tuning() { return CalamityPhases.tuning(phase(), phaseCount()); }

    protected final Random rng() { return rng; }

    protected double damageMult() { return (def() == null ? 1.0 : def().damageMult()) * WorldConfig.calamityDamage(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 0);
        b.define(ANIM, ANIM_IDLE);
        b.define(ANIM_START, 0L);
        b.define(SHIELD, false);
    }

    protected final void setAnim(int anim) {
        entityData.set(ANIM, anim);
        entityData.set(ANIM_START, level().getGameTime());
    }

    // ================================================================ budget (the per-second damage cap of the mod's bosses)

    @Override public BossBudget dpsBudget() { return dpsBudget; }

    @Override public int budgetStage() { return phase(); }

    @Override public int budgetFinalStage() { return thresholds().size(); }

    @Override
    public void baseTick() {
        super.baseTick();
        dpsBudget.tick(this, this);
    }

    @Override
    public void setHealth(float health) { super.setHealth(dpsBudget == null ? health : dpsBudget.setHealth(this, health)); }

    // ================================================================ goals

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new ChaseGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, e -> !(e instanceof Player p) || (!p.isCreative() && !p.isSpectator())));
    }

    /** Walks to the target and, when the machine is idle, strikes in melee. */
    private final class ChaseGoal extends Goal {
        ChaseGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() { return state == ST_IDLE && !fightEnded && getTarget() != null && getTarget().isAlive(); }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30f, 30f);
            double reach = meleeReach() + getBbWidth() * 0.5 + t.getBbWidth() * 0.5;
            if (distanceToSqr(t.getX(), t.getY(), t.getZ()) > reach * reach * 0.85) {
                getNavigation().moveTo(t, 1.0 * tuning().speedMult());
                return;
            }
            getNavigation().stop();
            long now = level().getGameTime();
            if (now >= meleeReadyAt) {
                meleeReadyAt = now + Math.max(8, Math.round(24 * tuning().cooldownMult()));
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                strike(t, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 0.9);
            }
        }
    }

    // ================================================================ the server tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (riseTick == 0L) {
            if (baseHealth < 0f) setup(calamityId());                          // a /summon'ed boss gets the data file's and the config's multipliers on its first tick
            riseTick = now - pendingFought;                                  // a boss loaded from disk keeps its fight clock
            nextMoveAt = now + 40;
            if (pendingFought == 0L) CalamityManager.bossRose(this);
            else CalamityManager.bossResumed(this);
        }
        refreshBar();
        if (fightEnded || !isAlive()) return;
        if (lastNear == 0L) lastNear = now;
        if (now % 40 == 0) {
            if (sl.getNearestPlayer(getX(), getY(), getZ(), 128.0, true) != null) lastNear = now;
            else if (now - lastNear > ABANDON_TICKS) {
                discard();                                                    // abandoned: the manager reports it as a Vanish
                return;
            }
        }
        if (getTarget() != null && !getTarget().isAlive()) setTarget(null);
        LivingEntity target = getTarget();
        stepPhase(sl);
        switch (state) {
            case ST_TRANSFORM -> {
                getNavigation().stop();
                if (--stateTicks <= 0) {
                    state = ST_IDLE;
                    setAnim(ANIM_IDLE);
                }
            }
            case ST_WINDUP -> windup(sl);
            case ST_ACTIVE -> active(sl);
            case ST_RECOVER -> {
                getNavigation().stop();
                if (--stateTicks <= 0) {
                    state = ST_IDLE;
                    setAnim(ANIM_IDLE);
                }
            }
            default -> {
                if (target != null && now >= nextMoveAt) chooseMove(sl, target, now);
            }
        }
        bossTick(sl, target);
    }

    private void stepPhase(ServerLevel sl) {
        if (state == ST_TRANSFORM) return;
        int due = CalamityPhases.phaseFor(fraction(), thresholds());
        if (due > phase()) beginPhase(sl, phase() + 1);
    }

    private void beginPhase(ServerLevel sl, int p) {
        entityData.set(PHASE, p);
        move = null;
        state = ST_TRANSFORM;
        stateTicks = TRANSFORM_TICKS;
        setAnim(ANIM_TRANSFORM);
        getNavigation().stop();
        RukhFx.burst(sl, this, rukh(), 12);
        sl.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 0.6f);
        onPhaseStart(p);
        CalamityManager.bossPhase(this, p);
    }

    private void chooseMove(ServerLevel sl, LivingEntity target, long now) {
        double dist = Math.sqrt(distanceToSqr(target.getX(), target.getY(), target.getZ()));
        MoveScheduler.Move m = MoveScheduler.choose(moves(), phase(), dist, readyAt, now, lastMove, rng);
        if (m == null) return;
        move = m;
        moveTarget = target;
        aim = target.position();
        lastMove = m.id();
        state = ST_WINDUP;
        stateTicks = Math.max(1, Math.round(m.windup() * (float) Math.max(0.6, tuning().cooldownMult())));
        activeTotal = stateTicks;
        faceAim();
        setAnim(m.windup() >= 36 ? ANIM_CAST : ANIM_WINDUP);
    }

    private void faceAim() {
        double dx = aim.x - getX(), dz = aim.z - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    private void windup(ServerLevel sl) {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.1, 1.0, 0.1));
        LivingEntity t = moveTarget != null && moveTarget.isAlive() ? moveTarget : null;
        telegraph(move, t, aim, stateTicks, activeTotal);
        if (--stateTicks > 0) return;
        long now = sl.getGameTime();
        readyAt.put(move.id(), now + MoveScheduler.scaledCooldown(move, tuning()));
        int extra = execute(move, t, aim);
        setAnim(ANIM_STRIKE);
        if (extra > 0) {
            state = ST_ACTIVE;
            stateTicks = extra;
            activeTotal = extra;
        } else {
            enterRecover(now);
        }
    }

    private void active(ServerLevel sl) {
        int elapsed = activeTotal - stateTicks;
        tickActive(move, moveTarget, aim, elapsed, activeTotal);
        if (--stateTicks <= 0) enterRecover(sl.getGameTime());
    }

    private void enterRecover(long now) {
        state = ST_RECOVER;
        stateTicks = Math.max(4, Math.round(move.recover() * (float) tuning().cooldownMult()));
        nextMoveAt = now + stateTicks + 6;
    }

    /** True while a move is in its wind-up or active part (the boss does not walk). */
    public boolean busy() { return state == ST_WINDUP || state == ST_ACTIVE || state == ST_TRANSFORM; }

    /** Staggers the boss: it stands still and takes extra damage for a while. */
    public void stagger(int ticks) {
        state = ST_RECOVER;
        stateTicks = Math.max(stateTicks, ticks);
        move = move == null ? moves().get(0) : move;
        setAnim(ANIM_TRANSFORM);
        staggerUntil = level().getGameTime() + ticks;
    }

    private long staggerUntil;

    public boolean staggered() { return level().getGameTime() < staggerUntil; }

    // ================================================================ hurting and being hurt

    /**
     * A blow of the boss against somebody: scaled by the phase and the multipliers, cut by {@link BalanceLaw#cap} (a player never loses more than the mod's share of their health in one hit),
     * and reported to the records through {@link CalamityDamageEvent}. Returns the health the victim really lost.
     */
    public float strike(LivingEntity victim, float base, double knock) {
        if (!(level() instanceof ServerLevel) || victim == null || !victim.isAlive() || !isFoe(victim)) return 0f;
        float scaled = (float) (base * tuning().damageMult() * damageMult());
        float dmg = BalanceLaw.cap(victim, scaled);
        float before = victim.getHealth() + victim.getAbsorptionAmount();
        boolean hit = victim.hurt(damageSources().mobAttack(this), dmg);
        if (hit && knock > 0.0) victim.knockback(knock, getX() - victim.getX(), getZ() - victim.getZ());
        float lost = before - (victim.getHealth() + victim.getAbsorptionAmount());
        if (hit && lost > 0f && victim instanceof ServerPlayer sp) {
            tookDamage = true;
            CalamityManager.fire(new CalamityDamageEvent(calamityId(), this, sp, lost, false, phase(), fightTicks()));
        }
        return Math.max(0f, lost);
    }

    /** Players, and the creatures that belong to a player, which are not creative or spectating. */
    public boolean isFoe(Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == this || e instanceof CalamityBoss || MobTouchHandler.isHelper(e)) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return e instanceof OwnableEntity o && o.getOwnerUUID() != null;
    }

    public List<LivingEntity> foesAround(Vec3 centre, double radius) {
        AABB box = new AABB(centre.x - radius, centre.y - 3.0, centre.z - radius, centre.x + radius, centre.y + 6.0, centre.z + radius);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box)) if (isFoe(e) && e.distanceToSqr(centre.x, e.getY(), centre.z) <= radius * radius) out.add(e);
        return out;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return dpsBudget.hurt(this, source, amount, x -> super.hurt(source, x));            // /kill and the void are capped by the budget too
        if (fightEnded || state == ST_TRANSFORM) return false;
        Entity attacker = source.getEntity();
        if (attacker instanceof CalamityBoss || (attacker != null && MobTouchHandler.isHelper(attacker))) return false;
        if (CalamityPhases.phaseFor(fraction(), thresholds()) > phase()) return false;                                   // waiting for the transformation
        float a = amount * damageTakenFactor(source);
        if (staggered()) a *= 1.3f;
        if (!(a > 0f)) return false;
        ServerPlayer who = creditedPlayer(attacker);
        float before = getHealth();
        boolean r = dpsBudget.hurt(this, source, a, x -> super.hurt(source, x));
        float dealt = before - getHealth();
        if (dealt > 0f && who != null) {
            fighters.merge(who.getUUID(), dealt, Float::sum);
            CalamityManager.fire(new CalamityDamageEvent(calamityId(), this, who, dealt, true, phase(), fightTicks()));
        }
        return r;
    }

    /** The health as a fraction of the maximum, in double so that a health on a line compares equal to the line. */
    private double fraction() { return (double) getHealth() / (double) getMaxHealth(); }

    /**
     * The phase guard: the damage that will really come off the health (after armor and the other reductions, the last step before the health is lowered) is cut so that the health
     * stops ON the next line. Done here and not on the raw blow because armor would leave the boss a little above its line after every cut and it would creep toward it forever.
     * Sources that bypass invulnerability (/kill, the void) are not cut: the damage budget already spreads them over seconds, and an operator must be able to remove a boss.
     */
    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float amount) {
        float a = super.getDamageAfterMagicAbsorb(source, amount);
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return a;
        return CalamityPhases.clampDamage(getHealth(), getMaxHealth(), phase(), thresholds(), a);
    }

    private static ServerPlayer creditedPlayer(Entity attacker) {
        if (attacker instanceof ServerPlayer p) return p;
        if (attacker instanceof net.minecraft.world.entity.projectile.Projectile pr && pr.getOwner() instanceof ServerPlayer p) return p;
        if (attacker instanceof OwnableEntity o && o.getOwner() instanceof ServerPlayer p) return p;
        return null;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide || fightEnded || !isDeadOrDying()) return;
        fightEnded = true;
        state = ST_IDLE;
        setShield(false);
        killHelpers();
        bar.setProgress(0f);
        CalamityManager.bossDefeated(this, source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        CalamityManager.dropLoot(this, level);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            BossBarSync.remove(bar);
            bar.removeAllPlayers();
            if (reason.shouldDestroy()) CalamityManager.bossGone(this, reason, fightEnded);
        }
        super.remove(reason);
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    @Override public boolean isAlliedTo(Entity other) { return other instanceof CalamityBoss || MobTouchHandler.isHelper(other) || super.isAlliedTo(other); }

    @Override public boolean canBeAffected(MobEffectInstance effect) { return !effect.is(MobEffects.WITHER) && !effect.is(MobEffects.POISON) && super.canBeAffected(effect); }

    @Override public boolean fireImmune() { return true; }

    // ================================================================ the bar

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.removePlayer(p);
    }

    private void refreshBar() {
        bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0f, 1f));
        int p = phase();
        if (barPhase != p || !barPrimed) {
            barPhase = p;
            barPrimed = true;
            CalamityDef d = def();
            Component roman = Component.translatable("nusmp.elite.phase." + (p + 1));
            bar.setName(Component.translatable("nusmp.elite.calamity.bar", getDisplayName(), roman));
            bar.setColor(com.newuniverse.nusmp.arc.ArcBoss.nearestBar(d == null ? 0x8B7B5A : d.color()));
        }
        CalamityDef d = def();
        BossBarSync.update(this, bar, BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath(), d == null ? 0x8B7B5A : d.color(), d == null ? 0xD8C9A0 : d.accent(),
                Math.min(4, p + (p > 0 ? 1 : 0)), p >= thresholds().size());
    }

    // ================================================================ helpers

    /** Alive helpers of this boss. */
    public int helperCount() {
        if (!(level() instanceof ServerLevel sl)) return 0;
        helpers.removeIf(id -> {
            Entity e = sl.getEntity(id);
            return e == null || !e.isAlive();
        });
        return helpers.size();
    }

    /** Summons one helper mob at a position, marked as a calamity helper (it drops nothing and is not touched by the world's mob rules). Returns it or null. */
    public Mob summonHelper(EntityType<? extends Mob> type, Vec3 at) {
        if (!(level() instanceof ServerLevel sl)) return null;
        Mob m = type.create(sl);
        if (m == null) return null;
        m.moveTo(at.x, at.y, at.z, random.nextFloat() * 360f, 0f);
        m.getPersistentData().putBoolean(MobTouchHandler.ADD, true);
        m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
        m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60 * 30, 0, false, false));
        m.setPersistenceRequired();
        if (getTarget() != null) m.setTarget(getTarget());
        sl.addFreshEntity(m);
        helpers.add(m.getUUID());
        RukhFx.burst(sl, at.add(0, 0.5, 0), rukh(), 8);
        return m;
    }

    public void killHelpers() {
        if (!(level() instanceof ServerLevel sl)) return;
        for (UUID id : new ArrayList<>(helpers)) {
            Entity e = sl.getEntity(id);
            if (e != null && e.isAlive()) e.hurt(damageSources().genericKill(), Float.MAX_VALUE);
        }
        helpers.clear();
    }

    /** A free standing spot on the ground at roughly this horizontal offset from a centre, or the centre's ground when none is found. */
    public Vec3 groundNear(Vec3 centre, double radius) {
        for (int i = 0; i < 8; i++) {
            double a = rng.nextDouble() * Math.PI * 2, r = radius * (0.5 + 0.5 * rng.nextDouble());
            Vec3 p = ground(centre.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            if (p != null) return p;
        }
        Vec3 g = ground(centre);
        return g == null ? centre : g;
    }

    /** The ground under a point (searching 6 blocks up and 10 down), or null in the air or in a wall. */
    public Vec3 ground(Vec3 p) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(Mth.floor(p.x), Mth.floor(p.y) + 6, Mth.floor(p.z));
        for (int i = 0; i < 16; i++) {
            if (!level().getBlockState(m).getCollisionShape(level(), m).isEmpty()) {
                BlockPos above = m.above();
                if (level().getBlockState(above).getCollisionShape(level(), above).isEmpty()) return new Vec3(p.x, above.getY(), p.z);
                return null;
            }
            m.move(0, -1, 0);
        }
        return null;
    }

    // ================================================================ save

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CalId", calamityId());
        tag.putInt("CalPhase", phase());
        tag.putBoolean("CalShield", shielded());
        tag.putLong("CalFought", fightTicks());
        tag.putFloat("CalBaseHp", baseHealth);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        dpsBudget.bypass(() -> super.readAdditionalSaveData(tag));                    // loading the saved health is not damage
        calamityId = tag.getString("CalId");
        entityData.set(PHASE, Math.max(0, tag.getInt("CalPhase")));
        entityData.set(SHIELD, tag.getBoolean("CalShield"));
        baseHealth = tag.contains("CalBaseHp") ? tag.getFloat("CalBaseHp") : -1f;
        riseTick = 0L;
        long fought = tag.getLong("CalFought");
        pendingFought = fought;
        bindDef();
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    private long pendingFought;
}
