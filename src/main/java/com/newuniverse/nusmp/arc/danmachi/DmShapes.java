package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathMath;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nine of the ten ordinary moves of every DanMachi monster are one of a few shapes on the floor (a cone, a ring round the body, a ring wave, a lane, a
 * set of marks, a charge, a leap, a volley, a set of spokes, a sweeping breath) and only the numbers and the material differ. This class runs a
 * {@link DmSpec} row of one of those shapes: first the telegraph, drawn from the aim the monster locked when the wind-up began (so a foe can step out of it), then
 * the strike, which asks {@link DmGeometry} the same question the telegraph did. Everything that lasts more than a tick (a wave, a charge, a leap, a breath,
 * a volley) is a zone or a delayed task of the spell runtime, wrapped in {@link DmSafe}; every hit goes through the boss's own damage law
 * ({@code strike}: power tuning, the victim's EP, the balance law's cap); a move that can hit one foe several times shares one damage budget. No block is
 * placed, moved or broken.
 */
final class DmShapes {
    private DmShapes() {}

    /** The ring wave: where its front starts, how fast it runs (blocks a tick), how thick it is, and how high a foe must be off the floor to jump it. */
    static final double WAVE_START = 1.2, WAVE_SPEED = 0.7, WAVE_THICK = 1.0, WAVE_JUMP = 0.75;
    /** How far above and below the plane of an effect a foe is still hit (a gallery or a heap of rubble is out of reach). */
    static final double BAND_UP = 2.8, BAND_DOWN = 1.8;
    /** The share of a victim's maximum health that one move may take in all, however often it hits. */
    static final double BUDGET = 0.45;
    /** Ticks between two pulses of a breath, and between two rings of a stomp. */
    static final int PULSE_GAP = 4;
    private static final int MAX_VICTIMS = 24;

    // ================================================================ the aim a wind-up locks
    /** What the first tick of a wind-up decided: the direction, the target's spot, the marks and the landing, so the telegraph and the strike agree. */
    static final class Aim {
        final String moveId;
        double fx, fz, tx, ty, tz, phase;
        List<Vec3> marks = List.of();
        Vec3 landing;

        Aim(String moveId) { this.moveId = moveId; }
    }

    /** Locks the aim of {@code s} on {@code target} as it stands now. */
    static Aim lock(DanMachiBoss b, DmSpec s, LivingEntity target) {
        Aim a = new Aim(s.id());
        double[] f = DmGeometry.unit(target.getX() - b.getX(), target.getZ() - b.getZ(), DmGeometry.dirOfYaw(b.getYRot()));
        a.fx = f[0];
        a.fz = f[1];
        a.tx = target.getX();
        a.ty = target.getY();
        a.tz = target.getZ();
        a.phase = b.getRandom().nextDouble() * Math.PI * 2;
        if (s.shape() == DmSpec.Shape.MARKS) a.marks = markSpots(b, s, target, a);
        if (s.shape() == DmSpec.Shape.LEAP) {
            Vec3 g = ZogratisBoss.groundOrNull(b.level(), new Vec3(a.tx, a.ty, a.tz));
            a.landing = g == null ? new Vec3(a.tx, a.ty, a.tz) : g;
        }
        return a;
    }

    /** The aim of this move: the locked one, or one made now when the wind-up never locked it (a move that began with no tell). */
    static Aim aimOf(DanMachiBoss b, DmSpec s, LivingEntity target) {
        Aim a = b.aim();
        if (a == null || !s.id().equals(a.moveId)) {
            a = lock(b, s, target);
            b.setAim(a);
        }
        return a;
    }

    private static List<Vec3> markSpots(DanMachiBoss b, DmSpec s, LivingEntity target, Aim a) {
        int n = s.scaledCount(b.tuning().castSlots());
        List<double[]> others = new ArrayList<>();
        List<LivingEntity> foes = b.foesAround(b.position(), 48.0);
        foes.sort((p, q) -> Double.compare(p.distanceToSqr(b), q.distanceToSqr(b)));
        for (LivingEntity e : foes) if (e != target) others.add(new double[]{e.getX(), e.getZ()});
        List<double[]> plan = DmGeometry.markPlan(n, a.tx, a.tz, others.toArray(new double[0][]), s.reach(), a.phase);
        List<Vec3> out = new ArrayList<>();
        for (double[] p : plan) {
            Vec3 g = ZogratisBoss.groundOrNull(b.level(), new Vec3(p[0], a.ty, p[1]));
            out.add(g == null ? new Vec3(p[0], a.ty, p[1]) : g);
        }
        return out;
    }

