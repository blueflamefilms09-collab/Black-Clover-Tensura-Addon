package com.newuniverse.nusmp.relics.wave;

/**
 * Wave 15 (footwear, cloaks, bracers): the numbers and small state machines behind the seventeen cards, kept free of Minecraft so they can be
 * tested. The glue ({@link Wave15}, {@link Wave15Util}, {@link Wave15Wear}, {@link Wave15Hands}, {@link Wave15Field}) only reads and writes the
 * values these produce.
 * <ul>
 *   <li>{@link Charges}: the Elvenkind cloak's five charges (a D&amp;D "regains 1d4+1 at dawn" pool).</li>
 *   <li>{@link #slipIndex}: where Boots of Burglery put you when you step through a wall.</li>
 *   <li>{@link #flurryEvades} / {@link #blurEvades}: the two "they cannot catch you" windows (First Snow, Autumn Cloak).</li>
 *   <li>{@link #throwPower}: how far a creature of a given size flies from the Bracers of Manhandling.</li>
 *   <li>{@link #reflected}, {@link #stabilised}, {@link #coin}: the Cape of Projection, the Faithful Friendship bond, the Phantom's Bracelet.</li>
 * </ul>
 */
final class Wave15Rules {
    private Wave15Rules() {}

    static final int DAY = 24000;

    // The named switches the cards carry (Passive.flag) and the handlers ask for. Every card of the wave also carries F_ANY, so a handler can leave
    // early for a player who wears none of them.
    static final String F_ANY = "w15_any", F_BOOTS = "w15_boots", F_TOWEL = "w15_towel", F_BEES = "w15_bees", F_SNOW = "w15_snow", F_AUTUMN = "w15_autumn",
            F_ELVEN = "w15_elven", F_LEVITATE = "w15_levitate", F_PROJECTION = "w15_projection", F_MANTLE = "w15_mantle", F_PHASING = "w15_phasing",
            F_INITIATIVE = "w15_initiative", F_PHANTOM = "w15_phantom", F_MANHANDLING = "w15_manhandling", F_BLADES = "w15_blades", F_BEAR = "w15_bear",
            F_FRIENDS = "w15_friends", F_CREST = "w15_crest";

    // Relic ids and act keys the handlers look up with RelicEngine.isOn.
    static final String ID_TOWEL = "handy_towel_cape", ID_ELVEN = "darkened_cloak_of_elvenkind", ID_LEVITATE = "cloak_of_levitation",
            ID_BLADES = "bracers_of_arcane_blades", ID_BEAR = "bear_bracer";

    /** The day number of a world clock reading (turns over at dawn, like the sun). */
    static long dayOf(long dayTime) { return Math.floorDiv(dayTime, (long) DAY); }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // ------------------------------------------------------------------ the Elvenkind cloak's charges
    static final int ELVEN_MAX = 5, TRACE_COST = 1, DARKNESS_COST = 2, INVISIBILITY_COST = 3;

    /**
     * A pool of charges that comes back at dawn. {@code day} is the world day of the last change plus one (0 for a pool never touched, which reads as
     * full). A dawn that has passed since adds {@code gain} charges for each day gone, up to the maximum.
     */
    record Charges(int max, int left, long day) {
        static Charges fresh(int max) { return new Charges(max, max, 0L); }

        Charges at(long today, int gain) {
            if (day <= 0L) return new Charges(max, max, today + 1);                 // never touched: full
            long gone = today - (day - 1);
            if (gone <= 0L) return this;
            long back = Math.min((long) max, gone * Math.max(0, gain));
            return new Charges(max, (int) Math.min((long) max, left + back), today + 1);
        }

        boolean canSpend(int n) { return n > 0 && left >= n; }

        Charges spend(int n) { return canSpend(n) ? new Charges(max, left - n, day) : this; }
    }

    /** The 1d4+1 of a dawn, from a roll in [0, 3]. */
    static int dawnGain(int roll0to3) { return 2 + clamp(roll0to3, 0, 3); }

