package com.newuniverse.nusmp.labyrinth.mega.decennial;

import java.util.List;

/**
 * Why: the 200 Decennial bosses (docs/labyrinth/decennial_bosses.md) hang on a handful of facts that every part of the feature must agree on: which dungeon level a floor belongs to,
 * which side of the dungeon a level is on, which grade of loot a level pays, how the Evil half is cut into the seven gates of the Underworld Root, and the hard caps that keep a data file
 * from making a boss unfair. They are arithmetic with no Minecraft type, so the roster loader, the phase machine, the arena hook, the facade and the tests all ask the same function and a
 * JUnit test can pin every number. The floors run -1000..+1000 (Floor 0 is the Hub); a LEVEL is ten floors and its boss stands on the tenth, so level L (1..100 Holy, -1..-100 Evil) has its boss on
 * floor 10 * L, and "level 0" does not exist.
 */
public final class DecennialRules {
    private DecennialRules() {}

    public static final int FLOORS_PER_LEVEL = 10, LEVELS_PER_SIDE = 100, MAX_FLOOR = 1000;

    // ================================================================ levels and floors
    /** Is this a dungeon level (-100..-1 or 1..100)? */
    public static boolean validLevel(int level) { return level != 0 && Math.abs(level) <= LEVELS_PER_SIDE; }

    /** The signed floor a level's boss stands on: level 3 is floor 30, level -3 is floor -30. */
    public static int floorOf(int level) { return level * FLOORS_PER_LEVEL; }

    /** The level whose boss stands on this signed floor, or 0 when the floor is not a boss floor of the roster (not a multiple of ten, Floor 0, or past +-1000). */
    public static int levelOfFloor(int floor) {
        if (floor == 0 || floor % FLOORS_PER_LEVEL != 0 || Math.abs(floor) > MAX_FLOOR) return 0;
        return floor / FLOORS_PER_LEVEL;
    }

    /** The level a floor belongs to (floors 1..10 are level 1, floors -11..-20 level -2), 0 for Floor 0 and past +-1000. */
    public static int levelContaining(int floor) {
        if (floor == 0 || Math.abs(floor) > MAX_FLOOR) return 0;
        int n = (Math.abs(floor) + FLOORS_PER_LEVEL - 1) / FLOORS_PER_LEVEL;
        return floor < 0 ? -n : n;
    }

    public static String side(int level) { return level < 0 ? "evil" : "holy"; }

    /** The roster id of a level: {@code holy_007}, {@code evil_100}. */
    public static String idOf(int level) {
        if (!validLevel(level)) throw new IllegalArgumentException("not a dungeon level: " + level);
        return side(level) + "_" + String.format("%03d", Math.abs(level));
    }

