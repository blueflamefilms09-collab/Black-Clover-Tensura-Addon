package com.newuniverse.nusmp.heroweapon.weapon.scythe;

/**
 * Why: the Scythe's finishing moves are arithmetic that decides how much it hurts and how much it heals, and the owner's rules bound both: the Soul Reap doubles
 * its damage only on a weak, non-boss, non-player foe (a reaper is no execution machine against players or bosses), the Protection Break grows with the foe's
 * armour up to a cap, the Soul Harvest grows with the souls collected up to the ultimate limit, and every heal is a small share of the wielder's health that
 * never passes a quarter. The rules are here, with no game imports, so each is tested with numbers; the skills only apply them.
 */
public final class ReapRules {
    private ReapRules() {}

    /** The most any one technique may heal, as a share of the healed one's maximum health (design.md section 5). */
    public static final double HEAL_LIMIT = 0.25;

    /**
     * True when a foe counts as weak: a mob (not a player, not a boss) whose health is at or under {@link ScytheNumbers#REAP_THRESHOLD} of its maximum. A
     * player or a boss is never reaped, whatever its health.
     */
    public static boolean isWeak(double health, double maxHealth, boolean player, boolean boss) {
        if (player || boss || maxHealth <= 0.0 || health <= 0.0) return false;
        return health / maxHealth <= ScytheNumbers.REAP_THRESHOLD;
    }

    /** Raw damage of the Soul Reap: 10, doubled on a weak foe. */
    public static double reapRaw(boolean weak) {
        return ScytheNumbers.REAP_BASE * (weak ? ScytheNumbers.REAP_WEAK_FACTOR : 1.0);
    }

    /** Raw damage of the Protection Break against a foe with this much armour: 9 and 0.6 per armour point, at most 8 more. */
    public static double breakRaw(double armor) {
        return ScytheNumbers.BREAK_BASE + Math.min(ScytheNumbers.BREAK_BONUS_CAP, Math.max(0.0, armor) * ScytheNumbers.BREAK_PER_ARMOR);
    }

    /**
     * Raw damage of the Soul Harvest: 16 and 1 per soul (at most {@code soulCap} count), times 1.2 on a weak foe, never above the ultimate limit of 32.
     *
     * @param souls   the souls spent
     * @param weak    whether the foe is weak (see {@link #isWeak})
     * @param soulCap the most souls this wielder can hold (10, or 14 with Dragon Soul)
     */
    public static double harvestRaw(int souls, boolean weak, int soulCap) {
        int counted = Math.max(0, Math.min(souls, Math.max(0, soulCap)));
        double raw = (ScytheNumbers.HARVEST_BASE + ScytheNumbers.HARVEST_PER_SOUL * counted) * (weak ? ScytheNumbers.HARVEST_WEAK_FACTOR : 1.0);
        return Math.min(32.0, raw);
    }

    /** What the Soul Reap heals the wielder for reaping {@code reaped} foes: 3 percent of their maximum health each, at most 12 percent, never over a quarter. */
    public static double reapHeal(int reaped, double maxHealth) {
        double share = Math.min(ScytheNumbers.REAP_HEAL_CAP, ScytheNumbers.REAP_HEAL_PER_FOE * Math.max(0, reaped));
        return Math.max(0.0, Math.min(share, HEAL_LIMIT) * maxHealth);
    }

    /** What the Soul Harvest heals the wielder for reaping {@code reaped} foes: 2 percent each, at most 20 percent, never over a quarter. */
    public static double harvestHeal(int reaped, double maxHealth) {
        double share = Math.min(ScytheNumbers.HARVEST_HEAL_CAP, ScytheNumbers.HARVEST_HEAL_PER_FOE * Math.max(0, reaped));
        return Math.max(0.0, Math.min(share, HEAL_LIMIT) * maxHealth);
    }
}
