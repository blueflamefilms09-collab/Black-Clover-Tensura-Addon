package com.newuniverse.nusmp.book;

import java.util.List;

/**
 * Mercury Magic (Nozel Silva), 1.93.0 rebuild: the pure numbers and ids of the book (MercuryRulesTest). The four old page ids (silver_blade, mercury_shield, mercury_rain, silver_eagle)
 * keep their order and mastery bits; the pages of MercuryExt are appended after them.
 */
public final class MercuryRules {
    private MercuryRules() {}

    public static final String BLADE = "silver_blade", GUARDIAN = "mercury_shield", RAIN = "mercury_rain", EAGLE = "silver_eagle";
    public static final String BULLET = "silver_bullet", SPEAR = "silver_spear", REGAL = "silver_shining_regal_spear", STAR = "silver_star_of_execution";
    public static final List<String> OLD_IDS = List.of(BLADE, GUARDIAN, RAIN, EAGLE);
    public static final List<String> NEW_IDS = List.of(BULLET, SPEAR, REGAL, STAR);

    public static final int GUARDIAN_TICKS = 160, SHORT_ARMOUR_TICKS = 100, REGAL_TICKS = 40, STAR_TICKS = 100, EAGLE_FLIGHT_TICKS = 26;
    /** The Silver Guardian's toxic coat costs the wearer this many hit points a second (never below SELF_HARM_FLOOR hit points): "this spell harms the user while in use". */
    public static final float SELF_HARM_PER_SECOND = 0.5f, SELF_HARM_FLOOR = 6f;

    /** Spears launched by Silver Spear: 1 at power 0.8, up to 3. */
    public static int spearCount(float power) { return Math.max(1, Math.min(3, 1 + (int) ((power - 0.8f) / 0.5f))); }

    /** Bullets of a Silver Bullet volley: 5, plus one for every 0.4 of power above 1.2, at most 8. */
    public static int bulletCount(float power) { return Math.max(5, Math.min(8, 5 + (int) Math.max(0, (power - 1.2f) / 0.4f))); }

    /** Spears shot by the cage of the Silver Star of Execution each time its bars strike: 2, up to 5. */
    public static int starVolley(float power) { return Math.max(2, Math.min(5, 2 + (int) Math.max(0, (power - 0.8f) / 0.5f))); }

    /** The cage's radius in blocks: 4.5 at power 0.8, up to 7. */
    public static float starRadius(float power) { return Math.max(4.5f, Math.min(7f, 3.2f + 1.5f * power)); }

    /** The Silver Shining Regal Spear's reach in blocks. */
    public static double regalReach(float power) { return Math.min(30.0, 16.0 + 6.0 * power); }

    /** Whether the wearer may still be hurt by the coat's toxin: only above the floor, and never in creative. */
    public static boolean selfHarmAllowed(float health, boolean creative) { return !creative && health > SELF_HARM_FLOOR; }
}
