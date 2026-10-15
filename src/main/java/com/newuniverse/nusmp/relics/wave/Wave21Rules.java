package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;

/**
 * Why: part 4, wave 21 (twelve weapons: Rift Breaker, Scimitar of the Seasons, Silver-Wind Mountain Glaive, Spider Sword, Stick of Youth, Sun Gift, The
 * Last Horseman, The Legacy, The Librarian's Maul, Time hammer, Vanguard Hammer, Willbreaker) needs its numbers somewhere with no Minecraft types, so they
 * can be unit-tested. The cards are tabletop; this is where their dice, saves, rests and areas become Minecraft numbers (1 round = 6 s = 120 ticks,
 * 5 ft = 1 block, a long rest = a Minecraft day, a minute = 1200 ticks).
 */
public final class Wave21Rules {
    private Wave21Rules() {}

    // ---------------------------------------------------------------- flags (one per weapon, set by the core while it is carried in the main hand)
    public static final String F_RIFT = "w21_rift_breaker", F_SEASONS = "w21_seasons", F_GLAIVE = "w21_silver_wind", F_SPIDER = "w21_spider_sword",
            F_STICK = "w21_stick_of_youth", F_SUN = "w21_sun_gift", F_HORSEMAN = "w21_last_horseman", F_LEGACY = "w21_legacy", F_MAUL = "w21_librarians_maul",
            F_TIME = "w21_time_hammer", F_VANGUARD = "w21_vanguard_hammer", F_WILL = "w21_willbreaker";

    // ---------------------------------------------------------------- Scimitar of the Seasons
    /** A Minecraft year: four seasons of eight days each. */
    public static final int SEASON_DAYS = 8;

    public enum Season { SPRING, SUMMER, AUTUMN, WINTER }

    /** The season of a day count (Minecraft days since the world began): spring first. */
    public static Season season(long day) { return Season.values()[(int) Math.floorMod(Math.floorDiv(Math.max(0L, day), (long) SEASON_DAYS), 4L)]; }

    /** The extra damage type of the season: radiant in spring, fire in summer, necrotic in autumn, cold in winter (damage type ids, resolved at run time). */
    public static String seasonDamageType(Season s) {
        return switch (s) {
            case SPRING -> "tensura:holy_damage";
            case SUMMER -> "minecraft:in_fire";
            case AUTUMN -> "minecraft:wither";
            case WINTER -> "minecraft:freeze";
        };
    }

    public static int seasonColour(Season s) {
        return switch (s) {
            case SPRING -> 0xFFE8A0;
            case SUMMER -> 0xFF8A30;
            case AUTUMN -> 0x8A5AA8;
            case WINTER -> 0x9AD8FF;
        };
    }

    public static String seasonName(Season s) { return s.name().charAt(0) + s.name().substring(1).toLowerCase(Locale.ROOT); }

    /** The card's 2d6 as a flat number (the average, 7), and the gap in ticks between two seasonal bites so a fast swing does not stack them. */
    public static final float SEASON_BONUS = 7.0f;
    public static final int SEASON_GAP = 8;

    // ---------------------------------------------------------------- Stick of Youth
    public static final String[] FORMS = {"club", "sword", "axe", "spear", "dagger"};
    private static final float[] FORM_SCALE = {1.0f, 1.2f, 1.4f, 1.1f, 0.85f};

    public static int nextForm(int form) { return Math.floorMod(form + 1, FORMS.length); }

    public static String formName(int form) { return FORMS[Math.floorMod(form, FORMS.length)]; }

    /** The multiplier a form puts on the blow: the sword is keen, the axe heavy, the dagger quick but light. */
    public static float formScale(int form) { return FORM_SCALE[Math.floorMod(form, FORMS.length)]; }

    /** Extra reach, in blocks, of the spear form (a stick cannot grow its reach, so the spear's blow also strikes the creature behind the target). */
    public static final double SPEAR_BEHIND = 2.0;

    // ---------------------------------------------------------------- charges (Silver-Wind 10, Sun Gift 4, Last Horseman 2)
    public static final int GLAIVE_MAX = 10, GLAIVE_TICKS_PER_CHARGE = 3200;           // 1d8 + 3 a long rest, about 7.5 a day
    public static final int SUN_MAX = 4, SUN_TICKS_PER_CHARGE = 3400;                  // 1d4 + 1 a day of sunlight (a daylight stretch is 12000 ticks)
    public static final int DEAD_MAX = 2, DEAD_TICKS_PER_CHARGE = 24000;               // one charge per long rest

    /** The state after {@code added} ticks of charging: {have, progress}. A weapon that is full keeps no progress. */
    public static int[] charge(int have, int progress, int added, int perCharge, int max) {
        have = Math.max(0, Math.min(max, have));
        if (have >= max) return new int[]{max, 0};
        long p = (long) Math.max(0, progress) + Math.max(0, added);
        long gained = p / Math.max(1, perCharge);
        long total = have + gained;
        if (total >= max) return new int[]{max, 0};
        return new int[]{(int) total, (int) (p % Math.max(1, perCharge))};
    }

    /** Can a spend of {@code cost} charges be paid? */
    public static boolean canSpend(int have, int cost) { return cost > 0 && have >= cost; }

