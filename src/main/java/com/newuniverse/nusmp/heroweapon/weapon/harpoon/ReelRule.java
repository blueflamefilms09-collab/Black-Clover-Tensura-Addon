package com.newuniverse.nusmp.heroweapon.weapon.harpoon;

/**
 * Why: reeling in a whale does not bring the whale to the boat. The Harpoon's Reel In pulls a light creature to its wielder, but a heavy one or a boss pulls the
 * wielder to IT instead (and a creature already in reach is left alone), which keeps the technique fair against the biggest foes and stops it from dragging a boss.
 * The decision is one small rule on the creature's size and distance, kept here without game classes so it can be tested exactly.
 */
public final class ReelRule {
    private ReelRule() {}

    public enum Reel { PULL_TARGET, PULL_SELF, REFUSE }

    /** Closer than this and there is nothing to reel in. */
    public static final double MIN_DISTANCE = 3.0;
    /** A creature whose body is this big (width x width x height, in cubic blocks) or whose health is at least {@link #HEAVY_HEALTH} is too heavy to pull. */
    public static final double HEAVY_VOLUME = 4.0, HEAVY_HEALTH = 150.0;

    public static Reel mode(boolean boss, double width, double height, double distance, double maxHealth) {
        if (distance < MIN_DISTANCE) return Reel.REFUSE;
        if (boss || width * width * height >= HEAVY_VOLUME || maxHealth >= HEAVY_HEALTH) return Reel.PULL_SELF;
        return Reel.PULL_TARGET;
    }
}
