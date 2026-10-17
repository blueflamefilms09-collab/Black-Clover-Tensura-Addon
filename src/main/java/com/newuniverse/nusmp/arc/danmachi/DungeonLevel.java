package com.newuniverse.nusmp.arc.danmachi;

import java.util.Arrays;
import java.util.Locale;

/**
 * Why: DanMachi measures every fighter in Levels (an adventurer's Falna, a monster's Guild rating) and the Dungeon is said to answer strong
 * adventurers with stronger monsters: a monster that eats magic stones becomes a "strengthened species", roughly one Level above its kind. The mod has
 * no Falna, but it has one number that already says how strong a fighter is, the Tensura existence points (EP) the whole balance law reads, so a Level
 * here is a band of EP on a logarithmic scale. This class is the whole rule, pure, so it can be tested without a game: the Level of one fighter, the
 * Level of a party, and whether (and by how much) a boss "Levels up" when the Gates of the fight open. The boss classes only ask it questions.
 *
 * <p>The scale (defaults): Level 1 below 3,162 EP, then one Level per half decade of EP: Level 2 from 3,162, 3 from 10,000, 4 from 31,623, 5 from 100,000,
 * 6 from 316,228, 7 from 1,000,000, 8 from 3,162,278 and 9 from 10,000,000 (the top). The base and the width of a Level are config values; the numbers above
 * are the defaults. A boss levels up at the Gates when the party's Level reaches the boss's own Level plus a margin (default 0); the further the party is
 * above that, the more steps (1 to 3): a step is a damage factor and a speed factor, and the first step also unlocks the boss's leveled form and moves.
 */
public final class DungeonLevel {
    private DungeonLevel() {}

    public static final int MIN_LEVEL = 1, MAX_LEVEL = 9;
    /** Nobody is there to measure (no fighter near the boss): the party Level is 0 and no boss ever levels up for it. */
    public static final int NO_PARTY = 0;
    public static final double DEFAULT_BASE_EP = 1000.0, DEFAULT_DECADES = 0.5, DEFAULT_DAMAGE_PER_STEP = 0.10, DEFAULT_SPEED_PER_STEP = 0.03;
    public static final int DEFAULT_MARGIN = 0, DEFAULT_MAX_STEPS = 3;

    /** How a party's members are folded into one Level. */
    public enum Mode {
        MEAN, MEDIAN, STRONGEST, WEAKEST;

        /** The mode with this name (any case), or {@link #MEAN} for null or an unknown name: a typo in a config file must never stop a boss. */
        public static Mode parse(String name) {
            if (name != null) for (Mode m : values()) if (m.name().equalsIgnoreCase(name.trim())) return m;
            return MEAN;
        }

        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    private static int clamp(int level) { return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level)); }

    /** The Level of one fighter from their EP, with the default scale. */
    public static int levelOfEp(double ep) { return levelOfEp(ep, DEFAULT_BASE_EP, DEFAULT_DECADES); }

    /**
     * The Level of one fighter from their EP: Level 1 below {@code baseEp} times 10 to the power of {@code decadesPerLevel}, then one more Level for
     * every {@code decadesPerLevel} decades of EP, at most {@link #MAX_LEVEL}. A base below 1 or a width below 0.05 is treated as 1 and 0.05 (a bad
     * config value must not divide by zero or make every fighter Level 9); NaN and negative EP are Level 1.
     */
    public static int levelOfEp(double ep, double baseEp, double decadesPerLevel) {
        double base = baseEp >= 1.0 ? baseEp : 1.0;
        double width = decadesPerLevel >= 0.05 ? decadesPerLevel : 0.05;
        if (!(ep >= 1.0)) return MIN_LEVEL;
        double decades = Math.log10(ep) - Math.log10(base);
        if (decades < width - 1e-9) return MIN_LEVEL;
        long level = 1 + (long) Math.floor(decades / width + 1e-9);
        return clamp((int) Math.min(level, MAX_LEVEL));
    }

    /** The Level of a party from the Levels of its members; {@link #NO_PARTY} when the array is null or empty. */
    public static int partyLevel(int[] levels, Mode mode) {
        if (levels == null || levels.length == 0) return NO_PARTY;
        int[] a = new int[levels.length];
        for (int i = 0; i < a.length; i++) a[i] = clamp(levels[i]);
        Arrays.sort(a);
        switch (mode == null ? Mode.MEAN : mode) {
            case MEDIAN:
                return a[(a.length - 1) / 2];
            case STRONGEST:
                return a[a.length - 1];
            case WEAKEST:
                return a[0];
            case MEAN:
            default: {
                long sum = 0;
                for (int v : a) sum += v;
                return (int) (sum / a.length);
            }
        }
    }

    /**
     * How many steps a boss of {@code bossLevel} levels up by when the party is at {@code partyLevel}: 0 when the party has not reached
     * {@code bossLevel + margin} (the boss does not need to; this is what keeps a weak party's fight fair), otherwise 1, plus one more for every two
     * Levels the party is above that line, at most {@code maxSteps}.
     */
    public static int steps(int partyLevel, int bossLevel, int margin, int maxSteps) {
        if (maxSteps <= 0 || partyLevel < MIN_LEVEL) return 0;
        int lead = partyLevel - (bossLevel + margin);
        if (lead < 0) return 0;
        return Math.min(maxSteps, 1 + lead / 2);
    }

    /** The boss's Level after {@code steps} steps (at most {@link #MAX_LEVEL}). */
    public static int bossLevelAfter(int baseLevel, int steps) { return clamp(baseLevel + Math.max(0, steps)); }

    /** The damage factor of a leveled boss: 1 plus {@code perStep} for every step. */
    public static double damageFactor(int steps, double perStep) { return 1.0 + Math.max(0, steps) * Math.max(0.0, perStep); }

    /** The speed factor of a leveled boss: 1 plus {@code perStep} for every step. */
    public static double speedFactor(int steps, double perStep) { return 1.0 + Math.max(0, steps) * Math.max(0.0, perStep); }

    /** "Lv.3". */
    public static String label(int level) { return "Lv." + clamp(level); }

    /** A Howl frightens a fighter whose Level is below the monster's. */
    public static boolean frightens(int monsterLevel, int fighterLevel) { return fighterLevel < monsterLevel; }
}
