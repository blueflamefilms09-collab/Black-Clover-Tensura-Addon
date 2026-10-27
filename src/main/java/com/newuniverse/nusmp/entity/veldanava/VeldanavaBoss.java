package com.newuniverse.nusmp.entity.veldanava;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.boss.budget.BossBudget;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.entity.zogratis.BlockLedger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Veldanava, the Creator God: one boss entity that wears three bodies ({@link VeldanavaRules#DRAGON}, {@link VeldanavaRules#HUMANOID}, {@link VeldanavaRules#AVATAR}),
 * drawn by client/veldanava/VeldanavaRenderer from the state this class keeps in its synced data.
 * <ul>
 *   <li><b>Dragon</b>: anchored to where he was summoned, huge area moves ({@link VeldanavaMoves}). A blow that would kill the dragon never does: the health stops at 1 and he changes
 *       body (4 seconds, invulnerable, the hitbox changes in the middle) into the Creator God, with a second full health bar.</li>
 *   <li><b>Creator God</b>: slim and fast (God Speed, Genesis Slash, Michael's guard, Uriel's prison, Law Manipulation). At the last 10% of this bar the Limiter is released and he
 *       changes again into the Dark Avatar of God (harder, quicker, with Genesis: Creation of the World).</li>
 *   <li><b>Soul Seizing</b>: in his last two bodies a blow that would kill a player takes their soul instead ({@link VeldanavaSoul}); they return when the fight ends.</li>
 *   <li><b>Death</b>: the Will of the Creator rises ({@link WillOfTheCreator}) and the fighters choose their boon; the loot comes from it.</li>
 * </ul>
 * <p>3.30.0 (the anime Veldanava, unless {@link VeldanavaConfig#classic()}): {@link VeldanavaStats} gives him god-tier attributes (one-shot blow, flash-step speed, armour 30, no knockback) and
 * five million health ({@link HealthCap} lifts vanilla's 1024 cap), he is immune to fall, fire, lava, the wither and every potion or status effect, he heals at ultra speed while he fights until a
 * hit of the counter-element ({@code #nusmp:veldanava_counter}) breaks it, his boss bar changes and pulses with his body and state ({@link VeldanavaBar}), and he has the seven Tensura-themed
 * moves of {@link VeldanavaMoves}.
 * All of it runs on the server. The damage he takes goes through the per-second boss budget, everything he changes in the world goes through a {@link BlockLedger} (only when the
 * griefBlocks rule is on, and all of it is put back), and the cost of every tick is bounded by the constants of {@link VeldanavaRules}.
 */
public final class VeldanavaBoss extends Monster implements BudgetedBoss {
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(VeldanavaBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(VeldanavaBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CASTING = SynchedEntityData.defineId(VeldanavaBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WIND_LEFT = SynchedEntityData.defineId(VeldanavaBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WIND_TOTAL = SynchedEntityData.defineId(VeldanavaBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GUARD = SynchedEntityData.defineId(VeldanavaBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EXPOSED = SynchedEntityData.defineId(VeldanavaBoss.class, EntityDataSerializers.BOOLEAN);

    /** Damage taken while exposed after an ultimate. */
    public static final float EXPOSED_BONUS = 1.25f;
    private static final int MAX_FIGHTERS_TRACKED = 64, ABANDON_SECONDS = 12;

    private final BossBudget dpsBudget = new BossBudget();
    private final ServerBossEvent bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> fighters = new LinkedHashSet<>();
    private final BlockLedger ledger = new BlockLedger(this::serverLevel);
    private final long[] readyAt = new long[VeldanavaRules.MOVES];
    private final Map<UUID, Long> lastHit = new HashMap<>();
    private BlockPos arena;
    private boolean initialised, fightActive, fightEnded, pendingDragonEnd, pendingLimiter;
    private int transformLeft, transformTo, castTick, blockBudget, fxBudget, emptySeconds;
    private long nextThink, recoveryUntil, nextMelee, guardUntil, exposedUntil, nextRestore;
    private VeldanavaRules.Spec casting;
    private UUID castTarget;
    private float castYaw;
    private int lastBarForm = -1;
    private Vec3 castPoint;
    private int castVariant;
    private final List<Vec3> marks = new ArrayList<>();
    /** The damage types that break his regeneration (data tag, so a pack can add to it). */
    public static final TagKey<DamageType> COUNTER = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "veldanava_counter"));
    private long regenBrokenUntil;
    private double wantedHealth;
    private long lastBarTick = -1;

    public VeldanavaBoss(EntityType<? extends VeldanavaBoss> type, Level level) {
        super(type, level);
        this.xpReward = 3000;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        HealthCap.raise();                                                                  // 3.30.0: five million health needs vanilla's 1024 cap lifted (before any entity is made)
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, VeldanavaStats.HEALTH).add(Attributes.ATTACK_DAMAGE, VeldanavaStats.ATTACK_DAMAGE).add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.MOVEMENT_SPEED, VeldanavaStats.MOVEMENT_SPEED).add(Attributes.FOLLOW_RANGE, VeldanavaStats.FOLLOW_RANGE).add(Attributes.ARMOR, VeldanavaStats.ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, VeldanavaStats.ARMOR_TOUGHNESS).add(Attributes.KNOCKBACK_RESISTANCE, VeldanavaStats.KNOCKBACK_RESISTANCE).add(Attributes.STEP_HEIGHT, 1.0);
    }

    /** True = the anime Veldanava of 3.30.0; false = the classic one of 3.11.0 - 3.29.0 (the server's classicMode setting). */
    private static boolean anime() { return !VeldanavaConfig.classic(); }

    /** The damage divisor of a pool the attribute could not hold (1 when it holds it all, which is the normal case). */
    public double healthDivisor() { return wantedHealth > 0.0 ? VeldanavaStats.healthDivisor(wantedHealth, getMaxHealth()) : 1.0; }

    /** The health the players are shown (the whole pool, even when the attribute is smaller). */
    public long shownHealth() { return VeldanavaStats.shown(getHealth(), healthDivisor()); }

    public long shownMaxHealth() { return VeldanavaStats.shown(getMaxHealth(), healthDivisor()); }

    // ================================================================ what the client reads
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FORM, VeldanavaRules.DRAGON);
        b.define(STATE, VeldanavaRules.STATE_IDLE);
        b.define(CASTING, -1);
        b.define(WIND_LEFT, 0);
        b.define(WIND_TOTAL, 1);
        b.define(GUARD, false);
        b.define(EXPOSED, false);
    }

    /** {@link VeldanavaRules#DRAGON}, {@link VeldanavaRules#HUMANOID} or {@link VeldanavaRules#AVATAR}. */
    public int form() { return VeldanavaRules.clampForm(entityData.get(FORM)); }

    public int state() { return entityData.get(STATE); }

    public boolean transforming() { return state() == VeldanavaRules.STATE_TRANSFORMING; }

    /** Id of the move being wound up or cast, or -1. */
    public int castingMove() { return entityData.get(CASTING); }

    /** 0..1 of the wind-up of the move (or of the change of body while transforming). */
    public float phaseProgress() {
        int total = Math.max(1, entityData.get(WIND_TOTAL));
        return Mth.clamp(1f - entityData.get(WIND_LEFT) / (float) total, 0f, 1f);
    }

    public boolean guarding() { return entityData.get(GUARD); }

    /** Between an ultimate and the end of its recovery: he takes extra damage. */
    public boolean exposed() { return entityData.get(EXPOSED); }

    private void setState(int s) { if (entityData.get(STATE) != s) entityData.set(STATE, s); }

    private void setForm(int f) { if (entityData.get(FORM) != f) entityData.set(FORM, f); }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        int f = entityData == null ? VeldanavaRules.DRAGON : form();
        return EntityDimensions.scalable(VeldanavaRules.width(f), VeldanavaRules.height(f));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key)) refreshDimensions();
    }

    // ================================================================ goals: only the targeting; the moves and the walking are his own
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ================================================================ the damage budget (docs/boss_dps_budget.md)
    @Override public BossBudget dpsBudget() { return dpsBudget; }

    @Override public int budgetStage() { return form(); }

    @Override public int budgetFinalStage() { return VeldanavaRules.AVATAR; }

    @Override
    public void baseTick() {
        super.baseTick();
        dpsBudget.tick(this, this);
    }

    /**
     * Every fall of health is cut to the budget, and the last blow never kills the dragon or the Creator God: the health stops at 1 and the next AI tick changes the body
     * (the dragon into the Creator God with a full bar, the Creator God into the Dark Avatar). Heals and his own restores pass.
     */
    @Override
    public void setHealth(float health) {
        if (dpsBudget == null) { super.setHealth(health); return; }
        float h = dpsBudget.setHealth(this, health);
        if (h <= 0f && !fightEnded && !level().isClientSide && entityData != null) {
            int f = form();
            if (f == VeldanavaRules.DRAGON) { pendingDragonEnd = true; h = 1f; }
            else if (f == VeldanavaRules.HUMANOID) { pendingLimiter = true; h = 1f; }
        }
        super.setHealth(h);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return dpsBudget.hurt(this, source, VeldanavaStats.scaledDamage(amount, healthDivisor()), x -> super.hurt(source, x));     // /kill and the void are capped too
        if (fightEnded || transformLeft > 0 || pendingDragonEnd || pendingLimiter) return false;
        if (anime() && nullified(source)) return false;                                      // Thermal Fluctuation Resistance, Fall nullification, the wither
        Entity attacker = source.getEntity();
        if (VeldanavaEntities.isOwn(attacker)) return false;
        if (guarding()) {                                                                  // Justice King Michael: nothing gets through and projectiles go home
            if (source.getDirectEntity() instanceof Projectile pr) {
                pr.setOwner(this);
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.0));
                pr.hurtMarked = true;
            }
            return false;
        }
        noteFighter(attacker);
        if (anime() && source.is(COUNTER)) regenBrokenUntil = VeldanavaStats.breakRegen(level().getGameTime(), regenBrokenUntil);     // the counter-element stops Ultraspeed Regeneration
        float a = amount;
        if (exposed()) a *= EXPOSED_BONUS;
        a = VeldanavaStats.scaledDamage(a, healthDivisor());
        return a > 0f && dpsBudget.hurt(this, source, a, x -> super.hurt(source, x));
    }

    /** Fall, every kind of fire and lava, and the wither never hurt the Creator. */
    private static boolean nullified(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FALL)) return true;
        return source.typeHolder().unwrapKey().map(k -> VeldanavaStats.nullifiesDamage(k.location().getPath())).orElse(false);
    }

    /** Is his Ultraspeed Regeneration broken by a counter hit right now? */
    public boolean regenBroken() { return level().getGameTime() < regenBrokenUntil; }

    private void noteFighter(Entity attacker) {
        if (fighters.size() >= MAX_FIGHTERS_TRACKED) return;
        if (attacker instanceof ServerPlayer p) fighters.add(p.getUUID());
        else if (attacker instanceof OwnableEntity o && o.getOwnerUUID() != null) fighters.add(o.getOwnerUUID());
    }

    public Set<UUID> fighters() { return fighters; }

    // ================================================================ small things he is
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean isAlliedTo(Entity other) { return VeldanavaEntities.isOwn(other) || super.isAlliedTo(other); }

    /** Thermal Fluctuation Resistance: fire and lava cannot burn him (anime mode). */
    @Override public boolean fireImmune() { return anime() || super.fireImmune(); }

    /**
     * Anime mode (Status Effect Nullification): no potion or status effect lands on him at all (poison, wither, slowness, ...). Classic mode: hard control never lands on the Creator
     * (paralysis, stun, petrification, sleep, fear, freezing, restraint, stasis).
     */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (anime()) return false;
        if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            String path = effect.getEffect().unwrapKey().map(k -> k.location().getPath()).orElse("");
            for (String hard : new String[]{"paraly", "stun", "petrif", "sleep", "fear", "freez", "restrain", "stasis", "time_stop"})
                if (path.contains(hard)) return false;
        }
        return super.canBeAffected(effect);
    }

    // ================================================================ the arena
    public void setArena(BlockPos p) { arena = p.immutable(); }

    public BlockPos arena() { return arena == null ? blockPosition() : arena; }

    public Vec3 arenaCenter() { return Vec3.atBottomCenterOf(arena()); }

    // ================================================================ every server tick
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl) || isDeadOrDying()) return;
        long t = sl.getGameTime();
        if (!initialised) initialise(sl);
        blockBudget = VeldanavaRules.BLOCKS_PER_TICK;
        fxBudget = VeldanavaRules.FX_PER_TICK;
        if (!fightActive && !ledger.isEmpty() && t >= nextRestore) {
            int before = ledger.size();
            ledger.restoreStep(sl, 160);
            if (ledger.size() >= before) nextRestore = t + 40;
        }
        bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0f, 1f));
        if (anime()) {
            if (lastBarForm != form() || t - lastBarTick >= VeldanavaBar.REFRESH) { lastBarForm = form(); lastBarTick = t; animateBar(t); }
        } else if (lastBarForm != form()) { lastBarForm = form(); refreshBar(); }
        if (entityData.get(GUARD) != (t < guardUntil)) entityData.set(GUARD, t < guardUntil);
        boolean ex = t < exposedUntil;
        if (entityData.get(EXPOSED) != ex) entityData.set(EXPOSED, ex);

        if (pendingDragonEnd) { pendingDragonEnd = false; beginTransform(sl, VeldanavaRules.HUMANOID); }
        else if (pendingLimiter || VeldanavaRules.limiterDue(form(), getHealth(), getMaxHealth())) { pendingLimiter = false; beginTransform(sl, VeldanavaRules.AVATAR); }
        if (transformLeft > 0) { tickTransform(sl); return; }
        if (anime()) {
            if (t % VeldanavaStats.REGEN_EVERY == 0) regenerate(t);
            if (t % 20 == 0 && !getActiveEffects().isEmpty()) removeAllEffects();            // Status Effect Nullification also clears what another mod forced on him
        }
        if (t % 20 == 0) upkeep(sl, t);

        LivingEntity target = getTarget();
        boolean valid = target != null && target.isAlive();
        if (!valid) {
            if (casting != null) endCast(t, true);
            if (state() != VeldanavaRules.STATE_IDLE && !fightActive) setState(VeldanavaRules.STATE_IDLE);
            return;
        }
        if (!fightActive) startFight(sl, t);
        if (casting != null) {
            tickCast(sl, target, t);
        } else if (t >= recoveryUntil) {
            if (state() != VeldanavaRules.STATE_COMBAT) setState(VeldanavaRules.STATE_COMBAT);
            faceTarget(target, form() == VeldanavaRules.DRAGON ? 5f : 40f);
            if (t >= nextThink) think(sl, target, t);
            melee(sl, target, t);
        } else {
            faceTarget(target, form() == VeldanavaRules.DRAGON ? 5f : 40f);
        }
    }

    private void initialise(ServerLevel sl) {
        initialised = true;
        if (arena == null) arena = blockPosition();
        lastBarForm = -1;
        refreshBar();
    }

    private void startFight(ServerLevel sl, long t) {
        fightActive = true;
        setState(VeldanavaRules.STATE_COMBAT);
        recoveryUntil = t + 40;
        say(form() == VeldanavaRules.DRAGON ? "Mortals... you stand before the Star King." : "So you are the ones who made it this far.");
    }

    /** Once a second: the fighters, the leash, the abandon rule. */
    private void upkeep(ServerLevel sl, long t) {
        Vec3 c = arenaCenter();
        int near = 0;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            double d2 = p.distanceToSqr(c);
            if (d2 < 100.0 * 100.0) near++;
            if (fighters.size() < MAX_FIGHTERS_TRACKED && p.distanceToSqr(this) < 48.0 * 48.0) fighters.add(p.getUUID());
        }
        if (fightActive) {
            emptySeconds = near == 0 ? emptySeconds + 1 : 0;
            if (emptySeconds >= ABANDON_SECONDS) { resetFight(sl); return; }
        }
        if (distanceToSqr(c) > (double) VeldanavaRules.ARENA_RADIUS * VeldanavaRules.ARENA_RADIUS) {
            Vec3 to = ground(sl, c);
            teleportTo(to.x, to.y, to.z);
            getNavigation().stop();
        }
        lastHit.values().removeIf(v -> v < t - 200);
    }

    /** Nobody is left: the fight starts over (full health in the body he has, blocks back, souls back). */
    private void resetFight(ServerLevel sl) {
        fightActive = false;
        emptySeconds = 0;
        if (casting != null) endCast(sl.getGameTime(), true);
        ledger.restoreAll(sl);
        VeldanavaSoul.release(sl.getServer(), getUUID());
        dpsBudget.bypass(() -> setHealth(getMaxHealth()));
        setTarget(null);
        guardUntil = 0;
        exposedUntil = 0;
        setState(VeldanavaRules.STATE_IDLE);
        fighters.clear();
    }

    // ================================================================ choosing and casting moves
    private void think(ServerLevel sl, LivingEntity target, long t) {
        nextThink = t + 4;
        double dist = Math.sqrt(distanceToSqr(target)) - getBbWidth() * 0.5;
        if (!anime()) for (int i = VeldanavaRules.CLASSIC_MOVES; i < VeldanavaRules.MOVES; i++) readyAt[i] = Long.MAX_VALUE;     // the classic Veldanava has only his eleven old moves
        int id = VeldanavaRules.choose(form(), Math.max(0.0, dist), readyAt, t, bound -> getRandom().nextInt(bound));
        if (id >= 0) { startCast(sl, VeldanavaRules.spec(id), target, t); return; }
        if (form() != VeldanavaRules.DRAGON && dist > 2.6 && t % 8 == 0) getNavigation().moveTo(target, 1.4);
    }

    private void startCast(ServerLevel sl, VeldanavaRules.Spec spec, LivingEntity target, long t) {
        casting = spec;
        castTick = 0;
        castTarget = target.getUUID();
        castYaw = yawTo(target.position());
        readyAt[spec.id()] = t + VeldanavaRules.cooldown(spec, form());
        getNavigation().stop();
        entityData.set(CASTING, spec.id());
        entityData.set(WIND_TOTAL, Math.max(1, spec.windup()));
        entityData.set(WIND_LEFT, spec.windup());
        setState(VeldanavaRules.STATE_CASTING);
        VeldanavaMoves.begin(this, spec, sl);
    }

    private void tickCast(ServerLevel sl, LivingEntity target, long t) {
        castTick++;
        int left = Math.max(0, casting.windup() - castTick);
        if (entityData.get(WIND_LEFT) != left) entityData.set(WIND_LEFT, left);
        VeldanavaMoves.tick(this, casting, castTick, sl);
        if (castTick >= casting.length()) endCast(t, false);
    }

    private void endCast(long t, boolean cancelled) {
        VeldanavaRules.Spec s = casting;
        casting = null;
        castTick = 0;
        castTarget = null;
        castPoint = null;
        castVariant = 0;
        marks.clear();
        entityData.set(CASTING, -1);
        entityData.set(WIND_LEFT, 0);
        if (s != null && s.id() == VeldanavaRules.SERAPH_GUARD) guardUntil = 0;
        if (s != null && s.ultimate() && !cancelled) exposedUntil = t + 60;
        recoveryUntil = t + VeldanavaRules.gap(form());
        if (state() == VeldanavaRules.STATE_CASTING) setState(VeldanavaRules.STATE_COMBAT);
    }

    /** The plain blow of the two human bodies (the dragon has the Bite move instead). */
    private void melee(ServerLevel sl, LivingEntity target, long t) {
        if (form() == VeldanavaRules.DRAGON || t < nextMelee) return;
        double reach = 2.6 + target.getBbWidth() * 0.5;
        if (distanceToSqr(target) > reach * reach) return;
        nextMelee = t + 14;
        swing(InteractionHand.MAIN_HAND, true);
        strike(target, anime() ? VeldanavaStats.meleeDamage(getAttributeValue(Attributes.ATTACK_DAMAGE), damageScale())
                : VeldanavaRules.damage(target.getMaxHealth(), 0.08f, 6f, 24f, damageScale()));
    }

    /** Ultraspeed Regeneration: a small heal every {@link VeldanavaStats#REGEN_EVERY} ticks while he fights, unless a counter hit broke it. Goes around the boss heal guard and the damage budget (his own health). */
    private void regenerate(long t) {
        if (pendingDragonEnd || pendingLimiter || !VeldanavaStats.regenerating(fightActive, transformLeft > 0, t, regenBrokenUntil, getHealth(), getMaxHealth())) return;
        float heal = VeldanavaStats.scaledDamage(VeldanavaStats.regenStep(getMaxHealth() * healthDivisor(), VeldanavaConfig.regenPercent()), healthDivisor());
        if (heal <= 0f) return;
        dpsBudget.bypass(() -> setHealth(Math.min(getMaxHealth(), getHealth() + heal)));
    }

    // ================================================================ what the moves use
    public ServerLevel sl() { return (ServerLevel) level(); }

    /** The server level, or null on a client (what the block ledger asks). */
    private ServerLevel serverLevel() { return level() instanceof ServerLevel s ? s : null; }

    public float damageScale() { return VeldanavaConfig.damageScale() * VeldanavaRules.damageScale(form()); }

    public UUID castTargetId() { return castTarget; }

    public float castYaw() { return castYaw; }

    public void setCastYaw(float yaw) { castYaw = yaw; }

    /** A point the running move fixed at its start (a prison's middle, a dash's goal), or null. */
    public Vec3 castPoint() { return castPoint; }

    public void setCastPoint(Vec3 p) { castPoint = p; }

    /** A small number the running move picked at its start (which law, which side). */
    public int castVariant() { return castVariant; }

    public void setCastVariant(int v) { castVariant = v; }

    /** The falling stars of a Starfall, at most {@link VeldanavaRules#MAX_MARKS}. */
    public List<Vec3> marks() { return marks; }

    public LivingEntity castTargetEntity() {
        if (castTarget == null || !(level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(castTarget);
        return e instanceof LivingEntity l && l.isAlive() ? l : getTarget();
    }

    /** Minecraft's yaw (degrees) from here toward a point. */
    public float yawTo(Vec3 p) { return (float) (Mth.atan2(p.z - getZ(), p.x - getX()) * (180.0 / Math.PI)) - 90f; }

    /** Turns the body (and the head) toward 'yaw' by at most 'rate' degrees. */
    public void turnTo(float yaw, float rate) {
        float y = Mth.approachDegrees(getYRot(), yaw, rate);
        setYRot(y);
        yBodyRot = y;
        yHeadRot = y;
    }

    private void faceTarget(LivingEntity target, float rate) { turnTo(yawTo(target.position()), rate); }

    /** The players he may hurt within 'radius' blocks of 'from' (alive, not creative, not spectating), at most {@link VeldanavaRules#MAX_TARGETS}. */
    public List<ServerPlayer> playersNear(Vec3 from, double radius) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl().players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator() || p.distanceToSqr(from) > radius * radius) continue;
            out.add(p);
            if (out.size() >= VeldanavaRules.MAX_TARGETS) break;
        }
        return out;
    }

    /** True once per 'gap' ticks for one creature (so a beam hurts each of them every few ticks, not every tick). */
    public boolean canHit(UUID id, long now, int gap) {
        Long last = lastHit.get(id);
        if (last != null && now - last < gap) return false;
        if (lastHit.size() >= 128) {
            for (Iterator<Map.Entry<UUID, Long>> it = lastHit.entrySet().iterator(); it.hasNext(); ) if (now - it.next().getValue() >= gap) it.remove();
            if (lastHit.size() >= 128) return false;
        }
        lastHit.put(id, now);
        return true;
    }

    /**
     * One blow of true damage (magic damage: armour does not reduce it, and the grace after a hit does not shield). In the two human bodies a blow that would kill a player
     * takes their soul instead (Soul Seizing: no Totem of Undying can answer it). Returns true when it hurt or seized.
     */
    public boolean strike(LivingEntity target, float amount) {
        if (!(level() instanceof ServerLevel sl) || amount <= 0f || !target.isAlive() || VeldanavaEntities.isOwn(target)) return false;
        if (target instanceof ServerPlayer p) {
            if (p.isCreative() || p.isSpectator()) return false;
            if (form() != VeldanavaRules.DRAGON && VeldanavaConfig.soulSeizing() && VeldanavaRules.isFatal(p.getHealth(), p.getAbsorptionAmount(), amount))
                return VeldanavaSoul.seize(p, getUUID());
        }
        target.invulnerableTime = 0;
        return target.hurt(sl.damageSources().indirectMagic(this, this), amount);
    }

    /** Sends one effect packet if this tick's budget has room. */
    public boolean fxBudget() {
        if (fxBudget <= 0) return false;
        fxBudget--;
        return true;
    }

    /** Removes one block of a beam's path (only with the griefBlocks rule on, a few per tick, every one recorded to be put back). */
    public boolean tryDestroy(BlockPos pos) {
        if (!(level() instanceof ServerLevel sl) || blockBudget <= 0 || ledger.isFull() || !NUGameRules.griefBlocks(sl)) return false;
        BlockPos p = pos.immutable();
        if (!sl.isInWorldBounds(p) || !sl.isLoaded(p)) return false;
        BlockState old = sl.getBlockState(p);
        if (old.isAir() || old.getDestroySpeed(sl, p) < 0f || old.getDestroySpeed(sl, p) > VeldanavaRules.MAX_DESTROY_SPEED || old.hasBlockEntity() || old.is(BlockTags.WITHER_IMMUNE)) return false;
        if (!sl.setBlock(p, Blocks.AIR.defaultBlockState(), 3)) return false;
        blockBudget--;
        ledger.record(p, old, Blocks.AIR.defaultBlockState());
        return true;
    }

    /** A safe blink: moves him to 'to' if his body fits there. */
    public boolean blinkTo(Vec3 to) {
        if (!(level() instanceof ServerLevel sl) || !sl.noCollision(this, getBoundingBox().move(to.subtract(position())))) return false;
        teleportTo(to.x, to.y, to.z);
        getNavigation().stop();
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.6f);
        return true;
    }

    public void startGuard(int ticks) { guardUntil = level().getGameTime() + ticks; }

    public void expose(int ticks) { exposedUntil = level().getGameTime() + ticks; }

    /**
     * 3.19.0 (Hercules' World-Breaking Strike): Michael's guard and Uriel's prison are shattered. The guard drops at once and the running move (Seraphim Guard, Uriel's Prison) is cancelled as if it were interrupted.
     * Returns true when one of the two was standing.
     */
    public boolean shatterSpatial() {
        if (!(level() instanceof ServerLevel sl)) return false;
        boolean broke = guarding() || guardUntil > sl.getGameTime();
        guardUntil = 0;
        if (entityData.get(GUARD)) entityData.set(GUARD, false);
        if (casting != null && (casting.id() == VeldanavaRules.URIEL || casting.id() == VeldanavaRules.SERAPH_GUARD)) {
            endCast(sl.getGameTime(), true);
            broke = true;
        }
        return broke;
    }

    /** The floor near 'p': the first spot with two blocks of air above solid ground, searching from 3 above to 6 below (p itself if none). */
    public static Vec3 ground(Level level, Vec3 p) {
        BlockPos base = BlockPos.containing(p);
        for (int dy = 3; dy >= -6; dy--) {
            BlockPos b = base.offset(0, dy, 0);
            if (level.getBlockState(b).isAir() && level.getBlockState(b.above()).isAir() && level.getBlockState(b.below()).blocksMotion()) return new Vec3(p.x, b.getY(), p.z);
        }
        return p;
    }

    /** One line to everybody within 100 blocks. */
    public void say(String line) {
        if (!(level() instanceof ServerLevel sl)) return;
        Component c = Component.literal("<Veldanava> " + line).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 100.0 * 100.0) p.sendSystemMessage(c);
    }

    // ================================================================ the change of body
    private void beginTransform(ServerLevel sl, int to) {
        if (transformLeft > 0 || fightEnded) return;
        if (casting != null) endCast(sl.getGameTime(), true);
        transformTo = to;
        transformLeft = VeldanavaRules.TRANSFORM_TICKS;
        guardUntil = 0;
        entityData.set(WIND_TOTAL, VeldanavaRules.TRANSFORM_TICKS);
        entityData.set(WIND_LEFT, VeldanavaRules.TRANSFORM_TICKS);
        setState(VeldanavaRules.STATE_TRANSFORMING);
        getNavigation().stop();
        say(to == VeldanavaRules.HUMANOID ? "The dragon was only a shape. Now see the Creator." : "Limiter released. Genesis begins.");
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4f, 0.5f);
        if (fxBudget()) com.newuniverse.nusmp.vfx.VfxSpawn.send(sl, com.newuniverse.nusmp.vfx.VfxShape.BORN_NOVA, position().add(0, 1, 0), position().add(0, 2, 0), 0xFF3EE0D0, 70, to == VeldanavaRules.HUMANOID ? 24f : 14f);
    }

    private void tickTransform(ServerLevel sl) {
        transformLeft--;
        int mid = VeldanavaRules.TRANSFORM_TICKS / 2;
        if (transformLeft == mid) {
            setForm(transformTo);
            if (transformTo == VeldanavaRules.HUMANOID) dpsBudget.bypass(() -> setHealth(getMaxHealth()));         // the second health bar
            if (fxBudget()) com.newuniverse.nusmp.vfx.VfxSpawn.send(sl, com.newuniverse.nusmp.vfx.VfxShape.LIGHT_FLARE, position().add(0, 1, 0), position().add(0, 2, 0), 0xFFFFFFFF, 30, 8f);
        }
        entityData.set(WIND_LEFT, transformLeft);
        if (transformLeft <= 0) {
            setState(VeldanavaRules.STATE_COMBAT);
            recoveryUntil = sl.getGameTime() + 30;
            Vec3 to = ground(sl, position());
            teleportTo(to.x, to.y, to.z);
        }
    }

    // ================================================================ boss bar
    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        refreshBar();
        bar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.removePlayer(p);
    }

    /** The animated bar of the anime Veldanava: his body's colour, overlay and title, a pulsing title, the health in numbers, and what he is doing. */
    private void animateBar(long t) {
        int f = form();
        VeldanavaBar.Style style = VeldanavaBar.style(f);
        boolean regenerating = VeldanavaStats.regenerating(fightActive, transformLeft > 0, t, regenBrokenUntil, getHealth(), getMaxHealth());
        int st = VeldanavaBar.state(fightEnded, transformLeft > 0, casting != null, entityData.get(GUARD), entityData.get(EXPOSED), fightActive && t < regenBrokenUntil, regenerating);
        float frac = Mth.clamp(getHealth() / Math.max(1f, getMaxHealth()), 0f, 1f);
        int rgb = VeldanavaBar.pulseColor(f, st, t, frac);
        String sym = VeldanavaBar.symbols(f, t);
        String status = VeldanavaBar.status(st, casting == null ? "" : casting.label());
        MutableComponent name = Component.literal(sym + " Veldanava - " + style.title() + " " + sym).withStyle(Style.EMPTY.withColor(rgb).withBold(true));
        name.append(Component.literal("   " + VeldanavaBar.healthText(shownHealth(), shownMaxHealth())).withStyle(Style.EMPTY.withColor(0xFFFFFF).withBold(false)));
        if (!status.isEmpty()) name.append(Component.literal("   " + status).withStyle(Style.EMPTY.withColor(style.accent()).withBold(false).withItalic(true)));
        bar.setName(name);
        bar.setColor(BossEvent.BossBarColor.valueOf(VeldanavaBar.color(f, st, t)));
        bar.setOverlay(BossEvent.BossBarOverlay.valueOf(style.overlay()));
    }

    private void refreshBar() {
        if (anime()) { animateBar(level().getGameTime()); return; }
        int f = form();
        bar.setName(Component.literal("Veldanava - " + VeldanavaRules.formName(f)).withStyle(f == VeldanavaRules.DRAGON ? ChatFormatting.DARK_AQUA : f == VeldanavaRules.HUMANOID ? ChatFormatting.WHITE : ChatFormatting.LIGHT_PURPLE));
        bar.setColor(f == VeldanavaRules.DRAGON ? BossEvent.BossBarColor.BLUE : f == VeldanavaRules.HUMANOID ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.PURPLE);
    }

    // ================================================================ death, removal, save data
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl) || fightEnded || !dead) return;       // 'dead' stays false when another mod cancelled the death
        fightEnded = true;
        fightActive = false;
        if (casting != null) endCast(sl.getGameTime(), true);
        setState(VeldanavaRules.STATE_DEAD);
        bar.setProgress(0f);
        say("The world... goes on without me.");
        ledger.restoreAll(sl);
        VeldanavaSoul.release(sl.getServer(), getUUID());
        WillOfTheCreator.spawn(sl, position(), fighters);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            bar.removeAllPlayers();
            if (level() instanceof ServerLevel sl) {
                VeldanavaSoul.release(sl.getServer(), getUUID());
                if (reason.shouldDestroy()) ledger.restoreAll(sl);
            }
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VForm", form());
        tag.putBoolean("VFight", fightActive);
        tag.putBoolean("VPendingDragon", pendingDragonEnd);
        tag.putBoolean("VPendingLimiter", pendingLimiter);
        if (wantedHealth > 0.0) tag.putDouble("VWant", wantedHealth);
        if (arena != null) tag.put("VArena", NbtUtils.writeBlockPos(arena));
        CompoundTag l = new CompoundTag();
        ledger.save(l);
        tag.put("VLedger", l);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        dpsBudget.bypass(() -> super.readAdditionalSaveData(tag));         // loading the saved health is not damage
        entityData.set(FORM, VeldanavaRules.clampForm(tag.getInt("VForm")));
        fightActive = tag.getBoolean("VFight");
        pendingDragonEnd = tag.getBoolean("VPendingDragon");
        pendingLimiter = tag.getBoolean("VPendingLimiter");
        wantedHealth = tag.contains("VWant") ? tag.getDouble("VWant") : 0.0;
        NbtUtils.readBlockPos(tag, "VArena").ifPresent(pos -> arena = pos);
        if (tag.contains("VLedger")) ledger.load(tag.getCompound("VLedger"), level().registryAccess().lookupOrThrow(Registries.BLOCK));
        refreshDimensions();
    }

    // ================================================================ spawning
    /** The Veldanava of a level, or none (at most one fight per level). */
    public static boolean exists(ServerLevel sl) { return !sl.getEntities(VeldanavaEntities.VELDANAVA.get(), e -> true).isEmpty(); }

    /** Spawns him at 'at' with the arena there. Null if the level already has one, the server turned him off, or he could not be created. */
    public static VeldanavaBoss spawnAt(ServerLevel sl, Vec3 at, float yaw) {
        if (!VeldanavaConfig.enabled() || exists(sl)) return null;
        VeldanavaBoss boss = VeldanavaEntities.VELDANAVA.get().create(sl);
        if (boss == null) return null;
        boss.applyStats(VeldanavaConfig.classic());
        var hp = boss.getAttribute(Attributes.MAX_HEALTH);
        double wanted = VeldanavaConfig.bossHealth();
        if (hp != null) hp.setBaseValue(wanted);
        boss.wantedHealth = wanted;
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setArena(BlockPos.containing(at));
        boss.dpsBudget.bypass(() -> boss.setHealth(boss.getMaxHealth()));
        sl.addFreshEntity(boss);
        sl.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.5f);
        return boss;
    }

    /** Sets the attributes of the chosen mode (the classic numbers, or the anime ones the entity type already has). */
    private void applyStats(boolean classic) {
        setBase(Attributes.ATTACK_DAMAGE, classic ? VeldanavaStats.CLASSIC_ATTACK : VeldanavaStats.ATTACK_DAMAGE);
        setBase(Attributes.MOVEMENT_SPEED, classic ? VeldanavaStats.CLASSIC_SPEED : VeldanavaStats.MOVEMENT_SPEED);
        setBase(Attributes.ARMOR, classic ? VeldanavaStats.CLASSIC_ARMOR : VeldanavaStats.ARMOR);
        setBase(Attributes.ARMOR_TOUGHNESS, classic ? VeldanavaStats.CLASSIC_TOUGHNESS : VeldanavaStats.ARMOR_TOUGHNESS);
        setBase(Attributes.FOLLOW_RANGE, classic ? VeldanavaStats.CLASSIC_FOLLOW : VeldanavaStats.FOLLOW_RANGE);
    }

    private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        var inst = getAttribute(attribute);
        if (inst != null) inst.setBaseValue(value);
    }

    /** The key the unit-free self-test uses to read the move table. */
    public static String moveName(int id) { return VeldanavaRules.spec(id).name(); }

    @SuppressWarnings("unused")
    private static final String MODID = NUSMP.MODID;
}
