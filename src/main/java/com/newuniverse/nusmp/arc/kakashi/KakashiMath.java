package com.newuniverse.nusmp.arc.kakashi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: every rule of the Kakashi boss that is a number or a small state machine lives here as pure code (no Minecraft type), so the rules are unit tested without a game: the four phases
 * and which move opens in which, the per-hit cap, the Kamui intangibility window (he can only phase while he is NOT attacking, so there is always an opening), the Kawarimi swap, the
 * Susanoo shield, the Sharingan copy ledger (what he remembers of the hits he took, bounded), the wet-then-lightning bonus, and the layouts of the clone marks, the Kamui Shuriken cross
 * and the ninken pack. {@link KakashiBoss} and {@link KakashiMoves} only apply these answers to the world.
 */
public final class KakashiMath {
    private KakashiMath() {}

    // ================================================================ phases
    /** Phase indexes. 0 = the Copy Ninja (Sharingan), 1 = the pack and the water (ninken, clones, mist), 2 = the Mangekyo (Kamui), 3 = the Perfect Susanoo and Purple Lightning. */
    public static final int COPY = 0, PACK = 1, MANGEKYO = 2, SUSANOO = 3, PHASES = 4;
    /** Health share at which phases 1, 2, 3 begin (descending). */
    public static final float[] THRESHOLDS = {0.75f, 0.50f, 0.25f};
    public static final String[] PHASE_NAMES = {"The Copy Ninja", "The Pack", "The Mangekyo", "The Perfect Susanoo"};

    /** The phase a health share asks for, never lower than {@code current} (a heal does not take a phase back). */
    public static int phaseOf(float share, int current) {
        int p = share <= THRESHOLDS[2] ? SUSANOO : share <= THRESHOLDS[1] ? MANGEKYO : share <= THRESHOLDS[0] ? PACK : COPY;
        return Math.max(Math.max(0, Math.min(PHASES - 1, current)), p);
    }

    /** The move ids, in book order (appended only; the client names by index). */
    public static final String KUNAI = "kunai_barrage", RAIKIRI = "raikiri", COPY_MOVE = "sharingan_copy", FANG = "beast_fang", DRAGON = "water_dragon", NINKEN = "ninken_pursuit",
            CLONES = "clone_ambush", MIST = "hidden_mist", WALL = "earth_wall", WARP = "kamui_step", KAMUI_RAIKIRI = "kamui_raikiri", KAMUI_SHURIKEN = "kamui_shuriken", VORTEX = "kamui_vortex",
            SUSANOO_MOVE = "perfect_susanoo", PURPLE = "purple_lightning";
    public static final List<String> BOOK = List.of(KUNAI, RAIKIRI, COPY_MOVE, FANG, DRAGON, NINKEN, CLONES, MIST, WALL, WARP, KAMUI_RAIKIRI, KAMUI_SHURIKEN, VORTEX, SUSANOO_MOVE, PURPLE);

    /** The first phase in which a move may be chosen. */
    public static int openingPhase(String move) {
        return switch (move) {
            case KUNAI, RAIKIRI, COPY_MOVE -> COPY;
            case FANG, DRAGON, NINKEN, CLONES, MIST, WALL -> PACK;
            case WARP, KAMUI_RAIKIRI, KAMUI_SHURIKEN, VORTEX -> MANGEKYO;
            case SUSANOO_MOVE, PURPLE -> SUSANOO;
            default -> 99;
        };
    }

    // ================================================================ damage
    /** One hit takes at most this share of a victim's maximum health (the mod's balance law is stricter still for players). */
    public static float capHit(float dealt, float victimMax, double share) {
        float cap = (float) Math.max(1.0, victimMax * share);
        return Math.min(dealt, cap);
    }

    /** A target that is wet takes this much more from lightning (Water Dragon Bullet then Raikiri: the series' conductivity). */
    public static final float WET_LIGHTNING = 1.35f;
    public static final int WET_TICKS = 120;

    public static float lightningFactor(boolean wet) { return wet ? WET_LIGHTNING : 1.0f; }

