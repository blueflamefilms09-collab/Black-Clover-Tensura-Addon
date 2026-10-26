package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.entity.veldanava.pocket.PocketLabyrinth;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Israfil, Lord of Trials: the second of the five Veldanava ultimate skills (the numbers are in {@link IsrafilRules}, the passive rules in {@link IsrafilEvents}). Ten modes: four toggle
 * passives (Spatial Domination, Trial of Endurance, Labyrinthian Mind, Weight of Consequence), five actives (Dimensional Labyrinth, Weight of Sins, Spatial Collapse, Coordinate Shift, Rift Walk) and
 * the mastery active The Grand Trumpet. The press, cost, cooldown and toggle code is {@link VeldanavaSkillBase}.
 *
 * <p>Worst case per press (rule 11): Labyrinth 1 line-of-sight query and one field; Weight of Sins 1 query, 1 hit; Spatial Collapse 1 query + 16 hits + 1 field; Coordinate Shift 1 query (+ 1 box query
 * of at most 8 projectiles and one of at most 24 mobs); Rift Walk one session (a few at most on a server); The Grand Trumpet one box query, 48 creatures touched, 48 players' skills walked once (at
 * most 16 modes each) and a field sweep. The passives run once a second per owner: Weight of Consequence touches at most 16 creatures. One effect packet per press.
 */
public class IsrafilSkill extends VeldanavaSkillBase {
    public IsrafilSkill() { super(IsrafilRules.ISRAFIL); }

    // ---------------------------------------------------------------- the passives that need a look once a second
    @Override
    protected void secondTick(ManasSkillInstance i, ServerPlayer p, int toggles, boolean mastered) {
        refresh(p, toggles);
        if (JudgmentRules.on(toggles, 3)) weight(p);
    }

    /** Spatial Domination's speed and Trial of Endurance's drain and gifts: brought in line with the toggles. */
    private static void refresh(ServerPlayer p, int toggles) {
        IsrafilState.applySpeed(p, JudgmentRules.on(toggles, 0));
        double store = IsrafilState.store(p);
        if (JudgmentRules.on(toggles, 1)) {
            store = IsrafilRules.drained(store, 1.0);
            IsrafilState.setStore(p, store);
            IsrafilState.applyStore(p, store);
        } else {
            IsrafilState.setStore(p, 0.0);
            IsrafilState.applyStore(p, 0.0);
        }
    }

    @Override
    protected void toggled(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean on) {
        refresh(p, JudgmentState.toggles(i));
    }

    @Override
    protected void forgotten(ServerPlayer p) {
        IsrafilState.clear(p);
        RiftWalk.end(p, true, null);
        PocketLabyrinth.endOf(p.getServer(), p.getUUID());
    }

