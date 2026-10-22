package com.newuniverse.nusmp.firecast;

/**
 * Why: Mereoleona's spells are released by her fists. This is the combo counter of one player's landed unarmed punches (pure, unit tested): every landed punch within the timeout of the
 * previous one raises the count, a longer pause starts again at one, and when the count reaches the finisher number of the readied page the combo is spent and the finisher fires. One object per
 * player UUID (the server keeps them in a map and drops them on logout).
 *
 * <p>The finisher number is {@link FireCastRules#finisherHits}: 3 for a starter or mid page and 5 for a zone or signature page, one fewer while the Mana Zone is open (never below 2); the pause
 * allowed between punches is {@link FireCastRules#comboTimeout}: 30 ticks, 12 more with the Mana Zone open.</p>
 */
public final class PunchCombo {
    public enum Result { HIT, FINISHER }

    private int count;
    private long lastHit = Long.MIN_VALUE / 4;

    /** The count of the running combo as of {@code now} (0 when it timed out). */
    public int count(long now, boolean zone) { return now - lastHit > FireCastRules.comboTimeout(zone) ? 0 : count; }

    /**
     * A punch landed at {@code now}. {@code costPercent} is the readied page's cost tier (decides the finisher number), {@code armed} whether a page is readied at all. Returns FINISHER (and
     * resets the combo) when the finisher number is reached with a page readied, else HIT.
     */
    public Result land(long now, boolean zone, double costPercent, boolean armed) {
        count = count(now, zone) + 1;
        lastHit = now;
        if (armed && count >= FireCastRules.finisherHits(costPercent, zone)) {
            reset();
            return Result.FINISHER;
        }
        if (count > FireCastRules.COMBO_CAP) count = FireCastRules.COMBO_CAP;       // an unarmed combo keeps counting up to a cap, no more
        return Result.HIT;
    }

    public void reset() { count = 0; lastHit = Long.MIN_VALUE / 4; }
}
