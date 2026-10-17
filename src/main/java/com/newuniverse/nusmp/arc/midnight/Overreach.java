package com.newuniverse.nusmp.arc.midnight;

/**
 * Why: this is the answer the fight gives to "light speed against foresight". In the story Patolli could not be outrun, but Julius Novachrono could see
 * where he would be, and every strike Patolli threw at a place his foe had already left was strength spent for nothing. The boss counts the same thing:
 * each close-in light-speed strike (the Light Step, the whip, the Dance of Cuts) either lands on somebody or finds only air. Three empty strikes in a row
 * and he has overreached: he is left open for a moment. A strike that lands resets the count, so the reward goes to the party that really does read the
 * lanes and leave them, and a party that stands in them never sees it. Pure (a counter and a limit), so the rule is proven by a test.
 */
public final class Overreach {
    private final int limit;
    private int whiffs;

    public Overreach(int limit) { this.limit = Math.max(1, limit); }

    /**
     * Records the outcome of one strike. Returns true exactly when this miss is the one that opens the boss (the count then starts again from nothing).
     * A strike that landed on at least one foe clears the count.
     */
    public boolean record(boolean landed) {
        if (landed) {
            whiffs = 0;
            return false;
        }
        whiffs++;
        if (whiffs >= limit) {
            whiffs = 0;
            return true;
        }
        return false;
    }

    public int whiffs() { return whiffs; }

    public int limit() { return limit; }

    public void reset() { whiffs = 0; }
}