    /** Weight of Consequence: whoever MOVES within twenty blocks (the sixteen nearest) owes one more second and is slowed. */
    private static void weight(ServerPlayer p) {
        double r = IsrafilRules.WEIGHT_RADIUS;
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                x -> x != p && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(p) && x.distanceToSqr(p) <= r * r);
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
        int n = 0;
        for (LivingEntity t : near) {
            if (n >= IsrafilRules.MAX_WEIGHT_TARGETS) break;
            if (Math.hypot(t.getX() - t.xo, t.getZ() - t.zo) < IsrafilRules.WEIGHT_MIN_SPEED) continue;
            n++;
            TemporalDebt.add(t, IsrafilRules.WEIGHT_DEBT);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, IsrafilRules.WEIGHT_SLOW_TICKS, 0, false, true, true));
        }
    }

    // ---------------------------------------------------------------- the press
    @Override
    protected boolean activate(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {
        return switch (m.index()) {
            case 4 -> labyrinth(p, mastered);
            case 5 -> sins(i, p);
            case 6 -> collapse(i, p);
            case 7 -> shift(i, p);
            default -> trumpet(i, p);
        };
    }

    /**
     * Dimensional Labyrinth: the creature in the line of sight is pulled into a pocket dimension, an ever-shifting maze ({@link PocketLabyrinth}); with the skill mastered, up to two creatures next to it
     * come too. A boss, a server whose setting {@code pocketDimension} is off, or a server without the {@code nusmp:trial} dimension gets the old cage instead ({@link Cage}).
     */
    private static boolean labyrinth(ServerPlayer p, boolean mastered) {
        LivingEntity t = JudgmentState.first(p, IsrafilRules.LABYRINTH_RANGE, 1.0);
        if (t == null) { fail(p, "Nothing in your sight to trap."); return false; }
        if (VeldanavaGuard.entrapmentImmune(t)) { fail(p, t.getDisplayName().getString() + " slips out of the labyrinth."); return false; }
        if (JudgmentConfig.pocketDimension() && !BalanceLaw.isBoss(t) && PocketLabyrinth.available(p.getServer())) {
            String why = PocketLabyrinth.open(p, t, mastered);
            if (why != null) { fail(p, why); return false; }
            return true;
        }
        ServerLevel l = p.serverLevel();
        Vec3 center = t.position();
        if (!JudgmentFields.add(p, l, null, center, IsrafilRules.LABYRINTH_RADIUS, IsrafilRules.LABYRINTH_TICKS, new Cage(t.getUUID(), BalanceLaw.controlTicks(t, IsrafilRules.LABYRINTH_TICKS)), "labyrinth")) {
            fail(p, "Too many fields stand on this server.");
            return false;
        }
        TemporalDebt.add(t, 5);
        TrialFx.labyrinth(l, t, IsrafilRules.LABYRINTH_RADIUS, IsrafilRules.LABYRINTH_TICKS);
        return true;
    }

    /** The cage of Dimensional Labyrinth: while it stands the creature is held inside its radius, disoriented, and (for the control time the Balance Law allows) locked. One creature, so a pulse is O(1). */
    static final class Cage implements JudgmentFields.Behavior {
        private final UUID target;
        private final int lockTicks;
        private int pulses;

        Cage(UUID target, int lockTicks) {
            this.target = target;
            this.lockTicks = lockTicks;
        }

        @Override public int period() { return 5; }

        @Override
        public void pulse(ServerPlayer owner, ServerLevel level, JudgmentFields.Field f) {
            Entity e = level.getEntity(target);
            if (!(e instanceof LivingEntity t) || !t.isAlive()) { f.endsAt = 0L; return; }
            if (VeldanavaGuard.entrapmentImmune(t)) { f.endsAt = 0L; return; }
            if (pulses++ == 0) {
                t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, lockTicks, 0, false, true, true));
                t.addEffect(new MobEffectInstance(JudgmentEffects.MOVEMENT_INTERFERENCE, lockTicks, 0, false, true, true));
            }
            t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false, true));
            if (!BalanceLaw.isBoss(t)) hold(t, f);
            if (pulses % 4 == 0) JudgmentDamage.share(owner, t, IsrafilRules.LABYRINTH_PULSE_SHARE);
        }

        /** Keeps the creature inside the walls: past the edge it is put back just inside. */
        private static void hold(LivingEntity t, JudgmentFields.Field f) {
            double dx = t.getX() - f.center.x, dz = t.getZ() - f.center.z;
            double d = Math.sqrt(dx * dx + dz * dz), limit = f.radius - 0.6;
            if (d <= limit || d < 1.0E-6) return;
            Vec3 in = new Vec3(f.center.x + dx / d * limit, t.getY(), f.center.z + dz / d * limit);
            IsrafilState.place(t, (ServerLevel) t.level(), in);
        }
    }

    /** Weight of Sins: heavy true damage that grows with the debt (and, for a player, the kills) of the target; flight is gone for ten minutes or until death. */
    private static boolean sins(ManasSkillInstance i, ServerPlayer p) {
        LivingEntity t = JudgmentState.first(p, IsrafilRules.SINS_RANGE, 1.0);
        if (t == null) { fail(p, "Nothing in your sight to weigh."); return false; }
        int kills = t instanceof ServerPlayer sp ? sp.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)) : 0;
        double dmg = IsrafilRules.sinsDamage(TemporalDebt.get(t), kills);
        JudgmentDamage.hit(p, t, (float) dmg, mastery(p, i));
        if (!t.isAlive()) { TrialFx.gravity(p.serverLevel(), t); return true; }
        int ticks = BalanceLaw.isBoss(t) ? 200 : IsrafilRules.SINS_CRUSH_TICKS;
        t.addEffect(new MobEffectInstance(JudgmentEffects.GRAVITY_CRUSH, ticks, 0, false, true, true));
        TemporalDebt.add(t, 8);
        TrialFx.gravity(p.serverLevel(), t);
        return true;
    }

    /** Spatial Collapse: the place under the target (or where the owner aims) implodes: sixteen creatures take true damage and are pulled in, then a Nihility Collapse field stays for six seconds. */
    private static boolean collapse(ManasSkillInstance i, ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        LivingEntity t = JudgmentState.first(p, IsrafilRules.COLLAPSE_RANGE, 1.0);
        Vec3 center = t != null ? t.position().add(0.0, 0.1, 0.0) : GrimoireBook.aim(p, IsrafilRules.COLLAPSE_RANGE);
        if (!JudgmentFields.add(p, l, null, center, IsrafilRules.NIHILITY_RADIUS, IsrafilRules.NIHILITY_TICKS, NIHILITY, "nihility")) {
            fail(p, "Too many fields stand on this server.");
            return false;
        }
        double r = IsrafilRules.COLLAPSE_RADIUS;
        List<LivingEntity> near = new ArrayList<>(l.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(r, 3.0, r),
                x -> x != p && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(p) && x.distanceToSqr(center) <= r * r + 9.0));
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(center)));
        double mastery = mastery(p, i);
        int n = 0;
        for (LivingEntity x : near) {
            if (n++ >= IsrafilRules.COLLAPSE_MAX_TARGETS) break;
            JudgmentDamage.hit(p, x, (float) IsrafilRules.COLLAPSE_DAMAGE, mastery);
            if (x.isAlive()) IsrafilState.pull(x, center, IsrafilRules.COLLAPSE_PULL);
        }
        TrialFx.collapse(l, center, (float) r, IsrafilRules.NIHILITY_TICKS);
        return true;
    }

    /** The Nihility Collapse field: each pulse marks (and slows) up to 24 creatures in it and takes a share of their health. */
    static final JudgmentFields.Behavior NIHILITY = new JudgmentFields.Behavior() {
        @Override public int period() { return IsrafilRules.NIHILITY_PULSE_TICKS; }

        @Override
        public void pulse(ServerPlayer owner, ServerLevel level, JudgmentFields.Field f) {
            AABB box = new AABB(f.center, f.center).inflate(f.radius, 3.0, f.radius);
            double r2 = f.radius * f.radius;
            int hit = 0;
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && !x.isSpectator() && x != owner && !x.isAlliedTo(owner))) {
                if (hit >= JudgmentRules.MAX_FIELD_TARGETS) break;
                double dx = t.getX() - f.center.x, dz = t.getZ() - f.center.z;
                if (dx * dx + dz * dz > r2) continue;
                hit++;
                t.addEffect(new MobEffectInstance(JudgmentEffects.NIHILITY_COLLAPSE, 30, 0, false, true, true));
                JudgmentDamage.share(owner, t, IsrafilRules.NIHILITY_PULSE_SHARE);
            }
        }

        /** The last implosion: only when the field runs out by itself (not evicted, cleared or ownerless), the owner online and alive in this level; one bounded box query. */
        @Override
        public void end(ServerLevel level, JudgmentFields.Field f) {
            if (!JudgmentConfig.israfilUpgrades() || level.getGameTime() < f.endsAt) return;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(f.owner);
            if (owner == null || !owner.isAlive() || owner.serverLevel() != level) return;
            AABB box = new AABB(f.center, f.center).inflate(f.radius, 3.0, f.radius);
            double r2 = f.radius * f.radius;
            int hit = 0;
            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && !x.isSpectator() && x != owner && !x.isAlliedTo(owner))) {
                if (hit >= JudgmentRules.MAX_FIELD_TARGETS) break;
                double dx = t.getX() - f.center.x, dz = t.getZ() - f.center.z;
                if (dx * dx + dz * dz > r2) continue;
                hit++;
                JudgmentDamage.share(owner, t, IsrafilRules.NIHILITY_FINAL_SHARE);
                IsrafilState.pull(t, f.center, IsrafilRules.COLLAPSE_PULL);
            }
            if (hit > 0) TrialFx.collapse(level, f.center, (float) (f.radius * 0.6), 10);
        }
    };

    /** Coordinate Shift: swaps places with the creature in the line of sight, or with the projectile nearest to it; every mob that was after the owner forgets who it was fighting. */
    private static boolean shift(ManasSkillInstance i, ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        boolean shear = JudgmentConfig.israfilUpgrades();
        Entity other = JudgmentState.first(p, IsrafilRules.SHIFT_RANGE, 1.2);
        if (other == null) other = projectile(p);
        if (other == null) { fail(p, "Nothing in your sight to trade places with."); return false; }
        if (other instanceof LivingEntity le && !IsrafilState.movable(le)) { fail(p, le.getDisplayName().getString() + " cannot be moved."); return false; }
        Vec3 mine = p.position(), theirs = other.position();
        dropAggro(p, mine);
        if (other instanceof Mob m) m.setTarget(null);
        TrialFx.shift(l, mine, theirs);
        IsrafilState.place(other, l, mine);
        IsrafilState.place(p, l, theirs);
        if (shear && other instanceof LivingEntity le && le.isAlive() && !le.isAlliedTo(p)) JudgmentDamage.hit(p, le, (float) IsrafilRules.SHIFT_SHEAR_DAMAGE, mastery(p, i));
        return true;
    }

    private static Entity projectile(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getViewVector(1f).scale(IsrafilRules.SHIFT_PROJECTILE_RANGE));
        List<Projectile> near = p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(eye, end).inflate(3.0), x -> x.isAlive() && x.getOwner() != p);
        Projectile best = null;
        double bd = Double.MAX_VALUE;
        int n = 0;
        for (Projectile x : near) {
            if (n++ >= 8) break;
            double d = x.distanceToSqr(p);
            if (d < bd) { bd = d; best = x; }
        }
        return best;
    }

    /** Every mob within thirty-two blocks (the twenty-four nearest) that has the owner as its target loses it. */
    private static void dropAggro(ServerPlayer p, Vec3 at) {
        double r = IsrafilRules.SHIFT_AGGRO_RADIUS;
        List<Mob> mobs = p.serverLevel().getEntitiesOfClass(Mob.class, new AABB(at, at).inflate(r), m -> m.isAlive() && m.getTarget() == p);
        int n = 0;
        for (Mob m : mobs) {
            if (n++ >= IsrafilRules.SHIFT_MAX_AGGRO) break;
            m.setTarget(null);
        }
    }

    // ---------------------------------------------------------------- Rift Walk (held)
    @Override
    protected void startHeld(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {
        double perSecond = JudgmentRules.cost(m, mastered, JudgmentConfig.costScale());
        if (!p.isCreative() && !TensuraBridge.spendMagicule(p, perSecond)) { fail(p, "Not enough magicules (need " + (long) perSecond + " a second)."); return; }
        String why = RiftWalk.begin(p, IsrafilRules.riftSeconds(mastered), perSecond);
        if (why != null) {
            if (!p.isCreative()) TensuraBridge.refundMagicule(p, perSecond);
            fail(p, why);
            return;
        }
        startCooldown(i, m, mastered);
        addMasteryPoint(i, p);
        i.markDirty();
    }

    @Override
    protected boolean held(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks, boolean mastered) {
        return RiftWalk.walking(p.getUUID());
    }

    @Override
    protected void released(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks) {
        if (RiftWalk.walking(p.getUUID())) RiftWalk.end(p, false, "The rift closes.");
    }

    // ---------------------------------------------------------------- The Grand Trumpet (mastery)
    /** Every creature within a hundred blocks (the forty-eight nearest) falls silent, every skill of an enemy player is put on a cooldown of at least a minute, and every field of another owner there ends. */
    static boolean trumpet(ManasSkillInstance i, ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        boolean blast = JudgmentConfig.israfilUpgrades();
        double mastery = mastery(p, i);
        double r = IsrafilRules.TRUMPET_RADIUS;
        List<LivingEntity> near = new ArrayList<>(l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r, Math.min(r, 64.0), r),
                x -> x != p && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(p) && x.distanceToSqr(p) <= r * r));
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= IsrafilRules.TRUMPET_MAX_TARGETS) break;
            JudgmentEffects.silence(t, IsrafilRules.TRUMPET_SILENCE_TICKS);
            if (t instanceof ServerPlayer sp) resetCooldowns(sp);
            if (blast) JudgmentDamage.hit(p, t, (float) IsrafilRules.trumpetDamage(Math.sqrt(t.distanceToSqr(p))), mastery);
        }
        JudgmentFields.clearAround(l, p.position(), r, p.getUUID());
        PocketLabyrinth.endNear(l, p.position(), r, p.getUUID());
        TrialFx.trumpet(l, p.position(), (float) r);
        return true;
    }

    /** Puts every cooldown of a player's skills at a minute or more (the Trumpet "resets enemy cooldowns to maximum"; the maximum is capped, so a skill is never locked for long). */
    private static void resetCooldowns(ServerPlayer target) {
        try {
            var skills = SkillAPI.getSkillsFrom(target);
            if (skills == null) return;
            for (ManasSkillInstance s : new ArrayList<>(skills.getLearnedSkills())) {
                int modes = Math.min(IsrafilRules.TRUMPET_MAX_MODES, Math.max(1, s.getModes()));
                boolean changed = false;
                for (int k = 0; k < modes; k++) {
                    int now = s.getCoolDown(k), want = IsrafilRules.trumpetCooldown(now);
                    if (want > now) { s.setCoolDown(want, k); changed = true; }
                }
                if (changed) s.markDirty();
            }
            skills.markDirty();
        } catch (RuntimeException failed) {
            // a skill that cannot be read is left alone: the Trumpet must never break a cast
        }
    }
}
