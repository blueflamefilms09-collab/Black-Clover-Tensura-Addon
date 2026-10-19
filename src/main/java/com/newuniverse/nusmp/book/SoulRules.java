package com.newuniverse.nusmp.book;

/**
 * Why: Soul Magic hurts the soul as well as the body: every page pairs a body blow with Spiritual Damage (EnergyBridge.spirit), feeds on what it takes (energy drain) and
 * harvests the souls of the fallen. The numbers are pure so a unit test can pin them: spiritual damage grows with the caster's EP scale but is capped (a boss's soul is not
 * a free kill: halved), a drain returns a share of what was taken, and a harvest returns a share of the caster's magicules and health per soul, with a cap per window.
 */
public final class SoulRules {
    private SoulRules() {}

    /** The cap of one spiritual blow. */
    public static final float SPIRIT_CAP = 80f;
    /** Harvest: the share of the maximum magicules and of the maximum health that one soul gives back, and how many souls count in one window. */
    public static final double HARVEST_MANA = 0.06, HARVEST_HEALTH = 0.08;
    public static final int HARVEST_MAX_SOULS = 6;

    /** Spiritual damage of a page worth {@code base}: x the EP scale (1..12), capped, halved for a boss. */
    public static float spirit(double epScale, float base, boolean boss) {
        float v = (float) Math.min(SPIRIT_CAP, base * Math.max(1.0, Math.min(12.0, epScale)));
        return boss ? v * 0.5f : v;
    }

    /** How many souls of a harvest still pay (the first {@link #HARVEST_MAX_SOULS}). */
    public static boolean pays(int soulsSoFar) { return soulsSoFar < HARVEST_MAX_SOULS; }

    /** The magicules a harvested soul returns. */
    public static double harvestMana(double maxMagicule) { return maxMagicule * HARVEST_MANA; }

    /** The health a harvested soul returns. */
    public static float harvestHealth(float maxHealth) { return (float) (maxHealth * HARVEST_HEALTH); }
}
