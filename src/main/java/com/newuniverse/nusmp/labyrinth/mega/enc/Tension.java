package com.newuniverse.nusmp.labyrinth.mega.enc;

/**
 * Why: the dice were fair and the dungeon was silent. A soloist who walks the first floor without a light reads minus three on the light, nothing on the threat
 * (floor one gives 0), nothing on the noise and nothing on the party, so the total of the d20 is the die minus three against a difficulty class of 17: only a
 * natural 20 (one pass in twenty, a quarter of a minute apart on average) can reach it. Over a long walk that is one encounter in a hundred seconds at best,
 * and a player who is waiting for the first one reads it as "encounters never trigger". A game master does not leave a table in silence for ever: the longer a
 * party has been left alone, the sooner something finds it. So every quiet pass adds to the party's tension, one point for every {@code every} quiet passes up to
 * {@code cap}, the points are added to the d20, and an encounter that spawns spends them all. The cap keeps the die meaningful (it never makes a roll certain), the
 * reset keeps the encounters apart (the cooldown still holds), and a server that wants the old behaviour sets {@code tensionCap} to 0. Pure.
 */
public final class Tension {
    private Tension() {}

    public static final int DEFAULT_EVERY = 2, DEFAULT_CAP = 5;
    /** The counter never grows past this (a party that stays quiet for an hour must not overflow it). */
    public static final int MAX_QUIET = 100_000;

    /** The d20 bonus after {@code quietPasses} quiet passes: one point per {@code every}, at most {@code cap}; 0 when either is not positive. */
    public static int bonus(int quietPasses, int every, int cap) {
        if (quietPasses <= 0 || every <= 0 || cap <= 0) return 0;
        return Math.min(cap, quietPasses / every);
    }

    /** One more quiet pass (saturating). */
    public static int quiet(int quietPasses) { return Math.min(MAX_QUIET, Math.max(0, quietPasses) + 1); }

    /**
     * The expected number of passes until the first non-quiet roll for a party whose modifiers before tension are {@code base}, with the bonus growing as above.
     * Exact (it walks the passes); capped at {@code limit} passes.
     */
    public static double expectedPasses(EncounterRoll.Mods base, int dc, int every, int cap, int limit) {
        double expected = 0.0, alive = 1.0;
        for (int pass = 0; pass < limit && alive > 1e-12; pass++) {
            double p = EncounterRoll.chance(base.withTension(bonus(pass, every, cap)), dc);
            expected += alive;                    // this pass happens when nothing came before it
            alive *= 1.0 - p;
        }
        return expected;
    }
}
