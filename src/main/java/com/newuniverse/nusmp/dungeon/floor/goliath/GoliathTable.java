package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.List;

/**
 * The Goliath's move table (pure, unit tested): every move's numbers in one place, as {@link MoveSpec}s the scheduler can read without
 * Minecraft. {@code GoliathBoss} turns each entry into a {@code Move} by adding its telegraph and its code, looked up by id.
 *
 * <p>The shape of the fight: a slow giant that fights with its weight. Close in it slams (the Ground Slam, the Twin Stomp), at range it
 * roars a shockwave across the whole pit (jump it), and at any distance it can smash a marked circle under the nearest foe (the Bedrock
 * Fist), drive a line of stone spikes along the floor, or drop rubble on every foe in the pit. The move list opens up as its stone cracks:
 * the slam, the fist and the shockwave from the start, the stomp and the line from 40% power, the rubble and the ultimate, the Bedrock
 * Quake, from 60%. The scheduler weights favour the slam close in and the shockwave far out (see {@link #SPECS}).
 */
public final class GoliathTable {
    private GoliathTable() {}

    public static final String GROUND_SLAM = "ground_slam", TWIN_STOMP = "twin_stomp", ROARING_SHOCKWAVE = "roaring_shockwave", BEDROCK_FIST = "bedrock_fist",
            SEISMIC_LINE = "seismic_line", RUBBLE_FALL = "rubble_fall", BEDROCK_QUAKE = "bedrock_quake";

    // ---------------------------------------------------------------- base damage (before the framework's damage law: power tuning, victim EP, the balance law's caps)
    public static final float SLAM = 14f, STOMP_FIRST = 11f, STOMP_SECOND = 7.5f, SHOCKWAVE = 12f, FIST = 30f, LINE = 12f, RUBBLE = 11f, QUAKE = 8f;
    /** The plain blow's share of the attack attribute: the giant is slow, and its real hits are the telegraphed ones. */
    public static final float MELEE_SHARE = 0.5f;

    // ---------------------------------------------------------------- reach (blocks) before the Gates make everything a quarter bigger
    public static final double SLAM_RADIUS = 7.0, STOMP_RADIUS = 8.0, FIST_RADIUS = 3.6, LINE_HALF_WIDTH = 1.7, RUBBLE_RADIUS = 2.4;
    /** How far the roar's ring and the quake's rings run: across the whole pit (its floor is 28 blocks in radius). */
    public static final double SHOCKWAVE_REACH = 34.0, QUAKE_REACH = 32.0;
    /** The Gates (25% health): the cracked, glowing giant swings wider. */
    public static final double GATES_REACH = 1.25;
    /** The farthest from the giant the fist may land: a foe beyond it is struck at the edge of its reach. */
    public static final double FIST_REACH = 11.0;

    // ---------------------------------------------------------------- timing (ticks)
    /** Ticks between the two rings of the Twin Stomp, between the rings of the Quake, and how long a ring wave lives per block of reach. */
    public static final int STOMP_GAP = 12, QUAKE_GAP = 14;
    /** How fast a ring runs (blocks a tick), and how thick it is (a foe within this of its edge is hit). */
    public static final double WAVE_SPEED = 1.1, QUAKE_SPEED = 1.2, WAVE_THICK = 1.3, WAVE_START = 0.5;
    /** A foe whose feet are this far above the floor has jumped a ring wave. */
    public static final double WAVE_JUMP = 0.75;
    /** The ticks between one stretch of the Seismic Line and the next, and how long the rubble falls once the wind-up (which showed its circles) is over. */
    public static final int LINE_STEP = 2, RUBBLE_LAND = 8;

    /** The most of its maximum health one move of several hits may take from one victim (a share): the twin stomp and the quake. */
    public static final double STOMP_BUDGET = 0.55, QUAKE_BUDGET = 0.70;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as {@code Move}): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    /**
     * Ranges and weights are what make the scheduler pick the slam close in and the shockwave far out: it only considers the moves whose range
     * holds the foe's distance, and prefers the one whose range is centred on it (see MoveScheduler#weightOf).
     */
    private static final List<Spec> SPECS = List.of(
            new Spec(GROUND_SLAM, "Ground Slam", 20, 30, 110, 0, 9, 6, false, false, 12),
            new Spec(BEDROCK_FIST, "Bedrock Fist", 20, 36, 170, 0, 14, 5, false, false, 16),
            new Spec(ROARING_SHOCKWAVE, "Roaring Shockwave", 20, 36, 200, 7, 40, 5, false, true, 14),
            new Spec(TWIN_STOMP, "Twin Stomp", 40, 24, 160, 0, 10, 4, false, false, 14),
            new Spec(SEISMIC_LINE, "Seismic Line", 40, 26, 140, 5, 30, 4, false, false, 10),
            new Spec(RUBBLE_FALL, "Rubble Fall", 60, 34, 230, 0, 48, 3, false, true, 12),
            new Spec(BEDROCK_QUAKE, "Bedrock Quake", 60, 60, 1200, 0, 40, 5, true, false, 60));

    /** The table, in the order of the boss's move list (the client reads the move being wound up by index). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }

    /** A reach grown for the Gates: x1.25 once they are open. */
    public static double reach(double base, boolean gatesOpen) { return gatesOpen ? base * GATES_REACH : base; }

    /** The warning the foes read on the action bar while a move winds up. */
    public static String warning(String id) {
        return switch (id) {
            case GROUND_SLAM -> "The Goliath lifts its fists: leave the ring or jump the wave!";
            case TWIN_STOMP -> "Two stomps: jump the first wave, then the second!";
            case ROARING_SHOCKWAVE -> "The Goliath roars: a shockwave crosses the pit. Jump it!";
            case BEDROCK_FIST -> "A fist comes down on the marked circle: step out of it!";
            case SEISMIC_LINE -> "The floor splits in a line: step off it!";
            case RUBBLE_FALL -> "Rubble falls on the marked circles: leave them!";
            case BEDROCK_QUAKE -> "The Bedrock Quake: three waves in a row. Jump each one!";
            default -> "";
        };
    }
}