    /** The level an id names, or 0 when it is not one. */
    public static int levelOfId(String id) {
        if (id == null || id.length() != 8) return 0;
        int sign = id.startsWith("holy_") ? 1 : id.startsWith("evil_") ? -1 : 0;
        if (sign == 0) return 0;
        try {
            int n = Integer.parseInt(id.substring(5));
            return n >= 1 && n <= LEVELS_PER_SIDE ? sign * n : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ================================================================ grades (loot) and tiers
    /** The grade names in rising order; the same seven as {@code relics.Grade} (lower case), kept as text so this class stays free of the game. */
    public static final List<String> GRADES = List.of("normal", "special", "rare", "unique", "legend", "god", "genesis");

    /**
     * The grade of the loot a level's boss pays: it only ever rises with the depth. Levels 1-12 Special, 13-30 Rare, 31-52 Unique, 53-75 Legend, 76-92 God, 93-100 Genesis (the same on both
     * sides: level 40 pays the same grade on the Holy and the Evil road, so neither side is the cheaper one).
     */
    public static String gradeOf(int level) {
        int n = Math.abs(level);
        if (n <= 12) return "special";
        if (n <= 30) return "rare";
        if (n <= 52) return "unique";
        if (n <= 75) return "legend";
        if (n <= 92) return "god";
        return "genesis";
    }

    /** The index of a grade in {@link #GRADES} (0 normal .. 6 genesis), -1 for an unknown name. */
    public static int gradeIndex(String grade) { return GRADES.indexOf(grade); }

    /** The damage range of a grade's weapons (docs/grade_damage.md), {@code {min, max}}: the table the loot hook quotes. */
    public static int[] damageRange(String grade) {
        return switch (grade) {
            case "normal" -> new int[] {20, 30};
            case "special" -> new int[] {35, 55};
            case "rare" -> new int[] {60, 85};
            case "unique" -> new int[] {90, 115};
            case "legend" -> new int[] {130, 175};
            case "god" -> new int[] {190, 240};
            case "genesis" -> new int[] {260, 350};
            default -> throw new IllegalArgumentException("unknown grade " + grade);
        };
    }

    /** The D&D tier (1..5) of a level: twenty levels (two hundred floors) to a tier, as {@code DndRules.tierOf} does for floors. */
    public static int tierOf(int level) { return Math.min(5, (Math.max(1, Math.abs(level)) - 1) / 20 + 1); }

    // ================================================================ caps (the balance law of the Decennial bosses)
    /** A phase may raise the boss's damage by at most this factor and its speed by at most this factor (the damage budget of docs/boss_dps_budget.md and the player's hit cap do the rest). */
    public static final double MAX_DAMAGE_MULT = 1.6, MAX_SPEED_MULT = 1.35, MIN_SPEED_MULT = 0.6;
    /** A lair action's hit is at most this share of the boss's attack damage (before the player-hit cap of BalanceLaw), and it never comes faster than this many seconds apart. */
    public static final double MAX_LAIR_SHARE = 0.6;
    public static final int MIN_LAIR_SECONDS = 8, MAX_LAIR_SECONDS = 60;
    /** The most adds alive at once, the most a fight may ever spawn, and the most a wave may bring. */
    public static final int MAX_ADDS_ALIVE = 14, MAX_ADDS_PER_FIGHT = 48, MAX_ADDS_PER_WAVE = 6;
    /** Phases per boss, lair actions per boss. */
    public static final int MIN_PHASES = 2, MAX_PHASES = 4, MIN_LAIRS = 1, MAX_LAIRS = 2;
    /** The least gap between two phase thresholds (as a share of health), so a boss never skips through two phases in one blow. */
    public static final double MIN_PHASE_GAP = 0.12;

    public static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // ================================================================ the Underworld Root (Tree of Qliphoth)
    /**
     * The seven gate bands of the Evil half: the last level of each band, in order. A band holds the levels after the previous gate up to and including its own gate level, so band 1 is levels
     * 1..14, band 2 15..28, band 3 29..42, band 4 43..57, band 5 58..71, band 6 72..85 and band 7 86..100. The gate boss of a band stands on its gate level, except band 7, whose seventh gate is level 97
     * and whose last three levels (98, 99, 100) are the three Supreme Devils on the root throne.
     */
    public static final int[] GATE_LEVELS = {14, 28, 42, 57, 71, 85, 97};

    /** The first level of each band (1-based band = index + 1). */
    public static final int[] BAND_START = {1, 15, 29, 43, 58, 72, 86};

    /** The band (1..7) of an Evil level given as a positive depth (1..100), 0 for anything else. */
    public static int bandOf(int depth) {
        if (depth < 1 || depth > LEVELS_PER_SIDE) return 0;
        int band = 1;
        for (int i = 1; i < BAND_START.length; i++) if (depth >= BAND_START[i]) band = i + 1;
        return band;
    }

    /** The band whose GATE is on this depth (1..7), or 0 when the level is not a gate level. */
    public static int gateBandOf(int depth) {
        for (int i = 0; i < GATE_LEVELS.length; i++) if (GATE_LEVELS[i] == depth) return i + 1;
        return 0;
    }

    /** The three Supreme Devils' levels (depth 98, 99, 100), in order. */
    public static boolean isSupremeDepth(int depth) { return depth >= 98 && depth <= 100; }

    /** The arena theme the Underworld Root forces on a depth, or null when the level keeps the roster's own theme ({@code qliphoth_gate_1..7}, {@code qliphoth_root_throne}). */
    public static String qliphothTheme(int depth) {
        int gate = gateBandOf(depth);
        if (gate > 0) return "qliphoth_gate_" + gate;
        if (isSupremeDepth(depth)) return "qliphoth_root_throne";
        return null;
    }

    /** All the theme ids of the Underworld Root. */
    public static final List<String> QLIPHOTH_THEMES = List.of("qliphoth_gate_1", "qliphoth_gate_2", "qliphoth_gate_3", "qliphoth_gate_4", "qliphoth_gate_5", "qliphoth_gate_6", "qliphoth_gate_7",
            "qliphoth_root_throne");
}
