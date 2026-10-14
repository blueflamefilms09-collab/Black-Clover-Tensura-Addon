package com.newuniverse.nusmp.relics.wave;

/**
 * Wave 11, the pure rules (no Minecraft imports, unit tested): the daily d6 of the Tripolar Sword, the percentile of Sorrow's Edge, the
 * dagger volley of the Dagger of Many Daggers, the breath table of the Dragon's Truth and the drum of Bianca.
 */
public final class Wave11Pure {
    private Wave11Pure() {}

    public static final long DAY = 24000L;

    /** The Minecraft day a game time falls in. */
    public static long dayOf(long gameTime) { return Math.floorDiv(Math.max(0L, gameTime), DAY); }

    // ---------------------------------------------------------------- Tripolar Sword: "at the start of each day roll 1d6"
    public enum Mood { LASHING, PLAIN, FAVOURED }

    /** 1-2 the sword lashes out and is clumsy for the day, 3-4 it is a plain sword, 5-6 it is favoured (the card's table). */
    public static Mood mood(int d6) { return d6 <= 2 ? Mood.LASHING : d6 <= 4 ? Mood.PLAIN : Mood.FAVOURED; }

    /** The outgoing damage multiplier of a mood: disadvantage for a day, nothing, or advantage with doubled damage dice (kept PvP-fair). */
    public static float moodFactor(Mood m) { return m == Mood.LASHING ? 0.6f : m == Mood.FAVOURED ? 1.6f : 1.0f; }

    /** The self-lash of a 1-2: the card's 1d10, never more than leaves one half heart. */
    public static float lash(int d10, float health) { return Math.max(0f, Math.min(d10, health - 1f)); }

    // ---------------------------------------------------------------- Sorrow's Edge: the geas
    public enum Demand { NONE, BLOODLUST, FASTING }

    /** The percentile die: 50 or above keeps your sanity (nothing happens); below it the spirit makes a demand (which one follows the roll). */
    public static Demand geas(int d100) {
        if (d100 >= 50) return Demand.NONE;
        return d100 % 2 == 0 ? Demand.BLOODLUST : Demand.FASTING;
    }

    /** Ticks the bloodlust allows before it bites (five minutes). */
    public static final int BLOODLUST_TICKS = 6000;

    public static boolean bloodlustOverdue(long now, long deadline, boolean satisfied) { return !satisfied && deadline > 0 && now >= deadline; }

    // ---------------------------------------------------------------- Dagger of Many Daggers: 2d4 extra throws
    /** 2d4 from two dice 1..4. */
    public static int copies(int d4a, int d4b) { return clamp(d4a, 1, 4) + clamp(d4b, 1, 4); }

    /** Copies that strike a player at most (PvP: 30 percent of 20 health in 2-point hits). */
    public static final int PLAYER_HITS = 3;

    public static int hitsAgainst(int copies, boolean player) { return player ? Math.min(copies, PLAYER_HITS) : copies; }

    /** Every fourth copy is "destined to miss": it flies and falls short. */
    public static boolean misses(int index) { return index % 4 == 3; }

    // ---------------------------------------------------------------- The Dragon's Truth: breath of a d8
    public static final String[] BREATH = {"acid", "cold", "fire", "lightning", "poison", "necrotic", "radiant", "thunder"};

    public static String breath(int d8) { return BREATH[clamp(d8, 1, 8) - 1]; }

    // ---------------------------------------------------------------- Bianca: six cylinders of three bolts
    public static final int DRUM = 18, VOLLEY = 3;

    /** Bolts left in the drum after a volley (a volley needs a full three; less than that and she fires one plain bolt). */
    public static int afterVolley(int drum) { return drum >= VOLLEY ? drum - VOLLEY : drum; }

    public static boolean canVolley(int drum) { return drum >= VOLLEY; }

    /** Reload: how many bolts move from the pack into the drum. */
    public static int reloaded(int drum, int inPack) { return Math.max(0, Math.min(DRUM - Math.max(0, drum), Math.max(0, inPack))); }

    /** The yaw offsets in degrees of the two extra bolts of a volley. */
    public static final float[] SPREAD = {-5.5f, 5.5f};

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