    // ------------------------------------------------------------------ stepping through a wall
    /** How many half-block steps of look direction the wall may start away and how many it may be thick. */
    static final int WALL_START_STEPS = 6, WALL_THICK_STEPS = 10, SLIP_STEPS = 18;

    /**
     * {@code blocked[i]} and {@code stand[i]} describe the point i half-blocks along the look ray (index 0 is the player's own place and is ignored).
     * Returns the index to land on, or -1: the first blocked point must come within {@link #WALL_START_STEPS}, and the first place to stand after it
     * within {@link #WALL_THICK_STEPS} further steps.
     */
    static int slipIndex(boolean[] blocked, boolean[] stand) {
        int n = Math.min(blocked.length, stand.length);
        int wall = -1;
        for (int i = 1; i < n && i <= WALL_START_STEPS; i++) if (blocked[i]) { wall = i; break; }
        if (wall < 0) return -1;
        for (int j = wall + 1; j < n && j <= wall + WALL_THICK_STEPS; j++) if (stand[j] && !blocked[j]) return j;
        return -1;
    }

    // ------------------------------------------------------------------ the two windows of not being caught
    static final int FLURRY_TICKS = 360, FLURRY_GAP = 20, BLUR_TICKS = 1200, BLUR_PERCENT = 25;

    /** First Snow's flurry: one melee blow in each second is turned aside while the flurry lasts. */
    static boolean flurryEvades(long now, long until, long lastEvade) { return now < until && now - lastEvade >= FLURRY_GAP; }

    /** The Autumn Cloak's blur: a quarter of the blows (melee or shot) miss while it lasts. {@code roll} is in [0, 99]. */
    static boolean blurEvades(long now, long until, int roll) { return now < until && roll < BLUR_PERCENT; }

    // ------------------------------------------------------------------ the Bracers of Manhandling
    /**
     * Distance multiplier of a throw by the size of the creature against the thrower's (volume ratio): a smaller one flies twice as far, a larger one
     * half as far, anything over four times the thrower's volume cannot be lifted (0).
     */
    static double throwPower(double volumeRatio) {
        if (volumeRatio > 4.0) return 0.0;
        if (volumeRatio >= 1.6) return 0.5;
        if (volumeRatio <= 0.6) return 2.0;
        return 1.0;
    }

    static final double THROW_SPEED = 0.85, THROW_LIFT = 0.45;
    static final float THROW_IMPACT = 4.0f, THROW_SPLASH = 2.0f;

    // ------------------------------------------------------------------ the small ones
    /** The Cape of Projection: a quarter of what hit you goes back at whoever did it, at most 6. */
    static float reflected(float taken) { return Math.min(6.0f, Math.max(0f, taken) * 0.25f); }

    /** What a bond partner's pull-back leaves of a blow that would kill: all but one hit point of the health. Never lets a 1-hp target be saved. */
    static float stabilised(float health, float incoming) {
        if (health <= 2.0f || incoming < health) return incoming;
        return health - 1.0f;
    }

    /** The Phantom's Bracelet coin: heads (true) when the low bit is set. */
    static boolean coin(int bits) { return (bits & 1) == 1; }

    static final double BOND_RANGE = 10.0, REST_RANGE = 16.0;

    static boolean inBond(double distSq) { return distSq <= BOND_RANGE * BOND_RANGE; }

    static boolean dailyReady(long stored, long today) { return stored == 0 || stored - 1 < today; }

    static long dailyMark(long today) { return today + 1; }

    // ------------------------------------------------------------------ the Bees
    static final int SWARM_TICKS = 120, SWARM_PULSE = 20;
    static final float SWARM_STING = 1.0f;

    /** Total sting damage one swarm can do to one creature: the cap that keeps it under a third of a 20 hp player. */
    static float swarmTotal() { return SWARM_STING * (SWARM_TICKS / SWARM_PULSE); }
}
