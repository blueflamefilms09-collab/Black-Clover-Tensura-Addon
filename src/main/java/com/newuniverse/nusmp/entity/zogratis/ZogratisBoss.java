package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.TensuraCaster;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The shared framework of the Zogratis family (Dante, Vanica, Zenon and Lucius), the rulers of the Spade Kingdom who fight with the
 * power of a highest-ranking devil. A boss class supplies its name, devil, attributes and {@link #moves() move list}; this class
 * runs the fight:
 * <ul>
 *   <li><b>Percentage Power System</b> ({@link PowerCurve}): it opens at 20% of its devil's power, draws 40, 60 and 80% as its health
 *       falls, and at 25% the Gates of the Tree of Qliphoth open: 100%, a 60-tick invulnerable transformation to a new
 *       {@link #form() form}. From 60% the mana is unbound. {@link TriadBond}: when a sibling falls the others rage (+10, up to 80).</li>
 *   <li><b>Moves</b> ({@link Move}, {@link MoveScheduler}): every few ticks the boss asks the scheduler for a move that fits the
 *       distance, its power, its mana and the pacing rules; it stops, shows a telegraph for the move's wind-up (state
 *       {@link #STATE_CASTING}, {@link #castingMove()}), runs it, and recovers (an ultimate leaves it exposed for 3 s).</li>
 *   <li><b>The arena</b>: where it was summoned. Fighters are the players who fight it; it leashes back, and when nobody is left
 *       the fight resets (full health, power 20%, blocks restored). Changes to the world go through {@link #tryChange} /
 *       {@link #enqueueBlockChange}: only when the griefBlocks rule is on, a few dozen per tick, all in the {@link BlockLedger},
 *       all put back when the fight ends.</li>
 *   <li><b>Dark Disciples</b> ({@link #summonDisciples}), a boss bar with the devil's power, chat lines, and everything synced
 *       through the entity's data for the client renderer.</li>
 * </ul>
 * Moves run on the server only. Subclasses override the hooks {@link #onPowerChanged}, {@link #onGatesOpened}, {@link #bossTick},
 * {@link #onFightStart}, {@link #onFightEnd}.
 */
public abstract class ZogratisBoss extends Monster {
    public static final int FORM_BASE = 0, FORM_HYBRID = 1, FORM_DIVINE = 2;
    public static final int STATE_IDLE = 0, STATE_COMBAT = 1, STATE_CASTING = 2, STATE_TRANSFORMING = 3, STATE_RECOVERING = 4, STATE_DEAD = 5;

    /** Shortest telegraph of any move that has one: nobody gets less than 8 ticks to react, whatever the power. */
    public static final int MIN_WINDUP = 8;
    /** Blocks the boss may change per tick, at most. */
    public static final int BLOCKS_PER_TICK = 48;
    /** The arena: a boss further than this from its centre is called back, players further than twice this are pulled in. */
    public static final int ARENA_RADIUS = 56;
    /** Damage taken while exposed after an ultimate. */
    public static final float EXPOSED_BONUS = 1.25f;

    private static final int MAX_PENDING_EDITS = 4096, EDITS_TRIED_PER_TICK = 256, ABANDON_SECONDS = 10;

    private static final EntityDataAccessor<Integer> POWER = SynchedEntityData.defineId(ZogratisBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(ZogratisBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(ZogratisBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CASTING = SynchedEntityData.defineId(ZogratisBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_LEFT = SynchedEntityData.defineId(ZogratisBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_TOTAL = SynchedEntityData.defineId(ZogratisBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(ZogratisBoss.class, EntityDataSerializers.INT);

    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "zogratis_power_speed");

    private record BlockEdit(BlockPos pos, BlockState to) {}

    private final ServerBossEvent bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> fighters = new HashSet<>();
    private final BlockLedger ledger = new BlockLedger(this::serverLevel);
    private final ArrayDeque<BlockEdit> pendingEdits = new ArrayDeque<>();
    private final MoveScheduler scheduler;
    private PowerCurve.Tuning tuning = PowerCurve.tuning(PowerCurve.OPENING);
    private final ManaPool mana = new ManaPool(tuning);
    private MoveBook<Move> book;
    private List<ResourceLocation> tensuraKit;

    private BlockPos arena;
    private boolean initialised, fightActive, fightEnded;
    private long nextThink, recoveryUntil, nextMelee, castEnd;
    private int transformLeft, blockBudget, emptySeconds;
    private Move casting;
    private UUID castTarget;
    private double bandLo = 1.5, bandHi = 4;

    protected ZogratisBoss(EntityType<? extends ZogratisBoss> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
        this.scheduler = new MoveScheduler(getRandom().nextLong());
        setPersistenceRequired();
    }

    /** Boss attributes with the framework's usual extras (follow range 48, full knockback resistance, step height 1). */
    public static AttributeSupplier.Builder bossAttributes(double maxHealth, double attackDamage, double movementSpeed, double armor, double toughness) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, maxHealth).add(Attributes.ATTACK_DAMAGE, attackDamage)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0).add(Attributes.MOVEMENT_SPEED, movementSpeed).add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, armor).add(Attributes.ARMOR_TOUGHNESS, toughness).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // ================================================================ what a boss class supplies
    /** The display name used in the boss bar and the chat ("Dante Zogratis"). */
    public abstract String bossName();

    /** The highest-ranking devil it is contracted to (Lucifero, Megicula, Beelzebub, Astaroth). */
    public abstract Devil devil();

    /**
     * Its moves. Called once, lazily; ids must be unique (a repeated id is dropped with a warning). Index in this list is what
     * {@link #castingMove()} reports to the client.
     */
    protected abstract List<Move> moves();

    // ---- hooks
    /** Power rose from 'old' to 'now' (a step of the curve, or Triad rage). Server side. */
    protected void onPowerChanged(int old, int now) {}
    /** The Gates of the Tree of Qliphoth have opened: the 60-tick transformation begins. Server side. */
    protected void onGatesOpened() {}
    /** Every server tick, after the framework's own work (including during the transformation). */
    protected void bossTick() {}
    /** The first target of a fight was acquired. */
    protected void onFightStart() {}
    /** The fight is over: won = the boss died; otherwise it was abandoned and reset. */
    protected void onFightEnd(boolean won) {}
    /** The form the Gates open into (the Triad become hybrids; Lucius overrides this with {@link #FORM_DIVINE}). */
    protected int gatesForm() { return FORM_HYBRID; }
    /** The model scale of a form (a pure function of the form: it is called while the entity is still being built). */
    protected float formScale(int form) { return form == FORM_HYBRID ? 1.12f : 1.0f; }
    /** Per-boss damage factor on top of the power tuning. */
    protected float damageScale() { return 1.0f; }
    /** Base damage of its plain melee blow. */
    protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE); }
    /** The distance it likes to keep, given the centre of its ready moves' ranges (override to kite or to brawl). */
    protected double preferredRange(double suggested) { return suggested; }
    /** Last look at incoming damage (after the invulnerability checks); return the amount to apply, 0 or less to ignore the hit. */
    protected float filterDamage(DamageSource source, float amount) { return amount; }
    /** Keywords of the Tensura magic skills it learns (empty = none); see {@link TensuraCaster#learn}. */
    protected String[] tensuraKeywords() { return new String[0]; }
    protected int tensuraKitSize() { return 4; }

    // ---- lines (overridable)
    protected String openingLine() { return "You stand before the chosen of " + devil().displayName + "."; }
    protected String powerLine(int old, int now) {
        return now >= PowerCurve.INFINITE_MANA_FROM && old < PowerCurve.INFINITE_MANA_FROM
                ? "I draw " + now + "% of " + devil().displayName + "'s power, and my mana knows no end."
                : "I draw " + now + "% of " + devil().displayName + "'s power.";
    }
    protected String gatesLine() { return "The Gates of the Tree of Qliphoth open. Behold " + devil().displayName + "'s true power!"; }
    protected String deathLine() { return "This cannot be the end..."; }

    // ================================================================ state for the client and the moves
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(POWER, PowerCurve.OPENING);
        b.define(FORM, FORM_BASE);
        b.define(STATE, STATE_IDLE);
        b.define(CASTING, -1);
        b.define(PHASE_LEFT, 0);
        b.define(PHASE_TOTAL, 1);
        b.define(TARGET, -1);
    }

    /** Percent of its devil's power it has drawn: 20 .. 80, 100 once the Gates are open (rage can give values like 50 or 70). */
    public int power() { return entityData.get(POWER); }
    public boolean gatesOpen() { return PowerCurve.gatesOpen(power()); }
    /** {@link #FORM_BASE}, {@link #FORM_HYBRID} or {@link #FORM_DIVINE}. */
    public int form() { return entityData.get(FORM); }
    /** The numbers for the current power (damage, cooldowns, disciples, mana ...). */
    public PowerCurve.Tuning tuning() { return tuning; }
    /** One of the STATE_ constants. */
    public int state() { return entityData.get(STATE); }
    /** Index in {@link #moves()} of the move being wound up, or -1. */
    public int castingMove() { return entityData.get(CASTING); }
    /** 0..1 progress of the wind-up (or of the transformation while {@link #STATE_TRANSFORMING}). */
    public float phaseProgress() {
        int total = Math.max(1, entityData.get(PHASE_TOTAL));
        return Math.max(0f, Math.min(1f, 1f - entityData.get(PHASE_LEFT) / (float) total));
    }
    public boolean transforming() { return state() == STATE_TRANSFORMING; }
    /** 0..1 over the transformation, 1 afterwards if the form has changed, 0 before. */
    public float transformProgress() { return transforming() ? phaseProgress() : form() != FORM_BASE ? 1f : 0f; }
    /** Between an ultimate and the end of its recovery: it takes extra damage. */
    public boolean exposed() { return state() == STATE_RECOVERING; }
    /** Entity id of its target (-1 none), synced so the client can aim tells and look. */
    public int targetId() { return entityData.get(TARGET); }
    public boolean fightActive() { return fightActive; }
    public ManaPool mana() { return mana; }
    public BlockLedger ledger() { return ledger; }

    /** Notes a block the boss has changed some other way (so the fight can undo it). False if the ledger is full. */
    public boolean recordBlock(BlockPos pos, BlockState old) { return ledger.record(pos, old); }

    /** Puts every recorded block back now. */
    public int restoreBlocks() { return ledger.restoreAll(); }

    /** The move at an index of the move list, or null. */
    public Move moveAt(int index) { return moveBook().get(index); }

    /** The index of a move id in the move list, or -1. */
    public int moveIndex(String id) { return moveBook().indexOf(id); }

    /** The move being wound up, or null. */
    public Move castingMoveOrNull() { return moveAt(castingMove()); }

    private MoveBook<Move> moveBook() {
        if (book == null) {
            book = MoveBook.of(moves());
            for (String d : book.dropped()) LogUtils.getLogger().warn("[nusmp] {}: duplicate or empty move dropped: {}", getClass().getSimpleName(), d);
        }
        return book;
    }

    private List<Move> book() { return moveBook().moves(); }

    private ServerLevel serverLevel() { return level() instanceof ServerLevel sl ? sl : null; }

    private void setState(int s) { if (entityData.get(STATE) != s) entityData.set(STATE, s); }

    protected void setForm(int form) {
        if (entityData.get(FORM) != form) entityData.set(FORM, form);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(formScale(entityData == null ? FORM_BASE : form()));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key)) refreshDimensions();
    }

    // ================================================================ goals
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Positioning(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Keeps the distance the ready moves want: closes in, backs off, or circles the target. Does nothing while it casts. */
    private static final class Positioning extends Goal {
        private final ZogratisBoss boss;
        private int repath, strafeFlip;
        private double strafeDir = 1;

        Positioning(ZogratisBoss boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = boss.getTarget();
            return t != null && t.isAlive() && boss.canWalk();
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity t = boss.getTarget();
            if (t == null) return;
            boss.getLookControl().setLookAt(t, 30f, 30f);
            if (--repath > 0) return;
            repath = 5;
            double d = boss.distanceTo(t);
            if (d > boss.bandHi) {
                boss.getNavigation().moveTo(t, 1.0);
            } else if (d < boss.bandLo) {
                Vec3 away = boss.position().subtract(t.position()).multiply(1, 0, 1);
                if (away.lengthSqr() < 1e-4) away = new Vec3(1, 0, 0);
                Vec3 to = boss.position().add(away.normalize().scale(6));
                boss.getNavigation().moveTo(to.x, to.y, to.z, 1.1);
            } else {
                if (--strafeFlip <= 0) { strafeFlip = 8 + boss.getRandom().nextInt(10); strafeDir = -strafeDir; }
                Vec3 off = boss.position().subtract(t.position()).multiply(1, 0, 1);
                if (off.lengthSqr() < 1e-4) off = new Vec3(1, 0, 0);
                double len = Math.max(boss.bandLo, Math.min(boss.bandHi, off.length()));
                Vec3 radial = off.normalize(), side = new Vec3(-radial.z, 0, radial.x).scale(strafeDir);
                Vec3 to = t.position().add(radial.scale(len)).add(side.scale(3.5));
                boss.getNavigation().moveTo(to.x, to.y, to.z, 0.8);
            }
        }
    }

    boolean canWalk() { return fightActive && casting == null && transformLeft <= 0 && state() != STATE_RECOVERING; }

    // ================================================================ boss bar and looks
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

    private BossEvent.BossBarColor barColor() {
        Devil d = devil();
        if (d == Devil.LUCIFERO) return BossEvent.BossBarColor.PURPLE;
        if (d == Devil.MEGICULA) return BossEvent.BossBarColor.RED;
        if (d == Devil.BEELZEBUB) return BossEvent.BossBarColor.BLUE;
        if (d == Devil.ASTAROTH) return BossEvent.BossBarColor.YELLOW;
        return BossEvent.BossBarColor.WHITE;
    }

    /** The chat colour of the devil. */
    protected ChatFormatting chatColor() {
        Devil d = devil();
        if (d == Devil.LUCIFERO) return ChatFormatting.DARK_PURPLE;
        if (d == Devil.MEGICULA) return ChatFormatting.DARK_RED;
        if (d == Devil.BEELZEBUB) return ChatFormatting.BLUE;
        if (d == Devil.ASTAROTH) return ChatFormatting.GOLD;
        return ChatFormatting.GRAY;
    }

    /** "Dante Zogratis - Lucifero 60%", and "- Gates open" once they are. */
    public String barTitle() {
        return bossName() + " - " + devil().displayName + " " + power() + "%" + (gatesOpen() ? " - Gates open" : "");
    }

    private void refreshBar() {
        bar.setName(Component.literal(barTitle()).withStyle(chatColor()));
        bar.setColor(barColor());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide || tickCount % 3 != 0 || power() < 40) return;
        var r = getRandom();
        int rgb = DevilStyle.color(devil());
        var dust = new DustParticleOptions(new Vector3f(DevilStyle.red(rgb), DevilStyle.green(rgb), DevilStyle.blue(rgb)), 1.2f);
        int n = state() == STATE_TRANSFORMING ? 6 : state() == STATE_CASTING ? 3 : 1;
        for (int i = 0; i < n; i++)
            level().addParticle(dust, getX() + (r.nextDouble() - 0.5) * getBbWidth() * 1.6, getY() + r.nextDouble() * getBbHeight(),
                    getZ() + (r.nextDouble() - 0.5) * getBbWidth() * 1.6, 0, 0.02, 0);
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean isAlliedTo(Entity other) { return ZogratisEntities.isFamily(other) || super.isAlliedTo(other); }

    /** Hard control never lands on a Zogratis (paralysis, stun, petrification, sleep, fear, freezing, restraint, stasis). */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            String path = effect.getEffect().unwrapKey().map(k -> k.location().getPath()).orElse("");
            for (String hard : new String[]{"paraly", "stun", "petrif", "sleep", "fear", "freez", "restrain", "stasis", "time_stop"})
                if (path.contains(hard)) return false;
        }
        return super.canBeAffected(effect);
    }

    // ================================================================ arena
    /** Where the arena is (its centre): where the boss was summoned. */
    public void setArena(BlockPos p) { arena = p.immutable(); }
    public BlockPos arena() { return arena == null ? blockPosition() : arena; }
    public Vec3 arenaCenter() { return Vec3.atBottomCenterOf(arena()); }

    // ================================================================ the fight, every tick
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl) || isDeadOrDying()) return;
        long t = tickCount;
        if (!initialised) initialise(sl, t);
        blockBudget = BLOCKS_PER_TICK;
        drainEdits();
        if (!fightActive && !ledger.isEmpty()) ledger.restoreStep(sl, 160);
        mana.tick();
        float frac = getHealth() / getMaxHealth();
        bar.setProgress(Math.max(0f, Math.min(1f, frac)));
        int np = PowerCurve.next(frac, power());
        if (np != power()) raisePower(sl, np, null);
        if (t % 20 == 0) upkeep(sl, t);
        if (transformLeft > 0) {
            tickTransform(sl);
            bossTick();
            return;
        }
        LivingEntity target = getTarget();
        boolean valid = target != null && target.isAlive();
        entityData.set(TARGET, valid ? target.getId() : -1);
        if (!valid) {
            if (casting != null) cancelCast();
            if (state() != STATE_RECOVERING || t >= recoveryUntil) setState(fightActive ? STATE_COMBAT : STATE_IDLE);
        } else {
            if (!fightActive) startFight(sl, t);
            if (casting != null) tickCast(sl, target, t);
            else if (t >= recoveryUntil) {
                if (state() == STATE_RECOVERING) setState(STATE_COMBAT);
                if (t >= nextThink) think(sl, target, t);
                melee(sl, target, t);
            }
        }
        bossTick();
    }

    private void initialise(ServerLevel sl, long t) {
        initialised = true;
        TriadBond.register(this);
        if (arena == null) arena = blockPosition();
        scheduler.reset(t);
        tuning = PowerCurve.tuning(power());
        mana.retune(tuning);
        applyTuning();
        refreshBar();
        if (gatesOpen() && form() == FORM_BASE) setForm(gatesForm());
        String[] kw = tensuraKeywords();
        if (kw.length > 0) {
            tensuraKit = TensuraCaster.learn(this, tensuraKitSize(), kw);
            TensuraCaster.ensureMana(this, 2_000_000);
        }
        book();
    }

    private void applyTuning() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrReplacePermanentModifier(new AttributeModifier(SPEED_ID, tuning.speedMultiplier() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private int thinkInterval() { return Math.max(2, (int) Math.round(5 * tuning.cooldownMultiplier())); }

    /** Once a second: regeneration, the fighters, the leash, the abandon rule. */
    private void upkeep(ServerLevel sl, long t) {
        if (getHealth() < getMaxHealth()) heal((float) (getMaxHealth() * tuning.regenFractionPerSecond()));
        if (tensuraKit != null && !tensuraKit.isEmpty() && t % 200 == 0) TensuraCaster.ensureMana(this, 2_000_000);
        Vec3 c = arenaCenter();
        int near = 0;
        for (ServerPlayer p : sl.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            double d2 = p.distanceToSqr(c);
            if (d2 < 100 * 100) near++;
            if (p.distanceToSqr(this) < 40 * 40) fighters.add(p.getUUID());
            if (fightActive && fighters.contains(p.getUUID()) && d2 > (2.0 * ARENA_RADIUS) * (2.0 * ARENA_RADIUS) && d2 < 200 * 200
                    && t >= p.getPersistentData().getLong("nusmp_zogratis_leash")) {
                p.getPersistentData().putLong("nusmp_zogratis_leash", t + 200);
                Vec3 to = ground(sl, c.add(p.position().subtract(c).multiply(1, 0, 1).normalize().scale(12)));
                say("Return.");
                p.teleportTo(to.x, to.y, to.z);
            }
        }
        if (fightActive) {
            emptySeconds = near == 0 ? emptySeconds + 1 : 0;
            if (emptySeconds >= ABANDON_SECONDS) { resetFight(sl); return; }
            LivingEntity tg = getTarget();
            if (tg != null && tg.distanceToSqr(c) > 90 * 90) setTarget(null);
        }
        if (distanceToSqr(c) > (double) ARENA_RADIUS * ARENA_RADIUS) {
            Vec3 home = ground(sl, c);
            VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, position().add(0, 1, 0), position().add(0, 2, 0), 0xFF000000 | DevilStyle.color(devil()), 14, 0.8f);
            teleportTo(home.x, home.y, home.z);
            getNavigation().stop();
        }
    }

    private void startFight(ServerLevel sl, long t) {
        fightActive = true;
        fightEnded = false;
        emptySeconds = 0;
        scheduler.reset(t);
        nextThink = t + 20;
        setState(STATE_COMBAT);
        say(openingLine());
        onFightStart();
    }

    /** Nobody is left: it heals, falls back to 20% power, forgets the fight, and the world is put back. */
    private void resetFight(ServerLevel sl) {
        fightActive = false;
        fighters.clear();
        cancelCast();
        pendingEdits.clear();
        removeDisciples(sl);
        transformLeft = 0;
        setTarget(null);
        entityData.set(POWER, PowerCurve.OPENING);
        entityData.set(FORM, FORM_BASE);
        tuning = PowerCurve.tuning(PowerCurve.OPENING);
        mana.retune(tuning);
        applyTuning();
        refreshBar();
        setHealth(getMaxHealth());
        scheduler.reset(tickCount);
        recoveryUntil = 0;
        setState(STATE_IDLE);
        Vec3 home = ground(sl, arenaCenter());
        teleportTo(home.x, home.y, home.z);
        getNavigation().stop();
        onFightEnd(false);
    }

    // ================================================================ power
    private void raisePower(ServerLevel sl, int to, String rageOf) {
        int old = power();
        if (to <= old) return;
        entityData.set(POWER, to);
        tuning = PowerCurve.tuning(to);
        mana.retune(tuning);
        applyTuning();
        refreshBar();
        if (rageOf != null) announce(rageOf + " has fallen. " + bossName() + " rages: " + old + "% -> " + to + "% of " + devil().displayName + "'s power.");
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, position(), position().add(0, 1, 0), 0xFF000000 | DevilStyle.color(devil()), 50, 2.2f);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.5f);
        if (to >= PowerCurve.FULL) beginGates(sl);
        else if (rageOf == null) say(powerLine(old, to));
        onPowerChanged(old, to);
    }

    /** The Dark Triad bond: a sibling fell, one rage step (+10, never past 80). Called by {@link TriadBond}. */
    public void enrage(String fallenName) {
        if (!(level() instanceof ServerLevel sl) || !isAlive() || isDeadOrDying()) return;
        int to = PowerCurve.rage(power());
        if (to > power()) raisePower(sl, to, fallenName);
        else announce(fallenName + " has fallen. " + bossName() + " is already at full fury.");
    }

    private void beginGates(ServerLevel sl) {
        transformLeft = PowerCurve.TRANSFORM_TICKS;
        cancelCast();
        setState(STATE_TRANSFORMING);
        entityData.set(PHASE_TOTAL, PowerCurve.TRANSFORM_TICKS);
        entityData.set(PHASE_LEFT, transformLeft);
        getNavigation().stop();
        announce("The Gates of the Tree of Qliphoth open!");
        say(gatesLine());
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, position(), position().add(0, 1, 0), 0xFF000000 | DevilStyle.color(devil()), PowerCurve.TRANSFORM_TICKS, 4f);
        sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.6f);
        onGatesOpened();
    }

    private void tickTransform(ServerLevel sl) {
        transformLeft--;
        entityData.set(PHASE_LEFT, Math.max(0, transformLeft));
        getNavigation().stop();
        int rgb = DevilStyle.color(devil());
        if (transformLeft % 6 == 0) Tells.ring(sl, position(), 2.5 + (PowerCurve.TRANSFORM_TICKS - transformLeft) * 0.08, rgb);
        if (transformLeft % 4 == 0) Tells.gather(sl, position().add(0, getBbHeight() / 2, 0), 1.2, DevilStyle.accent(devil()), 8);
        if (transformLeft == PowerCurve.TRANSFORM_TICKS / 2) {
            setForm(gatesForm());
            Tells.burst(sl, position().add(0, getBbHeight() / 2, 0), DevilStyle.accent(devil()), 60);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3f, 0.4f);
        }
        if (transformLeft <= 0) {
            setState(STATE_COMBAT);
            recoveryUntil = tickCount;
            nextThink = tickCount + 20;
        }
    }

    // ================================================================ moves
    private MoveScheduler.Situation situation(LivingEntity target, long t) {
        return new MoveScheduler.Situation(distanceTo(target), getHealth() / getMaxHealth(), power(), foes(32).size(), t, mana.available(), tuning.ultimateGapTicks());
    }

    private void think(ServerLevel sl, LivingEntity target, long t) {
        nextThink = t + thinkInterval();
        List<Move> bk = book();
        if (bk.isEmpty()) return;
        MoveScheduler.Situation s = situation(target, t);
        updateBand(bk, t);
        int idx = scheduler.pick(bk, s);
        if (idx >= 0) beginCast(sl, idx, bk.get(idx), target, s, t);
    }

    /** Where the boss wants to stand: the centre of the ranges of the moves that are ready (up close when none is). */
    private void updateBand(List<Move> bk, long t) {
        double sum = 0, weight = 0;
        for (Move m : bk) {
            if (m.ultimate() || m.minPower() > power() || scheduler.cooldownLeft(m.id(), t) > 0 || !mana.canAfford(m.manaCost())) continue;
            double mid = Math.max(2.5, Math.min(24, (m.minRange() + Math.min(m.maxRange(), 28)) / 2));
            sum += mid * m.weight();
            weight += m.weight();
        }
        double center = preferredRange(weight > 0 ? sum / weight : 2.5);
        bandLo = Math.max(1.5, center - 3);
        bandHi = Math.max(bandLo + 2, center + 3);
    }

    private int windUpOf(Move m) {
        return m.windUpTicks() <= 0 ? 0 : Math.max(MIN_WINDUP, Math.round(m.windUpTicks() * (float) tuning.windUpMultiplier()));
    }

    private void beginCast(ServerLevel sl, int idx, Move m, LivingEntity target, MoveScheduler.Situation s, long t) {
        scheduler.commit(m, s, Math.max(10, (int) Math.round(m.cooldownTicks() * tuning.cooldownMultiplier())));
        mana.spend(m.manaCost());
        int wind = windUpOf(m);
        casting = m;
        castTarget = target.getUUID();
        castEnd = t + wind;
        entityData.set(CASTING, idx);
        entityData.set(PHASE_TOTAL, Math.max(1, wind));
        entityData.set(PHASE_LEFT, wind);
        setState(STATE_CASTING);
        getNavigation().stop();
        if (m.ultimate()) for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 64 * 64)
            p.displayClientMessage(Component.literal(bossName() + ": " + m.label()).withStyle(chatColor(), ChatFormatting.BOLD), true);
        if (wind == 0) release(sl, target, t);
    }

    private void tickCast(ServerLevel sl, LivingEntity fallback, long t) {
        Move m = casting;
        LivingEntity target = castTarget != null && sl.getEntity(castTarget) instanceof LivingEntity le && le.isAlive() ? le : fallback;
        getNavigation().stop();
        getLookControl().setLookAt(target, 40f, 40f);
        int left = (int) Math.max(0, castEnd - t);
        entityData.set(PHASE_LEFT, left);
        if (left > 0 && left % 3 == 0) {
            try {
                if (m.tell() != null) m.tell().tell(this, target, left, entityData.get(PHASE_TOTAL));
                else defaultTell(sl, m, target, left);
            } catch (Throwable e) {
                LogUtils.getLogger().warn("[nusmp] {} tell of {} failed: {}", bossName(), m.id(), e.toString());
            }
        }
        if (left <= 0) release(sl, target, t);
    }

    /** The default tell: dust gathering at the boss, and a ring at the target's feet (round the boss for an ultimate). */
    protected void defaultTell(ServerLevel sl, Move m, LivingEntity target, int ticksLeft) {
        int rgb = DevilStyle.color(devil());
        Tells.gather(sl, position().add(0, getBbHeight() * 0.6, 0), 0.9, DevilStyle.accent(devil()), 4);
        if (m.ultimate()) Tells.ring(sl, position(), Math.min(14, 4 + (entityData.get(PHASE_TOTAL) - ticksLeft) * 0.2), rgb);
        else if (m.maxRange() > 8) Tells.ring(sl, target.position(), 1.5 + ticksLeft * 0.08, rgb);
        else Tells.ring(sl, position(), 2.2, rgb);
    }

    private void release(ServerLevel sl, LivingEntity target, long t) {
        Move m = casting;
        casting = null;
        castTarget = null;
        entityData.set(CASTING, -1);
        entityData.set(PHASE_LEFT, 0);
        if (m == null) return;
        try {
            m.run().run(this, target);
        } catch (Throwable e) {
            LogUtils.getLogger().warn("[nusmp] {} move {} failed: {}", bossName(), m.id(), e.toString());
        }
        recoveryUntil = t + m.recoveryTicks();
        nextThink = Math.max(nextThink, recoveryUntil);
        if (isAlive() && !transforming()) setState(m.recoveryTicks() >= Move.EXPOSED_FROM ? STATE_RECOVERING : STATE_COMBAT);
    }

    private void cancelCast() {
        casting = null;
        castTarget = null;
        castEnd = 0;
        entityData.set(CASTING, -1);
        entityData.set(PHASE_LEFT, 0);
        if (state() == STATE_CASTING) setState(STATE_COMBAT);
    }

    /** The plain blow, when nothing better is ready and the target is in reach. */
    private void melee(ServerLevel sl, LivingEntity target, long t) {
        if (casting != null || t < nextMelee || t < recoveryUntil) return;
        if (!getBoundingBox().inflate(0.8, 0.4, 0.8).intersects(target.getBoundingBox())) return;
        nextMelee = t + Math.max(10, Math.round(20 * (float) tuning.cooldownMultiplier()));
        swing(InteractionHand.MAIN_HAND);
        if (strike(target, meleeDamage(), false)) {
            Vec3 away = target.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) target.knockback(0.6, -away.x, -away.z);
        }
    }

    // ================================================================ helpers for moves
    /** True for the Zogratis family: the four bosses, Dark Disciples, Blood Beasts and clones. */
    public static boolean isFamily(Entity e) { return ZogratisEntities.isFamily(e); }

    /** Can this entity be the target of the boss's attacks? Players in survival, and the summons of players. */
    public boolean isFoe(Entity e) {
        if (!(e instanceof LivingEntity le) || e == this || !le.isAlive() || le.isSpectator() || ZogratisEntities.isFamily(e) || isAlliedTo(e)) return false;
        if (e instanceof Player p) return !p.isCreative();
        return e instanceof OwnableEntity o && o.getOwnerUUID() != null;
    }

    /** Foes within 'radius' of the boss: players in survival first, then the summons of players. */
    public List<LivingEntity> foes(double radius) { return foesAround(position(), radius); }

    /** Foes within 'radius' of a point: players in survival first, then the summons of players. */
    public List<LivingEntity> foesAround(Vec3 at, double radius) {
        double r2 = radius * radius;
        AABB box = new AABB(at, at).inflate(radius);
        List<LivingEntity> out = new ArrayList<>();
        for (Player p : level().getEntitiesOfClass(Player.class, box, pl -> isFoe(pl) && pl.distanceToSqr(at) <= r2)) out.add(p);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, le -> !(le instanceof Player) && isFoe(le) && le.distanceToSqr(at) <= r2)) out.add(e);
        return out;
    }

    /**
     * The final damage of a hit of 'base' on 'victim': base x the power's damage multiplier x {@link #damageScale} x a factor for the
     * victim's EP, then the balance law (a sliver of the victim's max health; a player takes at most the pvp cap per hit).
     */
    public float bossDamage(LivingEntity victim, float base) {
        return ZogratisDamage.amount(this, victim, base, tuning.damageMultiplier(), damageScale(), power() / 100.0);
    }

    /** Hurts 'victim' with {@link #bossDamage}: magic damage ignores armour, otherwise it is a mob attack. Returns whether it landed. */
    public boolean strike(LivingEntity victim, float base, boolean magic) {
        if (victim == null || !victim.isAlive() || ZogratisEntities.isFamily(victim)) return false;
        return ZogratisDamage.hit(this, victim, base, tuning.damageMultiplier(), damageScale(), power() / 100.0, magic);
    }

    /** A line of speech, shown in chat to everyone fighting it and everyone within 64 blocks. */
    public void say(String line) { broadcast(Component.literal(bossName() + ": \"" + line + "\"").withStyle(chatColor(), ChatFormatting.BOLD)); }

    /** A narration line (not spoken by the boss). */
    public void announce(String text) { broadcast(Component.literal(text).withStyle(chatColor())); }

    private void broadcast(Component c) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 64 * 64 || fighters.contains(p.getUUID())) p.sendSystemMessage(c);
    }

    /** Runs 'r' after 'ticks' ticks on the server, unless the boss is gone by then. */
    public void later(int ticks, Runnable r) {
        if (!(level() instanceof ServerLevel sl)) return;
        SpellRuntime.later(sl, Math.max(0, ticks), () -> { if (isAlive() && !isRemoved()) r.run(); });
    }

    /** A safe blink: moves the boss to 'to' if its body fits there. */
    public boolean blinkTo(Vec3 to) {
        if (!(level() instanceof ServerLevel sl) || !sl.noCollision(this, getBoundingBox().move(to.subtract(position())))) return false;
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, position().add(0, 1, 0), position().add(0, 1.5, 0), 0xFF000000 | DevilStyle.color(devil()), 12, 0.8f);
        teleportTo(to.x, to.y, to.z);
        getNavigation().stop();
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, to.add(0, 1, 0), to.add(0, 1.5, 0), 0xFF000000 | DevilStyle.color(devil()), 12, 0.8f);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.6f);
        return true;
    }

    /** The floor near 'p': the first air block with solid ground below, searching from 3 above to 6 below (p itself if none). */
    public static Vec3 ground(Level level, Vec3 p) {
        BlockPos base = BlockPos.containing(p);
        for (int dy = 3; dy >= -6; dy--) {
            BlockPos b = base.offset(0, dy, 0);
            if (level.getBlockState(b).isAir() && level.getBlockState(b.below()).blocksMotion()) return new Vec3(p.x, b.getY(), p.z);
        }
        return p;
    }

    // ---------------------------------------------------------------- Tensura's own magic
    /** Casts one of the learned Tensura skills at 'target' (see {@link #tensuraKeywords()}). False if there is none ready. */
    public boolean castTensura(LivingEntity target) {
        return tensuraKit != null && !tensuraKit.isEmpty() && TensuraCaster.cast(this, target, tensuraKit, 10) != null;
    }

    // ---------------------------------------------------------------- the world: changes are recorded and put back
    /** True if the server's griefBlocks rule lets bosses change the world. When false: effects only. */
    public boolean blocksMayChange() { return level() instanceof ServerLevel sl && NUGameRules.griefBlocks(sl); }

    /** Blocks this tick may still change (resets to {@link #BLOCKS_PER_TICK} every tick). */
    public int blockBudget() { return blockBudget; }

    /**
     * Changes a block now: only when {@link #blocksMayChange()}, within this tick's budget, for a loaded, ordinary block (not
     * unbreakable, not a block entity, not wither-immune). Records the old state; false if nothing was changed.
     */
    public boolean tryChange(BlockPos pos, BlockState to) {
        if (!(level() instanceof ServerLevel sl) || blockBudget <= 0 || ledger.isFull() || !blocksMayChange()) return false;
        BlockPos p = pos.immutable();
        if (!sl.isInWorldBounds(p) || !sl.isLoaded(p)) return false;
        BlockState old = sl.getBlockState(p);
        if (old == to || !changeable(sl, p, old)) return false;
        if (!sl.setBlock(p, to, 3)) return false;
        blockBudget--;
        ledger.record(p, old, to);
        return true;
    }

    /** Destroys a block like {@link #tryChange} (replaces it with air). */
    public boolean tryDestroy(BlockPos pos) { return tryChange(pos, Blocks.AIR.defaultBlockState()); }

    private static boolean changeable(ServerLevel sl, BlockPos p, BlockState old) {
        if (old.isAir()) return true;
        return old.getDestroySpeed(sl, p) >= 0 && !old.hasBlockEntity() && !old.is(BlockTags.WITHER_IMMUNE);
    }

    /**
     * Queues a block change that the boss applies a few dozen per tick (use this for anything big: a crater, a wall of bone).
     * False if the rule is off or the queue is full.
     */
    public boolean enqueueBlockChange(BlockPos pos, BlockState to) {
        if (pendingEdits.size() >= MAX_PENDING_EDITS || !blocksMayChange()) return false;
        pendingEdits.add(new BlockEdit(pos.immutable(), to));
        return true;
    }

    public int pendingBlockChanges() { return pendingEdits.size(); }

    private void drainEdits() {
        for (int i = 0; i < EDITS_TRIED_PER_TICK && blockBudget > 0 && !pendingEdits.isEmpty(); i++) {
            BlockEdit e = pendingEdits.poll();
            tryChange(e.pos(), e.to());
        }
    }

    // ---------------------------------------------------------------- Dark Disciples
    /** The live Dark Disciples of this boss. */
    public List<DarkDiscipleEntity> disciples() {
        if (!(level() instanceof ServerLevel sl)) return List.of();
        UUID me = getUUID();
        return sl.getEntitiesOfClass(DarkDiscipleEntity.class, getBoundingBox().inflate(96), d -> d.isAlive() && me.equals(d.ownerId()));
    }

    /** Summons up to 'count' elite followers around the boss, never more than the power's disciple cap. Returns how many came. */
    public int summonDisciples(int count) {
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return 0;
        int room = tuning.discipleCap() - disciples().size(), made = 0;
        for (int i = 0; i < Math.min(count, room); i++) {
            double a = getRandom().nextDouble() * Math.PI * 2, r = 3 + getRandom().nextDouble() * 3;
            if (DarkDiscipleEntity.spawnFor(this, sl, ground(sl, position().add(Math.cos(a) * r, 0, Math.sin(a) * r))) != null) made++;
        }
        if (made > 0) sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.5f);
        return made;
    }

    private void removeDisciples(ServerLevel sl) {
        for (DarkDiscipleEntity d : disciples()) {
            Tells.burst(sl, d.position().add(0, 1, 0), DevilStyle.color(devil()), 12);
            d.discard();
        }
    }

    // ================================================================ damage, death, removal
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (transformLeft > 0 || fightEnded) return false;
        Entity attacker = source.getEntity();
        if (attacker != null && ZogratisEntities.isFamily(attacker)) return false;
        noteFighter(attacker);
        float a = filterDamage(source, amount);
        if (a <= 0f) return false;
        if (exposed()) a *= EXPOSED_BONUS;
        a = PowerCurve.clampDamage(getHealth(), getMaxHealth(), power(), a);
        return a > 0f && super.hurt(source, a);
    }

    private void noteFighter(Entity attacker) {
        if (attacker instanceof ServerPlayer p) fighters.add(p.getUUID());
        else if (attacker instanceof OwnableEntity o && o.getOwnerUUID() != null) fighters.add(o.getOwnerUUID());
    }

    /** The UUIDs of the players who are fighting this boss. */
    public Set<UUID> fighters() { return fighters; }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl) || fightEnded) return;
        fightEnded = true;
        fightActive = false;
        cancelCast();
        pendingEdits.clear();
        setState(STATE_DEAD);
        bar.setProgress(0f);
        say(deathLine());
        ledger.restoreAll(sl);
        removeDisciples(sl);
        TriadBond.onFall(this);
        onFightEnd(true);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            TriadBond.unregister(this);
            bar.removeAllPlayers();
            if (reason.shouldDestroy() && level() instanceof ServerLevel sl) {
                ledger.restoreAll(sl);
                removeDisciples(sl);
            }
        }
        super.remove(reason);
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ZPower", power());
        tag.putInt("ZForm", form());
        tag.putBoolean("ZFight", fightActive);
        tag.putDouble("ZMana", mana.current());
        if (arena != null) tag.put("ZArena", NbtUtils.writeBlockPos(arena));
        CompoundTag l = new CompoundTag();
        ledger.save(l);
        tag.put("ZLedger", l);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int p = tag.contains("ZPower") ? tag.getInt("ZPower") : PowerCurve.OPENING;
        entityData.set(POWER, Math.max(0, Math.min(PowerCurve.FULL, p)));
        entityData.set(FORM, tag.getInt("ZForm"));
        tuning = PowerCurve.tuning(power());
        mana.retune(tuning);
        if (tag.contains("ZMana")) mana.set(tag.getDouble("ZMana"));
        fightActive = tag.getBoolean("ZFight");
        NbtUtils.readBlockPos(tag, "ZArena").ifPresent(pos -> arena = pos);
        if (tag.contains("ZLedger")) ledger.load(tag.getCompound("ZLedger"), level().registryAccess().lookupOrThrow(Registries.BLOCK));
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    // ================================================================ spawning
    /** Spawns a boss of 'type' at 'at' with its arena there, a summoning circle and a sound. Null if it could not be created. */
    public static <T extends ZogratisBoss> T spawnAt(ServerLevel sl, EntityType<T> type, Vec3 at, float yaw) {
        T boss = type.create(sl);
        if (boss == null) return null;
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setArena(BlockPos.containing(at));
        sl.addFreshEntity(boss);
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, at.add(0, 0.05, 0), at.add(0, 1, 0), 0xFF000000 | DevilStyle.color(boss.devil()), 60, 3f);
        sl.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.6f);
        return boss;
    }
}
