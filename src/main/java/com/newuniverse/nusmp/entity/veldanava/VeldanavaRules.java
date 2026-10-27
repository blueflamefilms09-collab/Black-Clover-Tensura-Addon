package com.newuniverse.nusmp.entity.veldanava;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Why: Veldanava, the Creator God, is one boss that wears three bodies, and every number of his fight (the sizes of the bodies, the move table, the
 * geometry of a sweeping beam, the pull of the gravity move, the loot, the soul-seizing test) must be small, bounded and checkable without starting the
 * game. This class has no game class in it: {@link VeldanavaBoss} and {@link VeldanavaMoves} ask it, and the unit test reads it.
 *
 * <p>The three bodies ({@link #DRAGON}, {@link #HUMANOID}, {@link #AVATAR}) are the three GeoLite models written by tools/gen_veldanava.py
 * (assets/nusmp/geo/entity/veldanava_*). The Star King Dragon is anchored to the arena and fights with huge area moves; when it would die it turns into the
 * Creator God (a slim, fast swordsman with a second full health bar); at the last 10% of that bar the Limiter is released and the Dark Avatar of God
 * (the same body, wearing his true form) finishes the fight.
 *
 * <p>Worst-case cost (rule 11): one boss entity; at most {@link #MAX_TARGETS} targets hurt per tick; at most {@link #MAX_MARKS} falling stars per cast;
 * at most {@link #BLOCKS_PER_TICK} blocks changed per tick (and only when the griefBlocks rule is on, all of them put back); at most {@link #FX_PER_TICK}
 * effect packets per tick, all sent to nearby players through the perf budget.
 */
public final class VeldanavaRules {
    private VeldanavaRules() {}

    // ---------------------------------------------------------------- the three bodies
    public static final int DRAGON = 0, HUMANOID = 1, AVATAR = 2;
    public static final int FORMS = 3;
    /** The GeoLite model of each body: {@code GeoSpec.of("veldanava", geo(form))}. */
    private static final String[] GEO = {"true_dragon", "humanoid", "dark_avatar"};
    /** The hitbox of each body (width, height in blocks); the dragon is about 24 blocks long on screen, so its box is a generous, anchored one. */
    private static final float[] WIDTH = {7.5f, 0.6f, 0.75f};
    private static final float[] HEIGHT = {9.0f, 1.95f, 2.5f};
    /** How much bigger than the model file each body is drawn (the dragon is 1.6 times the size of its model). */
    private static final float[] MODEL_SCALE = {1.6f, 1.0f, 1.0f};

    public static int clampForm(int form) { return form < 0 ? 0 : Math.min(FORMS - 1, form); }

    public static String geo(int form) { return GEO[clampForm(form)]; }

    public static float width(int form) { return WIDTH[clampForm(form)]; }

    public static float height(int form) { return HEIGHT[clampForm(form)]; }

    public static float modelScale(int form) { return MODEL_SCALE[clampForm(form)]; }

    public static String formName(int form) {
        return switch (clampForm(form)) {
            case DRAGON -> "Star King Dragon";
            case HUMANOID -> "Creator God";
            default -> "Dark Avatar of God";
        };
    }

    // ---------------------------------------------------------------- the fight's state
    public static final int STATE_IDLE = 0, STATE_COMBAT = 1, STATE_CASTING = 2, STATE_TRANSFORMING = 3, STATE_DEAD = 4;
    /** The change of body takes 4 seconds and the boss cannot be hurt meanwhile; the hitbox changes in the middle of it. */
    public static final int TRANSFORM_TICKS = 80;
    /** The Limiter is released (the Dark Avatar wakes) when the Creator God is at or under this share of his health. */
    public static final float LIMITER_FRACTION = 0.10f;
    /** At most this many hurt targets per tick, whatever the move. */
    public static final int MAX_TARGETS = 24;
    /** At most this many falling stars per Starfall. */
    public static final int MAX_MARKS = 12;
    /** At most this many blocks changed per tick. */
    public static final int BLOCKS_PER_TICK = 24;
    /** At most this many effect packets sent per tick by the boss. */
    public static final int FX_PER_TICK = 12;
    /** The hardest block a beam may remove (obsidian is 50, bedrock is unbreakable): the endgame blocks stay. */
    public static final float MAX_DESTROY_SPEED = 20f;
    /** A player who fights is remembered for the loot; at most this many get a share. */
    public static final int MAX_FIGHTERS = 8;
    /** The boss and his players: nobody further than this from the arena is a fighter; the boss calls itself back past it. */
    public static final int ARENA_RADIUS = 64;

    public static boolean limiterDue(int form, float health, float max) {
        return form == HUMANOID && max > 0f && health > 0f && health <= max * LIMITER_FRACTION;
    }

    /** True when a blow of 'amount' would finish the body of the dragon (it never dies in this body: it changes). */
    public static boolean dragonLethal(int form, float health, float amount) { return form == DRAGON && amount >= health; }

    /** Damage multiplier of a body (the Dark Avatar hits harder). */
    public static float damageScale(int form) { return form == AVATAR ? 1.3f : 1f; }

    /** Cooldown multiplier of a body (the faster the later). */
    public static float cooldownScale(int form) { return form == AVATAR ? 0.6f : form == HUMANOID ? 0.85f : 1f; }

    /** The pause between two moves of a body, in ticks. */
    public static int gap(int form) { return form == DRAGON ? 50 : form == HUMANOID ? 26 : 16; }

    // ---------------------------------------------------------------- the moves
    public static final int SWEEP = 0, GRAVITY = 1, STARFALL = 2, ROAR = 3, BITE = 4,
            GOD_SPEED = 5, GENESIS_SLASH = 6, SERAPH_GUARD = 7, URIEL = 8, LAW = 9, GENESIS = 10,
            // 3.30.0: the Tensura-themed moves of the anime Veldanava (appended; the classic mode does not use them)
            ABSOLUTE_SEVERANCE = 11, BLACK_LIGHTNING = 12, BLACK_BLAST = 13, DEATH_TORNADO = 14, DISINTEGRATION = 15, HELL_FLARE = 16, MEGIDDO = 17;
    public static final int MOVES = 18;
    /** The moves of 3.11.0 - 3.29.0: ids under this are the classic table. */
    public static final int CLASSIC_MOVES = 11;

    /**
     * One move: the bodies that have it (bit mask of 1 &lt;&lt; form), the ticks of its telegraph (windup), of its effect (active) and of the rest after it
     * (recovery, during which an ultimate leaves the boss exposed), its cooldown, the distance window it is used in, its weight in the choice, and how much of
     * the target's health one hit of it takes (a share, see {@link #damage}).
     */
    public record Spec(int id, String name, String label, int forms, int windup, int active, int recovery, int cooldown, double minRange, double maxRange,
                       int weight, boolean ultimate, float share) {
        public boolean has(int form) { return (forms & (1 << clampForm(form))) != 0; }

        /** The whole length of the move in ticks. */
        public int length() { return windup + active + recovery; }
    }

    private static final int D = 1 << DRAGON, H = 1 << HUMANOID, A = 1 << AVATAR;
    private static final Spec[] TABLE = {
            new Spec(SWEEP, "stardust_sweep", "Stardust Sweep", D, 30, 50, 20, 260, 0, 64, 5, false, 0.30f),
            new Spec(GRAVITY, "gravity_pull", "Gravity Pull", D, 20, 60, 20, 360, 6, 40, 4, false, 0.12f),
            new Spec(STARFALL, "starfall", "Starfall", D, 25, 40, 20, 300, 0, 64, 5, false, 0.28f),
            new Spec(ROAR, "roar", "Dragon Roar", D, 20, 10, 20, 400, 0, 32, 2, false, 0f),
            new Spec(BITE, "bite", "Bite", D, 12, 4, 14, 60, 0, 12, 8, false, 0.25f),
            new Spec(GOD_SPEED, "god_speed", "God Speed", H | A, 8, 6, 8, 120, 3, 40, 7, false, 0.22f),
            new Spec(GENESIS_SLASH, "genesis_slash", "Genesis Slash", H | A, 24, 6, 20, 240, 0, 40, 6, false, 0.35f),
            new Spec(SERAPH_GUARD, "seraphim_guard", "Justice King Michael", H | A, 6, 80, 10, 700, 0, 64, 2, false, 0f),
            new Spec(URIEL, "uriel_prison", "Covenant King Uriel", H | A, 20, 50, 10, 420, 0, 30, 3, false, 0.10f),
            new Spec(LAW, "law_manipulation", "Law Manipulation", H | A, 20, 6, 14, 500, 0, 30, 3, false, 0f),
            new Spec(GENESIS, "genesis", "Genesis: Creation of the World", A, 100, 8, 60, 1200, 0, 64, 4, true, 0.60f),
            new Spec(ABSOLUTE_SEVERANCE, "absolute_severance", "Absolute Severance", H | A, 22, 6, 16, 260, 0, 48, 6, false, 0.38f),
            new Spec(BLACK_LIGHTNING, "black_lightning", "Black Lightning", D | H | A, 16, 24, 12, 220, 0, 56, 6, false, 0.20f),
            new Spec(BLACK_BLAST, "black_lightning_blast", "Black Lightning Blast", H | A, 26, 8, 18, 360, 0, 22, 4, false, 0.30f),
            new Spec(DEATH_TORNADO, "death_tornado", "Death Tornado", D | A, 20, 70, 20, 420, 4, 48, 4, false, 0.12f),
            new Spec(DISINTEGRATION, "disintegration", "Disintegration", H | A, 30, 40, 20, 480, 0, 40, 3, false, 0.06f),
            new Spec(HELL_FLARE, "hell_flare", "Hell Flare", D | H | A, 24, 30, 16, 320, 0, 40, 5, false, 0.18f),
            new Spec(MEGIDDO, "megiddo", "Megiddo", D | A, 90, 14, 50, 1500, 0, 64, 2, true, 0.50f),
    };

    public static Spec spec(int id) { return TABLE[id < 0 ? 0 : Math.min(MOVES - 1, id)]; }

    /** The moves a body has. */
    public static List<Spec> movesOf(int form) {
        List<Spec> out = new ArrayList<>();
        for (Spec s : TABLE) if (s.has(form)) out.add(s);
        return out;
    }

    /** The move to start now, or -1: ready (its cooldown is over), in range, chosen by weight. 'readyAt' holds, per move id, the tick it is ready again. */
    public static int choose(int form, double distance, long[] readyAt, long now, IntUnaryOperator roll) {
        int total = 0;
        for (Spec s : TABLE) if (usable(s, form, distance, readyAt, now)) total += s.weight();
        if (total <= 0) return -1;
        int r = roll.applyAsInt(total);
        for (Spec s : TABLE) {
            if (!usable(s, form, distance, readyAt, now)) continue;
            r -= s.weight();
            if (r < 0) return s.id();
        }
        return -1;
    }

    private static boolean usable(Spec s, int form, double distance, long[] readyAt, long now) {
        return s.has(form) && distance >= s.minRange() && distance <= s.maxRange() && (readyAt == null || s.id() >= readyAt.length || now >= readyAt[s.id()]);
    }

    /** Does the renderer play the big cast animation for this move? (The classic three, and the wide or heavy new ones.) */
    public static boolean bigCast(int id) {
        return id == GENESIS || id == GRAVITY || id == GENESIS_SLASH || id == ABSOLUTE_SEVERANCE || id == BLACK_BLAST || id == DEATH_TORNADO || id == HELL_FLARE || id == MEGIDDO;
    }

    /** The cooldown of a move in the given body, in ticks (never under a second). */
    public static int cooldown(Spec s, int form) { return Math.max(20, Math.round(s.cooldown() * cooldownScale(form))); }

    // ---------------------------------------------------------------- damage
    /**
     * The damage of one hit on a target of 'targetMax' health: 'share' of it, at least 'floor', at most 'cap', times the body's and the server's scale. The caps keep it
     * bounded for a target with a huge health bar, the floor keeps it a threat for one with a small one.
     */
    public static float damage(double targetMax, float share, float floor, float cap, float scale) {
        double v = targetMax * share;
        v = Math.max(floor, Math.min(cap, v));
        return (float) (v * Math.max(0f, scale));
    }

    /** Would this blow finish a body with 'health' and 'absorption'? (The soul-seizing test: the blow is never dealt; the player is taken instead.) */
    public static boolean isFatal(double health, double absorption, double amount) { return amount > 0 && amount >= health + Math.max(0, absorption); }

    /** 1 up to 'full' blocks, then falling linearly to 'minimum' at 'max' blocks (the Genesis wave is weaker far from the middle). */
    public static float falloff(double distance, double full, double max, float minimum) {
        if (distance <= full) return 1f;
        if (distance >= max) return minimum;
        double t = (distance - full) / (max - full);
        return (float) (1.0 - t * (1.0 - minimum));
    }

    // ---------------------------------------------------------------- geometry
    /** The yaw (degrees, Minecraft's: 0 looks along +z) of a beam that turns one full circle in 'duration' ticks. */
    public static double sweepYaw(double startYaw, int tick, int duration) { return startYaw + 360.0 * tick / Math.max(1, duration); }

    /** Is the point (dx, dz) from the beam's origin inside a beam of 'length' and 'halfWidth' that points along 'yawDeg'? */
    public static boolean inBeam(double dx, double dz, double yawDeg, double length, double halfWidth) {
        double r = Math.toRadians(yawDeg);
        double fx = -Math.sin(r), fz = Math.cos(r);
        double along = dx * fx + dz * fz;
        double across = Math.abs(dx * fz - dz * fx);
        return along >= 0 && along <= length && across <= halfWidth;
    }

    /** How far (blocks per tick) the gravity move drags a target at 'distance' from the middle: nothing outside 'radius' or inside 3 blocks, stronger the closer. */
    public static double pullStep(double distance, double radius) {
        if (distance > radius || distance < 3.0) return 0.0;
        double k = 1.0 - distance / radius;
        return Math.min(0.42, 0.07 + 0.30 * k);
    }

    /** How many falling stars for 'players' players near the boss: three, plus two each, at most {@link #MAX_MARKS}. */
    public static int marks(int players) { return Math.max(0, Math.min(MAX_MARKS, players <= 0 ? 0 : 3 + 2 * players)); }

    // ---------------------------------------------------------------- the 3.30.0 moves (pure geometry)
    /** The most black bolts of one Black Lightning, and the most beams of one Megiddo. */
    public static final int MAX_BOLTS = 3, MAX_BEAMS = 8;

    /** One bolt for each of up to {@link #MAX_BOLTS} players, at least one when anyone is there. */
    public static int bolts(int players) { return players <= 0 ? 0 : Math.min(MAX_BOLTS, players); }

    /** Two beams, plus one for each player, at most {@link #MAX_BEAMS}. */
    public static int beams(int players) { return players <= 0 ? 0 : Math.min(MAX_BEAMS, 2 + players); }

    /** The tick of the move's active part at which bolt/beam number 'i' of 'n' falls: evenly spread over 'active' ticks. */
    public static int strikeTick(int i, int n, int active) { return n <= 0 ? 0 : (int) ((long) i * Math.max(1, active) / n); }

    /** How far (blocks) the Death Tornado moves in a tick toward its target: it never overshoots and never moves under 'step'. Returns the new distance still to go. */
    public static double tornadoRemaining(double distance, double step) { return Math.max(0.0, distance - step); }

    /** The Death Tornado's speed in blocks per tick (slower than a sprinting player: it can be outrun). */
    public static final double TORNADO_SPEED = 0.22;

    /** Hell Flare: the radius of ring 'ring' (0..2) 'since' ticks into the move; each ring starts 4 ticks after the last and takes 12 ticks to reach 'outer'. */
    public static double hellRingRadius(int ring, int since, double outer) {
        double t = (since - 4.0 * ring) / 12.0;
        return outer * Math.max(0.0, Math.min(1.0, t));
    }

    /** Is a creature at 'distance' from the middle inside the band of a ring of radius 'radius'? */
    public static boolean inRingBand(double distance, double radius, double halfBand) { return radius > 0.0 && Math.abs(distance - radius) <= halfBand; }

    /** Is the vertical gap between a creature and a flat beam small enough for the beam to cut it? */
    public static boolean withinHeight(double dy, double reach) { return Math.abs(dy) <= reach; }

    // ---------------------------------------------------------------- the will of the creator and the loot
    /** How long the Will of the Creator waits for the fighters to take their boon: five minutes, or what the server set (30 s .. 30 min). */
    public static int orbTicks(int configuredSeconds) { return Math.max(30, Math.min(1800, configuredSeconds)) * 20; }

    /** Per mille chances of the rare drops (one roll each, per fighter). */
    public static final int CHANCE_FRAGMENT = 150, CHANCE_DIVINE_CORE = 10;

    /** Is a roll of 0..999 a hit for 'perMille'? */
    public static boolean hit(int roll, int perMille) { return roll >= 0 && roll < perMille; }

    /** The ordered part of the loot of one fighter: how many of the Star King Dragon Core, the Genesis Fragment and the Creator's Essence. */
    public static int[] guaranteed() { return new int[]{1, 3, 1}; }

    /** The hardest the server may be asked for: a fighter list of 'n' is cut to {@link #MAX_FIGHTERS}. */
    public static int fighters(int n) { return Math.max(0, Math.min(MAX_FIGHTERS, n)); }
}
