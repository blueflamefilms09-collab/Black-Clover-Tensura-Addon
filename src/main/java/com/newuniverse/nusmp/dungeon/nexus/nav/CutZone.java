package com.newuniverse.nusmp.dungeon.nexus.nav;

/**
 * Where on a Carcass a Butcher Cleaver strikes decides the cut (docs/living_dungeon.md section 6, "anatomy-based harvesting"): the upper
 * third of the carcass is the organs, the middle third the meat (the cut the cleaver always made), the lower third hide and bone. A click
 * on the top face (looking down at it from above) is the back, the meat as before: so the old way of cutting still works, and the organs
 * and the hide are cut by aiming at the carcass's flank. Pure.
 */
public enum CutZone {
    HIDE, MEAT, ORGANS;

    /** The height of a Carcass block's shape: 6 pixels of 16. */
    public static final double HEIGHT = 6.0 / 16.0;

    /**
     * The zone of a click {@code y} blocks above the bottom of the carcass block (0 to {@link #HEIGHT}); {@code topFace} is a click on the top.
     * A click outside the shape is clamped into it.
     */
    public static CutZone of(double y, boolean topFace) {
        if (topFace) return MEAT;
        double f = Math.max(0.0, Math.min(1.0, y / HEIGHT));
        if (f >= 2.0 / 3.0) return ORGANS;
        return f < 1.0 / 3.0 ? HIDE : MEAT;
    }
}