    // ================================================================ Kamui: the intangibility window
    /**
     * He can turn intangible (Kamui) when a blow reaches him, but: only from the Mangekyo phase, only while he is not winding up or recovering a move (he is solid to attack, as in the
     * series), at most once per {@link #COOLDOWN} ticks and the window lasts {@link #WINDOW} ticks, so a combo that is timed after the first swing lands.
     */
    public static final class Kamui {
        public static final int WINDOW = 14, COOLDOWN = 90, SOLID_AFTER_MOVE = 20;
        private long readyAt, solidUntil, intangibleUntil;

        public void moveStarted(long now, int windUp) { solidUntil = Math.max(solidUntil, now + windUp + SOLID_AFTER_MOVE); }

        public boolean intangible(long now) { return now < intangibleUntil; }

        /** Called when a blow arrives: true when he phases through it (and the window opens). */
        public boolean tryPhase(long now, int phase, boolean enabled) {
            if (intangible(now)) return true;
            if (!enabled || phase < MANGEKYO || now < readyAt || now < solidUntil) return false;
            intangibleUntil = now + WINDOW;
            readyAt = now + COOLDOWN;
            return true;
        }

        public void reset() { readyAt = solidUntil = intangibleUntil = 0L; }

        public long readyAt() { return readyAt; }
    }

    // ================================================================ Kawarimi: the substitution
    /** A single hit of at least this share of his maximum health is swapped for a log (once per cooldown). */
    public static final float KAWARIMI_SHARE = 0.06f;
    public static final int KAWARIMI_COOLDOWN = 260;

    public static boolean kawarimi(float amount, float max, long now, long readyAt) { return now >= readyAt && amount >= max * KAWARIMI_SHARE; }

    // ================================================================ Susanoo: a shield that breaks
    public static final float SUSANOO_SHIELD = 0.07f;
    public static final int SUSANOO_TICKS = 360, SUSANOO_SLASH_EVERY = 64, SUSANOO_BROKEN_TICKS = 50;
    /** The damage taken while the Susanoo is broken is raised by this factor (he is open). */
    public static final float BROKEN_FACTOR = 1.3f;

    /** What a Susanoo shield lets through: {@code [left over after the shield, shield left]}. */
    public static float[] absorb(float shield, float amount) {
        float taken = Math.min(shield, amount);
        return new float[]{amount - taken, shield - taken};
    }

    // ================================================================ Sharingan copy: a bounded ledger of the hits he took
    /** The strongest recent hit each player landed on him. At most {@link #SIZE} players (the oldest is dropped), each entry good for {@link #KEEP} ticks. */
    public static final class Ledger {
        public static final int SIZE = 8, KEEP = 300;
        private final Map<UUID, float[]> hits = new LinkedHashMap<>();       // value: {amount, tick}

        public void record(UUID who, float amount, long now) {
            if (who == null || !(amount > 0f)) return;
            float[] old = hits.remove(who);
            float best = old != null && now - old[1] <= KEEP ? Math.max(old[0], amount) : amount;
            hits.put(who, new float[]{best, now});
            while (hits.size() > SIZE) hits.remove(hits.keySet().iterator().next());
        }

        /** The player whose remembered hit is the strongest, or null when he remembers nothing (or only stale hits). */
        public UUID strongest(long now) {
            UUID best = null;
            float v = 0f;
            for (Map.Entry<UUID, float[]> e : hits.entrySet()) {
                if (now - e.getValue()[1] > KEEP) continue;
                if (e.getValue()[0] > v) {
                    v = e.getValue()[0];
                    best = e.getKey();
                }
            }
            return best;
        }

        public float amount(UUID who) {
            float[] h = hits.get(who);
            return h == null ? 0f : h[0];
        }

        public void forget(UUID who) { hits.remove(who); }

        public void clear() { hits.clear(); }

        public int size() { return hits.size(); }
    }

    /** What the copy throws back: 90 percent of the remembered hit, at most this share of his own maximum health (a copy can never be worse than his own ultimate). */
    public static float copied(float remembered, float bossMax) { return Math.min(remembered * 0.9f, bossMax * 0.02f); }

    // ================================================================ layouts
    /** The three Shadow Clone marks round a centre: 120 degrees apart from a start angle, at a radius. {@code [x, z]} offsets. */
    public static double[][] cloneMarks(double startAngle, double radius) {
        double[][] out = new double[3][2];
        for (int i = 0; i < 3; i++) {
            double a = startAngle + i * Math.PI * 2.0 / 3.0;
            out[i][0] = Math.cos(a) * radius;
            out[i][1] = Math.sin(a) * radius;
        }
        return out;
    }

