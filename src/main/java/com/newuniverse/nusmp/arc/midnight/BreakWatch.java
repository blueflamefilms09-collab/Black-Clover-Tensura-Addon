package com.newuniverse.nusmp.arc.midnight;

/**
 * Why: two of the big moves can be broken by the party instead of dodged. Patolli's Heaven's Verdict (swords hung over every foe, as he hung them over the
 * citizens of the capital) and Fana's Evil Heart (the self-destruction that her Third Eye drives, ended in the story when the Eye was cut) both say: deal
 * a share of the boss's health while it is being cast and the move collapses. The honest measure of "damage dealt" is the health the boss actually lost
 * since the move began, not the sum of the raw hits, because the framework's per-second damage budget cuts a huge blow down and the party must not be
 * asked for more than it could possibly remove. A boss in this mod never heals in a fight, so lost health is simply start minus now. Pure, so the rule
 * (arm, measure, break at a fraction of maximum health) is proven without a game.
 */
public final class BreakWatch {
    private boolean armed;
    private float start;

    /** Starts watching from the boss's health now. */
    public void arm(float health) {
        armed = true;
        start = health;
    }

    public void disarm() { armed = false; }

    public boolean armed() { return armed; }

    /** Health lost since {@link #arm}; 0 when not armed or when the boss gained health (it never should, but the answer is never negative). */
    public float lost(float health) { return armed ? Math.max(0f, start - health) : 0f; }

    /** Has the boss lost at least {@code fraction} of its maximum health since it was armed? */
    public boolean broken(float health, float maxHealth, double fraction) {
        return armed && maxHealth > 0f && lost(health) >= maxHealth * fraction;
    }

    /** 0..1 of the way to breaking, for the party to read (the action bar shows it). */
    public float progress(float health, float maxHealth, double fraction) {
        if (!armed || maxHealth <= 0f || fraction <= 0) return 0f;
        return (float) MidnightMath.clamp(lost(health) / (maxHealth * fraction), 0, 1);
    }
}
