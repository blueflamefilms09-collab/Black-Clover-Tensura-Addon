package com.newuniverse.nusmp.celestial;

import com.newuniverse.nusmp.boss.budget.BossBudget;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * The Celestial Spirit King, ruler of the Spirit World and the boss of the Gate. He is built on the same shape as the Zogratis bosses and the Decennial bosses of the
 * Mega-Dungeon, written for one arena:
 * <ul>
 *   <li><b>Phases</b> ({@link KingRules#phase}): form one the Sovereign of the Gate (above two thirds), form two the Star-Crowned (above one third), form three the Celestial
 *       Ascendant (under it: blows hit a quarter harder, the actions come faster). Each phase changes his model (three geo files), is announced and gives the players a
 *       breath: a mercy of light that mends them a little.</li>
 *   <li><b>Legendary actions</b>: a pool of points that refills every round ({@link KingRules#points}); between the party's moves, and in reaction to being hit, he spends them
 *       on {@link KingRules#ACTIONS}: Star Fall (rings of falling stars on the players), Gate Slam (a ring you must jump), Constellation Bind (the farthest player is rooted to a
 *       star), Zodiac Beam (twelve beams from the rim, one after another) and, in the last phase, the Judgement (a huge blast; the gold circle in the middle is safe).</li>
 *   <li><b>Lair actions</b> every twelve seconds: a star tremor or a spirit wind. <b>Legendary resistances</b>: he shrugs off his first three harmful effects, loudly.</li>
 *   <li><b>No cheap kills, no cheap deaths</b>: every blow is telegraphed for at least a second (the Judgement for three), no blow takes more than 45% of a player's maximum
 *       health, the per-second damage budget of all bosses applies, and bosses never take more than the balance law's share from the spirits.</li>
 *   <li><b>Scaling</b>: health and damage follow the EP of the people in the Spirit World and the size of the party ({@link KingRules#health}).</li>
 * </ul>
 * The realm around him is {@link SpiritWorld}; his model is drawn by {@code client.celestial.SpiritKingRenderer}.
 */
public class SpiritKingEntity extends Monster implements BudgetedBoss {
    public static final String TAG = "nusmp_spirit_king";
    public static final String[] FORMS = {"Sovereign of the Gate", "The Star-Crowned", "Celestial Ascendant"};
    /** Cast kinds the model animates: 0 none, then one per action. */
    public static final int CAST_NONE = 0, CAST_STAR_FALL = 1, CAST_GATE_SLAM = 2, CAST_BIND = 3, CAST_BEAMS = 4, CAST_JUDGEMENT = 5;

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(SpiritKingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CAST_KIND = SynchedEntityData.defineId(SpiritKingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CAST_LEN = SynchedEntityData.defineId(SpiritKingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> CAST_AT = SynchedEntityData.defineId(SpiritKingEntity.class, EntityDataSerializers.LONG);

    /** A blow that lands at a time: a ring (r around x, y, z), a beam from (x,y,z) to (x2,z2) of width r, or the Judgement (a ring where the middle is safe). */
    private record Strike(long at, int kind, double x, double y, double z, double x2, double z2, double r, float mult) {}

    private final BossBudget dpsBudget = new BossBudget();
    private final ServerBossEvent bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_12);
    private final List<Strike> strikes = new ArrayList<>();
    private final Set<UUID> fighters = new HashSet<>();
    private int phase = 1, party = 1, points = 3, resistances = KingRules.RESISTANCES, realm = -1, actionIndex;
    private double ep = 1.0;
    private long roundAt, lairAt, nextActionAt, lastResistAt, lastMercyAt;
    private Vec3 arena = Vec3.ZERO;
    private boolean fought;

    public SpiritKingEntity(EntityType<? extends SpiritKingEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1200).add(Attributes.ATTACK_DAMAGE, 14).add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 10).add(Attributes.ARMOR_TOUGHNESS, 4).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
        b.define(CAST_KIND, CAST_NONE);
        b.define(CAST_LEN, 0);
        b.define(CAST_AT, 0L);
    }

    // ================================================================ what the renderer reads
    public int phase() { return entityData.get(PHASE); }

    public int castKind() { return entityData.get(CAST_KIND); }

    public int castLen() { return entityData.get(CAST_LEN); }

    public long castAt() { return entityData.get(CAST_AT); }

    public int realmSlot() { return realm; }

    public int resistancesLeft() { return resistances; }

    public int pointsLeft() { return points; }

    public Set<UUID> fighters() { return fighters; }

    public boolean hasFought() { return fought; }

    // ================================================================ set up
    /** Scales him for the people in the realm (health and blows follow the EP scale and the party), centres the arena and fills the bar. */
    public void configure(double epScale, int partySize, Vec3 center, int realmSlot) {
        this.ep = Math.max(1.0, Math.min(12.0, epScale));
        this.party = Math.max(1, Math.min(KingRules.MAX_PARTY, partySize));
        this.arena = center;
        this.realm = realmSlot;
        double hp = KingRules.health(ep, party);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(KingRules.attack(ep));
        dpsBudget.bypass(() -> setHealth((float) hp));
        points = KingRules.points(1, party);
        addTag(TAG);
        setCustomName(Component.literal("The Celestial Spirit King"));
        setCustomNameVisible(false);
        refreshBar();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Keep(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** He prefers six to ten blocks from his target (a king does not brawl); inside four he fights with his sceptre; he never walks while he casts. */
    private static final class Keep extends Goal {
        private final SpiritKingEntity k;
        Keep(SpiritKingEntity k) { this.k = k; setFlags(java.util.EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { return k.getTarget() != null && k.getTarget().isAlive() && k.casting(); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { k.getNavigation().stop(); }
    }

    private boolean casting() { return level().getGameTime() < castAt() + castLen(); }

    // ================================================================ bar and names
    private void refreshBar() {
        bar.setName(Component.literal("The Celestial Spirit King — " + FORMS[Math.max(1, Math.min(3, phase)) - 1]).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        bar.setProgress(Math.max(0f, Math.min(1f, getHealth() / Math.max(1f, getMaxHealth()))));
        bar.setColor(phase == 1 ? BossEvent.BossBarColor.YELLOW : phase == 2 ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.PURPLE);
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }

    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override public boolean fireImmune() { return true; }

    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    // ================================================================ the budget hooks (the same three as every boss)
    @Override public BossBudget dpsBudget() { return dpsBudget; }

    @Override public int budgetStage() { return phase - 1; }

    @Override public int budgetFinalStage() { return 2; }

    @Override
    public void baseTick() {
        super.baseTick();
        dpsBudget.tick(this, this);
    }

    @Override
    public void setHealth(float health) { super.setHealth(dpsBudget == null ? health : dpsBudget.setHealth(this, health)); }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() instanceof Player p) { fighters.add(p.getUUID()); fought = true; }
        boolean hit = dpsBudget.hurt(this, source, amount, x -> super.hurt(source, x));
        if (hit && !level().isClientSide && isAlive() && nextActionAt - level().getGameTime() > 25 && points > 0) nextActionAt -= 8;      // a blow hastens his next move: a legendary action in reaction
        return hit;
    }

    /** Legendary resistance: a harmful effect is turned away while he has some left (at most one per second). */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && resistances > 0 && !level().isClientSide) {
            long now = level().getGameTime();
            if (now - lastResistAt >= 20) {
                lastResistAt = now;
                resistances--;
                say(Component.translatableWithFallback("message.nusmp.celestial.king.resist", "The King shrugs it off. (Legendary Resistance, %s left)", resistances).withStyle(ChatFormatting.GRAY));
            }
            return false;
        }
        return super.canBeAffected(effect);
    }

    // ================================================================ the fight
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !(level() instanceof ServerLevel sl) || !isAlive()) return;
        long now = sl.getGameTime();
        try {
            if (!arena.equals(Vec3.ZERO) && tickCount % 20 == 0 && position().distanceToSqr(arena) > 40 * 40) teleportTo(arena.x, arena.y, arena.z);
            if (now >= roundAt) { roundAt = now + KingRules.ROUND_TICKS; points = KingRules.points(phase, party); }
            int next = KingRules.phase(getHealth() / Math.max(1f, getMaxHealth()));
            if (next > phase) enterPhase(sl, next);
            List<ServerPlayer> players = playersNear(sl, 48);
            if (!players.isEmpty()) {
                if (now >= lairAt) lair(sl, players, now);
                if (now >= nextActionAt && !casting()) act(sl, players, now);
            }
            runStrikes(sl, now);
            if (tickCount % 10 == 0) refreshBar();
            if (tickCount % 5 == 0) Fx.spray(sl, FxThemes.BOSS_CELESTIAL, FxOld.END_ROD, getX() + (random.nextDouble() - 0.5) * 2.0, getY() + random.nextDouble() * 3.2, getZ() + (random.nextDouble() - 0.5) * 2.0, 1, 0, 0.03, 0, 0.0);
        } catch (RuntimeException e) {
            // an entity that throws in its tick would crash the server: the fight goes on without the extras
        }
    }

    private List<ServerPlayer> playersNear(ServerLevel sl, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl.players()) if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) <= r * r) out.add(p);
        return out;
    }

    private void say(Component c) {
        if (level() instanceof ServerLevel sl) for (ServerPlayer p : playersNear(sl, 64)) p.sendSystemMessage(c);
    }

    private void enterPhase(ServerLevel sl, int to) {
        phase = to;
        entityData.set(PHASE, to);
        points = KingRules.points(phase, party);
        strikes.clear();
        nextActionAt = sl.getGameTime() + 60;                                                      // a breath for the party
        say(Component.translatableWithFallback("message.nusmp.celestial.king.phase" + to,
                to == 2 ? "The King's crown ignites: the stars answer him." : "The Gate blazes. The King rises as the Celestial Ascendant.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        VfxSpawn.send(sl, VfxShape.DIVINE_PILLAR, position(), position().add(0, 3, 0), 0xFFFFF3B0, 40, 2.6f);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0f, 0.6f);
        long now = sl.getGameTime();
        if (now - lastMercyAt > 100) {                                                              // the Gate's mercy: the players are mended a little
            lastMercyAt = now;
            for (ServerPlayer p : playersNear(sl, 64)) {
                BalanceLaw.heal(p, p.getMaxHealth() * 0.25f);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            }
        }
    }

    private float blow(float mult) {
        float phaseMult = phase == 3 ? 1.25f : 1f;
        return (float) (KingRules.attack(ep) * KingRules.partyDamage(party) * mult * phaseMult);
    }

    /** One hit on a player, capped to a share of their health. */
    private void hurtPlayer(ServerPlayer p, float mult) {
        float dmg = KingRules.cap(blow(mult), p.getMaxHealth());
        p.hurt(damageSources().mobAttack(this), dmg);
    }

    private void cast(ServerLevel sl, int kind, int len) {
        entityData.set(CAST_KIND, kind);
        entityData.set(CAST_LEN, len);
        entityData.set(CAST_AT, sl.getGameTime());
        getNavigation().stop();
    }

    // ================================================================ actions
    private void act(ServerLevel sl, List<ServerPlayer> players, long now) {
        KingRules.Action a = KingRules.pick(phase, points, actionIndex++);
        if (a == null) { nextActionAt = now + 20; return; }
        points -= a.cost();
        int gap = phase == 1 ? 70 : phase == 2 ? 55 : 40;
        ServerPlayer first = players.get(random.nextInt(players.size()));
        switch (a.id()) {
            case "star_fall" -> {
                cast(sl, CAST_STAR_FALL, 40);
                int spots = Math.min(players.size() + 2, 6);
                for (int i = 0; i < spots; i++) {
                    ServerPlayer t = players.get(i % players.size());
                    double ox = i < players.size() ? 0 : (random.nextDouble() - 0.5) * 8, oz = i < players.size() ? 0 : (random.nextDouble() - 0.5) * 8;
                    Vec3 at = new Vec3(t.getX() + ox, t.getY(), t.getZ() + oz);
                    telegraph(sl, at, 3.4, 24);
                    strikes.add(new Strike(now + 26, 0, at.x, at.y, at.z, 0, 0, 3.4, 0.9f));
                }
            }
            case "gate_slam" -> {
                cast(sl, CAST_GATE_SLAM, 40);
                telegraph(sl, position(), 9.5, 26);
                strikes.add(new Strike(now + 28, 1, getX(), getY(), getZ(), 0, 0, 9.5, 1.1f));
            }
            case "constellation_bind" -> {
                cast(sl, CAST_BIND, 36);
                ServerPlayer far = players.get(0);
                for (ServerPlayer p : players) if (p.distanceToSqr(this) > far.distanceToSqr(this)) far = p;
                telegraph(sl, far.position(), 2.2, 24);
                strikes.add(new Strike(now + 26, 2, far.getX(), far.getY(), far.getZ(), 0, 0, 2.2, 0.6f));
            }
            case "zodiac_beam" -> {
                cast(sl, CAST_BEAMS, 70);
                for (int i = 0; i < 12; i++) {
                    double ang = i * Math.PI / 6 + random.nextDouble() * 0.2;
                    double fx = arena.x + Math.cos(ang) * 22, fz = arena.z + Math.sin(ang) * 22;
                    long at = now + 24 + i * 4L;
                    strikes.add(new Strike(at - 16, 5, fx, getY() + 0.2, fz, arena.x, arena.z, 1.5, 0f));       // the telegraph line
                    strikes.add(new Strike(at, 3, fx, getY(), fz, arena.x, arena.z, 1.5, 1.0f));                // the beam
                }
            }
            case "judgement" -> {
                cast(sl, CAST_JUDGEMENT, 80);
                Vec3 safe = new Vec3(arena.x, getY(), arena.z);
                telegraph(sl, safe, 30, 60);
                VfxSpawn.send(sl, VfxShape.DIVINE_SIGIL, safe, safe.add(0, 1, 0), 0xFFFFF3B0, 70, 2.0f);
                say(Component.translatableWithFallback("message.nusmp.celestial.king.judgement", "The King raises his sceptre: stand in the golden circle!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                strikes.add(new Strike(now + 64, 4, safe.x, safe.y, safe.z, 0, 0, 5.0, 1.6f));
            }
            default -> { }
        }
        nextActionAt = now + gap + entityData.get(CAST_LEN);
    }

    private void telegraph(ServerLevel sl, Vec3 at, double r, int ticks) {
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, at, at.add(0, 1, 0), 0xFFFFE27A, ticks, (float) Math.max(0.8, r / 2.5));
        Fx.spray(sl, FxThemes.BOSS_CELESTIAL, FxOld.END_ROD, at.x, at.y + 0.2, at.z, 10, r * 0.4, 0.1, r * 0.4, 0.0);
    }

    private void runStrikes(ServerLevel sl, long now) {
        for (Iterator<Strike> it = strikes.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            if (now < s.at) continue;
            it.remove();
            Vec3 at = new Vec3(s.x, s.y, s.z);
            switch (s.kind) {
                case 0 -> {                                                                         // a falling star
                    VfxSpawn.send(sl, VfxShape.FX_SPIRIT_NOVA, at, at.add(0, 1, 0), 0xFFFFF3B0, 16, 1.2f);
                    sl.playSound(null, BlockPosOf(at), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.8f, 1.6f);
                    for (ServerPlayer p : playersNear(sl, 64)) if (p.position().distanceToSqr(at) <= s.r * s.r) hurtPlayer(p, s.mult);
                }
                case 1 -> {                                                                         // the Gate slams: jump it
                    VfxSpawn.send(sl, VfxShape.WIND_RING, at, at.add(0, 1, 0), 0xFFFFF3B0, 18, 2.4f);
                    sl.playSound(null, BlockPosOf(at), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.6f, 0.5f);
                    for (ServerPlayer p : playersNear(sl, 64)) {
                        if (p.position().distanceToSqr(at) > s.r * s.r || (!p.onGround() && p.getY() - getY() > 0.9)) continue;
                        hurtPlayer(p, s.mult);
                        Vec3 out = p.position().subtract(at).multiply(1, 0, 1);
                        out = out.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : out.normalize();
                        p.setDeltaMovement(p.getDeltaMovement().add(out.x * 1.1, 0.5, out.z * 1.1));
                        p.hurtMarked = true;
                    }
                }
                case 2 -> {                                                                         // the bind: rooted to a star, shortened by the balance law's caps
                    for (ServerPlayer p : playersNear(sl, 64)) if (p.position().distanceToSqr(at) <= s.r * s.r * 4) {
                        hurtPlayer(p, s.mult);
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(p, 50), 6));
                        VfxSpawn.send(sl, VfxShape.SEAL_CHAINS, p.position(), p.position().add(0, 1, 0), 0xFFFFF3B0, 30, 1.0f);
                    }
                }
                case 3 -> {                                                                         // a zodiac beam, rim to centre
                    Vec3 to = new Vec3(s.x2, s.y, s.z2);
                    VfxSpawn.send(sl, VfxShape.DIVINE_RAY, at.add(0, 1, 0), to.add(0, 1, 0), 0xFFFFF3B0, 10, 1.3f);
                    for (ServerPlayer p : playersNear(sl, 64)) if (nearSegment(p.position(), at, to, s.r)) hurtPlayer(p, s.mult);
                }
                case 4 -> {                                                                         // the Judgement: the golden circle is safe
                    VfxSpawn.send(sl, VfxShape.FX_SPIRIT_CATACLYSM, at, at.add(0, 2, 0), 0xFFFFF3B0, 40, 3.0f);
                    sl.playSound(null, BlockPosOf(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.5f);
                    for (ServerPlayer p : playersNear(sl, 64)) {
                        double dx = p.getX() - at.x, dz = p.getZ() - at.z;
                        if (dx * dx + dz * dz <= s.r * s.r) { p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.celestial.king.safe", "The golden circle holds.").withStyle(ChatFormatting.GOLD)); continue; }
                        hurtPlayer(p, s.mult * 1.8f);
                    }
                }
                case 5 -> VfxSpawn.send(sl, VfxShape.DIVINE_RAY, at.add(0, 0.3, 0), new Vec3(s.x2, s.y + 0.3, s.z2), 0x66FFE27A, 14, 0.5f);     // a beam's thin warning line
                default -> { }
            }
        }
    }

    private static net.minecraft.core.BlockPos BlockPosOf(Vec3 v) { return net.minecraft.core.BlockPos.containing(v); }

    private static boolean nearSegment(Vec3 p, Vec3 a, Vec3 b, double w) {
        double abx = b.x - a.x, abz = b.z - a.z, len2 = Math.max(1e-6, abx * abx + abz * abz);
        double t = Math.max(0, Math.min(1, ((p.x - a.x) * abx + (p.z - a.z) * abz) / len2));
        double nx = a.x + abx * t - p.x, nz = a.z + abz * t - p.z;
        return nx * nx + nz * nz <= (w + 0.4) * (w + 0.4);
    }

    private void lair(ServerLevel sl, List<ServerPlayer> players, long now) {
        lairAt = now + KingRules.LAIR_TICKS;
        String id = KingRules.LAIR_ACTIONS.get(random.nextInt(KingRules.LAIR_ACTIONS.size()));
        if (id.equals("gate_light")) {                                                              // the Gate's light: a small mending for the party, the king gets nothing
            for (ServerPlayer p : players) {
                BalanceLaw.heal(p, p.getMaxHealth() * 0.08f);
                VfxSpawn.send(sl, VfxShape.DIVINE_HALO, p.position(), p.position().add(0, 1, 0), 0xFFFFF3B0, 24, 0.9f);
            }
        } else if (id.equals("star_tremor")) {
            for (int i = 0; i < 5; i++) {
                Vec3 at = new Vec3(arena.x + (random.nextDouble() - 0.5) * 36, getY(), arena.z + (random.nextDouble() - 0.5) * 36);
                telegraph(sl, at, 2.6, 22);
                strikes.add(new Strike(now + 24, 0, at.x, at.y, at.z, 0, 0, 2.6, 0.5f));
            }
        } else {
            VfxSpawn.send(sl, VfxShape.WIND_GALE, position(), position().add(0, 1, 0), 0xFFFFF3B0, 30, 2.0f);
            for (ServerPlayer p : players) {
                Vec3 out = p.position().subtract(position()).multiply(1, 0, 1);
                out = out.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : out.normalize();
                p.setDeltaMovement(p.getDeltaMovement().add(out.x * 0.8, 0.25, out.z * 0.8));
                p.hurtMarked = true;
            }
        }
    }

    // ================================================================ his sceptre
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (target instanceof ServerPlayer p) {
            hurtPlayer(p, 0.55f);
            return true;
        }
        return super.doHurtTarget(target);
    }

    // ================================================================ the end
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl) || isAlive()) return;
        bar.setProgress(0f);
        bar.removeAllPlayers();
        VfxSpawn.send(sl, VfxShape.DIVINE_PILLAR, position(), position().add(0, 3, 0), 0xFFFFF3B0, 60, 3.0f);
        say(Component.translatableWithFallback("message.nusmp.celestial.king.death", "\"You kept faith with every one of them. Go, keeper of the stars.\"").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        SpiritWorld.kingDown(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("KPhase", phase);
        tag.putInt("KParty", party);
        tag.putInt("KRealm", realm);
        tag.putInt("KRes", resistances);
        tag.putDouble("KEp", ep);
        tag.putDouble("KAx", arena.x);
        tag.putDouble("KAy", arena.y);
        tag.putDouble("KAz", arena.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        dpsBudget.bypass(() -> super.readAdditionalSaveData(tag));
        phase = Math.max(1, Math.min(3, tag.getInt("KPhase")));
        party = Math.max(1, tag.getInt("KParty"));
        realm = tag.contains("KRealm") ? tag.getInt("KRealm") : -1;
        resistances = tag.contains("KRes") ? tag.getInt("KRes") : KingRules.RESISTANCES;
        ep = tag.contains("KEp") ? tag.getDouble("KEp") : 1.0;
        arena = new Vec3(tag.getDouble("KAx"), tag.getDouble("KAy"), tag.getDouble("KAz"));
        entityData.set(PHASE, phase);
        addTag(TAG);
        refreshBar();
    }
}
