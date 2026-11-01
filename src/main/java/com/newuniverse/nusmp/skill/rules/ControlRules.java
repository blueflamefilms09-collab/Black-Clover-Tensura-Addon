package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Chains of Heaven (Gate of Babylon, 3.47.0) and the other holds of the new Ultimates stop a creature for a number of ticks, and one rule decides how long: a boss is held two fifths as
 * long and a player half as long, so no hold can lock down a boss or a player for good. Pure, so it is tested without a world.
 */
public final class ControlRules {
    private ControlRules() {}

    /** The longest any hold of these skills may last, in ticks (fifteen seconds). */
    public static final int MAX_HOLD = 300;

    /** The ticks a hold of {@code base} ticks lasts on this target: bosses 0.4x, players 0.5x, never less than one tick and never more than {@link #MAX_HOLD}; a hold of nothing is nothing. */
    public static int controlTicks(int base, boolean boss, boolean player) {
        if (base <= 0) return 0;
        double f = boss ? 0.4 : (player ? 0.5 : 1.0);
        return Math.max(1, Math.min(MAX_HOLD, (int) Math.round(base * f)));
    }
}
