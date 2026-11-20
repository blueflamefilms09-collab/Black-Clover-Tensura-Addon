package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.arc.BossFx;
import com.newuniverse.nusmp.arc.BossShapes;
import com.newuniverse.nusmp.arc.sds.SdsaStrikes;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.particle.fx.FxOld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the three NEW attacks of the Goku boss (docs/boss_redesign_goku_sinbad.md), original shapes made for him and not copies of any player skill: the Zanzoken Cage (four afterimage pillars are
 * marked round the target, then he streaks through all four and finishes in the middle), the Solar Flare (a star kindles on his brow; whoever LOOKS at him is blinded, whoever looks away is not) and the
 * Kienzan Boomerang (a flat spinning disc flies out and comes back, cutting everyone it passes both ways). Each has a telegraph, a way to avoid it, and a bounded cost:
 * <ul>
 *   <li>Cage: tell = at most 36 dust points every third tick; fire = 4 blinks (each one safe-spot check, one foe query), 1 slam (one foe query), at most 5 effect bursts.</li>
 *   <li>Solar Flare: tell = at most 32 dust points every third tick; fire = one foe query, one line-of-sight check per foe, three rings of at most 32 points.</li>
 *   <li>Disc: 28 steps one tick apart, each at most 12 dust points; one foe query every second step (14), one wall ray at the throw. At most one scheduled step is pending at a time.</li>
 * </ul>
 * No move spawns an entity; every point goes through {@link BossFx} (the per-tick particle budget); every delayed step is dropped when he is gone (the boss's {@code later}). The only state is the cage
 * centre kept per boss UUID until the move fires (at most 8 entries, cleared past that).
 */
public final class GokuArts {
    private GokuArts() {}

    public static final String CAGE = "zanzoken_cage", FLARE = "solar_flare", DISC = "ki_disc";

    static final double CAGE_RADIUS = 5.5, CAGE_HIT = 2.8, SLAM_RADIUS = 4.2, FLARE_REACH = 28.0, DISC_REACH = 22.0, DISC_HIT = 1.9;
    static final int CAGE_PILLARS = 4, CAGE_GAP = 7, DISC_OUT = 14;

    private static final Map<UUID, Vec3> CENTRES = new HashMap<>();

    /** The three moves, appended after the five of {@link GokuMoves#build}. Ids are unique and never renamed. */
    static List<Move> build() {
        List<Move> out = new ArrayList<>();
        out.add(Move.builder(CAGE, "Zanzoken Cage").minPower(0).windUp(40).cooldown(260).range(4, 30).weight(3).recovery(14)
                .tell((b, t, left, total) -> cageTell((GokuBoss) b, t, left, total)).run((b, t) -> cageFire((GokuBoss) b, t)));
        out.add(Move.builder(FLARE, "Solar Flare").minPower(0).windUp(30).cooldown(360).range(0, 24).weight(3).recovery(14)
                .tell((b, t, left, total) -> flareTell((GokuBoss) b, left, total)).run((b, t) -> { flareFire((GokuBoss) b); }));
        out.add(Move.builder(DISC, "Kienzan Boomerang").minPower(0).windUp(24).cooldown(220).range(5, 30).weight(3).recovery(16)
                .tell((b, t, left, total) -> discTell((GokuBoss) b, t, left, total)).run((b, t) -> discFire((GokuBoss) b, t)));
        return out;
    }

    private static int rgb(GokuBoss g) { return GokuLadder.rgb(g.formIdx()) & 0xFFFFFF; }

    private static Vec3 chest(ZogratisBoss g) { return g.position().add(0.0, g.getBbHeight() * 0.62, 0.0); }

    /** True when {@code p} is inside the arena's safe radius (the same leash the Instant Transmission keeps). */
    static boolean inLeash(ZogratisBoss g, Vec3 p) {
        double leash = ZogratisBoss.ARENA_RADIUS - 6.0;
        return p.distanceToSqr(g.arenaCenter()) <= leash * leash;
    }

    // ================================================================ Zanzoken Cage
    private static Vec3 lockCentre(GokuBoss g, LivingEntity t) {
        Vec3 c = t.position();
        double lim = ZogratisBoss.ARENA_RADIUS - 12.0;
        Vec3 mid = g.arenaCenter();
        Vec3 off = c.subtract(mid).multiply(1, 0, 1);
        if (off.length() > lim) c = new Vec3(mid.x + off.x / off.length() * lim, c.y, mid.z + off.z / off.length() * lim);
        if (CENTRES.size() > 8) CENTRES.clear();
        CENTRES.put(g.getUUID(), c);
        return c;
    }

    private static Vec3 pillar(Vec3 c, int i) {
        double a = BossShapes.pillarAngle(i, CAGE_PILLARS, Math.PI / 4.0);
        return c.add(Math.cos(a) * CAGE_RADIUS, 0.0, Math.sin(a) * CAGE_RADIUS);
    }

    private static void cageTell(GokuBoss g, LivingEntity t, int left, int total) {
        if (!(g.level() instanceof ServerLevel sl) || t == null) return;
        Vec3 c = left >= total - 3 || !CENTRES.containsKey(g.getUUID()) ? lockCentre(g, t) : CENTRES.get(g.getUUID());
        int col = rgb(g);
        double k = BossShapes.grow(total - left, total);
        double spin = (total - left) * 0.14;
        BossFx.points(sl, c.add(0, 0.12, 0), BossShapes.ring(CAGE_RADIUS * k, 0.0, 16, spin), col, 1.3f);
        BossFx.points(sl, c.add(0, 0.12, 0), BossShapes.ring(CAGE_RADIUS * k * 0.45, 0.0, 8, -spin), 0xFFFFFF, 1.0f);
        for (int i = 0; i < CAGE_PILLARS; i++) BossFx.column(sl, pillar(c, i).add(0, 0.05, 0), 3.2 * k, 5, col, 1.1f);
    }

    private static void cageFire(GokuBoss g, LivingEntity t) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        Vec3 c = CENTRES.remove(g.getUUID());
        if (c == null) c = t.position();
        final Vec3 centre = c;
        final int col = rgb(g);
        for (int i = 0; i < CAGE_PILLARS; i++) {
            final Vec3 spot = ZogratisBoss.groundOrNull(sl, pillar(centre, i));
            g.later(i * CAGE_GAP, () -> cageStep(g, sl, spot, col));
        }
        g.later(CAGE_PILLARS * CAGE_GAP + 3, () -> cageSlam(g, sl, centre, col));
    }

    /** One streak of the cage: he blinks to a pillar (when it is inside the leash and his body fits), leaves an afterimage, and cuts whoever stands within {@link #CAGE_HIT}. Returns the foes hit. */
    static int cageStep(GokuBoss g, ServerLevel sl, Vec3 spot, int col) {
        if (spot == null || !inLeash(g, spot)) return 0;
        Vec3 from = g.position();
        if (!g.blinkTo(spot)) return 0;
        BossFx.column(sl, from, 2.2, 8, col, 1.5f);                                                     // the afterimage he leaves
        BossFx.points(sl, from.add(0, 1.1, 0), lineRel(from, g.position()), 0xFFFFFF, 1.1f);          // the streak between the two
        int hits = 0;
        for (LivingEntity f : g.foesAround(g.position(), CAGE_HIT)) {
            if (g.strike(f, g.base() * 0.5f, false)) {
                hits++;
                Vec3 away = f.position().subtract(g.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) f.knockback(0.5, -away.x, -away.z);
            }
        }
        BossFx.spark(sl, FxOld.END_ROD, g.position().add(0, 1.0, 0), 6, 0.5, 0.2);
        return hits;
    }

    /** The finish in the middle of the cage. Returns the foes hit. */
    static int cageSlam(GokuBoss g, ServerLevel sl, Vec3 centre, int col) {
        int hits = 0;
        for (LivingEntity f : g.foesAround(centre, SLAM_RADIUS)) {
            if (g.strike(f, g.base() * 0.85f, false)) {
                hits++;
                Vec3 away = f.position().subtract(centre).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) f.knockback(1.0, -away.x, -away.z);
            }
        }
        BossFx.points(sl, centre.add(0, 0.15, 0), BossShapes.ring(SLAM_RADIUS, 0.0, 24, 0.0), 0xFFFFFF, 1.6f);
        BossFx.points(sl, centre.add(0, 0.2, 0), BossShapes.spiral(0.4, SLAM_RADIUS * 0.8, 0.2, 3.2, 24, 2.0, 0.0), col, 1.4f);
        sl.playSound(null, BlockPos.containing(centre), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6f, 1.4f);
        return hits;
    }

    /** A straight line of {@code 8} points from {@code a} to {@code b}, relative to {@code a} + 1.1 up (so the caller draws it from {@code a.add(0, 1.1, 0)}). */
    private static double[] lineRel(Vec3 a, Vec3 b) {
        double[] out = new double[8 * 3];
        for (int i = 0; i < 8; i++) {
            double k = (i + 1) / 9.0;
            out[i * 3] = (b.x - a.x) * k;
            out[i * 3 + 1] = (b.y - a.y) * k;
            out[i * 3 + 2] = (b.z - a.z) * k;
        }
        return out;
    }

    // ================================================================ Solar Flare
    private static void flareTell(GokuBoss g, int left, int total) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        if (left >= total - 3) g.announce("Goku raises both hands to his brow. Look away from him!");
        double k = BossShapes.grow(total - left, total);
        Vec3 head = g.getEyePosition();
        BossFx.points(sl, head, BossShapes.spiral(2.4 * (1.0 - k) + 0.15, 0.1, -0.6, 0.0, 20, 2.5, (total - left) * 0.35), 0xFFF2A0, 1.2f);
        BossFx.spark(sl, FxOld.END_ROD, head, 3, 0.2, 0.02);
        if (left <= 9) BossFx.points(sl, head, BossShapes.ring(0.3 + (9 - left) * 0.05, 0.0, 10, 0.0), 0xFFFFFF, 2.0f);
    }

    /** Returns how many foes were blinded (the ones who looked at him with a clear line). */
    static int flareFire(GokuBoss g) {
        if (!(g.level() instanceof ServerLevel sl)) return 0;
        Vec3 head = g.getEyePosition();
        List<LivingEntity> blinded = new ArrayList<>();
        for (LivingEntity f : g.foesAround(g.position(), FLARE_REACH)) {
            Vec3 view = f.getViewVector(1.0F), to = head.subtract(f.getEyePosition());
            if (BossShapes.facing(view.x, view.y, view.z, to.x, to.y, to.z, 0.25) && f.hasLineOfSight(g)) {
                f.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), g);
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), g);
                blinded.add(f);
            }
        }
        for (int i = 0; i < 3; i++) {
            final double r = 4.0 + i * 5.5;
            g.later(i * 2, () -> BossFx.points(sl, head, BossShapes.ring(r, 0.0, 32, 0.0), ((int) r) % 2 == 0 ? 0xFFFFFF : 0xFFF2A0, 2.2f));
        }
        BossFx.spark(sl, FxOld.FLASH, head, 1, 0.1, 0.0);
        BossFx.spark(sl, FxOld.END_ROD, head, 6, 1.2, 0.35);
        sl.playSound(null, g.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 3.0f, 1.7f);
        for (LivingEntity f : blinded) {                                                                        // failing the check has a price: he follows up on whoever was blinded
            g.later(14, () -> {
                if (f.isAlive() && g.distanceTo(f) < 24.0) g.transmit(f);
            });
            break;                                                                                              // one follow-up per flare, on the first one blinded
        }
        return blinded.size();
    }

    // ================================================================ Kienzan Boomerang
    private static void discTell(GokuBoss g, LivingEntity t, int left, int total) {
        if (!(g.level() instanceof ServerLevel sl) || t == null) return;
        int col = rgb(g);
        Vec3 hand = chest(g).add(0, 0.9, 0);
        BossFx.points(sl, hand, BossShapes.disc(0.35 + (total - left) * 0.02, 0.0, 12, (total - left) * 0.9), col, 1.3f);
        if (left % 9 == 0 || left >= total - 3) {
            Vec3 dir = SdsaStrikes.unitFlat(t.position().subtract(g.position()), new Vec3(0, 0, 1));
            BossFx.points(sl, chest(g), lineRel(chest(g), chest(g).add(dir.scale(Math.min(DISC_REACH, 14.0)))), 0xFFFFFF, 0.9f);
        }
    }

    private static void discFire(GokuBoss g, LivingEntity t) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        Vec3 o = chest(g);
        Vec3 dir = SdsaStrikes.unitFlat(t.position().subtract(g.position()), SdsaStrikes.unitFlat(g.getViewVector(1.0F), new Vec3(0, 0, 1)));
        Vec3 end = SdsaStrikes.clipEnd(sl, g, o, o.add(dir.scale(DISC_REACH)));
        double reach = Math.max(4.0, o.distanceTo(end));
        g.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        sl.playSound(null, g.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.6f, 1.6f);
        discStep(g, o, dir, reach, 1, new HashSet<>(), new HashSet<>(), rgb(g), null);
    }

    /** One tick of the disc's flight; schedules the next only when the flight is not over (so one closure is pending at most). With a {@code tally} (the self-test) the caller drives the ticks and nothing is scheduled. */
    static void discStep(GokuBoss g, Vec3 o, Vec3 dir, double reach, int tick, Set<Integer> out, Set<Integer> back, int col, int[] tally) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        double d = BossShapes.discDistance(reach, tick, DISC_OUT);
        Vec3 far = o.add(dir.scale(reach));
        Vec3 p = tick <= DISC_OUT ? o.add(dir.scale(d)) : chest(g).lerp(far, d / reach);
        BossFx.points(sl, p, BossShapes.disc(1.1, 0.0, 12, tick * 0.95), col, 1.3f);
        if ((tick & 1) == 0) {
            Set<Integer> seen = tick <= DISC_OUT ? out : back;
            for (LivingEntity f : g.foesAround(p, DISC_HIT)) {
                if (seen.add(f.getId()) && g.strike(f, g.base() * 0.7f, true)) {
                    if (tally != null) tally[0]++;
                    Vec3 push = dir.scale(tick <= DISC_OUT ? 1.0 : -1.0);
                    f.knockback(0.35, -push.x, -push.z);
                    BossFx.spark(sl, FxOld.CRIT, f.position().add(0, f.getBbHeight() * 0.5, 0), 5, 0.3, 0.25);
                }
            }
        }
        if (tick == DISC_OUT) BossFx.spark(sl, FxOld.END_ROD, p, 6, 0.6, 0.2);
        if (tick < DISC_OUT * 2 && tally == null) g.later(1, () -> discStep(g, o, dir, reach, tick + 1, out, back, col, tally));
    }

    // ================================================================ test seams (used by the CI self-test; never by the game)
    /** Runs the whole disc flight synchronously from {@code o} along {@code dir} and returns the foes it hit (out and back). */
    static int discForTest(GokuBoss g, Vec3 o, Vec3 dir, double reach) {
        int[] tally = new int[1];
        Set<Integer> out = new HashSet<>(), back = new HashSet<>();
        for (int tick = 1; tick <= DISC_OUT * 2; tick++) discStep(g, o, dir, reach, tick, out, back, rgb(g), tally);
        return tally[0];
    }

    /** Draws the three telegraphs once each (the first call of each, which locks its aim) and then fires the cage's pillars and slam synchronously; returns the foes hit. */
    static int cageForTest(GokuBoss g, LivingEntity t) {
        if (!(g.level() instanceof ServerLevel sl)) return 0;
        cageTell(g, t, 40, 40);
        cageTell(g, t, 20, 40);
        Vec3 c = CENTRES.remove(g.getUUID());
        if (c == null) return -1;
        int hits = 0;
        for (int i = 0; i < CAGE_PILLARS; i++) hits += cageStep(g, sl, ZogratisBoss.groundOrNull(sl, pillar(c, i)), rgb(g));
        return hits + cageSlam(g, sl, c, rgb(g));
    }

    static boolean centreKept(GokuBoss g) { return CENTRES.containsKey(g.getUUID()); }

    static void tellsForTest(GokuBoss g, LivingEntity t) {
        flareTell(g, 30, 30);
        flareTell(g, 6, 30);
        discTell(g, t, 24, 24);
        discTell(g, t, 9, 24);
    }
}
