package com.newuniverse.nusmp.arc.early;

import java.util.List;

/**
 * Why: Heath Grice's move table and numbers in one pure place (read by the boss class, checked by {@code EarlyTablesTest}). After the anime (his attack on
 * Saussy Village): an Ice Magic user who combines his ice with the Mist Magic of his subordinates, keeps a pocket watch and an appointment book, raises a
 * shield of snowflake crystals, and as a last act freezes himself and everything round him with a device in his body. The fight is built on those four
 * things: the Endless Ice Cage comes on a fixed rhythm (the Timetable) and leaves one corridor open (the HailPattern); the Crystal Ice Shield is a wall
 * against range and not against a blade (FrostShield); the Ice Funeral is the ritual that can be broken; and a plan that fails flusters him.
 */
public final class HeathTable {
    private HeathTable() {}

    public static final String FANG = "ice_fang", VOLLEY = "icicle_volley", RIME = "rime_burst", SHIELD = "crystal_ice_shield", SLIDE = "frost_slide",
            MIST_STEP = "mist_step", COFFIN = "ice_coffin", CAGE = "endless_ice_cage", FUNERAL = "ice_funeral";

    /** Base damage of the moves. */
    public static final float FANG_HIT = 10f, ICICLE = 5f, RIME_HIT = 9f, SLIDE_HIT = 6f, STEP_HIT = 7f, COFFIN_HIT = 12f, HAIL_HIT = 7f, FUNERAL_HIT = 18f;

    /** The shield: how long it stands, and its pool as a share of his maximum health. */
    public static final int SHIELD_TICKS = 140;
    public static final double SHIELD_POOL = 0.18;

    /** The cage: radius, waves, icicles per wave at the opening (one more per two stages), landing radius, the open corridor, and the ticks between waves and from warning to landing. */
    public static final double CAGE_RADIUS = 8.5, CAGE_IMPACT = 1.5, CAGE_CORRIDOR = 1.4;
    public static final int CAGE_WAVES = 3, CAGE_BASE_STRIKES = 6, CAGE_WAVE_GAP = 14, CAGE_WARN = 16;

    /** The Timetable's base gap between two cages and how long a failed plan leaves him flustered; a flustered Heath takes this much more damage and starts no move. */
    public static final int CHIME_TICKS = 300, FLUSTER_TICKS = 70;
    public static final float FLUSTER_BONUS = 1.3f;

    /** The Ice Funeral: radius (bigger at the Gates), the share of his health that breaks it, and ticks of freeze. */
    public static final double FUNERAL_RADIUS = 10.5, FUNERAL_BREAK = 0.12;
    public static final int FUNERAL_FREEZE = 40;

    /** Reach of the close moves and the length of the fang's lane and the slide. */
    public static final double RIME_RADIUS = 4.8, COFFIN_RADIUS = 2.6, FANG_LENGTH = 16, SLIDE_LENGTH = 9, STEP_BEHIND = 4.5;

    private static final List<MoveRow> ROWS = List.of(
            MoveRow.of(FANG, "Heavenly Ice Fang", 20, 18, 90, 3, 18, 6),
            MoveRow.of(VOLLEY, "Icicle Volley", 20, 18, 110, 6, 30, 5),
            MoveRow.of(RIME, "Rime Burst", 20, 16, 130, 0, 6, 6),
            MoveRow.of(SHIELD, "Crystal Ice Shield", 20, 16, 420, 0, 48, 2),
            MoveRow.of(SLIDE, "Frost Slide", 20, 16, 200, 0, 8, 4),
            MoveRow.of(MIST_STEP, "Mist Step", 20, 18, 220, 8, 30, 3),
            MoveRow.of(COFFIN, "Ice Coffin", 40, 24, 240, 3, 24, 4),
            MoveRow.crowd(CAGE, "Endless Ice Cage", 40, 44, 330, 4, 28, 4),
            MoveRow.ultimate(FUNERAL, "Ice Funeral", 80, 56, 1400, 0, 30, 5));

    public static List<MoveRow> rows() { return ROWS; }

    public static MoveRow byId(String id) { return MoveRow.find(ROWS, id); }

    /** Icicles per wave at a power stage 0 to 4: six at the opening, more as he warms up. */
    public static int strikesPerWave(int stage) { return CAGE_BASE_STRIKES + Math.max(0, stage) / 2 * 2; }

    /** The pool of the shield for a boss of this maximum health. */
    public static double shieldPool(double maxHealth) { return maxHealth * SHIELD_POOL; }

    /** Icicles of a volley at a power's cast slots (1 to 4): three to six. */
    public static int volley(int castSlots) { return 2 + Math.max(1, Math.min(4, castSlots)); }
}
