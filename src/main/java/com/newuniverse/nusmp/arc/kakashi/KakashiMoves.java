package com.newuniverse.nusmp.arc.kakashi;

import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the fifteen moves of the Kakashi boss as the framework's {@link Move}s, so each gets its telegraph, cooldown, range, pacing and the exposed-after-an-ultimate rule for free. The work
 * (the lane, the warp, the wolf, the shield) is in {@link KakashiBoss}, where it is bounded; a move here only decides where and when. Every move that lands a blow shows WHERE first (a ring, a
 * line or marks drawn by its own tell) and fixes that place for the last {@link KakashiBoss#FREEZE} ticks of the wind-up, so a player who moves is never hit by a spot they could not see.
 * Worst case per use (no move spawns an entity, none changes a block): Kunai Barrage 5 timed volleys of 1 foe query each; Raikiri 1 blink and 1 lane query; Copy 1 hit; Beast Fang a state of 44 ticks
 * with 1 small box query per tick; Water Dragon 1 foe query; Ninken Pursuit 8 timed hits on at most 4 foes; Clone Ambush 3 timed marks of 1 query each; Mist and Wall no query (Mist 1 per second);
 * Kamui Step 1 blink and 1 hit; Kamui Raikiri 1 blink and 1 lane query; Kamui Shuriken 4 lane checks over one foe query; Vortex 15 pulls over at most 12 foes; Susanoo 1 slash per 64 ticks;
 * Purple Lightning 3 marks and 3 foe queries. The canon kit pass adds: Lightning Cutter 1 blink and 1 lane query; Lightning Clone 2 decoys (one foe query per 4 ticks each while they stand, at most 100 ticks); Purple Strike 1 lane query; Purple Burst 1 foe query; Purple Chain 1 foe query and at most 5 timed hits; Distant Warp 1 foe query and 1 teleport; Earth Fang 3 timed bursts and 1 query; Eight Gates 0 queries; Kubikiribocho 1 foe query; Chakra Sabre 3 timed lane queries; Kunai Wire 1 lane query; Flying Thunder God 3 timed blinks and lane queries; Great Fireball 1 lane query; Six Paths no query. Each move sends at most {@link KakashiMath#MAX_VFX_PER_MOVE} effect packets (through the shared budgets) and the particles of {@link KakashiFx}.
 */
public final class KakashiMoves {
    private KakashiMoves() {}

    public static final String KUNAI = KakashiMath.KUNAI, RAIKIRI = KakashiMath.RAIKIRI, COPY = KakashiMath.COPY_MOVE, FANG = KakashiMath.FANG, DRAGON = KakashiMath.DRAGON,
            NINKEN = KakashiMath.NINKEN, CLONES = KakashiMath.CLONES, MIST = KakashiMath.MIST, WALL = KakashiMath.WALL, WARP = KakashiMath.WARP, KAMUI_RAIKIRI = KakashiMath.KAMUI_RAIKIRI,
            SHURIKEN = KakashiMath.KAMUI_SHURIKEN, VORTEX = KakashiMath.VORTEX, SUSANOO = KakashiMath.SUSANOO_MOVE, PURPLE = KakashiMath.PURPLE;

    /** The ids of the canon kit pass (appended; see {@link KakashiMath#BOOK}). */
    public static final String CUTTER = KakashiMath.CUTTER, LCLONE = KakashiMath.LCLONE, PURPLE_STRIKE = KakashiMath.PURPLE_STRIKE, PURPLE_BURST = KakashiMath.PURPLE_BURST,
            PURPLE_CHAIN = KakashiMath.PURPLE_CHAIN, KAMUI_WARP = KakashiMath.KAMUI_WARP, EARTH_FANG = KakashiMath.EARTH_FANG, GATES = KakashiMath.GATES, KUBIKIRI = KakashiMath.KUBIKIRI,
            SABRE = KakashiMath.SABRE, WIRE = KakashiMath.WIRE, THUNDER_GOD = KakashiMath.THUNDER_GOD, FIREBALL = KakashiMath.FIREBALL, SIX_PATHS = KakashiMath.SIX_PATHS;

    /** The voice group each move speaks from (a group of {@code data/anime_multiverse/kakashi/lines.json}). */
    public static String voiceGroup(String moveId) {
        return switch (moveId) {
            case KUNAI -> "kunai";
            case RAIKIRI -> "raikiri";
            case COPY -> "copy";
            case FANG -> "beast_fang";
            case DRAGON -> "water_dragon";
            case NINKEN -> "ninken";
            case CLONES -> "clones";
            case MIST -> "mist";
            case WALL -> "wall";
            case WARP -> "kamui_step";
            case KAMUI_RAIKIRI -> "kamui_raikiri";
            case SHURIKEN -> "kamui_shuriken";
            case VORTEX -> "kamui_vortex";
            case SUSANOO -> "susanoo";
            case PURPLE -> "purple";
            case KakashiMath.CUTTER -> "lightning_cutter";
            case KakashiMath.LCLONE -> "lightning_clone";
            case KakashiMath.PURPLE_STRIKE -> "purple_strike";
            case KakashiMath.PURPLE_BURST -> "purple_burst";
            case KakashiMath.PURPLE_CHAIN -> "purple_chain";
            case KakashiMath.KAMUI_WARP -> "kamui_warp";
            case KakashiMath.EARTH_FANG -> "earth_fang";
            case KakashiMath.GATES -> "eight_gates";
            case KakashiMath.KUBIKIRI -> "kubikiri";
            case KakashiMath.SABRE -> "chakra_sabre";
            case KakashiMath.WIRE -> "kunai_wire";
            case KakashiMath.THUNDER_GOD -> "thunder_god";
            case KakashiMath.FIREBALL -> "great_fireball";
            case KakashiMath.SIX_PATHS -> "six_paths";
            default -> null;
        };
    }

    private static KakashiBoss k(ZogratisBoss b) { return (KakashiBoss) b; }

    private static Vec3 chest(KakashiBoss b) { return b.position().add(0, b.getBbHeight() * 0.65, 0); }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : f.normalize();
    }

    /** The moves, in book order (ids are unique and never renamed; new ones are appended). */
    public static List<Move> build() {
        List<Move> out = new ArrayList<>();
        out.add(Move.builder(KUNAI, "Kunai Barrage").minPower(0).windUp(14).cooldown(80).range(4, 22).weight(5).recovery(10)
                .tell((b, t, left, total) -> kunaiTell(k(b), t, left)).run((b, t) -> kunai(k(b), t)));
        out.add(Move.builder(RAIKIRI, "Raikiri").minPower(0).windUp(22).cooldown(160).range(4, 18).weight(5).recovery(26)
                .tell((b, t, left, total) -> dashTell(k(b), t, left, KakashiFx.RAIKIRI, false)).run((b, t) -> raikiri(k(b), t, false)));
        out.add(Move.builder(COPY, "Sharingan Copy").minPower(0).windUp(16).cooldown(300).range(0, 30).weight(3).recovery(10)
                .tell((b, t, left, total) -> copyTell(k(b), left)).run((b, t) -> copy(k(b))));
        out.add(Move.builder(FANG, "Lightning Beast Tracking Fang").minPower(0).windUp(24).cooldown(260).range(6, 36).weight(4).recovery(12)
                .tell((b, t, left, total) -> fangTell(k(b), t, left, total)).run((b, t) -> k(b).releaseFang(t)));
        out.add(Move.builder(DRAGON, "Water Dragon Bullet").minPower(0).windUp(26).cooldown(200).range(5, 30).weight(4).recovery(12)
                .tell((b, t, left, total) -> dragonTell(k(b), t, left, total)).run((b, t) -> dragon(k(b), t)));
        out.add(Move.builder(NINKEN, "Ninken Pursuit").minPower(0).windUp(20).cooldown(320).range(3, 30).weight(3).crowd().recovery(14)
                .tell((b, t, left, total) -> ninkenTell(k(b), t, left)).run((b, t) -> ninken(k(b), t)));
        out.add(Move.builder(CLONES, "Shadow Clone Ambush").minPower(0).windUp(18).cooldown(340).range(0, 24).weight(3).crowd().recovery(14)
                .tell((b, t, left, total) -> clonesTell(k(b), t, left)).run((b, t) -> clones(k(b), t)));
        out.add(Move.builder(MIST, "Hidden Mist").minPower(0).windUp(14).cooldown(600).range(0, 48).weight(2).recovery(6)
                .tell((b, t, left, total) -> KakashiFx.gather((ServerLevel) b.level(), KakashiFx.MIST, b.position().add(0, 1, 0), 2.5, 8)).run((b, t) -> k(b).raiseMist(200)));
        out.add(Move.builder(WALL, "Earth Style: Earth Wall").minPower(0).windUp(12).cooldown(360).range(0, 48).weight(2).recovery(6)
                .tell((b, t, left, total) -> KakashiFx.gather((ServerLevel) b.level(), KakashiFx.EARTH, b.position().add(0, 0.3, 0), 2.0, 8)).run((b, t) -> k(b).raiseWall(100, t.position())));
        out.add(Move.builder(WARP, "Kamui Step").minPower(0).windUp(10).cooldown(140).range(6, 48).weight(4).recovery(8)
                .tell((b, t, left, total) -> warpTell(k(b), t, left)).run((b, t) -> warpStrike(k(b), t)));
        out.add(Move.builder(KAMUI_RAIKIRI, "Kamui Raikiri").minPower(0).windUp(26).cooldown(240).range(4, 26).weight(4).recovery(28)
                .tell((b, t, left, total) -> dashTell(k(b), t, left, KakashiFx.KAMUI, true)).run((b, t) -> raikiri(k(b), t, true)));
        out.add(Move.builder(SHURIKEN, "Kamui Shuriken").minPower(0).windUp(34).cooldown(300).range(4, 30).weight(3).recovery(16)
                .tell((b, t, left, total) -> shurikenTell(k(b), t, left)).run((b, t) -> shuriken(k(b), t)));
        out.add(Move.builder(VORTEX, "Kamui Vortex").minPower(0).windUp(40).cooldown(700).range(0, 28).weight(3).ultimate().recovery(70)
                .tell((b, t, left, total) -> vortexTell(k(b), t, left, total)).run((b, t) -> {
                    Vec3 at = k(b).aim(t, 0);
                    k(b).clearLock();
                    k(b).startVortex(at);
                }));
        out.add(Move.builder(SUSANOO, "Perfect Susanoo").minPower(0).windUp(30).cooldown(700).range(0, 48).weight(4).recovery(10)
                .tell((b, t, left, total) -> susanooTell(k(b), left, total)).run((b, t) -> k(b).raiseSusanoo()));
        out.add(Move.builder(PURPLE, "Purple Lightning").minPower(0).windUp(40).cooldown(600).range(0, 40).weight(4).ultimate().recovery(70)
                .tell((b, t, left, total) -> purpleTell(k(b), t, left, total)).run((b, t) -> purple(k(b), t)));
        appendCanonMoves(out);                                                  // the canon kit pass: fourteen more, appended (KakashiMath.CLASSIC_MOVES = the first fifteen)
        return out;
    }

    // ================================================================ Kunai Barrage: five volleys down a fixed line, the last one with a paper tag
    private static void kunaiTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, left);
        KakashiFx.line(sl, KakashiFx.KUNAI, chest(b), aim.add(0, 1, 0), 8);
        if (left <= KakashiBoss.FREEZE) KakashiFx.ring(sl, KakashiFx.KUNAI, aim, 2.4, 10);
    }

    private static void kunai(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, 0), dir = flat(aim.subtract(b.position()));
        for (int i = 0; i < 5; i++) {
            final int n = i;
            b.later(i * 3, () -> {
                b.swing(n % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                b.getLookControl().setLookAt(aim.x, aim.y + 1, aim.z);
                Vec3 from = chest(b), to = aim.add(dir.scale(5.0)).add(0, 1, 0);
                KakashiFx.line(sl, KakashiFx.KUNAI, from, to, 8);
                for (LivingEntity f : b.lane(b.position(), dir, aim.distanceTo(b.position()) + 5.0, 1.0)) b.strike(f, b.base() * 0.34f, false);
                if (n == 4) {                                                 // the tag at the end of the line goes off
                    KakashiFx.burst(sl, KakashiFx.KUNAI, aim.add(0, 0.6, 0), 12, 1.0);
                    for (LivingEntity f : b.foesAround(aim, 2.6)) if (b.strike(f, b.base() * 0.55f, true)) b.shove(f, aim, 0.7);
                    sl.playSound(null, net.minecraft.core.BlockPos.containing(aim), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.6f);
                    b.clearLock();
                }
            });
        }
    }

    // ================================================================ Raikiri and Kamui Raikiri: a lane marked on the ground, a dash, a thrust
    private static void dashTell(KakashiBoss b, LivingEntity t, int left, com.newuniverse.nusmp.particle.fx.ThemeKey theme, boolean kamui) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        b.raikiriHand(true);
        Vec3 aim = b.aim(t, left);
        Vec3 hand = b.position().add(flat(b.getLookAngle()).scale(0.6)).add(0, b.getBbHeight() * 0.55, 0);
        KakashiFx.puff(sl, KakashiFx.RAIKIRI, hand, 6, 0.3);                    // the chirping hand
        if (left % 2 == 0) KakashiFx.chidori(sl, hand, 1.4, 4, b.tickCount / 2);   // white-cyan core and forking arcs (the reference look)
        KakashiFx.line(sl, theme, b.position().add(0, 0.2, 0), aim.add(0, 0.2, 0), 10);
        if (left <= KakashiBoss.FREEZE) KakashiFx.ring(sl, theme, aim, 2.4, 12);
        if (kamui && left % 6 == 0) KakashiFx.shape(sl, VfxShape.SPACE_PORTAL, aim.add(0, 1, 0), aim.add(0, 1.5, 0), KakashiFx.RGB_KAMUI, 8, 1.0);   // where he will come out
    }

    private static void raikiri(KakashiBoss b, LivingEntity t, boolean kamui) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, 0), dir = flat(aim.subtract(b.position()));
        b.clearLock();
        Vec3 stop = ZogratisBoss.groundOrNull(sl, aim.subtract(dir.scale(1.6)));
        if (kamui) b.phaseFor(14);
        boolean moved = stop != null && (kamui ? b.warp(stop) : b.blinkTo(stop) && b.withinLeash(stop));
        b.raikiriHand(false);
        if (!moved) return;
        b.getLookControl().setLookAt(aim.x, aim.y + 1, aim.z);
        b.later(kamui ? 5 : 4, () -> {
            b.swing(InteractionHand.MAIN_HAND);
            Vec3 from = b.position().add(0, 1.1, 0);
            KakashiFx.line(sl, kamui ? KakashiFx.KAMUI : KakashiFx.RAIKIRI, from, from.add(dir.scale(3.4)), 10);
            KakashiFx.burst(sl, KakashiFx.RAIKIRI, from.add(dir.scale(2.2)), 14, 0.7);
            KakashiFx.shape(sl, VfxShape.LIGHTNING_SPEAR, from, from.add(dir.scale(3.6)), KakashiFx.RGB_RAIKIRI, 8, 1.2);
            boolean hit = false;
            for (LivingEntity f : b.lane(b.position(), dir, 3.4, 1.2)) hit |= b.lightning(f, kamui ? 2.7f : 2.3f);
            sl.playSound(null, b.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.8f, 1.8f);
            if (!hit) b.speak("raikiri_miss", false);
        });
    }

    // ================================================================ Sharingan Copy: a weaker copy of the hardest hit a player landed on him
    private static void copyTell(KakashiBoss b, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        KakashiFx.puff(sl, KakashiFx.COPY, b.position().add(0, b.getBbHeight() * 0.9, 0), 5, 0.25);
        ServerPlayer p = b.copyTarget();
        if (p != null && left % 6 == 0) KakashiFx.line(sl, KakashiFx.COPY, b.position().add(0, b.getBbHeight() * 0.9, 0), p.position().add(0, 1, 0), 8);
    }

    private static void copy(KakashiBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        ServerPlayer p = b.copyTarget();
        if (p == null) return;
        float amount = KakashiMath.copied(b.ledger.amount(p.getUUID()), b.getMaxHealth());
        b.ledger.forget(p.getUUID());
        if (amount <= 0f) return;
        Vec3 at = p.position().add(0, 1, 0);
        KakashiFx.line(sl, KakashiFx.COPY, b.position().add(0, b.getBbHeight() * 0.9, 0), at, 10);
        KakashiFx.burst(sl, KakashiFx.COPY, at, 12, 0.6);
        b.strike(p, amount, true);                                           // strike() applies his tuning and the per-hit cap on top
        sl.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.4f, 0.7f);
    }

    // ================================================================ Lightning Beast Tracking Fang
    private static void fangTell(KakashiBoss b, LivingEntity t, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 hand = chest(b);
        KakashiFx.gather(sl, KakashiFx.FANG, hand, 0.4 + (total - left) * 0.02, 6);
        if (left % 9 == 0) KakashiFx.line(sl, KakashiFx.FANG, hand, t.position().add(0, 1, 0), 10);
    }

    // ================================================================ Water Dragon Bullet: a coil that soaks (and makes lightning hit harder)
    private static void dragonTell(KakashiBoss b, LivingEntity t, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, left), dir = flat(aim.subtract(b.position()));
        double[][] path = KakashiMath.dragonPath(18.0, 10, 2.2, 0.0);
        for (int i = 0; i < path.length; i += 2) KakashiFx.puff(sl, KakashiFx.WATER, coil(b.position(), dir, path[i]).add(0, 1.0, 0), 2, 0.2);
        if (left % 9 == 0) KakashiFx.gather(sl, KakashiFx.WATER, chest(b), 1.0, 8);
    }

    private static Vec3 coil(Vec3 origin, Vec3 dir, double[] p) { return origin.add(dir.scale(p[0])).add(-dir.z * p[1], 0, dir.x * p[1]); }

    private static void dragon(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, 0), dir = flat(aim.subtract(b.position()));
        b.clearLock();
        b.swing(InteractionHand.MAIN_HAND);
        double[][] path = KakashiMath.dragonPath(18.0, 10, 2.2, 0.0);
        Vec3 end = coil(b.position(), dir, path[path.length - 1]);
        KakashiFx.shape(sl, VfxShape.WATER_DRAGON, chest(b), end.add(0, 1, 0), KakashiFx.RGB_WATER, 16, 1.6);
        int hits = 0;
        for (LivingEntity f : b.foes(20.0)) {
            if (hits >= KakashiMath.MAX_FOES) break;
            for (int i = 0; i + 1 < path.length; i++) {
                Vec3 a = coil(b.position(), dir, path[i]), c = coil(b.position(), dir, path[i + 1]);
                if (dist2(f.position().add(0, 1, 0), a, c) <= 2.0 * 2.0 + f.getBbWidth()) {
                    if (b.strike(f, b.base() * 1.3f, true)) {
                        b.makeWet(f);
                        b.shove(f, b.position(), 0.9);
                        KakashiFx.burst(sl, KakashiFx.WATER, f.position().add(0, 1, 0), 10, 0.6);
                        hits++;
                    }
                    break;
                }
            }
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 2.0f, 0.7f);
    }

    /** Squared distance from a point to a segment. */
    static double dist2(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr(), t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceToSqr(a.add(ab.scale(t)));
    }

    // ================================================================ Ninken Pursuit: eight hounds, 4 ticks apart, on at most four foes
    private static void ninkenTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        KakashiFx.ring(sl, KakashiFx.NINKEN, b.position(), 2.5 + (20 - Math.min(20, left)) * 0.15, 12);
        if (left % 9 == 0) KakashiFx.puff(sl, KakashiFx.NINKEN, t.position().add(0, 0.3, 0), 6, 0.8);
    }

    private static void ninken(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        List<LivingEntity> prey = new ArrayList<>();
        prey.add(t);
        for (LivingEntity f : b.foes(30.0)) {
            if (prey.size() >= 4) break;
            if (f != t) prey.add(f);
        }
        b.announce("Pakkun, Bull, Guruko, Uhei, Shiba, Bisuke, Akino and Urushi answer his call.");
        int i = 0;
        for (KakashiMath.Hound h : KakashiMath.PACK_ROSTER) {
            final LivingEntity v = prey.get(i++ % prey.size());
            b.later(h.delay(), () -> {
                if (!v.isAlive() || v.distanceTo(b) > 34.0) return;
                Vec3 from = b.position().add(0, 0.4, 0), to = v.position().add(0, 0.5, 0);
                KakashiFx.line(sl, KakashiFx.NINKEN, from, to, 8);
                KakashiFx.puff(sl, KakashiFx.NINKEN, to, 5, 0.4);
                if (!b.strike(v, b.base() * h.mul(), false)) return;
                switch (h.role()) {
                    case "pin" -> v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false));
                    case "shove", "heavy" -> b.shove(v, from, h.role().equals("heavy") ? 1.1 : 0.8);
                    case "drag" -> b.shove(v, to.add(to.subtract(from)), 0.5);          // pulled toward him
                    default -> { }
                }
            });
        }
    }

    // ================================================================ Shadow Clone Ambush: three marks round the target, each struck in turn
    private static void clonesTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, left);
        if (left > KakashiBoss.FREEZE) b.lockAngle = b.getRandom().nextDouble() * Math.PI * 2;
        for (double[] m : KakashiMath.cloneMarks(b.lockAngle, 4.5)) {
            Vec3 p = c.add(m[0], 0, m[1]);
            KakashiFx.ring(sl, KakashiFx.CLONE, p, 1.6, 8);
            KakashiFx.puff(sl, KakashiFx.CLONE, p.add(0, 1, 0), 3, 0.3);
        }
    }

    private static void clones(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, 0);
        double[][] marks = KakashiMath.cloneMarks(b.lockAngle, 4.5);
        b.clearLock();
        for (int i = 0; i < marks.length; i++) {
            Vec3 p = c.add(marks[i][0], 0, marks[i][1]);
            b.later(i * 4, () -> {
                KakashiFx.burst(sl, KakashiFx.CLONE, p.add(0, 1, 0), 12, 0.8);
                KakashiFx.line(sl, KakashiFx.CLONE, p.add(0, 1.2, 0), c.add(0, 1.2, 0), 6);
                for (LivingEntity f : b.foesAround(p, 2.2)) if (b.strike(f, b.base() * 0.8f, false)) b.shove(f, p, 0.7);
                sl.playSound(null, net.minecraft.core.BlockPos.containing(p), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.4f, 1.2f);
            });
        }
    }

    // ================================================================ Kamui Step: out of one door and in at his back
    private static void warpTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 behind = behind(t);
        KakashiFx.dark(sl, KakashiFx.KAMUI, b.position().add(0, 1, 0), 6, 0.4);
        KakashiFx.ring(sl, KakashiFx.KAMUI, behind, 1.4, 8);
    }

    private static Vec3 behind(LivingEntity t) {
        Vec3 look = flat(t.getLookAngle());
        return t.position().subtract(look.scale(2.4));
    }

    private static void warpStrike(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl) || b.distanceTo(t) > 48.0) return;
        Vec3 dest = ZogratisBoss.groundOrNull(sl, behind(t));
        if (dest == null || !b.warp(dest)) return;
        b.getLookControl().setLookAt(t, 60f, 60f);
        b.later(6, () -> {
            if (t.isAlive() && b.distanceTo(t) < 6.0) {
                b.swing(InteractionHand.MAIN_HAND);
                b.strike(t, b.base() * 1.2f, false);
            }
        });
    }

    // ================================================================ Kamui Shuriken: four swirls round the target; the diagonals are safe
    private static void shurikenTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, left);
        for (double[] s : KakashiMath.shurikenCross(7.0)) {
            Vec3 sw = c.add(s[0], 1.2, s[1]);
            KakashiFx.dark(sl, KakashiFx.KAMUI, sw, 4, 0.4);
            if (left <= 20) KakashiFx.line(sl, KakashiFx.KAMUI, sw, sw.add(s[2] * 14.0, 0, s[3] * 14.0), 8);        // the lanes show for the last second
        }
    }

    private static void shuriken(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, 0);
        b.clearLock();
        double[][] cross = KakashiMath.shurikenCross(7.0);
        for (int i = 0; i < cross.length; i++) {
            final double[] s = cross[i];
            b.later(i * 3, () -> {
                Vec3 sw = c.add(s[0], 1.2, s[1]), far = sw.add(s[2] * 14.0, 0, s[3] * 14.0);
                KakashiFx.shape(sl, VfxShape.SPATIAL_RIFT, sw, sw.add(0, 0.5, 0), KakashiFx.RGB_KAMUI, 10, 1.0);
                KakashiFx.line(sl, KakashiFx.KAMUI, sw, far, 12);
                for (LivingEntity f : b.foesAround(c, 12.0)) {
                    Vec3 r = f.position().subtract(c);
                    if (KakashiMath.inShurikenLane(r.x, r.z, s, 1.4)) {
                        b.strike(f, b.base() * 1.5f, true);
                        b.shove(f, sw, 0.6);
                    }
                }
            });
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0f, 1.5f);
    }

    // ================================================================ Kamui Vortex (ultimate)
    private static void vortexTell(KakashiBoss b, LivingEntity t, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 at = b.aim(t, left);
        KakashiFx.dark(sl, KakashiFx.KAMUI, at.add(0, 1.2, 0), 8, 1.2 + (total - left) * 0.03);
        if (left % 9 == 0) KakashiFx.ring(sl, KakashiFx.KAMUI, at, 14.0, 20);
    }

    // ================================================================ Perfect Susanoo
    private static void susanooTell(KakashiBoss b, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        KakashiFx.gather(sl, KakashiFx.SUSANOO, b.position().add(0, 1, 0), 0.6 + (total - left) * 0.06, 8);
        if (left % 9 == 0) KakashiFx.ring(sl, KakashiFx.SUSANOO, b.position(), 3.0 + (total - left) * 0.1, 14);
    }

    // ================================================================ Purple Lightning (ultimate): up to three marked spots, each struck at once
    private static void purpleTell(KakashiBoss b, LivingEntity t, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        if (b.marks.isEmpty() || left > KakashiBoss.FREEZE) {
            b.marks.clear();
            List<LivingEntity> foes = b.foes(36.0);
            if (foes.size() > KakashiMath.MAX_FOES) foes = foes.subList(0, KakashiMath.MAX_FOES);
            double[] d = new double[foes.size()];
            for (int i = 0; i < d.length; i++) d[i] = foes.get(i).distanceTo(b);
            for (int i : KakashiMath.purpleTargets(d, 3)) b.marks.add(foes.get(i).position());
        }
        KakashiFx.gather(sl, KakashiFx.PURPLE, chest(b).add(0, 1.2, 0), 0.5 + (total - left) * 0.03, 8);
        for (Vec3 m : b.marks) {
            KakashiFx.ring(sl, KakashiFx.PURPLE, m, 3.2, 12);
            if (left <= KakashiBoss.FREEZE) KakashiFx.puff(sl, KakashiFx.PURPLE, m.add(0, 0.6, 0), 4, 0.9);
        }
    }

    private static void purple(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        List<Vec3> spots = new ArrayList<>(b.marks);
        b.clearLock();
        if (spots.isEmpty()) spots.add(t.position());
        int vfx = 0;
        for (Vec3 m : spots) {
            if (vfx++ < KakashiMath.MAX_VFX_PER_MOVE) KakashiFx.shape(sl, VfxShape.LIGHTNING_GOD, m, m.add(0, 6, 0), KakashiFx.RGB_PURPLE, 14, 3.2);
            KakashiFx.burst(sl, KakashiFx.PURPLE, m.add(0, 1, 0), 16, 1.6);
            for (LivingEntity f : b.foesAround(m, 3.2)) if (b.lightning(f, 3.0f)) b.shove(f, m, 1.0);
            sl.playSound(null, net.minecraft.core.BlockPos.containing(m), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.0f, 1.2f);
        }
    }

    // ================================================================ the canon kit pass: fourteen moves appended (docs/bosses/kakashi.md, "Canon audit")
    private static Vec3 ground(ServerLevel sl, Vec3 p) {
        Vec3 g = ZogratisBoss.groundOrNull(sl, p);
        return g == null ? p : g;
    }

    /** Roots a foe (cannot walk) and, with {@code fatigue}, slows the arm: the paralysis of the clone of lightning, the jaws of the ninken. */
    private static void root(LivingEntity f, int ticks, boolean fatigue) {
        f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, true, false));
        if (fatigue) f.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 2, true, false));
    }

    private static void appendCanonMoves(List<Move> out) {
        out.add(Move.builder(CUTTER, "Lightning Cutter").minPower(0).windUp(26).cooldown(360).range(4, 20).weight(4).recovery(24)
                .tell((b, t, left, total) -> dashTell(k(b), t, left, KakashiFx.RAIKIRI, false)).run((b, t) -> cutter(k(b), t)));
        out.add(Move.builder(LCLONE, "Lightning Release: Shadow Clone").minPower(0).windUp(16).cooldown(380).range(0, 26).weight(3).recovery(10)
                .tell((b, t, left, total) -> lcloneTell(k(b), t, left)).run((b, t) -> lclone(k(b), t)));
        out.add(Move.builder(PURPLE_STRIKE, "Purple Electricity: Empowered Strike").minPower(0).windUp(24).cooldown(260).range(0, 9).weight(4).recovery(20)
                .tell((b, t, left, total) -> dashTell(k(b), t, left, KakashiFx.PURPLE, false)).run((b, t) -> purpleStrike(k(b), t)));
        out.add(Move.builder(PURPLE_BURST, "Purple Electricity: Burst").minPower(0).windUp(30).cooldown(320).range(0, 7).weight(3).recovery(24)
                .tell((b, t, left, total) -> burstTell(k(b), left, total)).run((b, t) -> purpleBurst(k(b))));
        out.add(Move.builder(PURPLE_CHAIN, "Purple Electricity: Chain").minPower(0).windUp(28).cooldown(340).range(3, 30).weight(4).recovery(18)
                .tell((b, t, left, total) -> chainTell(k(b), t, left, total)).run((b, t) -> purpleChain(k(b), t)));
        out.add(Move.builder(KAMUI_WARP, "Kamui: Distant Warp").minPower(0).windUp(34).cooldown(420).range(8, 44).weight(3).recovery(16)
                .tell((b, t, left, total) -> distantTell(k(b), t, left)).run((b, t) -> distantWarp(k(b), t)));
        out.add(Move.builder(EARTH_FANG, "Summoning: Earth Release: Tracking Fang").minPower(0).windUp(26).cooldown(330).range(0, 44).weight(3).recovery(14)
                .tell((b, t, left, total) -> earthFangTell(k(b), t, left)).run((b, t) -> earthFang(k(b), t)));
        out.add(Move.builder(GATES, "Eight Gates").minPower(0).windUp(30).cooldown(900).range(0, 48).weight(2).recovery(8)
                .tell((b, t, left, total) -> gatesTell(k(b), left, total)).run((b, t) -> k(b).openGates()));
        out.add(Move.builder(KUBIKIRI, "Kubikiribocho").minPower(0).windUp(28).cooldown(280).range(0, 7).weight(4).recovery(22)
                .tell((b, t, left, total) -> kubikiriTell(k(b), t, left)).run((b, t) -> kubikiri(k(b), t)));
        out.add(Move.builder(SABRE, "White Light Chakra Sabre").minPower(0).windUp(12).cooldown(150).range(0, 6).weight(5).recovery(12)
                .tell((b, t, left, total) -> sabreTell(k(b), t, left)).run((b, t) -> sabre(k(b), t)));
        out.add(Move.builder(WIRE, "Kunai Wire").minPower(0).windUp(14).cooldown(200).range(4, 24).weight(4).recovery(12)
                .tell((b, t, left, total) -> wireTell(k(b), t, left)).run((b, t) -> wire(k(b), t)));
        out.add(Move.builder(THUNDER_GOD, "Flying Thunder God Kunai").minPower(0).windUp(20).cooldown(420).range(6, 40).weight(3).recovery(14)
                .tell((b, t, left, total) -> thunderTell(k(b), t, left)).run((b, t) -> thunderGod(k(b), t)));
        out.add(Move.builder(FIREBALL, "Fire Release: Great Fireball").minPower(0).windUp(26).cooldown(240).range(5, 30).weight(4).recovery(14)
                .tell((b, t, left, total) -> fireballTell(k(b), t, left, total)).run((b, t) -> fireball(k(b), t)));
        out.add(Move.builder(SIX_PATHS, "Six Paths Awakening").minPower(0).windUp(40).cooldown(9000).range(0, 48).weight(1).recovery(10)
                .tell((b, t, left, total) -> sixPathsTell(k(b), left, total)).run((b, t) -> k(b).startSixPaths()));
    }

    // ---------------------------------------------------------------- Lightning Cutter (S rank): wider and harder than Raikiri, and his hand cuts any lightning that reaches him
    private static void cutter(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, 0), dir = flat(aim.subtract(b.position()));
        b.clearLock();
        Vec3 stop = ZogratisBoss.groundOrNull(sl, aim.subtract(dir.scale(1.8)));
        boolean moved = stop != null && b.blinkTo(stop) && b.withinLeash(stop);
        b.raikiriHand(false);
        if (!moved) return;
        b.getLookControl().setLookAt(aim.x, aim.y + 1, aim.z);
        b.later(4, () -> {
            b.swing(InteractionHand.MAIN_HAND);
            long seed = sl.getGameTime();
            Vec3 from = b.position().add(0, 1.1, 0), tip = from.add(dir.scale(4.2));
            KakashiFx.chidori(sl, from.add(dir.scale(0.8)), 2.4, 5, seed);
            KakashiFx.bolt(sl, KakashiFx.CORE, from, tip, 4, 0.25, seed);
            KakashiFx.shape(sl, VfxShape.LIGHTNING_SPEAR, from, tip, KakashiFx.RGB_RAIKIRI, 10, 1.5);
            KakashiFx.shape(sl, VfxShape.LIGHTNING_FIEND, from.add(dir.scale(1.0)), from.add(dir.scale(1.0)).add(0, 1, 0), KakashiFx.RGB_CORE, 12, 1.6);
            KakashiFx.burst(sl, KakashiFx.RAIKIRI, from.add(dir.scale(3.0)), 14, 0.8);
            boolean hit = false;
            for (LivingEntity f : b.lane(b.position(), dir, 4.2, 1.6)) hit |= b.lightning(f, 3.1f);
            sl.playSound(null, b.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 2.0f, 2.0f);
            if (!hit) b.speak("raikiri_miss", false);
        });
    }

    // ---------------------------------------------------------------- Lightning Release: Shadow Clone: two decoys that crackle; a foe who touches one is paralysed
    private static void lcloneTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, left);
        if (left > KakashiBoss.FREEZE) b.lockAngle = b.getRandom().nextDouble() * Math.PI * 2;
        double[][] marks = KakashiMath.cloneMarks(b.lockAngle, 3.4);
        for (int i = 0; i < KakashiMath.LCLONE_MAX; i++) {
            Vec3 p = c.add(marks[i][0], 0, marks[i][1]);
            KakashiFx.ring(sl, KakashiFx.CLONE, p, 1.2, 8);
            KakashiFx.puff(sl, KakashiFx.RAIKIRI, p.add(0, 1, 0), 2, 0.4);
        }
    }

    private static void lclone(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, 0);
        double[][] marks = KakashiMath.cloneMarks(b.lockAngle, 3.4);
        b.clearLock();
        b.swing(InteractionHand.MAIN_HAND);
        for (int i = 0; i < KakashiMath.LCLONE_MAX; i++) {
            Vec3 p = ground(sl, c.add(marks[i][0], 0, marks[i][1]));
            b.plantLClone(p);
            KakashiFx.burst(sl, KakashiFx.CLONE, p.add(0, 1, 0), 10, 0.6);
            KakashiFx.line(sl, KakashiFx.RAIKIRI, b.position().add(0, 1.2, 0), p.add(0, 1.2, 0), 6);
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2f, 1.4f);
    }

    // ---------------------------------------------------------------- Purple Electricity, mode 2: the empowered strike (a fast close thrust that goes through)
    private static void purpleStrike(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, 0), dir = flat(aim.subtract(b.position()));
        b.clearLock();
        Vec3 stop = ZogratisBoss.groundOrNull(sl, aim.subtract(dir.scale(1.5)));
        if (stop != null && b.withinLeash(stop) && b.position().distanceToSqr(stop) > 2.0) b.blinkTo(stop);
        b.raikiriHand(false);
        b.getLookControl().setLookAt(aim.x, aim.y + 1, aim.z);
        b.later(4, () -> {
            b.swing(InteractionHand.MAIN_HAND);
            long seed = sl.getGameTime();
            Vec3 from = b.position().add(0, 1.1, 0), tip = from.add(dir.scale(4.6));
            KakashiFx.bolt(sl, KakashiFx.PURPLE, from, tip, 4, 0.3, seed);
            KakashiFx.chidori(sl, from.add(dir.scale(1.0)), 2.0, 4, seed);
            KakashiFx.shape(sl, VfxShape.LIGHTNING_SPEAR, from, tip, KakashiFx.RGB_PURPLE, 10, 1.6);
            for (LivingEntity f : b.lane(b.position(), dir, 4.6, 1.4)) if (b.lightning(f, 2.8f)) b.shove(f, b.position(), 1.0);
            sl.playSound(null, b.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 2.0f, 1.4f);
        });
    }

    // ---------------------------------------------------------------- Purple Electricity, mode 3: the burst all round him
    private static void burstTell(KakashiBoss b, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        KakashiFx.gather(sl, KakashiFx.PURPLE, b.position().add(0, 1.2, 0), 0.4 + (total - left) * 0.02, 8);
        if (left % 6 == 0) KakashiFx.ring(sl, KakashiFx.PURPLE, b.position(), 5.5, 20);
        if (left % 3 == 0) KakashiFx.chidori(sl, b.position().add(0, 1.2, 0), 1.3, 3, b.tickCount);
    }

    private static void purpleBurst(KakashiBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position().add(0, 1.0, 0);
        KakashiFx.shape(sl, VfxShape.FX_SPIRIT_NOVA, c, c.add(0, 1, 0), KakashiFx.RGB_PURPLE, 14, 5.5);
        KakashiFx.shape(sl, VfxShape.LIGHTNING_FIEND, c, c.add(0, 1, 0), KakashiFx.RGB_PURPLE, 14, 2.2);
        KakashiFx.burst(sl, KakashiFx.PURPLE, c, 20, 2.6);
        KakashiFx.ring(sl, KakashiFx.CORE, b.position(), 5.5, 22);
        for (LivingEntity f : b.foesAround(b.position(), 5.5)) if (b.lightning(f, 2.4f)) b.shove(f, b.position(), 1.2);
        sl.playSound(null, b.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5f, 1.3f);
    }

    // ---------------------------------------------------------------- Purple Electricity, mode 4: through water (a soaked foe passes it to every soaked foe near)
    private static void chainTell(KakashiBoss b, LivingEntity t, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 hand = chest(b);
        KakashiFx.gather(sl, KakashiFx.PURPLE, hand, 0.4 + (total - left) * 0.02, 6);
        if (left % 6 == 0) {
            KakashiFx.line(sl, KakashiFx.PURPLE, hand, t.position().add(0, 1, 0), 10);
            KakashiFx.ring(sl, b.isWet(t) || t.isInWater() ? KakashiFx.WATER : KakashiFx.PURPLE, t.position(), 3.0, 12);
        }
    }

    private static void purpleChain(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        List<LivingEntity> near = new ArrayList<>(b.foesAround(t.position(), 10.0));
        if (!near.contains(t)) near.add(0, t);
        if (near.size() > KakashiMath.MAX_FOES) near = near.subList(0, KakashiMath.MAX_FOES);
        double[][] pos = new double[near.size()][2];
        boolean[] wet = new boolean[near.size()];
        int first = 0;
        for (int i = 0; i < pos.length; i++) {
            LivingEntity f = near.get(i);
            pos[i][0] = f.getX();
            pos[i][1] = f.getZ();
            wet[i] = b.isWet(f) || f.isInWater();
            if (f == t) first = i;
        }
        List<Integer> order = KakashiMath.chain(pos, wet, first, 6.0, 4);
        Vec3 prev = chest(b);
        for (int n = 0; n < order.size(); n++) {
            final LivingEntity f = near.get(order.get(n));
            final Vec3 from = prev;
            prev = f.position().add(0, 1.0, 0);
            b.later(n * 3, () -> {
                Vec3 at = f.position().add(0, 1.0, 0);
                KakashiFx.bolt(sl, KakashiFx.PURPLE, from, at, 4, 0.3, sl.getGameTime());
                if (n0(from, at)) KakashiFx.shape(sl, VfxShape.FX_LIGHTNING_ARC, from, at, KakashiFx.RGB_PURPLE, 8, 1.0);
                KakashiFx.burst(sl, KakashiFx.PURPLE, at, 8, 0.5);
                if (b.lightning(f, 2.2f)) b.shove(f, from, 0.5);
            });
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0f, 1.6f);
    }

    /** A chain link is drawn as a big effect only when it is long enough to see (keeps the effect packets of a move low). */
    private static boolean n0(Vec3 a, Vec3 b) { return a.distanceToSqr(b) > 4.0; }

    // ---------------------------------------------------------------- Kamui, long range: the Distant Warp (a foe is taken into the dimension and comes out elsewhere, dazed)
    private static void distantTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, left);
        Vec3 eye = b.position().add(0, b.getBbHeight() * 0.9, 0);
        KakashiFx.dark(sl, KakashiFx.KAMUI, eye, 4, 0.2);                                   // the left eye
        if (left % 5 == 0) KakashiFx.line(sl, KakashiFx.KAMUI, eye, aim.add(0, 1, 0), 12);
        KakashiFx.ring(sl, KakashiFx.KAMUI, aim, 2.2, 12);                                  // the barrier that will close round the spot
        if (left <= KakashiBoss.FREEZE && left % 3 == 0) KakashiFx.dark(sl, KakashiFx.KAMUI, aim.add(0, 1, 0), 6, 0.9);
    }

    private static void distantWarp(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, 0);
        b.clearLock();
        double a0 = b.getRandom().nextDouble() * Math.PI * 2;
        Vec3 dest = null;
        for (int i = 0; i < 4 && dest == null; i++) {                                       // the first of four directions that has ground inside the arena
            Vec3 g = ZogratisBoss.groundOrNull(sl, c.add(Math.cos(a0 + i * Math.PI / 2) * 9.0, 0, Math.sin(a0 + i * Math.PI / 2) * 9.0));
            if (g != null && b.withinLeash(g)) dest = g;
        }
        KakashiFx.shape(sl, VfxShape.SPACE_PORTAL, c.add(0, 1, 0), c.add(0, 1.5, 0), KakashiFx.RGB_KAMUI, 16, 1.6);
        if (dest != null) KakashiFx.shape(sl, VfxShape.SPACE_PORTAL, dest.add(0, 1, 0), dest.add(0, 1.5, 0), KakashiFx.RGB_KAMUI, 16, 1.6);
        for (LivingEntity f : b.foesAround(c, 2.4)) {
            KakashiFx.dark(sl, KakashiFx.KAMUI, f.position().add(0, 1, 0), 10, 0.6);
            if (!b.strike(f, b.base() * 1.0f, true)) continue;
            if (dest != null) {
                f.randomTeleport(dest.x, dest.y, dest.z, true);
                f.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, true, false));
                KakashiFx.dark(sl, KakashiFx.KAMUI, dest.add(0, 1, 0), 8, 0.6);
            }
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.HOSTILE, 1.0f, 0.8f);
    }

    // ---------------------------------------------------------------- Summoning: Earth Release: Tracking Fang (the hounds burst out of the ground and hold the foe)
    private static void earthFangTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, left);                                                          // follows the marked foe wherever he goes, then freezes
        KakashiFx.ring(sl, KakashiFx.EARTH, aim, 2.0, 12);
        if (left % 4 == 0) KakashiFx.puff(sl, KakashiFx.NINKEN, aim.add(0, 0.2, 0), 5, 0.7);
        if (left % 9 == 0) KakashiFx.line(sl, KakashiFx.NINKEN, b.position().add(0, 0.3, 0), aim.add(0, 0.3, 0), 10);
    }

    private static void earthFang(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, 0);
        b.clearLock();
        b.announce("Pakkun, Bull and Bisuke burst out of the ground.");
        for (int i = 0; i < 3; i++) {
            final double a = i * Math.PI * 2.0 / 3.0 + 0.4;
            b.later(i * 4, () -> {
                Vec3 p = c.add(Math.cos(a) * 1.3, 0, Math.sin(a) * 1.3);
                KakashiFx.burst(sl, KakashiFx.EARTH, p.add(0, 0.4, 0), 10, 0.6);
                KakashiFx.burst(sl, KakashiFx.NINKEN, p.add(0, 0.6, 0), 6, 0.5);
                sl.playSound(null, net.minecraft.core.BlockPos.containing(p), SoundEvents.GRAVEL_BREAK, SoundSource.HOSTILE, 1.4f, 0.7f);
            });
        }
        KakashiFx.shape(sl, VfxShape.EARTH_CLAWS, c, c.add(0, 1, 0), KakashiFx.RGB_EARTH, 24, 2.4);
        b.later(8, () -> {
            for (LivingEntity f : b.foesAround(c, 2.4)) {
                if (b.strike(f, b.base() * 0.7f, false)) root(f, 44, false);                // held in the jaws: cannot walk for 2 s
            }
        });
    }

    // ---------------------------------------------------------------- Eight Gates
    private static void gatesTell(KakashiBoss b, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        KakashiFx.gather(sl, KakashiFx.GATES, b.position().add(0, 1, 0), 0.5 + (total - left) * 0.04, 8);
        if (left % 9 == 0) KakashiFx.ring(sl, KakashiFx.GATES, b.position(), 2.5 + (total - left) * 0.1, 14);
    }

    // ---------------------------------------------------------------- Kubikiribocho: one huge slow sweep in front of him
    private static void kubikiriTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, left), dir = flat(aim.subtract(b.position()));
        for (int i = -3; i <= 3; i++) {                                                     // the arc of the blade, drawn on the ground
            double a = i * 0.5;
            Vec3 d = new Vec3(dir.x * Math.cos(a) - dir.z * Math.sin(a), 0, dir.x * Math.sin(a) + dir.z * Math.cos(a));
            KakashiFx.puff(sl, KakashiFx.KUNAI, b.position().add(d.scale(4.4)).add(0, 0.2, 0), 1, 0.1);
        }
        if (left <= KakashiBoss.FREEZE) KakashiFx.ring(sl, KakashiFx.KUNAI, b.position(), 4.6, 14);
    }

    private static void kubikiri(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, 0), dir = flat(aim.subtract(b.position()));
        b.clearLock();
        b.swing(InteractionHand.MAIN_HAND);
        b.getLookControl().setLookAt(aim.x, aim.y + 1, aim.z);
        Vec3 from = b.position().add(0, 1.2, 0);
        KakashiFx.shape(sl, VfxShape.WIND_SLASH, from, from.add(dir.scale(5.0)), KakashiFx.RGB_SABRE, 12, 2.6);
        KakashiFx.burst(sl, KakashiFx.KUNAI, from.add(dir.scale(3.5)), 14, 1.6);
        for (LivingEntity f : b.foesAround(b.position(), 4.8)) {
            Vec3 r = f.position().subtract(b.position());
            Vec3 rf = flat(r);
            if (rf.x * dir.x + rf.z * dir.z > -0.2 && b.strike(f, b.base() * 1.9f, false)) b.shove(f, b.position(), 1.1);
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.2f, 0.5f);
    }

    // ---------------------------------------------------------------- White Light Chakra Sabre: three fast cuts, the last one a stab
    private static void sabreTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, left);
        KakashiFx.puff(sl, KakashiFx.SABRE, b.position().add(0, 1.1, 0), 4, 0.25);
        if (left % 4 == 0) KakashiFx.line(sl, KakashiFx.SABRE, b.position().add(0, 1.1, 0), aim.add(0, 1.1, 0), 8);
    }

    private static void sabre(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, 0), dir = flat(aim.subtract(b.position()));
        b.clearLock();
        for (int i = 0; i < 3; i++) {
            final int n = i;
            b.later(i * 5, () -> {
                b.swing(n % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                b.getLookControl().setLookAt(aim.x, aim.y + 1, aim.z);
                Vec3 from = b.position().add(0, 1.2, 0), tip = from.add(dir.scale(n == 2 ? 3.8 : 3.0));
                KakashiFx.line(sl, KakashiFx.SABRE, from, tip, 8);
                if (n < 2) KakashiFx.shape(sl, VfxShape.WIND_SLASH, from, tip, KakashiFx.RGB_SABRE, 8, 1.4);
                else KakashiFx.shape(sl, VfxShape.LIGHTNING_SPEAR, from, tip, KakashiFx.RGB_SABRE, 8, 1.0);
                for (LivingEntity f : b.lane(b.position(), dir, n == 2 ? 3.8 : 3.0, n == 2 ? 0.8 : 1.3)) b.strike(f, b.base() * (n == 2 ? 1.0f : 0.6f), false);
                sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 1.8f);
            });
        }
    }

    // ---------------------------------------------------------------- Kunai Wire: a thin line to the foe, who is dragged to him
    private static void wireTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, left);
        KakashiFx.line(sl, KakashiFx.SABRE, b.position().add(0, 1.2, 0), aim.add(0, 1.0, 0), 10);
        if (left <= KakashiBoss.FREEZE) KakashiFx.ring(sl, KakashiFx.SABRE, aim, 1.6, 10);
    }

    private static void wire(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, 0), dir = flat(aim.subtract(b.position()));
        b.clearLock();
        b.swing(InteractionHand.OFF_HAND);
        Vec3 from = b.position().add(0, 1.2, 0);
        KakashiFx.shape(sl, VfxShape.THREAD_LINE, from, aim.add(0, 1.0, 0), KakashiFx.RGB_SABRE, 12, 1.0);
        for (LivingEntity f : b.lane(b.position(), dir, aim.distanceTo(b.position()) + 2.0, 1.0)) {
            if (!b.strike(f, b.base() * 0.5f, false)) continue;
            Vec3 pull = flat(b.position().subtract(f.position()));
            f.setDeltaMovement(f.getDeltaMovement().add(pull.scale(1.0)).add(0, 0.2, 0));      // dragged to the blade
            f.hurtMarked = true;
            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, false));
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.0f, 1.6f);
    }

    // ---------------------------------------------------------------- Flying Thunder God Kunai: three markers round the foe, and he is at each of them in turn
    private static void thunderTell(KakashiBoss b, LivingEntity t, int left) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, left);
        if (left > KakashiBoss.FREEZE) b.lockAngle = b.getRandom().nextDouble() * Math.PI * 2;
        double[][] marks = KakashiMath.cloneMarks(b.lockAngle, 4.0);
        for (double[] m : marks) {
            Vec3 p = c.add(m[0], 0, m[1]);
            KakashiFx.ring(sl, KakashiFx.THUNDER_GOD, p, 1.0, 8);
            if (left % 5 == 0) KakashiFx.line(sl, KakashiFx.THUNDER_GOD, b.position().add(0, 1.3, 0), p.add(0, 0.5, 0), 6);       // the kunai in flight
        }
    }

    private static void thunderGod(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.aim(t, 0);
        double[][] marks = KakashiMath.cloneMarks(b.lockAngle, 4.0);
        b.clearLock();
        b.swing(InteractionHand.OFF_HAND);
        for (int i = 0; i < marks.length; i++) {
            final Vec3 mark = c.add(marks[i][0], 0, marks[i][1]);
            b.later(6 + i * 9, () -> {
                Vec3 stop = ZogratisBoss.groundOrNull(sl, mark);
                if (stop == null || !b.withinLeash(stop)) return;
                Vec3 from = b.position().add(0, 1, 0);
                KakashiFx.burst(sl, KakashiFx.THUNDER_GOD, from, 10, 0.5);                  // the flash where he leaves
                if (!b.blinkTo(stop)) return;
                KakashiFx.burst(sl, KakashiFx.THUNDER_GOD, stop.add(0, 1, 0), 12, 0.6);     // and where he arrives
                KakashiFx.line(sl, KakashiFx.THUNDER_GOD, from, stop.add(0, 1, 0), 10);
                Vec3 dir = flat(c.subtract(stop));
                b.getLookControl().setLookAt(c.x, c.y + 1, c.z);
                b.swing(InteractionHand.MAIN_HAND);
                for (LivingEntity f : b.lane(stop, dir, 3.2, 1.2)) if (b.strike(f, b.base() * 0.9f, false)) b.shove(f, stop, 0.6);
                sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.2f, 1.8f);
            });
        }
    }

    // ---------------------------------------------------------------- Fire Release: Great Fireball (a copied jutsu: a lane of fire that sets the foe alight)
    private static void fireballTell(KakashiBoss b, LivingEntity t, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, left), dir = flat(aim.subtract(b.position()));
        KakashiFx.gather(sl, KakashiFx.FIRE, chest(b).add(dir.scale(0.6)), 0.3 + (total - left) * 0.02, 6);
        if (left % 6 == 0) KakashiFx.line(sl, KakashiFx.FIRE, chest(b), aim.add(0, 1.0, 0), 10);
        if (left <= KakashiBoss.FREEZE) KakashiFx.ring(sl, KakashiFx.FIRE, aim, 3.2, 14);
    }

    private static void fireball(KakashiBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 aim = b.aim(t, 0), dir = flat(aim.subtract(b.position()));
        b.clearLock();
        b.swing(InteractionHand.MAIN_HAND);
        Vec3 from = chest(b), end = from.add(dir.scale(Math.min(18.0, aim.distanceTo(b.position()) + 2.0)));
        KakashiFx.shape(sl, VfxShape.FIRE_SPIRAL, from, end, KakashiFx.RGB_FIRE, 14, 2.0);
        KakashiFx.shape(sl, VfxShape.FLAME_EXPLOSION, end, end, KakashiFx.RGB_FIRE, 12, 3.0);
        KakashiFx.burst(sl, KakashiFx.FIRE, end, 16, 1.6);
        for (LivingEntity f : b.lane(b.position(), dir, from.distanceTo(end), 2.0)) {
            if (b.strike(f, b.base() * 1.4f, true)) f.setRemainingFireTicks(60);
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 1.3f);
    }

    // ---------------------------------------------------------------- Six Paths Awakening (once per fight, below 20 percent): the charge; the work is in KakashiBoss#startSixPaths
    private static void sixPathsTell(KakashiBoss b, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        KakashiFx.gather(sl, KakashiFx.CORE, b.position().add(0, 1, 0), 0.6 + (total - left) * 0.05, 10);
        if (left % 6 == 0) KakashiFx.ring(sl, KakashiFx.SUSANOO, b.position(), 3.0 + (total - left) * 0.15, 16);
        if (left % 4 == 0) KakashiFx.chidori(sl, b.position().add(0, 1.2, 0), 1.0 + (total - left) * 0.04, 3 + (total - left) / 12, b.tickCount);
    }
}