    // ================================================================ the telegraph
    /** Every third tick of the wind-up. The first call (the first three ticks) locks the aim and draws the long-lived effects; the later calls only refresh the dust. */
    static void tell(DanMachiBoss b, DmSpec s, LivingEntity target, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        boolean first = TellClock.first(left, total);
        if (first) b.setAim(lock(b, s, target));
        Aim a = aimOf(b, s, target);
        DmFx.Style st = b.style();
        Vec3 at = b.position();
        int wind = first ? total : 0;
        switch (s.shape()) {
            case CONE -> DmFx.coneTell(b, st, at, a.fx, a.fz, s.width(), s.reach(), wind);
            case SPIN -> groundRing(b, st, at, s.reach(), wind);
            case WAVE -> groundRing(b, st, at, s.reach(), wind);
            case LANE -> {
                for (int i = 0; i < s.count(); i++) {
                    Vec3 o = laneOrigin(b, s, a, i);
                    DmFx.laneTell(b, st, o, o.add(a.fx * s.reach(), 0, a.fz * s.reach()), s.width(), wind);
                }
            }
            case MARKS -> {
                int shown = 0;
                for (Vec3 m : a.marks) {
                    if (first && shown++ < 4) DmFx.spotTell(b, st, m, s.reach(), total);
                    else Tells.ring(sl, m, s.reach(), st.tint(b), 1.1f, 20);
                }
            }
            case CHARGE -> {
                Vec3 end = at.add(a.fx * s.reach(), 0, a.fz * s.reach());
                DmFx.laneTell(b, st, at, end, s.width(), wind);
            }
            case LEAP -> {
                if (a.landing != null) {
                    if (first) DmFx.spotTell(b, st, a.landing, s.reach(), total);
                    else Tells.ring(sl, a.landing, s.reach(), st.tint(b), 1.2f, 28);
                }
            }
            case VOLLEY -> {
                double[] fan = DmGeometry.fan(s.scaledCount(b.tuning().castSlots()), s.width());
                for (double off : fan) {
                    double[] d = DmGeometry.rotate(a.fx, a.fz, off);
                    DmFx.line(b, st, at.add(0, b.getBbHeight() * 0.6, 0), at.add(d[0] * s.reach(), b.getBbHeight() * 0.6, d[1] * s.reach()));
                }
            }
            case SPOKES -> {
                double[][] sp = spokeDirs(s.scaledCount(b.tuning().castSlots()), a);
                for (double[] d : sp) DmFx.laneTell(b, st, at, at.add(d[0] * s.reach(), 0, d[1] * s.reach()), s.width(), wind);
            }
            case BREATH -> {
                double sweep = s.extra();
                double half = s.width() + (sweep >= 360 ? 0 : sweep / 2.0);
                if (sweep >= 360) groundRing(b, st, at, s.reach(), wind);
                else DmFx.coneTell(b, st, at, a.fx, a.fz, half, s.reach(), wind);
            }
            case CUSTOM -> groundRing(b, st, at, s.reach(), wind);
        }
    }

