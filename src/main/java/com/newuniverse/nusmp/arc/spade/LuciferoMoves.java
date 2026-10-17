package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.GravityMath;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: what Lucifero's moves DO to the world, as plain static functions on any {@link ZogratisBoss}, so {@code LuciferoBoss} stays a list of moves, their
 * telegraphs and the passive rules, and each effect can be read (and fixed) on its own. Everything is bounded the way Dante's are: a field or a lane is a
 * zone that looks at a few dozen foes every four ticks, a hit is one blow through the boss's damage law and Tensura's own gravity damage types (so
 * gravity resistance counts), a hard hold is asked of the Balance Law first and capped by it, and nothing here ever changes a block. The numbers come from
 * {@link LuciferoTable} and {@link WeightMath}; the rules of who is spared are in {@link BowRules}.
 */
final class LuciferoMoves {
    private LuciferoMoves() {}

    private static final String WHO = "Lucifero";
    /** Plain RGB colours for the dust (the VFX layers add their own alpha). */
    static final int VIOLET_RGB = SpadeRoster.LUCIFERO_COLOR, ACCENT_RGB = SpadeRoster.LUCIFERO_ACCENT, WARN_RGB = 0xFF4A4A, STILL_RGB = 0xE8F4FF;

    static final double PALM_REACH = 5.6, HEAVEN_RADIUS = 2.4, GRASP_REACH = 16, WANDER_RADIUS = 5.5, WANDER_DRIFT = 0.1;
    static final double PALM_COS = Math.cos(Math.toRadians(55));