    /** The four Kamui Shuriken swirls (north, east, south, west) round the target: {@code [x, z, dirX, dirZ]}, each throws across the middle. The diagonals are the safe lanes. */
    public static double[][] shurikenCross(double radius) {
        return new double[][]{{0, -radius, 0, 1}, {radius, 0, -1, 0}, {0, radius, 0, -1}, {-radius, 0, 1, 0}};
    }

    /** True when a point is inside the lane of one shuriken: within {@code half} of the segment from the swirl through the middle to the far side. */
    public static boolean inShurikenLane(double px, double pz, double[] swirl, double half) {
        double sx = swirl[0], sz = swirl[1], dx = swirl[2], dz = swirl[3];
        double len = Math.hypot(sx, sz) * 2.0;
        double t = Math.max(0.0, Math.min(len, (px - sx) * dx + (pz - sz) * dz));
        double cx = sx + dx * t, cz = sz + dz * t;
        return Math.hypot(px - cx, pz - cz) <= half;
    }

    /** One ninken: its name, what it does and its launch order. */
    public record Hound(String name, String role, float mul, int delay) {}

    /** The eight hounds of the Pursuit, in the order they are sent (4 ticks apart). Names are those of Kakashi's pack. */
    public static final List<Hound> PACK_ROSTER = List.of(
            new Hound("Pakkun", "pin", 0.45f, 0), new Hound("Bull", "shove", 0.60f, 4), new Hound("Guruko", "bite", 0.45f, 8), new Hound("Uhei", "bite", 0.40f, 12),
            new Hound("Shiba", "drag", 0.40f, 16), new Hound("Bisuke", "heavy", 0.70f, 20), new Hound("Akino", "bite", 0.40f, 24), new Hound("Urushi", "dash", 0.25f, 28));

    /** The sum of the multipliers of the pack (an upper bound of what one foe can take from one Pursuit). */
    public static float packTotal() {
        float t = 0f;
        for (Hound h : PACK_ROSTER) t += h.mul();
        return t;
    }

    // ================================================================ Lightning Beast Tracking Fang: a homing step
    /** Turns a heading (radians) toward a wanted heading by at most {@code maxTurn}; the wolf follows its prey but cannot curl back at once. */
    public static double steer(double heading, double wanted, double maxTurn) {
        double d = wanted - heading;
        while (d > Math.PI) d -= Math.PI * 2;
        while (d < -Math.PI) d += Math.PI * 2;
        return heading + Math.max(-maxTurn, Math.min(maxTurn, d));
    }

    public static final int FANG_TICKS = 44;
    public static final double FANG_SPEED = 0.85, FANG_TURN = Math.toRadians(11.0), FANG_HIT = 1.25;

    /** The points of the Water Dragon's coil: {@code n} samples from the origin along the aim, swaying sideways with a travelling sine. */
    public static double[][] dragonPath(double length, int n, double sway, double phase) {
        double[][] out = new double[n][2];
        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0 : i / (double) (n - 1);
            out[i][0] = t * length;
            out[i][1] = Math.sin(t * Math.PI * 3.0 + phase) * sway * Math.sin(t * Math.PI);
        }
        return out;
    }

    /** The ground marks of Purple Lightning: at most {@code max} of the given distances (nearest first) get a mark; returns the indexes in the order they are marked. */
    public static List<Integer> purpleTargets(double[] distances, int max) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) idx.add(i);
        idx.sort((a, b) -> Double.compare(distances[a], distances[b]));
        return idx.subList(0, Math.min(max, idx.size()));
    }

    // ================================================================ bounds (stated in docs/bosses/kakashi.md)
    /** The most particle packets and effect packets any one call of the boss may ask for (the shared perf budgets cut it further). */
    public static final int MAX_POINTS = 24, MAX_VFX_PER_MOVE = 6;
    /** The most foes any one move looks at. */
    public static final int MAX_FOES = 12;
    /** Kamui Vortex: pull strength per tick (added velocity), ticks, radius. */
    public static final double PULL = 0.11;
    public static final int PULL_TICKS = 30;
}