    /** A ring round {@code at}: the long-lived effect of the material when {@code wind} is the wind-up (the first call), only the dust ring when it is 0 (the refreshes). */
    static void groundRing(DanMachiBoss b, DmFx.Style st, Vec3 at, double reach, int wind) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        if (wind > 0) DmFx.ringTell(b, st, at, reach, wind);
        else Tells.ring(sl, at, reach, st.tint(b), 1.1f, (int) Math.max(16, Math.min(60, reach * 5)));
    }

    // ================================================================ the strike
    /** Runs the row. Returns the ticks the effect keeps going after the call (the boss starts nothing else meanwhile). */
    static int run(DanMachiBoss b, DmSpec s, LivingEntity target) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        Aim a = aimOf(b, s, target);
        b.swing(InteractionHand.MAIN_HAND);
        return switch (s.shape()) {
            case CONE -> cone(b, s, a);
            case SPIN -> spin(b, s);
            case WAVE -> waves(b, b.position(), s.reach(), (float) s.damage(), s.knock(), s.count(), (int) s.extra());
            case LANE -> lanes(b, s, a, sl);
            case MARKS -> marks(b, s, a);
            case CHARGE -> charge(b, a.fx, a.fz, s.reach(), s.width(), (float) s.damage(), s.knock(), s.extra(), null);
            case LEAP -> leap(b, s, a);
            case VOLLEY -> volley(b, s, a);
            case SPOKES -> spokes(b, s, a);
            case BREATH -> breath(b, s, a);
            case CUSTOM -> 0;
        };
    }

    // ---------------------------------------------------------------- landing a blow
    /** Takes a gliding player out of the glide before they are thrown, so a shove into a wall can never become the game's kinetic-energy damage. */
    private static void grounded(LivingEntity e) {
        if (e instanceof Player p && p.isFallFlying()) p.stopFallFlying();
    }

    /** Throws a victim away from where the blow came from (the unit vector (dx, dz) points from the blow to the victim) and a little up. */
    static void push(LivingEntity e, double knock, double lift, double dx, double dz) {
        grounded(e);
        if (knock > 0) e.knockback(knock, -dx, -dz);
        if (lift > 0) {
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x, Math.max(v.y, lift), v.z);
        }
        e.hurtMarked = true;
        e.fallDistance = 0;
    }

    /**
     * Hurts {@code e} through the boss's damage law and throws them. With a budget the hit is cut so that the victim never loses more than the budget's
     * share of their maximum health to the move in all. False if nothing landed.
     */
    static boolean land(DanMachiBoss b, LivingEntity e, float base, double knock, double lift, double dx, double dz, GoliathMath.DamageBudget budget, boolean magic) {
        float amount = base;
        double expected = 0.0;
        if (budget != null) {
            expected = b.bossDamage(e, base * b.victimFactor(e));
            double scale = budget.allow(e.getId(), e.getMaxHealth(), expected);
            if (scale <= 0.0) return false;
            amount = (float) (base * scale);
            expected *= scale;
        }
        if (!b.strike(e, amount, magic)) return false;
        if (budget != null) budget.spend(e.getId(), expected);
        push(e, knock, lift, dx, dz);
        return true;
    }

    /** The blow of a shape row on one foe: the row's own damage, knock and lift. */
    private static boolean blow(DanMachiBoss b, DmSpec s, LivingEntity e, double dx, double dz, GoliathMath.DamageBudget budget) {
        double d = Math.max(1e-3, Math.hypot(dx, dz));
        return land(b, e, (float) s.damage(), s.knock(), s.extra() == 1 && s.shape() == DmSpec.Shape.CONE ? 0.55 : 0.0, dx / d, dz / d, budget, false);
    }

    static boolean inBand(double dy) { return dy >= -BAND_DOWN && dy <= BAND_UP; }

    /** How far the feet of {@code e} are above the floor under them (0 if they stand on it). */
    static double floorHeight(ServerLevel sl, LivingEntity e) {
        if (e.onGround()) return 0;
        return Math.max(0, e.getY() - ZogratisBoss.ground(sl, e.position()).y);
    }

    static GoliathMath.DamageBudget budget() { return new GoliathMath.DamageBudget(BUDGET); }

    static void sound(ServerLevel sl, Vec3 at, SoundEvent ev, float volume, float pitch) {
        sl.playSound(null, BlockPos.containing(at), ev, SoundSource.HOSTILE, volume, pitch);
    }

    // ---------------------------------------------------------------- a cone
    private static int cone(DanMachiBoss b, DmSpec s, Aim a) {
        ServerLevel sl = (ServerLevel) b.level();
        Vec3 at = b.position();
        DmFx.coneStrike(b, b.style(), at.add(0, 0.2, 0), a.fx, a.fz, s.reach());
        sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.4f);
        coneHit(b, at, a.fx, a.fz, s.reach(), s.width(), (float) s.damage(), s.knock(), s.extra() == 1 ? 0.55 : 0.0, null, false);
        return 0;
    }

    /** Hurts every foe inside a cone from {@code at} (facing the unit vector (fx, fz), opening {@code half} degrees each side) once. Returns how many it hit. */
    static int coneHit(DanMachiBoss b, Vec3 at, double fx, double fz, double reach, double half, float base, double knock, double lift, GoliathMath.DamageBudget budget, boolean magic) {
        int n = 0, seen = 0;
        for (LivingEntity e : b.foesAround(at, reach + 2.5)) {
            if (++seen > MAX_VICTIMS) break;
            double dx = e.getX() - at.x, dz = e.getZ() - at.z;
            if (!inBand(e.getY() - at.y) || !DmGeometry.inCone(dx, dz, fx, fz, half, reach, e.getBbWidth() / 2.0)) continue;
            double d = Math.max(1e-3, Math.hypot(dx, dz));
            if (land(b, e, base, knock, lift, dx / d, dz / d, budget, magic)) n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- a circle round the body
    private static int spin(DanMachiBoss b, DmSpec s) {
        ServerLevel sl = (ServerLevel) b.level();
        Vec3 at = b.position();
        DmFx.ringStrike(b, b.style(), at, s.reach(), 14);
        DmFx.burst(b, b.style(), at.add(0, 0.5, 0), 24);
        sound(sl, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.6f);
        int seen = 0;
        for (LivingEntity e : b.foesAround(at, s.reach() + 2.0)) {
            if (++seen > MAX_VICTIMS) break;
            double dx = e.getX() - at.x, dz = e.getZ() - at.z;
            if (!inBand(e.getY() - at.y) || Math.hypot(dx, dz) > s.reach() + e.getBbWidth() / 2.0) continue;
            blow(b, s, e, dx, dz, null);
        }
        return 0;
    }

    // ---------------------------------------------------------------- ring waves (a stomp, a slam, a surge, a nova)
    /**
     * {@code waves} rings that run out from {@code centre}, {@code gap} ticks apart, each passing every foe once: whoever it passes on the floor takes the
     * blow unless they have jumped it. One budget is shared by all the rings of the move. Returns the ticks the last ring needs.
     */
    static int waves(DanMachiBoss b, Vec3 centre, double reach, float base, double knock, int waves, int gap) {
        int n = Math.max(1, waves);
        GoliathMath.DamageBudget budget = n > 1 ? budget() : null;
        for (int w = 0; w < n; w++) {
            if (w == 0) oneWave(b, centre, reach, base, knock, budget);
            else b.later(w * Math.max(1, gap), DmSafe.task("ring wave", () -> oneWave(b, centre, reach, base, knock, budget)));
        }
        return DmGeometry.waveTicks(reach, WAVE_START, WAVE_SPEED, WAVE_THICK) + (n - 1) * Math.max(1, gap);
    }

    private static void oneWave(DanMachiBoss b, Vec3 centre, double reach, float base, double knock, GoliathMath.DamageBudget budget) {
        if (!(b.level() instanceof ServerLevel sl) || !b.isAlive()) return;
        Set<Integer> hit = new HashSet<>();
        int ticks = DmGeometry.waveTicks(reach, WAVE_START, WAVE_SPEED, WAVE_THICK);
        DmFx.ringStrike(b, b.style(), centre, reach, ticks);
        DmFx.burst(b, b.style(), centre.add(0, 0.4, 0), 20);
        sound(sl, centre, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.5f);
        SpellRuntime.zone(sl, ticks, 1, DmSafe.step("ring wave", age -> {
            if (!b.isAlive() || b.isRemoved()) return;
            double wr = DmGeometry.waveRadius(age, WAVE_START, WAVE_SPEED);
            if (age % 2 == 0) Tells.ring(sl, centre, Math.min(wr, reach), b.style().tint(b), 1.0f, (int) Math.max(16, Math.min(44, wr * 4)));
            int seen = 0;
            for (LivingEntity e : b.foesAround(centre, reach + 3)) {
                if (hit.contains(e.getId())) continue;
                if (++seen > MAX_VICTIMS) break;
                double dx = e.getX() - centre.x, dz = e.getZ() - centre.z, dist = Math.hypot(dx, dz);
                if (dist > reach + e.getBbWidth() / 2.0 || !inBand(e.getY() - centre.y) || !DmGeometry.ringHit(dist, wr, WAVE_THICK)) continue;
                if (floorHeight(sl, e) > WAVE_JUMP) {
                    if (e instanceof ServerPlayer sp && hit.add(-e.getId() - 1)) sp.displayClientMessage(Component.literal("You jumped the wave."), true);
                    continue;
                }
                hit.add(e.getId());
                double d = Math.max(1e-3, dist);
                land(b, e, base, knock, 0.45, dx / d, dz / d, budget, false);
            }
        }));
    }

    // ---------------------------------------------------------------- lanes in front of the boss
    private static Vec3 laneOrigin(DanMachiBoss b, DmSpec s, Aim a, int i) {
        double off = (i - (s.count() - 1) / 2.0) * (s.width() + 0.8);
        return b.position().add(-a.fz * off, 0, a.fx * off);
    }

    private static int lanes(DanMachiBoss b, DmSpec s, Aim a, ServerLevel sl) {
        int n = Math.max(1, s.count());
        double perBlock = s.extra();
        int ticks = perBlock <= 0 ? 0 : (int) Math.ceil(s.reach() * perBlock);
        GoliathMath.DamageBudget budget = budget();
        Set<Integer> hit = new HashSet<>();
        for (int i = 0; i < n; i++) {
            Vec3 o = laneOrigin(b, s, a, i);
            DmFx.laneStrike(b, b.style(), o, o.add(a.fx * s.reach(), 0, a.fz * s.reach()), s.width(), Math.max(14, ticks));
        }
        sound(sl, b.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.6f, 0.6f);
        if (ticks <= 0) {
            laneHits(b, s, a, n, s.reach(), hit, budget);
            return 0;
        }
        SpellRuntime.zone(sl, ticks, 1, DmSafe.step("lane", age -> {
            if (!b.isAlive() || b.isRemoved()) return;
            laneHits(b, s, a, n, Math.min(s.reach(), (age + 1) / perBlock), hit, budget);
        }));
        return ticks;
    }

    private static void laneHits(DanMachiBoss b, DmSpec s, Aim a, int n, double front, Set<Integer> hit, GoliathMath.DamageBudget budget) {
        for (int i = 0; i < n; i++) laneBlow(b, laneOrigin(b, s, a, i), a.fx, a.fz, front, s.width(), (float) s.damage(), s.knock(), 0.3, hit, budget, false);
    }

    /**
     * Hurts, once each, the foes inside a lane that starts at {@code origin}, runs {@code front} blocks along the unit vector (fx, fz) and is {@code width} wide;
     * those already in {@code hit} are skipped and every foe hurt is added to it. Returns how many it hurt.
     */
    static int laneBlow(DanMachiBoss b, Vec3 origin, double fx, double fz, double front, double width, float base, double knock, double lift, Set<Integer> hit,
                        GoliathMath.DamageBudget budget, boolean magic) {
        int n = 0, seen = 0;
        for (LivingEntity e : b.foesAround(origin, front + 3)) {
            if (hit.contains(e.getId())) continue;
            if (++seen > MAX_VICTIMS) break;
            double dx = e.getX() - origin.x, dz = e.getZ() - origin.z;
            if (!inBand(e.getY() - origin.y) || !DmGeometry.inLane(dx, dz, fx, fz, front, width, e.getBbWidth() / 2.0)) continue;
            double d = Math.max(1e-3, Math.hypot(dx, dz));
            if (land(b, e, base, knock, lift, dx / d, dz / d, budget, magic)) {
                hit.add(e.getId());
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- marks on the ground (a fist, boulders, pillars, spouts)
    private static int marks(DanMachiBoss b, DmSpec s, Aim a) {
        int delay = (int) s.extra();
        List<Vec3> spots = a.marks;
        GoliathMath.DamageBudget budget = spots.size() > 1 ? budget() : null;
        Runnable land = DmSafe.task("marks", () -> {
            if (!(b.level() instanceof ServerLevel sl) || !b.isAlive()) return;
            Set<Integer> hit = new HashSet<>();
            for (Vec3 m : spots) {
                DmFx.spotStrike(b, b.style(), m, s.reach());
                sound(sl, m, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 0.7f);
                int seen = 0;
                for (LivingEntity e : b.foesAround(m, s.reach() + 3)) {
                    if (hit.contains(e.getId())) continue;
                    if (++seen > MAX_VICTIMS) break;
                    double dx = e.getX() - m.x, dz = e.getZ() - m.z;
                    if (!inBand(e.getY() - m.y) || Math.hypot(dx, dz) > s.reach() + e.getBbWidth() / 2.0) continue;
                    if (blow(b, s, e, dx, dz, budget)) hit.add(e.getId());
                }
            }
        });
        if (delay <= 0) {
            land.run();
            return 0;
        }
        int shown = 0;
        for (Vec3 m : spots) if (shown++ < 4) DmFx.spotTell(b, b.style(), m, s.reach(), delay);
        b.later(delay, land);
        return delay;
    }

    // ---------------------------------------------------------------- a charge along a lane
    /**
     * The boss runs {@code length} blocks along (fx, fz) at {@code speed} blocks a tick, hurting every foe in its path once. A wall in the way stops it and
     * stuns it ({@link DanMachiBoss#crash}). {@code after} (or null) runs when the run ends, crash or not. Returns the ticks the run takes.
     */
    static int charge(DanMachiBoss b, double fx, double fz, double length, double width, float base, double knock, double speed, Runnable after) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        int ticks = ChargeMath.ticks(length, speed);
        Vec3 start = b.position();
        GoliathMath.DamageBudget budget = budget();
        Set<Integer> hit = new HashSet<>();
        double yaw = DmGeometry.yawOf(fx, fz);
        DmFx.laneStrike(b, b.style(), start, start.add(fx * length, 0, fz * length), width, ticks + 6);
        sound(sl, start, SoundEvents.RAVAGER_ROAR, 1.2f, 0.9f);
        double[] last = {start.x, start.z};
        boolean[] done = {false};
        SpellRuntime.zone(sl, ticks, 1, DmSafe.step("charge", age -> {
            if (done[0]) return;
            if (!b.isAlive() || b.isRemoved() || b.stunned()) { done[0] = true; return; }
            double travelled = Math.hypot(b.getX() - start.x, b.getZ() - start.z);
            double moved = Math.hypot(b.getX() - last[0], b.getZ() - last[1]);
            last[0] = b.getX();
            last[1] = b.getZ();
            if (age >= 2 && b.horizontalCollision && moved < speed * 0.4) {
                done[0] = true;
                b.crash(new Vec3(b.getX() + fx * 1.2, b.getY() + 1.0, b.getZ() + fz * 1.2));
                if (after != null) after.run();
                return;
            }
            if (travelled >= length || age >= ticks) {
                done[0] = true;
                b.setDeltaMovement(0, b.getDeltaMovement().y, 0);
                if (after != null) after.run();
                return;
            }
            b.setYRot((float) yaw);
            b.setYHeadRot((float) yaw);
            b.yBodyRot = (float) yaw;
            b.setDeltaMovement(fx * speed, Math.min(0.0, b.getDeltaMovement().y), fz * speed);
            Vec3 at = b.position();
            int seen = 0;
            for (LivingEntity e : b.foesAround(at, width + 3.0)) {
                if (hit.contains(e.getId())) continue;
                if (++seen > MAX_VICTIMS) break;
                double dx = e.getX() - at.x, dz = e.getZ() - at.z;
                if (!inBand(e.getY() - at.y) || !DmGeometry.inLane(dx, dz, fx, fz, 2.2, width, e.getBbWidth() / 2.0)) continue;
                double d = Math.max(1e-3, Math.hypot(dx, dz));
                if (land(b, e, base, knock, 0.35, (dx / d + fx) * 0.5, (dz / d + fz) * 0.5, budget, false)) hit.add(e.getId());      // thrown ahead of the bull and to the side
            }
            if (age % 2 == 0) DmFx.burst(b, b.style(), at.add(0, 0.3, 0), 6);
        }));
        return ticks + 4;
    }

    // ---------------------------------------------------------------- a leap onto the target's mark
    private static int leap(DanMachiBoss b, DmSpec s, Aim a) {
        if (!(b.level() instanceof ServerLevel sl) || a.landing == null) return 0;
        Vec3 from = b.position();
        Vec3 to = a.landing;
        int air = Math.max(4, DmGeometry.leapTicks(from.distanceTo(to), (int) s.extra()));
        double peak = Math.max(2.0, Math.min(6.0, from.distanceTo(to) * 0.3 + 1.5));
        boolean[] done = {false};
        sound(sl, from, SoundEvents.RAVAGER_ATTACK, 1.4f, 0.7f);
        DmFx.burst(b, b.style(), from.add(0, 0.3, 0), 18);
        SpellRuntime.zone(sl, air, 1, DmSafe.step("leap", age -> {
            if (done[0]) return;
            if (!b.isAlive() || b.isRemoved()) { done[0] = true; return; }
            int k = age + 1;
            if (k >= air) {
                done[0] = true;
                b.setDeltaMovement(0, 0, 0);
                if (sl.noCollision(b, b.getBoundingBox().move(to.subtract(b.position())))) b.teleportTo(to.x, to.y, to.z);      // exactly on the mark, unless a wall is there
                landing(b, s, to);
                return;
            }
            double t = k / (double) air;
            Vec3 want = new Vec3(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t + DmGeometry.arcHeight(k, air, peak), from.z + (to.z - from.z) * t);
            Vec3 step = want.subtract(b.position());
            double len = step.length();
            if (len > 3.5) step = step.scale(3.5 / len);
            b.setDeltaMovement(step);
            b.fallDistance = 0;
        }));
        return air + 4;
    }

    /** The landing of a leap: a ring of the row's reach round the spot, and the dust of the impact. */
    private static void landing(DanMachiBoss b, DmSpec s, Vec3 at) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        DmFx.spotStrike(b, b.style(), at, s.reach());
        DmFx.ringStrike(b, b.style(), at, s.reach(), 16);
        sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.5f);
        int seen = 0;
        for (LivingEntity e : b.foesAround(at, s.reach() + 2.5)) {
            if (++seen > MAX_VICTIMS) break;
            double dx = e.getX() - at.x, dz = e.getZ() - at.z;
            if (!inBand(e.getY() - at.y) || Math.hypot(dx, dz) > s.reach() + e.getBbWidth() / 2.0) continue;
            blow(b, s, e, dx, dz, null);
        }
    }

    // ---------------------------------------------------------------- projectiles that fly in a fan
    private static int volley(DanMachiBoss b, DmSpec s, Aim a) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        int n = s.scaledCount(b.tuning().castSlots());
        double[] fan = DmGeometry.fan(n, s.width());
        double speed = Math.max(0.3, s.extra());
        int ticks = (int) Math.ceil(s.reach() / speed);
        Vec3 origin = b.position().add(0, b.getBbHeight() * 0.6, 0);
        GoliathMath.DamageBudget budget = budget();
        List<double[]> shots = new ArrayList<>();
        for (double off : fan) {
            double[] d = DmGeometry.rotate(a.fx, a.fz, off);
            shots.add(new double[]{origin.x + d[0] * 1.2, origin.y, origin.z + d[1] * 1.2, d[0], d[1], 1.0});
            DmFx.laneStrike(b, b.style(), origin, origin.add(d[0] * s.reach(), 0, d[1] * s.reach()), 1.0, ticks);
        }
        sound(sl, origin, SoundEvents.SKELETON_SHOOT, 1.4f, 0.5f);
        SpellRuntime.zone(sl, ticks, 1, DmSafe.step("volley", age -> {
            if (!b.isAlive() || b.isRemoved()) return;
            for (double[] p : shots) {
                if (p[5] == 0.0) continue;
                p[0] += p[3] * speed;
                p[2] += p[4] * speed;
                Vec3 at = new Vec3(p[0], p[1], p[2]);
                if (sl.getBlockState(BlockPos.containing(at)).blocksMotion()) {
                    p[5] = 0.0;
                    DmFx.burst(b, b.style(), at, 6);
                    continue;
                }
                for (LivingEntity e : b.foesAround(at, 2.5)) {
                    if (Math.abs(e.getY() + e.getBbHeight() / 2 - at.y) > e.getBbHeight() / 2 + 0.8) continue;
                    if (Math.hypot(e.getX() - at.x, e.getZ() - at.z) > 0.9 + e.getBbWidth() / 2.0) continue;
                    p[5] = 0.0;
                    DmFx.burst(b, b.style(), at, 10);
                    land(b, e, (float) s.damage(), s.knock(), 0.2, p[3], p[4], budget, false);
                    break;
                }
            }
        }));
        return ticks;
    }

    // ---------------------------------------------------------------- spokes of cracks from the boss
    private static double[][] spokeDirs(int count, Aim a) {
        double phase = Math.atan2(a.fz, a.fx);
        return DmGeometry.spokes(count, phase);
    }

    private static int spokes(DanMachiBoss b, DmSpec s, Aim a) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        int n = s.scaledCount(b.tuning().castSlots());
        double[][] dirs = spokeDirs(n, a);
        Vec3 at = b.position();
        for (double[] d : dirs) DmFx.laneStrike(b, b.style(), at, at.add(d[0] * s.reach(), 0, d[1] * s.reach()), s.width(), 40);
        DmFx.burst(b, b.style(), at.add(0, 0.4, 0), 24);
        sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.6f);
        int seen = 0;
        for (LivingEntity e : b.foesAround(at, s.reach() + 3)) {
            if (++seen > MAX_VICTIMS) break;
            double dx = e.getX() - at.x, dz = e.getZ() - at.z;
            if (!inBand(e.getY() - at.y) || !DmGeometry.inAnySpoke(dx, dz, dirs, s.reach(), s.width(), e.getBbWidth() / 2.0)) continue;
            blow(b, s, e, dx, dz, null);
        }
        return 0;
    }

    /**
     * One set of spokes at once from the boss: draws them, and hurts every foe in one of them that is not already in {@code hit}. For the moves that write
     * their own spokes (the Wild Cross, the Dungeon's Wrath). Returns the foes hit.
     */
    static int spokesAt(DanMachiBoss b, double[][] dirs, double length, double width, float base, double knock, Set<Integer> hit, GoliathMath.DamageBudget budget) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        Vec3 at = b.position();
        for (double[] d : dirs) DmFx.laneStrike(b, b.style(), at, at.add(d[0] * length, 0, d[1] * length), width, 30);
        DmFx.burst(b, b.style(), at.add(0, 0.4, 0), 20);
        sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.7f);
        int n = 0, seen = 0;
        for (LivingEntity e : b.foesAround(at, length + 3)) {
            if (hit.contains(e.getId())) continue;
            if (++seen > MAX_VICTIMS) break;
            double dx = e.getX() - at.x, dz = e.getZ() - at.z;
            if (!inBand(e.getY() - at.y) || !DmGeometry.inAnySpoke(dx, dz, dirs, length, width, e.getBbWidth() / 2.0)) continue;
            double d = Math.max(1e-3, Math.hypot(dx, dz));
            if (land(b, e, base, knock, 0.3, dx / d, dz / d, budget, false)) {
                hit.add(e.getId());
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- a breath that sweeps
    private static int breath(DanMachiBoss b, DmSpec s, Aim a) {
        double aimYaw = DmGeometry.yawOf(a.fx, a.fz);
        double sweep = s.extra();
        boolean flip = b.getRandom().nextBoolean();
        return sweep(b, aimYaw, flip ? -sweep : sweep, s.width(), s.reach(), s.count(), (float) s.damage(), 1, true);
    }

    /**
     * A breath: {@code pulses} pulses {@value #PULSE_GAP} ticks apart, each a cone of half angle {@code half} and reach {@code reach} from the boss, whose
     * axis turns through {@code sweepDeg} degrees from {@code startYaw} (a sweep of 360 or more turns all the way round). {@code beams} cones stand
     * opposite one another. A foe the cone stands on is hit by every pulse, but the move's budget bounds what it can take in all. Returns the ticks it lasts.
     */
    static int sweep(DanMachiBoss b, double startYaw, double sweepDeg, double half, double reach, int pulses, float base, int beams, boolean magic) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        int n = Math.max(1, pulses);
        GoliathMath.DamageBudget budget = budget();
        boolean full = Math.abs(sweepDeg) >= 360.0;
        int ticks = n * PULSE_GAP;
        sound(sl, b.position(), SoundEvents.ENDER_DRAGON_GROWL, 1.6f, 1.1f);
        SpellRuntime.zone(sl, ticks, 1, DmSafe.step("breath", age -> {
            if (!b.isAlive() || b.isRemoved() || b.stunned()) return;
            if (age % PULSE_GAP != 0) return;
            int k = age / PULSE_GAP;
            double turn = full ? sweepDeg * k / n : sweepDeg * (n == 1 ? 0.0 : k / (double) (n - 1) - 0.5);
            Vec3 mouth = b.position().add(0, b.getBbHeight() * 0.65, 0);
            for (int j = 0; j < Math.max(1, beams); j++) {
                double yaw = startYaw + turn + j * 180.0;
                double[] d = DmGeometry.dirOfYaw(yaw);
                if (j == 0) {
                    b.setYRot((float) yaw);
                    b.setYHeadRot((float) yaw);
                    b.yBodyRot = (float) yaw;
                }
                DmFx.breathStrike(b, b.style(), mouth, mouth.add(d[0] * reach, -0.4, d[1] * reach), PULSE_GAP + 4);
                int seen = 0;
                for (LivingEntity e : b.foesAround(b.position(), reach + 2.5)) {
                    if (++seen > MAX_VICTIMS) break;
                    double dx = e.getX() - b.getX(), dz = e.getZ() - b.getZ();
                    if (!inBand(e.getY() - b.getY()) || !DmGeometry.inCone(dx, dz, d[0], d[1], half, reach, e.getBbWidth() / 2.0)) continue;
                    double dist = Math.max(1e-3, Math.hypot(dx, dz));
                    land(b, e, base, 0.25, 0.0, dx / dist, dz / dist, budget, magic);
                }
            }
        }));
        return ticks + 2;
    }

    // ================================================================ small things the bosses share
    /** Gives the foes within {@code radius} of {@code at} a fear (slow and weak) of the given strength: the Howl. Returns how many were frightened. */
    static int frighten(DanMachiBoss b, Vec3 at, double radius, int monsterLevel, int power) {
        if (!DanMachiConfig.howlFear() || !com.newuniverse.nusmp.balance.BalanceLaw.beginControl(b)) return 0;
        int n = 0;
        for (LivingEntity e : b.foesAround(at, radius + 2.0)) {
            int level = PartyLevels.levelOf(e);
            boolean anti = e instanceof Player p && com.newuniverse.nusmp.antimagic.AntiMagic.isUser(p);
            boolean real = !(e instanceof Player q) || !q.isCreative() && !q.isSpectator();
            if (!HowlMath.afraid(monsterLevel, level, anti, real, e.distanceTo(b), radius)) continue;
            int ticks = com.newuniverse.nusmp.balance.BalanceLaw.controlTicks(e, HowlMath.fearTicks(power));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, HowlMath.slowAmplifier(monsterLevel, level)));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
            if (e instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("Fear grips you: " + DungeonLevel.label(level) + " against "
                    + DungeonLevel.label(monsterLevel) + ". Anti-magic frees you from it."), true);
            n++;
        }
        return n;
    }
}
