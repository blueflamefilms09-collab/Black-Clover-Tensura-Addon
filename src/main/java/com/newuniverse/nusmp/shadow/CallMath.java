package com.newuniverse.nusmp.shadow;

/**
 * Why: Gallus's call stuns for 1.5 to 2.5 seconds depending on how close the enemy stands, and its second use roots one enemy for 4 seconds.
 * Both then pass through the balance law's control clamps (3 s for players, 1 s for bosses, halved on repeats); this class only holds the
 * requested numbers so the range and the clamps can be tested.
 */
public final class CallMath {
    private CallMath() {}

    public static final int MIN_STUN = 30, MAX_STUN = 50, ROOT_TICKS = 80;
    public static final double CALL_RADIUS = 8.0;

    /** Requested stun ticks for an enemy {@code distance} blocks away: 2.5 s point blank down to 1.5 s at the edge; 0 outside the call. */
    public static int stunTicks(double distance) {
        if (distance < 0 || distance > CALL_RADIUS) return 0;
        double k = 1.0 - distance / CALL_RADIUS;
        return (int) Math.round(MIN_STUN + (MAX_STUN - MIN_STUN) * k);
    }
}
