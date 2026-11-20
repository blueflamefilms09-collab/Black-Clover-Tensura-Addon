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
    /**
     * The canon kit pass (docs/bosses/kakashi.md, "Canon audit"; source: the English Naruto wiki rundown the owner supplied): fourteen moves appended after the first fifteen, in this order.
     * Lightning Cutter (S rank, cuts lightning, 4 charges per phase), Lightning Release Shadow Clone (paralyses), the other three modes of Purple Electricity (empowered strike, self-centred
     * burst, chain through water), Kamui long range (the Distant Warp), Earth Release Tracking Fang (the ninken root), the Eight Gates, Kubikiribocho, White Light Chakra Sabre, the kunai wire,
     * the Flying Thunder God kunai, the copied Great Fireball and the Six Paths awakening.
     */
    public static final String CUTTER = "lightning_cutter", LCLONE = "lightning_clone", PURPLE_STRIKE = "purple_strike", PURPLE_BURST = "purple_burst", PURPLE_CHAIN = "purple_chain",
            KAMUI_WARP = "kamui_warp", EARTH_FANG = "earth_fang", GATES = "eight_gates", KUBIKIRI = "kubikiribocho", SABRE = "chakra_sabre", WIRE = "kunai_wire", THUNDER_GOD = "thunder_god",
            FIREBALL = "great_fireball", SIX_PATHS = "six_paths";
    public static final List<String> BOOK = List.of(KUNAI, RAIKIRI, COPY_MOVE, FANG, DRAGON, NINKEN, CLONES, MIST, WALL, WARP, KAMUI_RAIKIRI, KAMUI_SHURIKEN, VORTEX, SUSANOO_MOVE, PURPLE,
            CUTTER, LCLONE, PURPLE_STRIKE, PURPLE_BURST, PURPLE_CHAIN, KAMUI_WARP, EARTH_FANG, GATES, KUBIKIRI, SABRE, WIRE, THUNDER_GOD, FIREBALL, SIX_PATHS);
    /** The first fifteen moves (the book before the canon kit pass): with {@code canonKit} off only these are chosen. */
    public static final int CLASSIC_MOVES = 15;

    /** The first phase in which a move may be chosen. */
    public static int openingPhase(String move) {
        return switch (move) {
            case KUNAI, RAIKIRI, COPY_MOVE -> COPY;
            case FANG, DRAGON, NINKEN, CLONES, MIST, WALL -> PACK;
            case WARP, KAMUI_RAIKIRI, KAMUI_SHURIKEN, VORTEX -> MANGEKYO;
            case SUSANOO_MOVE, PURPLE -> SUSANOO;
            case SABRE, WIRE -> COPY;
            case CUTTER, LCLONE, EARTH_FANG, KUBIKIRI, FIREBALL -> PACK;
            case KAMUI_WARP, GATES, THUNDER_GOD -> MANGEKYO;
            case PURPLE_STRIKE, PURPLE_BURST, PURPLE_CHAIN, SIX_PATHS -> SUSANOO;
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

    // ================================================================ canon limits as the balance rule (charges, strain, the observe-first rule, the Six Paths)
    /** Part I canon: chakra for 4 Lightning Cutters or 3 Kamui. Here: charges per phase, refilled when a phase begins. Chidori (the plain Raikiri) is free. */
    public static final class Charges {
        public static final int CUTTERS = 4, KAMUI = 3;
        private int cutters = CUTTERS, kamui = KAMUI;

        /** Which pool a move draws from: 1 = Lightning Cutter, 2 = Kamui warp, 0 = none. */
        public static int pool(String move) {
            return switch (move) {
                case CUTTER -> 1;
                case WARP, KAMUI_RAIKIRI, KAMUI_WARP -> 2;
                default -> 0;
            };
        }

        public boolean canUse(String move) {
            return switch (pool(move)) {
                case 1 -> cutters > 0;
                case 2 -> kamui > 0;
                default -> true;
            };
        }

        public void spend(String move) {
            switch (pool(move)) {
                case 1 -> cutters = Math.max(0, cutters - 1);
                case 2 -> kamui = Math.max(0, kamui - 1);
                default -> { }
            }
        }

        public void refill() {
            cutters = CUTTERS;
            kamui = KAMUI;
        }

        public int cutters() { return cutters; }

        public int kamui() { return kamui; }
    }

    /**
     * The Sharingan drains him all the time (canon: he cannot switch it off). A meter that rises by itself and with every strong eye technique; when it is full the eye bleeds: he is
     * exhausted for {@link #EXHAUST_TICKS} (open, no Kamui) and the meter falls to {@link #RESET_TO}. The Six Paths awakening has no strain (canon: Obito's chakra).
     */
    public static final class Strain {
        public static final int MAX = 100, EXHAUST_TICKS = 80, RESET_TO = 35, PASSIVE_EVERY = 40;

        /** What a move adds to the meter. */
        public static int cost(String move) {
            return switch (move) {
                case COPY_MOVE -> 8;
                case WARP -> 7;
                case KAMUI_RAIKIRI -> 12;
                case KAMUI_WARP -> 16;
                case KAMUI_SHURIKEN -> 10;
                case VORTEX -> 22;
                case GATES -> 28;
                case CUTTER -> 5;
                case THUNDER_GOD -> 6;
                default -> 0;
            };
        }

        private int value;

        public int value() { return value; }

        /** Adds strain; true when the meter just filled (he is exhausted now). */
        public boolean add(int n) {
            if (n <= 0) return false;
            value = Math.min(MAX, value + n);
            if (value < MAX) return false;
            value = RESET_TO;
            return true;
        }

        public void reset() { value = 0; }
    }

    /** What kind of blow the player's damage is (the observe-and-counter rule reads it). */
    public static final int E_FIRE = 0, E_LIGHTNING = 1, E_WATER = 2, E_EARTH = 3, E_PROJECTILE = 4, E_MELEE = 5, E_MAGIC = 6, E_COUNT = 7;

    /** Sorts a hit by its damage type id and tags; {@code direct} = a player's own hand. */
    public static int classify(String msgId, boolean fireTag, boolean lightningTag, boolean projectileTag, boolean direct) {
        String m = msgId == null ? "" : msgId.toLowerCase(java.util.Locale.ROOT);
        if (lightningTag || m.contains("lightning") || m.contains("thunder") || m.contains("electr")) return E_LIGHTNING;
        if (fireTag || m.contains("fire") || m.contains("flame") || m.contains("burn") || m.contains("lava")) return E_FIRE;
        if (m.contains("water") || m.contains("ice") || m.contains("frost") || m.contains("drown")) return E_WATER;
        if (m.contains("earth") || m.contains("stone") || m.contains("rock")) return E_EARTH;
        if (projectileTag || m.contains("arrow") || m.contains("trident") || m.contains("thrown")) return E_PROJECTILE;
        if (m.contains("magic") || m.contains("tensura") || m.contains("spell")) return E_MAGIC;
        return direct ? E_MELEE : E_MAGIC;
    }

    /** What the boss remembers of how the players hurt him: a decaying count per kind of blow (7 numbers, no allocation per hit). */
    public static final class Elements {
        public static final double DECAY = 0.96, MIN_HITS = 3.0, MIN_SHARE = 0.45;
        private final double[] w = new double[E_COUNT];

        public void record(int e) {
            if (e < 0 || e >= E_COUNT) return;
            for (int i = 0; i < E_COUNT; i++) w[i] *= DECAY;
            w[e] += 1.0;
        }

        /** The kind of blow used most, or -1 when there are too few hits or no clear favourite. */
        public int dominant() {
            double sum = 0, best = 0;
            int bi = -1;
            for (int i = 0; i < E_COUNT; i++) {
                sum += w[i];
                if (w[i] > best) {
                    best = w[i];
                    bi = i;
                }
            }
            return bi >= 0 && best >= MIN_HITS && best >= sum * MIN_SHARE ? bi : -1;
        }

        public void clear() { java.util.Arrays.fill(w, 0.0); }
    }

    /** The answers to a kind of blow, best first (the series' nature cycle: fire loses to water, water to lightning, lightning to earth, earth to water; a wall for arrows; a clone for fists). */
    public static List<String> counters(int element) {
        return switch (element) {
            case E_FIRE -> List.of(DRAGON, MIST);
            case E_LIGHTNING -> List.of(WALL, MIST);
            case E_WATER -> List.of(CUTTER, PURPLE_CHAIN, RAIKIRI);
            case E_EARTH -> List.of(DRAGON, FIREBALL);
            case E_PROJECTILE -> List.of(WALL, CLONES);
            case E_MELEE -> List.of(LCLONE, KUBIKIRI, CLONES);
            default -> List.of(COPY_MOVE, KAMUI_WARP, KAMUI_SHURIKEN);
        };
    }

    /** Observe first: for this many ticks after the fight starts he only uses clones, replacement and probing moves, and watches. */
    public static final int OBSERVE_TICKS = 360;

    public static boolean observeMove(String move) {
        return switch (move) {
            case KUNAI, CLONES, LCLONE, MIST, WIRE, WALL, SABRE -> true;
            default -> false;
        };
    }

    /** The Six Paths awakening: one per fight, below this share of health, for this many ticks; afterwards he is spent (open) for {@link #SIX_PATHS_SPENT}. */
    public static final float SIX_PATHS_BELOW = 0.20f;
    public static final int SIX_PATHS_TICKS = 400, SIX_PATHS_SPENT = 100;
    /** How high (blocks) the Perfect Susanoo flight carries him above the ground. */
    public static final double SIX_PATHS_HOVER = 2.6;

    public static boolean sixPathsReady(float share, boolean used) { return !used && share <= SIX_PATHS_BELOW; }

    /**
     * Purple Electricity through water: from a first foe it jumps to the nearest not yet struck foe that is wet (or standing in water) within {@code reach}, at most {@code jumps} times.
     * {@code pos} = x,z of each foe, {@code wet} = soaked. Returns the order of the foes struck (the first included).
     */
    public static List<Integer> chain(double[][] pos, boolean[] wet, int first, double reach, int jumps) {
        List<Integer> out = new ArrayList<>();
        if (first < 0 || first >= pos.length) return out;
        out.add(first);
        int at = first;
        for (int j = 0; j < jumps; j++) {
            int best = -1;
            double bd = reach * reach;
            for (int i = 0; i < pos.length; i++) {
                if (out.contains(i) || !wet[i]) continue;
                double dx = pos[i][0] - pos[at][0], dz = pos[i][1] - pos[at][1], d = dx * dx + dz * dz;
                if (d <= bd) {
                    bd = d;
                    best = i;
                }
            }
            if (best < 0) break;
            out.add(best);
            at = best;
        }
        return out;
    }

    /** Lightning Release Shadow Clone: how long it stands, how close a foe must come to break it, how long the paralysis lasts, how many stand at once. */
    public static final int LCLONE_TICKS = 100, LCLONE_PARALYSE = 36, LCLONE_MAX = 2;
    public static final double LCLONE_TOUCH = 1.7;

    /** Flying Thunder God: kunai markers (at most this many standing) and how long they stay. */
    public static final int FTG_MARKERS = 3, FTG_TICKS = 90;

    /** Lightning Cutter: how long his hand cuts any lightning that reaches him (the counter window). */
    public static final int CUTTER_COUNTER_TICKS = 40;

    /** Eight Gates: how long the opened gates last and the damage factor they give. */
    public static final int GATES_TICKS = 200;
    public static final float GATES_DAMAGE = 1.25f;

    /** The branching arcs of a Chidori: for each of {@code n} arcs, an angle (radians) and a length factor, deterministic from a seed so the same call draws the same hand. */
    public static double[][] arcs(int n, long seed) {
        java.util.Random r = new java.util.Random(seed);
        double[][] out = new double[n][2];
        for (int i = 0; i < n; i++) {
            out[i][0] = (i + r.nextDouble() * 0.8) * Math.PI * 2.0 / Math.max(1, n);
            out[i][1] = 0.6 + r.nextDouble() * 0.8;
        }
        return out;
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