    // ================================================================ the Crushing Palm
    /** A heavy open hand on everything in the 110 degree wedge in front of him: crushed, knocked down, slowed for a second. */
    static void palm(ZogratisBoss b, float base) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        b.swing(InteractionHand.MAIN_HAND);
        Vec3 face = SpadeFx.flatUnit(b.getViewVector(1f));
        Vec3 hand = ZogratisBoss.ground(sl, b.position().add(face.scale(PALM_REACH * 0.6)));
        ZogratisVfx.gravityImpact(sl, hand, 3.4, VIOLET_RGB);
        sl.playSound(null, b.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.8f, 0.5f);
        for (LivingEntity e : b.foes(PALM_REACH + 1.5)) {
            if (!SpadeFx.inWedge(b.position(), face, e, PALM_REACH, PALM_COS)) continue;
            if (SpadeFx.crush(b, e, (float) (base * SpadeFx.guard01(e)), TensuraDamageTypes.GRAVITY_PRESS)) {
                Vec3 away = SpadeFx.flatUnit(e.position().subtract(b.position()));
                SpadeFx.slam(e, 0.9);
                e.knockback(0.9, -away.x, -away.z);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
            }
        }
    }

    // ================================================================ Falling Heavens
    /** The marks of a rain of {@code n} spots: on the target first, then the others, then scattered (see {@link RainMath}). Snapped to the floor. */
    static List<Vec3> heavenMarks(ZogratisBoss b, LivingEntity target, int n) {
        List<double[]> others = new ArrayList<>();
        for (LivingEntity e : b.foes(40)) if (e != target) others.add(new double[]{e.getX(), e.getZ()});
        List<double[]> plan = RainMath.plan(n, target.getX(), target.getZ(), others.toArray(new double[0][]), HEAVEN_RADIUS, 11, b.getRandom().nextLong());
        List<Vec3> out = new ArrayList<>();
        for (double[] m : plan) out.add(ZogratisBoss.ground(b.level(), new Vec3(m[0], target.getY(), m[1])));
        return out;
    }

    /** The debris lands, mark by mark, a few ticks apart; a foe is hit by at most one piece of the rain. */
    static void heavens(ZogratisBoss b, List<Vec3> marks, float base) {
        if (!(b.level() instanceof ServerLevel sl) || marks.isEmpty()) return;
        Set<Integer> hit = new HashSet<>();
        for (int i = 0; i < marks.size(); i++) {
            final Vec3 m = marks.get(i);
            b.later(3 + (i % 5) * 2, SpadeFx.safe(WHO, "falling heaven", () -> {
                ZogratisVfx.gravityImpact(sl, m, HEAVEN_RADIUS * 1.5, VIOLET_RGB);
                Tells.burst(sl, m.add(0, 0.8, 0), ACCENT_RGB, 22);
                sl.playSound(null, BlockPos.containing(m), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.3f, 0.5f);
                for (LivingEntity e : b.foesAround(m, HEAVEN_RADIUS + 1)) {
                    double dx = e.getX() - m.x, dz = e.getZ() - m.z, dy = e.getY() - m.y;
                    if (dx * dx + dz * dz > HEAVEN_RADIUS * HEAVEN_RADIUS || dy > 4 || dy < -4 || !hit.add(e.getId())) continue;
                    if (SpadeFx.crush(b, e, (float) (base * SpadeFx.guard01(e)), TensuraDamageTypes.GRAVITY_PRESS)) SpadeFx.slam(e, 0.8);
                }
            }));
        }
    }

    // ================================================================ Distortion Step
    /** Where the step ends: 8 to 13 blocks from where he stands, on the side away from the target, with floor under it. Null when there is none. */
    static Vec3 stepDestination(ZogratisBoss b, LivingEntity target) {
        Vec3 away = SpadeFx.flatUnit(b.position().subtract(target.position()));
        double[] dists = {11, 9, 13, 8};
        double[] turns = {0, 0.6, -0.6, 1.2, -1.2, 1.9, -1.9, Math.PI};
        Vec3 centre = b.arenaCenter();
        for (double d : dists)
            for (double t : turns) {
                Vec3 g = ZogratisBoss.groundOrNull(b.level(), b.position().add(SpadeFx.rotateY(away, t).scale(d)));
                if (g != null && g.distanceTo(centre) < ZogratisBoss.ARENA_RADIUS - 6) return g;
            }
        return null;
    }

    /** The place he stood becomes a lens of gravity: for 14 ticks it pulls gently inwards, then it collapses on whoever followed him there. */
    static void lens(ZogratisBoss b, Vec3 where, float base) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        final Vec3 c = where;
        final boolean[] done = {false};
        ZogratisVfx.well(sl, c, 4.0, 18, VIOLET_RGB, false);
        SpellRuntime.zone(sl, 16, 2, SpadeFx.safe(WHO, "lens", age -> {
            if (!b.isAlive() || b.isRemoved() || done[0]) return;
            if (age < 14) {
                for (LivingEntity e : b.foesAround(c.add(0, 2, 0), 7)) {
                    Vec3 to = SpadeFx.flat(c.subtract(e.position()));
                    double d = to.length();
                    if (d < 1.2 || d > 6.5) continue;
                    SpadeFx.grounded(e);
                    Vec3 v = e.getDeltaMovement(), push = to.normalize().scale(0.13 * SpadeFx.guard01(e));
                    e.setDeltaMovement(v.x + push.x, v.y, v.z + push.z);
                    e.hurtMarked = true;
                }
            } else {
                done[0] = true;
                ZogratisVfx.gravityImpact(sl, c, 4.0, VIOLET_RGB);
                for (LivingEntity e : b.foesAround(c.add(0, 2, 0), 4.6)) {
                    if (e.distanceToSqr(c.x, e.getY(), c.z) > 3.6 * 3.6) continue;
                    if (SpadeFx.crush(b, e, (float) (base * SpadeFx.guard01(e)), TensuraDamageTypes.GRAVITY_EXPLODE)) SpadeFx.slam(e, 0.8);
                }
            }
        }));
    }

    // ================================================================ The Bow
    /**
     * The weight falls on everyone who did not bow. {@code crouched} maps a foe to the last tick it was seen crouching during the wind-up. One ask of the
     * Balance Law covers the whole order (the shared 12 second lock), made only if somebody is crushed; without it the crushed are slowed hard for a
     * moment but not held. Returns how many were crushed.
     */
    static int bow(ZogratisBoss b, Map<UUID, Long> crouched, float base) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        long now = sl.getGameTime();
        SpadeFx.Control law = new SpadeFx.Control(b);
        int grace = SpadeConfig.bowGrace(), crushed = 0;
        sl.playSound(null, b.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.5f, 0.5f);
        for (LivingEntity e : b.foes(56)) {
            boolean bowed = BowRules.bowed(now, crouched.getOrDefault(e.getUUID(), -1L), grace);
            BowRules.Verdict v = BowRules.verdict(SpadeFx.antiMagic(e), false, bowed);
            if (!BowRules.pinnedByBow(v)) {
                if (e instanceof Player p) {
                    if (v == BowRules.Verdict.UNBOWED) SpadeFx.hint(p, "message.nusmp.spade.unbowed", "Your anti-magic holds the weight off you.");
                    else SpadeFx.hint(p, "message.nusmp.spade.bowed", "You bowed. He lets you stand.");
                }
                continue;
            }
            crushed++;
            ZogratisVfx.gravityImpact(sl, e.position(), 2.8, VIOLET_RGB);
            SpadeFx.crush(b, e, (float) (base * BowRules.bowFactor(v) * SpadeFx.guard01(e)), TensuraDamageTypes.GRAVITY_PRESS);
            boolean held = law.granted();
            int ticks = held ? WeightMath.kneelTicks(SpadeConfig.kneelSeconds(), BalanceLaw.controlTicks(e, WeightMath.LAW_CAP)) : 12;
            if (held) SpadeFx.kneel(b, e, ticks);
            else {
                SpadeFx.slam(e, 1.0);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4));
            }
            if (e instanceof Player p) SpadeFx.hint(p, "message.nusmp.spade.crushed", "You did not bow. Crouch (hold sneak) when he orders it, or hold anti-magic.");
        }
        return crushed;
    }

    // ================================================================ Gravity Comb
    /** The comb's lanes on the ground: the lane centre lines as {from, to} pairs for {@code lanes} lanes round {@code origin} along {@code aim}. */
    static List<Vec3[]> laneLines(Vec3 origin, Vec3 aim, int lanes) {
        Vec3 side = SpadeFx.side(aim);
        List<Vec3[]> out = new ArrayList<>();
        for (double o : WeightMath.laneOffsets(lanes)) {
            Vec3 mid = origin.add(side.scale(o));
            out.add(new Vec3[]{mid.subtract(aim.scale(WeightMath.LANE_LENGTH / 2)), mid.add(aim.scale(WeightMath.LANE_LENGTH / 2))});
        }
        return out;
    }

    private static boolean inComb(LivingEntity e, Vec3 origin, Vec3 aim, int lanes) {
        Vec3 rel = e.position().subtract(origin);
        double lateral = rel.x * SpadeFx.side(aim).x + rel.z * SpadeFx.side(aim).z, along = rel.x * aim.x + rel.z * aim.z;
        return Math.abs(rel.y) <= 4.5 && WeightMath.inComb(lateral, along, lanes);
    }

    /** The lanes crush at once, then stay for three seconds as a field that slows and sinks, with one more pulse in the middle of it. */
    static void comb(ZogratisBoss b, Vec3 origin, Vec3 aim, int lanes, float base, float pulse) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        double reach = WeightMath.LANE_LENGTH / 2 + lanes * WeightMath.LANE_SPACING / 2 + 4;
        sl.playSound(null, BlockPos.containing(origin), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2f, 0.6f);
        for (Vec3[] line : laneLines(origin, aim, lanes)) ZogratisVfx.lane(sl, line[0], line[1], WeightMath.LANE_WIDTH, 64, ACCENT_RGB, false);
        for (LivingEntity e : b.foesAround(origin.add(0, 3, 0), reach)) {
            if (!inComb(e, origin, aim, lanes)) continue;
            if (SpadeFx.crush(b, e, (float) (base * SpadeFx.guard01(e)), TensuraDamageTypes.GRAVITY_PRESS)) {
                SpadeFx.slam(e, 0.8);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            }
        }
        final boolean[] pulsed = {false};
        SpellRuntime.zone(sl, 60, 4, SpadeFx.safe(WHO, "comb field", age -> {
            if (!b.isAlive() || b.isRemoved()) return;
            boolean strike = !pulsed[0] && age >= 28;
            if (strike) pulsed[0] = true;
            for (LivingEntity e : b.foesAround(origin.add(0, 3, 0), reach)) {
                if (!inComb(e, origin, aim, lanes)) continue;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1));
                SpadeFx.grounded(e);
                Vec3 v = e.getDeltaMovement();
                e.setDeltaMovement(v.x * 0.85, e.onGround() ? Math.min(v.y, -0.08) : Math.min(v.y, -0.3), v.z * 0.85);
                e.hurtMarked = true;
                e.fallDistance = 0;
                if (strike) SpadeFx.crush(b, e, (float) (pulse * SpadeFx.guard01(e)), TensuraDamageTypes.GRAVITY_PRESS);
            }
        }));
    }

    // ================================================================ Gravity Grasp
    /** What the hand found: the foe it met (null if none) and whether anti-magic cut it. */
    record Grasp(LivingEntity foe, boolean parried) {}

    /**
     * A hand of gravity along a line locked when the wind-up began: the nearest foe on the line within 16 blocks is seized. A holder of anti-magic cuts
     * the hand (nothing is touched). A seized foe is lifted, held for half a second when the Balance Law allows a hold, and slammed.
     */
    static Grasp grasp(ZogratisBoss b, Vec3 dir, float base) {
        if (!(b.level() instanceof ServerLevel sl)) return new Grasp(null, false);
        Vec3 from = b.position();
        LivingEntity best = null;
        double bestAlong = Double.MAX_VALUE;
        for (LivingEntity e : b.foes(GRASP_REACH + 2)) {
            Vec3 rel = SpadeFx.flat(e.position().subtract(from));
            double along = rel.dot(dir), lateral = Math.abs(rel.x * -dir.z + rel.z * dir.x);
            if (along < 0 || along > GRASP_REACH || lateral > 1.4 + e.getBbWidth() / 2 || along >= bestAlong) continue;
            best = e;
            bestAlong = along;
        }
        Vec3 end = best == null ? from.add(dir.scale(GRASP_REACH)) : best.position();
        ZogratisVfx.lane(sl, from.add(0, 1.0, 0), end.add(0, 1.0, 0), 1.6, 12, ACCENT_RGB, false);
        if (best == null) return new Grasp(null, false);
        sl.playSound(null, b.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2f, 0.4f);
        if (SpadeFx.antiMagic(best)) {
            ZogratisVfx.gravityImpact(sl, best.position(), 2.4, ACCENT_RGB);
            if (best instanceof Player p) SpadeFx.hint(p, "message.nusmp.spade.parried", "Your anti-magic cuts the hand of gravity.");
            return new Grasp(best, true);
        }
        final LivingEntity seized = best;
        boolean hold = BalanceLaw.beginControl(b);
        SpadeFx.grounded(seized);
        if (hold) {
            seized.setDeltaMovement(0, 0.55, 0);
            seized.hurtMarked = true;
            b.later(10, SpadeFx.safe(WHO, "grasp slam", () -> {
                if (!seized.isAlive()) return;
                SpadeFx.slam(seized, 1.6);
                ZogratisVfx.gravityImpact(sl, seized.position(), 3.0, VIOLET_RGB);
                SpadeFx.crush(b, seized, (float) (base * SpadeFx.guard01(seized)), TensuraDamageTypes.GRAVITY_EXPLODE);
            }));
        } else {
            SpadeFx.slam(seized, 1.2);
            SpadeFx.crush(b, seized, (float) (base * 0.6 * SpadeFx.guard01(seized)), TensuraDamageTypes.GRAVITY_PRESS);
        }
        return new Grasp(seized, false);
    }

    // ================================================================ Wandering Weight
    /** A small field of heavy air that sets out from {@code start} after {@code chase} (or the nearest foe) at two blocks a second for {@code ticks} ticks. */
    static void wandering(ZogratisBoss b, Vec3 start, UUID chase, int ticks, float strength, float pulse) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        final Vec3[] c = {start};
        sl.playSound(null, BlockPos.containing(start), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.4f);
        SpellRuntime.zone(sl, ticks, GravityMath.STEP, SpadeFx.safe(WHO, "wandering weight", age -> {
            if (!b.isAlive() || b.isRemoved()) return;
            LivingEntity goal = chase != null && sl.getEntity(chase) instanceof LivingEntity le && le.isAlive() && b.isFoe(le) ? le : null;
            if (goal == null) {
                double best = Double.MAX_VALUE;
                for (LivingEntity e : b.foesAround(c[0], 40)) {
                    double d = e.distanceToSqr(c[0].x, e.getY(), c[0].z);
                    if (d < best) { best = d; goal = e; }
                }
            }
            if (goal != null) {
                Vec3 to = SpadeFx.flat(goal.position().subtract(c[0]));
                double step = Math.min(to.length(), WANDER_DRIFT * GravityMath.STEP);
                if (to.lengthSqr() > 1e-6) c[0] = ZogratisBoss.ground(sl, c[0].add(to.normalize().scale(step)));
            }
            if (age % 8 == 0) ZogratisVfx.well(sl, c[0], WANDER_RADIUS, 10, VIOLET_RGB, false);
            Tells.ring(sl, c[0], WANDER_RADIUS, ACCENT_RGB, 1.2f, 36);
            double ramp = GravityMath.ramp(age);
            for (LivingEntity e : b.foesAround(c[0].add(0, 4, 0), Math.sqrt(WANDER_RADIUS * WANDER_RADIUS + 100) + 1)) {
                double dx = e.getX() - c[0].x, dz = e.getZ() - c[0].z, dy = e.getY() - c[0].y;
                if (dy < -4 || dy > 10 || dx * dx + dz * dz > WANDER_RADIUS * WANDER_RADIUS) continue;
                double w = GravityMath.weight(Math.sqrt(dx * dx + dz * dz) / WANDER_RADIUS), g = strength * SpadeFx.guard01(e);
                int amp = GravityMath.slowAmplifier(g, w, ramp);
                if (amp >= 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GravityMath.STEP * 2 + 2, amp));
                SpadeFx.grounded(e);
                Vec3 v = e.getDeltaMovement();
                e.setDeltaMovement(v.x * 0.85, e.onGround() ? Math.min(v.y, -0.08) : Math.min(v.y, GravityMath.sinkSpeed(g, w, ramp)), v.z * 0.85);
                e.hurtMarked = true;
                e.fallDistance = 0;
                if (age >= 8 && age % 20 == 8) SpadeFx.crush(b, e, (float) (pulse * w * ramp * SpadeFx.guard01(e) + 0.5f), TensuraDamageTypes.GRAVITY_PRESS);
            }
        }));
    }

    // ================================================================ Flatten the Air
    /** One pass of the flattened air over the foes in the arena: the effect (no jumping, double gravity) and fliers brought down. Holders of anti-magic are left alone. */
    static void flattenPass(ZogratisBoss b, int ticks) {
        for (LivingEntity e : b.foes(48)) {
            if (SpadeFx.antiMagic(e)) continue;
            if (SpadeConfig.flattenedAir()) SpadeFx.flatten(e, ticks);
            SpadeFx.grounded(e);
            if (!e.onGround() && SpadeFx.floorHeight(b.level(), e) > 1.6) {
                Vec3 v = e.getDeltaMovement();
                e.setDeltaMovement(v.x, Math.min(v.y, -0.5), v.z);
                e.hurtMarked = true;
                e.fallDistance = 0;
            }
        }
    }

    // ================================================================ Kneel Before the King
    /**
     * The pockets of still air, laid for the foes: planned by {@link PocketMath}, snapped to standing floor, at most {@code count} of them. If a cramped room
     * leaves fewer than two with floor under them, the foes are sheltered where they stand, so the ultimate always has its counter.
     */
    static List<Vec3> pockets(ZogratisBoss b, int count) {
        List<LivingEntity> foes = b.foes(48);
        if (foes.isEmpty()) return List.of();
        double[][] at = new double[foes.size()][];
        for (int i = 0; i < at.length; i++) at[i] = new double[]{foes.get(i).getX(), foes.get(i).getZ()};
        List<double[]> plan = PocketMath.plan(at, b.getX(), b.getZ(), count + 3, WeightMath.POCKET_RADIUS, b.getRandom().nextLong());
        List<Vec3> out = new ArrayList<>();
        for (double[] p : plan) {
            if (out.size() >= count) break;
            Vec3 g = ZogratisBoss.groundOrNull(b.level(), new Vec3(p[0], b.getY(), p[1]));
            if (g != null && g.distanceTo(b.arenaCenter()) < ZogratisBoss.ARENA_RADIUS - 4) out.add(g);
        }
        // a cramped room can leave no candidate with floor under it, and the ultimate must always have its counter: shelter the foes where they stand then
        if (out.size() < Math.min(2, count)) {
            for (LivingEntity e : foes) {
                if (out.size() >= count) break;
                Vec3 g = ZogratisBoss.groundOrNull(b.level(), e.position());
                if (g != null && out.stream().noneMatch(p -> p.distanceTo(g) < WeightMath.POCKET_RADIUS * 2)) out.add(g);
            }
        }
        return out;
    }

    private static boolean inAnyPocket(LivingEntity e, List<Vec3> pockets) {
        for (Vec3 p : pockets) if (WeightMath.inPocket(e.getX(), e.getZ(), p.x, p.z) && Math.abs(e.getY() - p.y) <= 3.5) return true;
        return false;
    }

    /**
     * The sky falls. Whoever stands in a pocket of still air takes a quarter of it and is not forced down, whoever holds anti-magic takes 40% and is not
     * forced down, everyone else is crushed and made to kneel (one ask of the Balance Law for the whole room, made only if somebody is crushed).
     * Returns how many were crushed.
     */
    static int kneelBeforeTheKing(ZogratisBoss b, List<Vec3> pockets, float base) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        SpadeFx.Control law = new SpadeFx.Control(b);
        int crushed = 0;
        sl.playSound(null, b.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.4f);
        ZogratisVfx.gravityImpact(sl, b.position(), 26, VIOLET_RGB);
        Tells.burst(sl, b.position().add(0, 1.2, 0), ACCENT_RGB, 90);
        for (LivingEntity e : b.foes(56)) {
            BowRules.Verdict v = BowRules.verdict(SpadeFx.antiMagic(e), inAnyPocket(e, pockets), false);
            double factor = BowRules.kneelFactor(v);
            SpadeFx.crush(b, e, (float) (base * factor * (v == BowRules.Verdict.CRUSHED ? SpadeFx.guard01(e) : 1.0)), TensuraDamageTypes.GRAVITY_EXPLODE);
            if (e instanceof Player p) {
                if (v == BowRules.Verdict.POCKET) SpadeFx.hint(p, "message.nusmp.spade.pocket", "The still air shelters you.");
                else if (v == BowRules.Verdict.UNBOWED) SpadeFx.hint(p, "message.nusmp.spade.unbowed", "Your anti-magic holds the weight off you.");
                else SpadeFx.hint(p, "message.nusmp.spade.kneel_ultimate", "Crushed. Find the still air (white rings) or hold anti-magic: crouching is not enough.");
            }
            if (!BowRules.pinnedByKneel(v)) continue;
            crushed++;
            ZogratisVfx.gravityImpact(sl, e.position(), 2.8, VIOLET_RGB);
            if (law.granted()) SpadeFx.kneel(b, e, WeightMath.kneelTicks(SpadeConfig.kneelSeconds(), BalanceLaw.controlTicks(e, WeightMath.LAW_CAP)));
            else {
                SpadeFx.slam(e, 1.2);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4));
            }
        }
        return crushed;
    }

    // ================================================================ Event Horizon
    /** One pull step of the great well: every foe within 30 blocks is dragged towards {@code c}, fliers are brought down, the pull never beats walking. */
    static void horizonPull(ZogratisBoss b, Vec3 c) {
        for (LivingEntity e : b.foes(WeightMath.PULL_RADIUS + 4)) {
            Vec3 to = SpadeFx.flat(c.subtract(e.position()));
            double d = to.length();
            double speed = WeightMath.pullSpeed(d) * (SpadeFx.antiMagic(e) ? 0.5 : 1.0);       // one impulse every other tick settles on the ground at about the nominal drift
            if (speed <= 0) continue;
            SpadeFx.grounded(e);
            Vec3 v = e.getDeltaMovement(), push = to.normalize().scale(speed);
            e.setDeltaMovement(v.x + push.x, SpadeFx.floorHeight(b.level(), e) > 1.6 ? Math.min(v.y, -0.5) : v.y, v.z + push.z);
            e.hurtMarked = true;
            e.fallDistance = 0;
        }
    }

    /** The well collapses. Everyone inside the core is crushed and thrown outwards; anti-magic and a raised shield cut it. Returns how many were caught. */
    static int horizonCollapse(ZogratisBoss b, Vec3 c, float base) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        sl.playSound(null, BlockPos.containing(c), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.4f);
        ZogratisVfx.gravityImpact(sl, c, WeightMath.CORE_RADIUS * 1.5, VIOLET_RGB);
        Tells.burst(sl, c.add(0, 1.2, 0), ACCENT_RGB, 80);
        int caught = 0;
        for (LivingEntity e : b.foesAround(c.add(0, 3, 0), WeightMath.CORE_RADIUS + 3)) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z, dy = e.getY() - c.y;
            if (!WeightMath.inCore(Math.hypot(dx, dz)) || dy < -4 || dy > 8) continue;
            caught++;
            if (SpadeFx.crush(b, e, (float) (base * SpadeFx.guard01(e)), TensuraDamageTypes.GRAVITY_EXPLODE)) {
                SpadeFx.grounded(e);
                double d = Math.max(1e-3, Math.hypot(dx, dz));
                e.knockback(1.4, -dx / d, -dz / d);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                e.fallDistance = 0;
            }
        }
        return caught;
    }

    // ================================================================ Six Limbs
    /** One limb's strike (a wing turned into an arm, or a hand): a wedge of 24 degrees and 5.8 blocks; a foe is struck at most three times by the six limbs together. */
    static void arm(ZogratisBoss b, double angle, float base, Map<Integer, Integer> hits) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 from = b.position(), dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        ZogratisVfx.lane(sl, from.add(0, 0.2, 0), from.add(dir.scale(WeightMath.ARM_REACH)), 2.2, 8, ACCENT_RGB, false);
        sl.playSound(null, b.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.0f, 0.6f);
        for (LivingEntity e : b.foes(WeightMath.ARM_REACH + 1.5)) {
            Vec3 rel = e.position().subtract(from);
            double dist = Math.hypot(rel.x, rel.z);
            if (!WeightMath.inArm(CageMath.angle(rel.x, rel.z), angle, dist) || Math.abs(rel.y) > 3.5) continue;
            if (hits.merge(e.getId(), 1, Integer::sum) > 3) continue;
            if (SpadeFx.crush(b, e, (float) (base * SpadeFx.guard01(e)), TensuraDamageTypes.GRAVITY_PRESS)) {
                SpadeFx.grounded(e);
                e.knockback(0.5, -dir.x, -dir.z);
            }
        }
    }

    /** A fresh hit counter for one Six Limbs. */
    static Map<Integer, Integer> newHits() { return new HashMap<>(); }
}
