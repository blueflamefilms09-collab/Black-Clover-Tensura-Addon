package com.newuniverse.nusmp.heroweapon.weapon.shield;

/**
 * Why: the Shield answers what it blocks, and the owner's rule is that an answer is never worth more than the blow it answers ("cap reflected damage at the damage
 * taken"). The balance law scales every hit by the wielder's existence, so a raw number cannot be compared with the damage that was blocked directly; the caller
 * asks the law what a raw number WOULD come to, and this class turns that into a raw number that fits under the limit. The attacks that spend the Counter charge
 * also scale between a floor and a ceiling by the charge, which is a line of arithmetic that deserves a test. No Minecraft types.
 */
public final class CounterMath {
    private CounterMath() {}

    /** The raw damage of a counter attack: {@code base} with no charge, {@code base + span} with a full one. The charge is clamped to 0 to 100. */
    public static double scaled(double base, double span, double charge) {
        double c = Math.max(0.0, Math.min(ShieldNumbers.COUNTER_MAX, charge)) / ShieldNumbers.COUNTER_MAX;
        return base + span * c;
    }

    /** The most final damage an answer may do: {@code share} of the blow that was blocked (0 when the blow or the share is not positive). */
    public static double limit(double blocked, double share) { return blocked > 0 && share > 0 ? blocked * share : 0.0; }

    /**
     * The raw damage to use so that the final damage stays at or under {@code limit}: {@code raw} as it is when the law's answer for it ({@code probe}, the final
     * damage {@code raw} would do) already fits, else {@code raw} scaled down in proportion (the law is linear in the raw number for a given pair of fighters).
     */
    public static double fit(double raw, double probe, double limit) {
        if (!(raw > 0) || !(limit > 0)) return 0.0;
        if (!(probe > limit)) return raw;
        return raw * (limit / probe);
    }

    /** The Counter charge a block gives: the soaked damage as a share of the maximum health, times 100 and the rate (a parry doubles it); never below 0. */
    public static double gain(double soaked, double maxHealth, double rate, boolean parry) {
        if (!(soaked > 0)) return 0.0;
        return soaked / Math.max(1.0, maxHealth) * ShieldNumbers.COUNTER_MAX * Math.max(0.0, rate) * (parry ? ShieldNumbers.PARRY_COUNTER_FACTOR : 1.0);
    }

    /** Health the wielder may give for a technique: {@code share} of the maximum, but never leaving less than {@code floor} of it. 0 when already at or under the floor. */
    public static double price(double health, double maxHealth, double share, double floor) {
        double room = health - Math.max(0.0, floor) * maxHealth;
        if (!(room > 0)) return 0.0;
        return Math.max(0.0, Math.min(maxHealth * share, room));
    }
}
