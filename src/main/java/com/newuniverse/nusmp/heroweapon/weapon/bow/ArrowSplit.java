package com.newuniverse.nusmp.heroweapon.weapon.bow;

/**
 * Why: an arrow of the Legendary Bow hurts twice: a PHYSICAL hit (a normal arrow hit, the number the hitscan arrow used) and a MAGIC hit sized as a share of
 * the physical one, so armour and magic resistance both matter. The split, the per-arrow cut of the rain and the child arrows of the Shower are small pure
 * rules; a test pins them, and they only ever produce numbers the balance law then scales (the law and the boss caps still apply to each hit).
 */
public final class ArrowSplit {
    private ArrowSplit() {}

    /** The most the setting {@code heroweapon.bow.magicShareScale} may scale the magic share by. */
    public static final double MAX_SCALE = 2.0;
    /** A rain arrow aimed at a foe carries the wave's damage; an arrow of the scatter carries this share of it. */
    public static final double SCATTER_SHARE = 0.5;
    /** The child arrows of the Arrow Shower: how many, and the share of the parent's physical damage each carries. */
    public static final int SPLIT_CHILDREN = 3;
    public static final double SPLIT_SHARE = 0.4;

    /** The physical part is the raw damage itself (what the hitscan arrow dealt). */
    public static double physical(double raw) { return Math.max(0.0, raw); }

    /** The raw damage of the magic hit: a share of the physical raw damage, the share scaled by the setting (0 turns the magic hit off). */
    public static double magic(double raw, double share, double scale) {
        double s = Math.max(0.0, Math.min(1.0, share)) * Math.max(0.0, Math.min(MAX_SCALE, scale));
        return Math.max(0.0, raw) * s;
    }

    /** The raw damage of one child arrow of a split. */
    public static double child(double parentRaw) { return Math.max(0.0, parentRaw) * SPLIT_SHARE; }

    /** The most raw damage one arrow can deal to one foe: the physical hit plus the magic hit at the largest scale. */
    public static double worst(double raw, double share) { return physical(raw) + magic(raw, share, MAX_SCALE); }
}
