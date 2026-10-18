package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

/**
 * Why: a living thing that was hurt a moment ago shrugs off a second blow that is not larger than the first (vanilla's damage immunity, twenty ticks of which the
 * first ten block a blow of the same size), so a flurry of small cuts, or a rider that follows a melee blow in the very same tick, would mostly do nothing and the
 * damage the budget promises (three thrusts of 5.75, five cuts of 2.4) would be about half of that. The older weapons of this mod cut the immunity short before such
 * a blow; the four weapons of this kit do it in one place, {@code GlassKit.strike}, with one deliberate exception: against a PLAYER the hits of a quick series keep the
 * vanilla immunity, so a flurry cannot stack past the cap of the balance law (one hit takes at most 40 percent of a player's health) any more than a sword can. This is
 * that rule as plain arithmetic, so it is tested on its own. No game imports.
 */
public final class Immunity {
    private Immunity() {}

    /** Vanilla blocks a blow that is not larger than the last one only while MORE than this many immunity ticks are left, so this many lets a blow land in full. */
    public static final int LANDS_IN_FULL = 10;

    /**
     * The immunity ticks a foe should have right before one of the weapon's strikes. It never raises the number.
     *
     * @param current the immunity ticks the foe has now
     * @param rapid   the strike is one of a quick series on the same foe (hits less than ten ticks apart)
     * @param player  the foe is a player
     */
    public static int before(int current, boolean rapid, boolean player) {
        if (current <= LANDS_IN_FULL) return current;
        return rapid && player ? current : LANDS_IN_FULL;
    }
}
