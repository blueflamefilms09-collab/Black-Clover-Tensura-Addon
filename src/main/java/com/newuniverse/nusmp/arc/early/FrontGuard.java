package com.newuniverse.nusmp.arc.early;

/**
 * Why: Mars's Nemean Armor is crystal over the front of his body. A guard that simply halves all damage is only a longer fight; a guard that has a
 * direction is a lesson: walk round him. This pure rule is the whole of it: while the armor stands, a blow that comes from within {@value #HALF_DEGREES}
 * degrees of the way he faces is cut to {@value #FACTOR} of its size, every other blow is not touched, and anti-magic cuts straight through (it is the
 * thing that beat his crystal in the anime's tools of choice). The caller works out the angle between his facing and the attacker.
 */
public final class FrontGuard {
    private FrontGuard() {}

    public static final double HALF_DEGREES = 65;
    public static final double HALF_ANGLE = Math.toRadians(HALF_DEGREES);
    public static final double FACTOR = 0.4;

    /**
     * The factor on a blow's damage.
     *
     * @param up              is the armor standing?
     * @param angleToAttacker radians between the way Mars faces and the direction to the attacker (0 straight ahead, pi behind)
     * @param antiMagic       is the blow anti-magic?
     */
    public static double factor(boolean up, double angleToAttacker, boolean antiMagic) {
        if (!up || antiMagic) return 1.0;
        return angleToAttacker <= HALF_ANGLE ? FACTOR : 1.0;
    }
}
