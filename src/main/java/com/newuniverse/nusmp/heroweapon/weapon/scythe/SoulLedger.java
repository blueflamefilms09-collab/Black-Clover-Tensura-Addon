package com.newuniverse.nusmp.heroweapon.weapon.scythe;

/**
 * Why: the Vassal Scythe of the series attaches a monster's soul to the weapon (its "Spirit Enchantment"). In play that is the Scythe's own loop: every kill
 * with the scythe in hand gathers a soul (up to 10, or 14 with the Demon Dragon Scythe's Dragon Soul), the souls fade two minutes after the last one was
 * gathered, and the ultimate (Soul Harvest) spends them all for more damage. This is the counter and nothing else, with no game imports, so the cap, the fading
 * and the spending are tested. The clock is the level's game time. One ledger per wielder; it lives in memory only.
 */
public final class SoulLedger {
    private int souls;
    private long lastGain = Long.MIN_VALUE / 2;

    /** The souls held at {@code now}; they have faded to none when the last one was gathered more than {@link ScytheNumbers#SOUL_FADE_TICKS} ago. */
    public int souls(long now) {
        fade(now);
        return souls;
    }

    /** Gathers {@code n} souls (never above {@code cap}); true when the count changed. Gathering restarts the fading time. */
    public boolean gain(int n, int cap, long now) {
        fade(now);
        if (n <= 0 || souls >= cap) return false;
        souls = Math.min(cap, souls + n);
        lastGain = now;
        return true;
    }

    /** Spends every soul held at {@code now} and returns how many there were. */
    public int spend(long now) {
        fade(now);
        int s = souls;
        souls = 0;
        return s;
    }

    /** Drops the souls if they have faded; true when it dropped some. */
    public boolean fade(long now) {
        if (souls > 0 && now - lastGain > ScytheNumbers.SOUL_FADE_TICKS) {
            souls = 0;
            return true;
        }
        return false;
    }

    public void reset() {
        souls = 0;
        lastGain = Long.MIN_VALUE / 2;
    }
}