    // ---------------------------------------------------------------- Last Horseman: damage that reduces maximum health
    public static final float WITHER_STEP = 0.04f, WITHER_CAP = 0.30f;
    public static final int WITHER_TICKS = 20 * 60 * 5;

    /** The fraction of maximum health a target has lost after {@code hits} blows: 4 percent a blow, never more than 30 percent. */
    public static float witherLoss(int hits) { return Math.min(WITHER_CAP, WITHER_STEP * Math.max(0, hits)); }

    /** How much extra necrotic damage the blow adds (1d8 on the card, 4.5 on average). */
    public static final float NECROTIC_BONUS = 4.5f;

    /** Word of Ruin: 12 plus a third of the target's maximum health (it never kills outright: the caller's cap and the target's health still apply). */
    public static float ruinDamage(float maxHealth) { return 12.0f + Math.max(0f, maxHealth) * 0.33f; }

    // ---------------------------------------------------------------- The Legacy
    public static final int HEADLESS_TICKS = 160;
    public static final float HEADLESS_SCALE = 0.4f;
    public static final double TOSS_RANGE = 36.0;                                     // 120 ft
    public static final float THUNDER_BONUS = 4.5f;                                   // 1d8 thunder

    /** The blow of a hammer that has lost its head is an improvised weapon: 40 percent. */
    public static float headlessScale(boolean headless) { return headless ? HEADLESS_SCALE : 1.0f; }

    // ---------------------------------------------------------------- Librarian's Maul
    /** "Applies Intelligence": the wielder's experience level stands for it, 0.15 a level, at most 6. */
    public static float libraryBonus(int xpLevel) { return Math.min(6.0f, Math.max(0, xpLevel) * 0.15f); }

    /** The one-handed d8 against the two-handed d12: with the off hand empty the blow is 15 percent heavier. */
    public static float handScale(boolean offHandEmpty) { return offHandEmpty ? 1.15f : 1.0f; }

    // ---------------------------------------------------------------- Time hammer
    public static final int STOP_TICKS = 100, SMITE_WINDOW = 40, SMITE_GRACE = 20;
    public static final double STOP_RADIUS = 12.0;

    /** Hammertime may be called in the last 40 ticks of the stop (and 20 ticks after it ends). */
    public static boolean smiteReady(long now, long until) { return until > 0 && now >= until - SMITE_WINDOW && now <= until + SMITE_GRACE; }

    // ---------------------------------------------------------------- Vanguard Hammer, Willbreaker: fear
    public static final double CONE_RANGE = 12.0;                                     // 40 ft
    public static final double CONE_COS = 0.5;                                        // 60 degrees either side

    /** Is the point (dx, dz) from the caster inside the cone along (lx, lz) of the given range and the given cosine of the half angle? */
    public static boolean inCone(double dx, double dz, double lx, double lz, double range, double cosHalf) {
        double d = Math.sqrt(dx * dx + dz * dz), l = Math.sqrt(lx * lx + lz * lz);
        if (d < 1.0e-6) return true;
        if (d > range || l < 1.0e-6) return false;
        return (dx * lx + dz * lz) / (d * l) >= cosHalf;
    }

    /** Fear lasts a minute on creatures, 8 seconds on players. */
    public static int fearTicks(boolean player) { return player ? 160 : 1200; }

    /** The Wisdom save: the chance a creature is frightened (creatures 75, players 40 percent, armoured by a Tensura resistance fraction 0 to 1). */
    public static double fearChance(boolean player, double resist) { return (player ? 0.40 : 0.75) * (1.0 - Math.max(0.0, Math.min(0.9, resist))); }

    // ---------------------------------------------------------------- Rift Breaker
    public static final double RIFT_RANGE = 18.0;                                      // 60 ft
    public static final int ZONE_TICKS = 120;                                          // one round
    public static final double ZONE_RADIUS = 1.6;                                      // a 10 ft circle is 5 blocks across; 3.2 keeps it a doorway
    public static final int SCATTER_MIN = 30, SCATTER_MAX = 60;

    /** Distance from a point to the segment a-b (the rift-thrown spear's line). */
    public static double distToSegment(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double vx = bx - ax, vy = by - ay, vz = bz - az, wx = px - ax, wy = py - ay, wz = pz - az;
        double l2 = vx * vx + vy * vy + vz * vz;
        double t = l2 < 1.0e-9 ? 0.0 : Math.max(0.0, Math.min(1.0, (wx * vx + wy * vy + wz * vz) / l2));
        double dx = px - (ax + vx * t), dy = py - (ay + vy * t), dz = pz - (az + vz * t);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Is a point inside the escape circle? (flat distance and a two-block height window) */
    public static boolean inZone(double px, double py, double pz, double cx, double cy, double cz) {
        double dx = px - cx, dz = pz - cz;
        return dx * dx + dz * dz <= ZONE_RADIUS * ZONE_RADIUS && Math.abs(py - cy) <= 2.0;
    }

    // ---------------------------------------------------------------- Silver-Wind Glaive
    public static final int WHIRL_TICKS = 100, WHIRL_PULSE = 10;
    public static final double WHIRL_RADIUS = 4.0;                                    // the card's 10 ft out in every direction
    public static final double GUST_RANGE = 8.0;

    /** Steel Wind Strike hits at most five creatures. */
    public static final int HURRICANE_TARGETS = 5;
}
