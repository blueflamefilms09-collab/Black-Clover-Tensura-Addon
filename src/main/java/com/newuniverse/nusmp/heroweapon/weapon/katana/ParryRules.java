package com.newuniverse.nusmp.heroweapon.weapon.katana;

/**
 * Why: a parry that turns every blow into nothing would make the wielder immune, which the owner's balance rules forbid, so what a parry stops and what it
 * returns are rules with numbers: it stops at most 60 percent of the wielder's maximum health (the rest of a huge blow gets through), and it answers with a cut
 * of 4 to 12 raw damage that grows with the blow it stopped. They live here, with no game imports, so they are tested; the logic only applies them.
 */
public final class ParryRules {
    private ParryRules() {}

    /** How much of a blow of {@code amount} the parry stops, given the wielder's maximum health: all of it, up to 60 percent of that health. */
    public static float stopped(float amount, float maxHealth) {
        if (amount <= 0f) return 0f;
        float limit = (float) (KatanaNumbers.PARRY_NEGATE_SHARE * Math.max(0f, maxHealth));
        return Math.min(amount, limit);
    }

    /** What still gets through the parry. */
    public static float passThrough(float amount, float maxHealth) {
        return Math.max(0f, amount - stopped(amount, maxHealth));
    }

    /** The raw damage of the answering cut for a blow of {@code amount}: 4 plus half the blow, between 4 and 12. */
    public static double counterRaw(float amount) {
        double raw = KatanaNumbers.COUNTER_MIN + KatanaNumbers.COUNTER_PER_DAMAGE * Math.max(0f, amount);
        return Math.max(KatanaNumbers.COUNTER_MIN, Math.min(KatanaNumbers.COUNTER_MAX, raw));
    }
}
