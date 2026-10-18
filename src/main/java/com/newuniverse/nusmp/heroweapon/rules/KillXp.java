package com.newuniverse.nusmp.heroweapon.rules;

/**
 * Why: the weapon's experience from a kill (a quarter of the victim's maximum health, five times that for a boss) has two edges the owner would feel: a boss
 * with a huge health pool must not take a weapon from level 0 to 60 in one blow, and a mob with a tiny pool must still be worth something. The formula and its
 * cap are pure so the numbers are pinned by a test and the config only chooses the share, the boss factor and the cap.
 */
public final class KillXp {
    private KillXp() {}

    /**
     * The experience for killing something with {@code maxHealth}: {@code maxHealth * share}, times {@code bossFactor} (at least 1) when it is a boss, rounded
     * to the nearest point and never above {@code cap}. Zero for a victim with no health pool or a non-positive share or cap.
     */
    public static long forKill(double maxHealth, boolean boss, double share, double bossFactor, long cap) {
        if (!(maxHealth > 0.0) || !(share > 0.0) || cap <= 0) return 0;
        double v = maxHealth * share * (boss ? Math.max(1.0, bossFactor) : 1.0);
        return Math.min(cap, Math.round(v));
    }
}
