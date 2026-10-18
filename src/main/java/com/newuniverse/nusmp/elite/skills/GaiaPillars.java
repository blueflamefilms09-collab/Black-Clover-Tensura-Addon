package com.newuniverse.nusmp.elite.skills;

/**
 * Why: Gaia's pillars are earth rising in a line in front of the caster, one after another. The layout (how many, how far apart, when each rises) is pure geometry and timing, kept
 * Minecraft-free so it is tested; the damage and the knock-up are applied by {@code GaiaWorld}. No blocks are placed: a pillar is a strike zone with the mod's earth effects, so
 * the volley never changes the terrain for good.
 */
public final class GaiaPillars {
    private GaiaPillars() {}

    public static final double DAMAGE = 18.0;
    public static final double RADIUS = 2.0;
    public static final double LAUNCH = 0.9;
    public static final int STAGGER_TICKS = 4;
    public static final double FIRST_DISTANCE = 3.0;
    public static final double SPACING = 3.5;

    /** Pillars in a volley: 3, 5 once the Ultimate is mastered. */
    public static int count(boolean ownMastered) { return ownMastered ? 5 : 3; }

    /** Pillar i is centred this far in front of the caster. */
    public static double distance(int i) { return FIRST_DISTANCE + SPACING * i; }

    /** Ticks after the cast at which pillar i rises. */
    public static int delay(int i) { return STAGGER_TICKS * i; }

    /** Centres {x, z} of the pillars, from an origin and a horizontal direction (any length; zero points north-south). */
    public static double[][] centres(double ox, double oz, double dirX, double dirZ, int count) {
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        double ux = len < 1.0e-6 ? 0.0 : dirX / len, uz = len < 1.0e-6 ? 1.0 : dirZ / len;
        double[][] out = new double[Math.max(0, count)][2];
        for (int i = 0; i < out.length; i++) {
            out[i][0] = ox + ux * distance(i);
            out[i][1] = oz + uz * distance(i);
        }
        return out;
    }

    /** Damage of pillar i: the volley builds, the last pillar hits hardest. */
    public static double damage(int i, int count, boolean ownMastered) {
        double base = DAMAGE * (ownMastered ? 1.2 : 1.0);
        return count <= 1 ? base : base * (0.8 + 0.4 * i / (count - 1));
    }
}
