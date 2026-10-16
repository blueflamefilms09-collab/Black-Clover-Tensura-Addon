package com.newuniverse.nusmp.song;

/**
 * Why: Crescendo asks the singer to hold a loud note for about three seconds and then let go. The rule is kept apart from the
 * world so it can be tested: loud ticks fill the charge, quiet ticks drain it twice as fast (a short breath does not ruin the build
 * up, a real stop does), and the first quiet tick after the charge is full is the release.
 */
public final class CrescendoCharge {
    public enum Result { CHARGING, IDLE, RELEASE }

    private int charge;
    private float peak;

    public int charge() { return charge; }

    /** The loudest level held while charging, 0..1 (the burst's strength). */
    public float peak() { return peak; }

    public void reset() { charge = 0; peak = 0f; }

    /** One server tick. {@code needed} is the charge that counts as full. */
    public Result tick(float level, float loud, int needed) {
        if (level >= loud) {
            charge = Math.min(needed, charge + 1);
            peak = Math.max(peak, level);
            return Result.CHARGING;
        }
        if (charge >= needed) return Result.RELEASE;
        charge = Math.max(0, charge - 2);
        if (charge == 0) peak = 0f;
        return charge > 0 ? Result.CHARGING : Result.IDLE;
    }
}
