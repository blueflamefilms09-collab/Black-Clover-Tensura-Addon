package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Why: Jimmy, Alfredo and Carl are Rades's own named puppets, so they are real entities of their own (three {@code EntityType}s, a
 * stat row each in {@link NamedCorpses}) and not renamed zombies: a brawler, a bruiser and a bone-throwing caster have to look, move
 * and fight differently, and they have their own health and a longer life (three minutes) than a reanimated body. The class is one
 * body for all three, driven by the row it was built with.
 * <p>
 * They fight for the owner: {@link FoeGoal} asks {@link Corpses#pickFoe} for a target, {@link FollowOwnerGoal} keeps them close and
 * the caster role keeps its distance and fires through {@link #volley}/{@link #bolt}. Nothing heals anyone. They are never saved with
 * the world ({@link #shouldBeSaved}), so a stopped server leaves none behind, and they drop nothing. The owner cannot hurt them by
 * accident. {@link #castLeft()} tells the renderer the caster is throwing (arm raised).
 * <p>
 * Rades's numbered undead (No.4 Jimmy, No.3 David, No.2 Alfredo, No.1 Carl, No.0 Michael Caesal) are this one class: the three old rows keep their stats and registry ids, and the
 * {@link NamedCorpses.Signature} of a row adds one move that is decided here on the server only: Alfredo's soul barrier (Absorption for the caster's side), David's ground slam and Michael
 * Caesal's soul barrage. Each waits {@link NamedCorpses#SIGNATURE_OPENING_TICKS} after the summon, has its own cooldown, touches a capped number of targets, only ever hurts the
 * owner's foes ({@link Corpses#isFoe}) and goes through {@link BalanceLaw#cap} against players. They are flagged undead (data tag {@code minecraft:undead}) and, being plain mobs, do not
 * burn in daylight.
 */
public class NamedCorpseEntity extends PathfinderMob implements OwnableEntity {
    private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(NamedCorpseEntity.class, EntityDataSerializers.INT);
    /** Ticks of the throwing animation the renderer is told about. */
    public static final int THROW_TICKS = 12;

    private final NamedCorpses kind;
    private UUID ownerId;
    private int shardCd = 20, boltCd = 80, pending;          // pending: 0 nothing, 1 volley, 2 bolt, 3 ground slam, 4 soul barrage
    private int sigCd = NamedCorpses.SIGNATURE_OPENING_TICKS, barrageLeft;
    private LivingEntity pendingTarget;

    public NamedCorpseEntity(EntityType<? extends NamedCorpseEntity> type, Level level, NamedCorpses kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = 0;
        setPersistenceRequired();
        addGoals();                                           // the Mob constructor ran registerGoals() before 'kind' was set, so the goals are added here
    }

    public static AttributeSupplier.Builder createAttributes(NamedCorpses k) {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, k.health).add(Attributes.ATTACK_DAMAGE, k.attack)
                .add(Attributes.MOVEMENT_SPEED, k.speed).add(Attributes.ARMOR, k.armor).add(Attributes.KNOCKBACK_RESISTANCE, k.knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, 24).add(Attributes.ATTACK_KNOCKBACK, k.role == NamedCorpses.Role.BRUISER ? 1.5 : k.role == NamedCorpses.Role.ELITE ? 0.8 : 0.4);
    }

    public NamedCorpses kind() { return kind; }

    @Override public UUID getOwnerUUID() { return ownerId; }

    public void setOwner(UUID id) { this.ownerId = id; }

    /** Ticks left of the throwing animation (0 when idle). */
    public int castLeft() { return entityData.get(CAST); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(CAST, 0);
    }

    /** Called by the Mob constructor while {@code kind} is still null (that threw a NullPointerException and no numbered undead could be created): does nothing until {@link #addGoals()}. */
    @Override
    protected void registerGoals() {
        if (kind != null) addGoals();
    }

    private void addGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (kind.ranged()) goalSelector.addGoal(2, new CasterGoal(this));
        else goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new FollowOwnerGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new FoeGoal(this));
    }

    // ---------------------------------------------------------------- sides
    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == null) return false;
        if (ownerId != null && (ownerId.equals(other.getUUID()) || ownerId.equals(Corpses.ownerOf(other)))) return true;
        Player owner = ownerPlayer();
        return owner != null && owner.isAlliedTo(other) || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isAlliedTo(target) && super.canAttack(target);
    }

    private Player ownerPlayer() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity by = source.getEntity();
        if (by != null && by != this && isAlliedTo(by)) return false;                 // the owner and the owner's friends cannot kill it by accident
        return super.hurt(source, amount);
    }

    @Override public boolean shouldBeSaved() { return false; }            // a named corpse is a loan from this session only

    // ---------------------------------------------------------------- the daylight rule (3.7.1)
    /** Sunlight never ignites a numbered undead: it is a plain {@link PathfinderMob} (not a zombie or a skeleton, the only bodies that read daylight), and both daylight questions answer no besides. */
    @Override public boolean isSunBurnTick() { return false; }

    /** True when daylight can ignite this body (never): the self-test and the source scan read this. */
    public boolean burnsInDaylight() { return false; }

    // ---------------------------------------------------------------- why did it go (3.7.1)
    private boolean dismissed;

    /** The sweep, the cap or the owner's logout took this body on purpose: the diagnosis of an early removal does not speak. */
    void dismissed() { this.dismissed = true; }

    /** A numbered body that leaves the world within 5 seconds by anything but a dismissal or a chunk or dimension move writes one server-log line with the reason (rate limited, {@code corpse.diagnose}). */
    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && !dismissed && ownerId != null) {
            boolean normal = RadesCastDiagnosis.normalEnd(reason.name());
            if (RadesCastDiagnosis.vanishedEarly(tickCount, normal))
                Corpses.diag(ownerId, level().getGameTime(), RadesCastDiagnosis.vanishedLine(kind.id, ownerId.toString(), tickCount, RadesCastDiagnosis.removalReason(reason.name(), false, false)));
        }
        super.remove(reason);
    }

    @Override protected boolean shouldDropLoot() { return false; }

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    // ---------------------------------------------------------------- ranged moves (Carl)
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (shardCd > 0) shardCd--;
        if (boltCd > 0) boltCd--;
        int c = castLeft();
        if (c > 0) {
            entityData.set(CAST, --c);
            if (c == 0) release();
        }
        if (kind.signature != NamedCorpses.Signature.NONE && level() instanceof ServerLevel sl) {
            if (sigCd > 0) sigCd--;
            signature(sl);
        }
    }

    // ---------------------------------------------------------------- signature moves (Alfredo, David, Michael Caesal)
    private void signature(ServerLevel sl) {
        LivingEntity t = getTarget();
        switch (kind.signature) {
            case SOUL_BARRIER -> { if (sigCd <= 0 && t != null && t.isAlive() && tickCount % 10 == 0) barrier(sl); }
            case GROUND_SLAM -> {
                if (sigCd <= 0 && readyToThrow() && t != null && t.isAlive() && canAttack(t) && distanceTo(t) <= NamedCorpses.SLAM_TRIGGER) {
                    pending = 3;
                    sigCd = NamedCorpses.SLAM_EVERY_TICKS;
                    entityData.set(CAST, THROW_TICKS);
                    playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.0f, 0.6f);
                }
            }
            case SOUL_BARRAGE -> {
                if (sigCd <= 0 && readyToThrow() && barrageLeft == 0 && t != null && t.isAlive() && canAttack(t)
                        && distanceTo(t) <= NamedCorpses.BARRAGE_RANGE && hasLineOfSight(t)) {
                    pending = 4;
                    sigCd = NamedCorpses.BARRAGE_EVERY_TICKS;
                    entityData.set(CAST, THROW_TICKS);
                    playSound(SoundEvents.SOUL_ESCAPE.value(), 1.2f, 0.5f);
                }
                if (barrageLeft > 0) tickBarrage(sl);
            }
            default -> { }
        }
    }

    /** Alfredo: Absorption for the owner's side within range (at most {@link NamedCorpses#BARRIER_MAX_TARGETS}); a shield, not a heal. */
    private void barrier(ServerLevel sl) {
        sigCd = NamedCorpses.BARRIER_EVERY_TICKS;
        double r = NamedCorpses.BARRIER_RANGE;
        List<LivingEntity> side = new ArrayList<>();
        List<UUID> allies = ownerId == null ? List.of() : Pacts.RULES.allies(ownerId);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 4, r), e -> e.isAlive() && e.distanceToSqr(this) <= r * r)) {
            boolean mine = e == this || ownerId != null && (ownerId.equals(e.getUUID()) || ownerId.equals(Corpses.ownerOf(e)) || allies.contains(e.getUUID()));
            if (mine) side.add(e);
        }
        side.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
        int n = 0;
        for (LivingEntity e : side) {
            if (n++ >= NamedCorpses.BARRIER_MAX_TARGETS) break;
            e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, NamedCorpses.BARRIER_TICKS, NamedCorpses.BARRIER_AMPLIFIER, true, true), this);
            Fx.burst(sl, FxThemes.CORPSE, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), FxTone.LIGHT, 6);
        }
        Fx.ring(sl, FxThemes.CORPSE, FxTone.LIGHT, getX(), getY() + 0.2, getZ(), 2.2, 24);
        sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 1.0f, 1.4f);
    }

    /** David: a shock round him after the wind-up: every foe within the radius (at most {@link NamedCorpses#SLAM_MAX_TARGETS}) takes part of his blow, is thrown back and slowed. */
    private void slam(ServerLevel sl) {
        ServerPlayer owner = ownerPlayerServer();
        Fx.ring(sl, FxThemes.CORPSE, FxTone.DARK, getX(), getY() + 0.1, getZ(), NamedCorpses.SLAM_RADIUS, 28);
        Fx.burst(sl, FxThemes.CORPSE, getX(), getY() + 0.3, getZ(), FxTone.DARK, 14);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.8f, 0.5f);
        if (owner == null) return;
        double r = NamedCorpses.SLAM_RADIUS;
        List<LivingEntity> foes = sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 1.5, r), e -> e != this && e.distanceToSqr(this) <= r * r && Corpses.isFoe(owner, e));
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
        int n = 0;
        for (LivingEntity e : foes) {
            if (n++ >= NamedCorpses.SLAM_MAX_TARGETS) break;
            hit(e, (float) (kind.attack * NamedCorpses.SLAM_FACTOR), false);
            e.knockback(NamedCorpses.SLAM_KNOCKBACK, getX() - e.getX(), getZ() - e.getZ());
            e.hurtMarked = true;
            if (!BalanceLaw.isBoss(e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, NamedCorpses.SLAM_SLOW_TICKS, 1), this);
        }
    }

    /** Michael Caesal: one soul bolt every {@link NamedCorpses#BARRAGE_GAP_TICKS} ticks at the target he has in sight, {@link NamedCorpses#BARRAGE_BOLTS} in all. */
    private void tickBarrage(ServerLevel sl) {
        LivingEntity t = getTarget();
        barrageLeft--;
        if (t == null || !t.isAlive() || !canAttack(t) || distanceTo(t) > NamedCorpses.BARRAGE_RANGE * 1.3) { barrageLeft = 0; return; }
        if (barrageLeft % NamedCorpses.BARRAGE_GAP_TICKS != 0 || !hasLineOfSight(t)) return;
        Vec3 from = getEyePosition().add(0, -0.2, 0), to = t.getBoundingBox().getCenter();
        VfxSpawn.send(sl, VfxShape.WIKI_MAGIC_CAST, from, to, 0xFF6C7899, 10, 0.7f);
        t.invulnerableTime = 0;                                              // each bolt is its own hit (the six are spread over 12 ticks, inside the usual invulnerability window)
        hit(t, (float) NamedCorpses.BARRAGE_DAMAGE, true);
    }

    /** True while the caster is free to start a new throw. */
    boolean readyToThrow() { return castLeft() == 0 && pending == 0; }

    /** Starts the wind-up of the next move against 'target': a Soul Corpse bolt when it is ready, else a volley of bone shards. */
    void beginThrow(LivingEntity target) {
        if (!readyToThrow()) return;
        if (boltCd <= 0) { pending = 2; boltCd = NamedCorpses.BOLT_EVERY_TICKS; shardCd = 30; }
        else if (shardCd <= 0) { pending = 1; shardCd = NamedCorpses.SHARD_EVERY_TICKS; }
        else return;
        pendingTarget = target;
        entityData.set(CAST, THROW_TICKS);
        playSound(SoundEvents.SKELETON_AMBIENT, 0.8f, 0.6f);
    }

    private void release() {
        int move = pending;
        LivingEntity t = pendingTarget;
        pending = 0;
        pendingTarget = null;
        if (move == 3 || move == 4) { releaseSignature(move); return; }
        if (move == 0 || t == null || !t.isAlive() || !(level() instanceof ServerLevel sl) || !canAttack(t)) return;
        if (move == 2) bolt(sl, t); else volley(sl, t);
    }

    private void releaseSignature(int move) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (move == 3) slam(sl);
        else if (move == 4) {
            barrageLeft = NamedCorpses.BARRAGE_BOLTS * NamedCorpses.BARRAGE_GAP_TICKS;
            entityData.set(CAST, barrageLeft);
        }
    }

    /** Three bone shards at the target, each a short hit-scan with a trail of bone chips; they hit only what Carl can see. */
    private void volley(ServerLevel sl, LivingEntity t) {
        Vec3 from = getEyePosition().add(0, -0.25, 0);
        if (!hasLineOfSight(t)) return;
        for (int i = 0; i < NamedCorpses.SHARDS_PER_VOLLEY; i++) {
            Vec3 aim = t.getBoundingBox().getCenter().add((random.nextDouble() - 0.5) * t.getBbWidth(), (random.nextDouble() - 0.5) * t.getBbHeight() * 0.6, (random.nextDouble() - 0.5) * t.getBbWidth());
            trail(sl, from, aim);
            if (random.nextFloat() < 0.85f) hit(t, (float) (kind.shardDamage * (0.85 + random.nextDouble() * 0.3)), false);
        }
        sl.playSound(null, blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 0.9f, 0.7f);
    }

    /** A Soul Corpse bolt: one heavier hit-scan in the Soul Corpse palette that leaves the target withering for three seconds. */
    private void bolt(ServerLevel sl, LivingEntity t) {
        Vec3 from = getEyePosition().add(0, -0.2, 0), to = t.getBoundingBox().getCenter();
        VfxSpawn.send(sl, VfxShape.WIKI_MAGIC_CAST, from, to, 0xFF6C7899, 14, 0.9f);
        sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.2f, 0.6f);
        if (!hasLineOfSight(t)) return;
        hit(t, (float) (kind.shardDamage * NamedCorpses.BOLT_FACTOR), true);
        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), this);
    }

    private void hit(LivingEntity t, float amount, boolean magic) {
        float dmg = t instanceof Player p ? com.newuniverse.nusmp.balance.BalanceLaw.cap(p, amount) : amount;
        t.hurt(magic ? damageSources().indirectMagic(this, this) : damageSources().mobAttack(this), dmg);
    }

    private void trail(ServerLevel sl, Vec3 a, Vec3 b) {
        int n = (int) Math.min(14, Math.max(3, a.distanceTo(b) * 1.5));
        ItemParticleOption chip = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BONE_MEAL));
        for (int k = 1; k <= n; k++) {
            Vec3 p = a.lerp(b, k / (double) n);
            sl.sendParticles(chip, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    // ---------------------------------------------------------------- goals
    /** Picks the owner's nearest foe (see {@link Corpses#pickFoe}) twice a second. */
    private static final class FoeGoal extends TargetGoal {
        private final NamedCorpseEntity me;
        private int wait;
        private LivingEntity found;

        FoeGoal(NamedCorpseEntity me) {
            super(me, false);
            this.me = me;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (--wait > 0) return false;
            wait = 10;
            if (!(me.level() instanceof ServerLevel)) return false;
            found = Corpses.pickFoe(me.ownerPlayerServer(), me);
            return found != null;
        }

        @Override public void start() { me.setTarget(found); super.start(); }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = me.getTarget();
            return t != null && t.isAlive() && me.canAttack(t) && me.distanceToSqr(t) < 40 * 40;
        }
    }

    ServerPlayer ownerPlayerServer() {
        Player p = ownerPlayer();
        return p instanceof ServerPlayer sp ? sp : null;
    }

    /** Stays within a few blocks of the owner when there is nothing to fight, and hurries (or steps through) when left far behind. */
    private static final class FollowOwnerGoal extends Goal {
        private final NamedCorpseEntity me;
        private int repath;

        FollowOwnerGoal(NamedCorpseEntity me) {
            this.me = me;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player o = me.ownerPlayer();
            return o != null && me.getTarget() == null && me.distanceToSqr(o) > 36;
        }

        @Override
        public boolean canContinueToUse() {
            Player o = me.ownerPlayer();
            return o != null && me.getTarget() == null && me.distanceToSqr(o) > 9;
        }

        @Override
        public void tick() {
            Player o = me.ownerPlayer();
            if (o == null) return;
            me.getLookControl().setLookAt(o, 30f, 30f);
            if (me.distanceToSqr(o) > 28 * 28) { me.teleportTo(o.getX() + 1.5, o.getY(), o.getZ() + 1.5); me.getNavigation().stop(); return; }
            if (--repath > 0) return;
            repath = SoulZombieRules.REPATH_TICKS;
            me.getNavigation().moveTo(o, 1.2);
        }
    }

    /** The caster role: stays 7 to 13 blocks from its target, faces it and throws when a move is ready. */
    private static final class CasterGoal extends Goal {
        private final NamedCorpseEntity me;
        private int repath;

        CasterGoal(NamedCorpseEntity me) {
            this.me = me;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = me.getTarget();
            return t != null && t.isAlive();
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity t = me.getTarget();
            if (t == null) return;
            me.getLookControl().setLookAt(t, 40f, 40f);
            double d = me.distanceTo(t);
            if (--repath <= 0) {
                repath = 6;
                if (d < NamedCorpses.CASTER_KEEP_AWAY) {
                    Vec3 away = me.position().subtract(t.position()).multiply(1, 0, 1);
                    if (away.lengthSqr() < 1e-4) away = new Vec3(1, 0, 0);
                    Vec3 to = me.position().add(away.normalize().scale(5));
                    me.getNavigation().moveTo(to.x, to.y, to.z, 1.15);
                } else if (d > NamedCorpses.CASTER_RANGE * 0.7 || !me.hasLineOfSight(t)) {
                    me.getNavigation().moveTo(t, 1.0);
                } else {
                    me.getNavigation().stop();
                }
            }
            if (d <= NamedCorpses.CASTER_RANGE && me.hasLineOfSight(t)) me.beginThrow(t);
        }
    }

    // ---------------------------------------------------------------- sounds
    // 1.99.0 (docs/audio/mob_audio.md): a named corpse groans like a ghoul of the bespoke audio engine, not like a vanilla zombie
    @Override protected SoundEvent getAmbientSound() { return com.newuniverse.nusmp.audio.MobVoice.ambient("ghoul"); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return com.newuniverse.nusmp.audio.MobVoice.hurt("ghoul"); }
    @Override protected SoundEvent getDeathSound() { return com.newuniverse.nusmp.audio.MobVoice.death("ghoul"); }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { playSound(com.newuniverse.nusmp.audio.MobVoice.event("ghoul", com.newuniverse.nusmp.audio.Cue.STEP), 0.5f, 0.8f); }
    @Override public float getVoicePitch() {
        return switch (kind) { case ALFREDO -> 0.55f; case CARL -> 0.85f; case DAVID -> 0.45f; case MICHAEL_CAESAL -> 0.95f; default -> 0.75f; };
    }
}
