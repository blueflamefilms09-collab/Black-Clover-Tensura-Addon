package com.newuniverse.nusmp.entity.zogratis;

/**
 * The numbers of Zenon's Spatial Mana Domination by power (pure, unit tested): how big the cube is, how long it stands, and whether
 * a foe standing in its middle when the telegraph starts can walk out before it seals. It opens at 60% power (where the mana is
 * unbound) and grows with each step; at the Gates (100%) it is larger, lasts longer and the portals chain.
 */
public final class DomainMath {
    private DomainMath() {}

    /** Least power at which the domain can be cast. */
    public static final int MIN_POWER = 60;
    /** A walking player covers about this many blocks a tick (4.3 blocks a second). */
    public static final double WALK = 0.2158;

    /** Half the side of the cube: 8 blocks at 60%, 9.5 at 80%, 11 at the Gates (a cube 16, 19 or 22 blocks across). */
    public static double half(int power) {
        double p = Math.max(MIN_POWER, Math.min(PowerCurve.FULL, power));
        return p <= 80 ? 8 + 1.5 * (p - 60) / 20.0 : 9.5 + 1.5 * (p - 80) / 20.0;
    }

    /** Ticks the cube stands: 7 s at 60%, 9 s at 80%, 13 s at the Gates. */
    public static int ticks(int power) {
        double p = Math.max(MIN_POWER, Math.min(PowerCurve.FULL, power));
        return (int) Math.round(p <= 80 ? 140 + 40 * (p - 60) / 20.0 : 180 + 80 * (p - 80) / 20.0);
    }

    /** Portal chain waves inside the domain: none before the Gates, then a second and a third wave. */
    public static int chainWaves(int power) { return PowerCurve.gatesOpen(power) ? 2 : 0; }

    /** The damage factor the caster gets from ruling the space: +15% on every blow while its own cube stands. */
    public static final double RULER_DAMAGE = 1.15;

    /** Ticks a walker standing in the middle needs to reach the nearest wall. */
    public static double ticksToLeave(double half) { return half / WALK; }

    /**
     * Can a walker at the middle of the cube when the telegraph starts get out before the seal bites? The telegraph (the wind-up) and
     * the closing both count, so this checks {@code windUp + FORM_TICKS >= half / walking speed}.
     */
    public static boolean leavableOnFoot(double half, int windUpTicks) {
        return windUpTicks + DomainClock.FORM_TICKS >= ticksToLeave(half);
    }

    /** The clock for a power. */
    public static DomainClock clock(int power) { return DomainClock.of(ticks(power)); }
}
