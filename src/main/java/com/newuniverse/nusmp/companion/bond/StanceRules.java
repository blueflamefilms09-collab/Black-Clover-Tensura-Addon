package com.newuniverse.nusmp.companion.bond;

/**
 * Why: what a stance DOES, as one pure rule the target goal of a fighting companion asks for each candidate (so the rule is tested without a game). PASSIVE never starts a fight. GUARD goes for a
 * foe that is tied up with the player she walks with (it struck them, or they struck it, or it is targeting them) and for a hostile thing that comes within {@link #GUARD_RADIUS} blocks of
 * that player. AGGRESSIVE goes for any foe that is hostile or tied up with the player, within {@link #SEEK_RADIUS} blocks of her. A candidate that is not a foe (see {@link FoeRules}) is never
 * attacked in any stance.
 */
public final class StanceRules {
    private StanceRules() {}

    /** A guard reacts to hostile things this close to the player. */
    public static final double GUARD_RADIUS = 8.0;
    /** Nothing further than this from her is ever a target. */
    public static final double SEEK_RADIUS = 16.0;

    public static boolean wants(Stance stance, boolean foe, boolean hostile, boolean involvedWithOwner, double distToOwnerSqr, double distToSelfSqr) {
        if (!foe || stance == null || stance == Stance.PASSIVE) return false;
        if (distToSelfSqr > SEEK_RADIUS * SEEK_RADIUS) return false;
        return switch (stance) {
            case GUARD -> involvedWithOwner || (hostile && distToOwnerSqr <= GUARD_RADIUS * GUARD_RADIUS);
            case AGGRESSIVE -> hostile || involvedWithOwner;
            case PASSIVE -> false;
        };
    }
}
