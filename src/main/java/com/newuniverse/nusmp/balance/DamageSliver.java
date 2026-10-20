package com.newuniverse.nusmp.balance;

/**
 * The "sliver of the target's max health" that {@link BalanceLaw#damage} used to add to every hit: {@code maxHealth * 1% * raw / 10}. It was meant to let a spell of 5 to 15 damage
 * still scratch a big target, but it grows with the raw number, so a hit of 400 (Elsdocia, a relic act) added 40% of the target's max health: a 1,000,000 HP boss lost about 400,000 to
 * one blow. It is no longer a feature of weapons: a hit of weapon scale gets none, a boss (or anything with 300 or more max health, see {@link BalanceLaw#isBoss}) gets none, and for
 * the small spells that remain the sliver is capped at {@link #MAX_FRACTION} of the target's max health. Pure, so it is tested without Minecraft.
 */
public final class DamageSliver {
    private DamageSliver() {}

    /** The largest raw hit that still gets a sliver: spell scale. A weapon-scale hit (above this) gets none. */
    public static final double SPELL_RAW_LIMIT = 20.0;
    /** The sliver never exceeds this share of the target's max health. */
    public static final double MAX_FRACTION = 0.02;

    /** The extra damage for a hit of {@code raw} on a target with {@code maxHealth}; 0 for a boss or a weapon-scale hit. */
    public static double of(double raw, double maxHealth, boolean boss) {
        if (boss || raw <= 0 || raw > SPELL_RAW_LIMIT || maxHealth <= 0) return 0;
        return Math.min(maxHealth * 0.01 * raw / 10.0, maxHealth * MAX_FRACTION);
    }
}
