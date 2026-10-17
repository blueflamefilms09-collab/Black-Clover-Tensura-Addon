package com.newuniverse.nusmp.arc.sds;

/**
 * Why: Cruel Sun is Escanor's signature, a small sun thrown at the ground, and Zenith Cleave is what The One does with Rhitta; both are the kind of
 * move that must be big, must be fair, and must have a counter the party can learn. The numbers live here, pure, so a test can prove the promises
 * the fight makes: the burst can always be walked out of in the time the telegraph gives; cover (a block between the sun and the target) and a
 * raised shield really cut it; and a party that hurts him hard enough while he winds it up breaks it (the counter to an ultimate is a burst of damage
 * at the right moment, not a bigger health pool).
 */
public final class CruelSunMath {
    private CruelSunMath() {}

    /** Ticks the sun takes from his hand to the mark after the wind-up (it is drawn in flight, and the ring is on the ground the whole time). */
    public static final int FLIGHT = 24;
    /** The share of his maximum health the party must take off him during the wind-up to break the sun. */
    public static final double INTERRUPT = 0.08;
    /** What reaches a victim when a block stands between the sun and them, and when they hold a shield up. */
    public static final float COVER_FACTOR = 0.30f, SHIELD_FACTOR = 0.35f;
    /** The pace of a player on foot (blocks a second) the burst must always allow to leave on. */
    public static final double WALK_SPEED = 4.3;

    /** Radius of the burst: 9.4 blocks at the lowest sun that may throw it (0.72) up to 10.5 at noon, 1.5 more with the Gates open. */
    public static double radius(double sun, boolean gatesOpen) {
        return 6.5 + 4.0 * Math.max(0.0, Math.min(1.0, sun)) + (gatesOpen ? 1.5 : 0.0);
    }

    /** Base damage of the burst (before the boss's damage law): 30.5 at the lowest sun that may throw it, 33.6 at noon. */
    public static float damage(double sun) { return (float) (28.0 * (0.8 + 0.4 * Math.max(0.0, Math.min(1.0, sun)))); }

    /** Ticks the scorched ground burns after the burst. */
    public static int scorchTicks(double sun) { return (int) (60 + 40 * Math.max(0.0, Math.min(1.0, sun))); }

    /** Radius of the scorched ground, a little over half the burst. */
    public static double scorchRadius(double burstRadius) { return burstRadius * 0.55; }

    /** Base damage of one burn tick on the scorched ground (every 20 ticks). */
    public static final float SCORCH_DAMAGE = 2.5f;

    /** The factor on the burst for one victim: cover first, then the shield; the better of the two applies, never both. */
    public static float factor(boolean covered, boolean blocking) {
        float f = 1f;
        if (covered) f = Math.min(f, COVER_FACTOR);
        if (blocking) f = Math.min(f, SHIELD_FACTOR);
        return f;
    }

    /** Has the party taken enough off him (as a share of his maximum health) to break the sun? */
    public static boolean broken(double damageShare) { return damageShare >= INTERRUPT; }

    /** Can someone at the centre walk out of a burst of this radius in {@code ticks}? (The telegraph, the flight and the ring on the ground give them {@code windUp + FLIGHT}.) */
    public static boolean walkable(double radius, int ticks) { return radius <= WALK_SPEED * ticks / 20.0; }

    // ---------------------------------------------------------------- Zenith Cleave (The One's own ultimate)
    /** Radius of the slam at the centre, how far the shock runs, and the base damage of each. */
    public static final double ZENITH_CENTRE = 3.5, ZENITH_REACH = 15.0;
    public static final float ZENITH_CENTRE_DAMAGE = 32f, ZENITH_WAVE_DAMAGE = 22f;
}
