package com.newuniverse.nusmp.book;

/**
 * Why: Soul Corpse Magic puts puppets and grasping hands in the world, and how they move and when they strike is a handful of numbers that a unit test can pin: a puppet walks
 * at 0.2 blocks per tick towards its foe and stops at arm's reach, strikes every 20 ticks, never more than 4 puppets of one caster stand at a time, and a hand closes on
 * whatever is within 1.8 blocks every 24 ticks. No Minecraft types here.
 */
public final class SoulCorpseRules {
    private SoulCorpseRules() {}

    public static final int PUPPET_CAP = 4, ATTACK_EVERY = 20, HAND_EVERY = 24, HAND_SHOW = 10;
    public static final double PUPPET_SPEED = 0.2, ATTACK_RANGE = 2.0, HAND_REACH = 1.8, LEASH = 24.0;

    /**
     * The horizontal step of a puppet at (x, z) towards (tx, tz): the zero step when it is within {@code stop} blocks, else {@code speed} blocks long (never overshooting).
     * Returns {dx, dz}.
     */
    public static double[] step(double x, double z, double tx, double tz, double speed, double stop) {
        double dx = tx - x, dz = tz - z, d = Math.sqrt(dx * dx + dz * dz);
        if (d <= stop || d < 1e-6) return new double[]{0, 0};
        double s = Math.min(speed, d - stop);
        return new double[]{dx / d * s, dz / d * s};
    }

    /** True when the puppet may strike at this tick of its life (every {@link #ATTACK_EVERY} ticks, in reach). */
    public static boolean strikes(int life, double distance) { return distance <= ATTACK_RANGE && life % ATTACK_EVERY == 0; }

    /** True while the hand shows its closing fist (the first {@link #HAND_SHOW} ticks of every cycle). */
    public static boolean handClosing(int life) { return life % HAND_EVERY < HAND_SHOW; }

    /** True when the hand closes on its victims at this tick. */
    public static boolean handCloses(int life) { return life > 0 && life % HAND_EVERY == 0; }

    /** A puppet of the caster beyond the leash walks back instead of hunting. */
    public static boolean beyondLeash(double distanceToOwner) { return distanceToOwner > LEASH; }
}
