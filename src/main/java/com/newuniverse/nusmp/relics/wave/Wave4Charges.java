package com.newuniverse.nusmp.relics.wave;

/**
 * Wave 4: the charges of the Arcane Glass of Seeing (6, back by 1d4+2 each dawn, a last charge may crack the gem) and of the
 * Pendant of the Dreamer (6, back by 1d6 each midnight). A charge state is three plain values, kept on the amulet itself; the
 * day it last recovered on is what makes the recovery happen once per dawn or midnight, whenever the amulet is next looked at.
 * Pure Java.
 */
final class Wave4Charges {
    private Wave4Charges() {}

    static final int DAY = 24000;
    /** Day 0 starts at tick 0 (morning); dawn is about tick 23000 of the day before it, midnight tick 18000. */
    static final int DAWN_OFFSET = 1000;
    static final int MIDNIGHT_OFFSET = 6000;

    /** charges now, the day index it last recovered on, and whether the gem is cracked (skips the next recovery). */
    record State(int charges, long day, boolean cracked) {}

    static State fresh(int max, long today) { return new State(max, today, false); }

    /** The day number for a game time, turning over at dawn or at midnight depending on the offset. */
    static long dayIndex(long gameTime, int offset) { return Math.floorDiv(gameTime + offset, (long) DAY); }

    /** Recovery owed since the state was last looked at: {@code roll} charges once per new day, none on a cracked day. */
    static State refill(State s, long today, int max, int roll) {
        if (today < s.day()) return new State(Math.min(s.charges(), max), today, s.cracked());   // a clock set back: do not owe days
        if (today == s.day()) return new State(Math.min(s.charges(), max), s.day(), s.cracked());
        if (s.cracked()) return new State(Math.min(s.charges(), max), today, false);
        return new State(Math.min(max, s.charges() + Math.max(0, roll)), today, false);
    }

    static boolean canSpend(State s, int n) { return n > 0 && s.charges() >= n; }

    static State spend(State s, int n) { return new State(Math.max(0, s.charges() - n), s.day(), s.cracked()); }

    /** True when spending {@code n} empties the amulet (the Glass then risks a crack). */
    static boolean emptiesOn(State s, int n) { return s.charges() > 0 && s.charges() - n <= 0; }

    /** The d20 of the Glass: a natural 1 cracks the gem. */
    static boolean cracks(int d20) { return d20 == 1; }

    static State crack(State s) { return new State(s.charges(), s.day(), true); }

    /** 1d4+2 for the Glass. {@code d4} is 1..4. */
    static int glassRecovery(int d4) { return Math.max(1, Math.min(4, d4)) + 2; }

    /** 1d6 for the Dreamer. {@code d6} is 1..6. */
    static int dreamerRecovery(int d6) { return Math.max(1, Math.min(6, d6)); }

    /** Once per {@code span} ticks: true when never used (a negative stamp), when the span has passed, or when the stamp lies in the future. */
    static boolean ready(long lastUse, long now, int span) { return lastUse < 0 || now - lastUse >= span || now < lastUse; }

    /** Minutes (rounded up) until {@code span} ticks after {@code lastUse} have passed. */
    static int minutesLeft(long lastUse, long now, int span) { return (int) Math.max(1, (lastUse + span - now + 1199) / 1200); }

    /**
     * The Dreamer's Force of Mhigh'Raine: 3d6+2 of the card, scaled to hit points (x0.6: 3 to 12, 7.5 on average). The "saving throw"
     * halves it; against a player it never exceeds 6, a third of a full health bar.
     */
    static float mindBlast(int d6a, int d6b, int d6c, boolean saved, boolean againstPlayer) {
        float raw = (clampDie(d6a) + clampDie(d6b) + clampDie(d6c) + 2) * 0.6f;
        if (saved) raw *= 0.5f;
        return againstPlayer ? Math.min(raw, PLAYER_CAP) : raw;
    }

    static final float PLAYER_CAP = 6f;

    private static int clampDie(int d) { return Math.max(1, Math.min(6, d)); }
}
