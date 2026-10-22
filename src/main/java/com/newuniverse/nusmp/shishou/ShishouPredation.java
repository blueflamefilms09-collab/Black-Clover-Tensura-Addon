package com.newuniverse.nusmp.shishou;

import java.util.Locale;

/**
 * Why: Skill Eater turns what a monster leaves into experience. The numbers are one pure place: the experience of the kill itself (from the monster's health), the tier of the monster (so a magic stone from a
 * strong monster is worth more than one from a weak monster: in the novel each absorbed magic stone raises the sword's magic-stone value by an amount that depends on the stone), the value of each kind of
 * stone, and the cap of one kill. No game classes.
 */
public final class ShishouPredation {
    private ShishouPredation() {}

    public static final long MAX_EXP_PER_KILL = 3000L, MAX_EXP_PER_ABSORB = 3000L;
    /** The 2.0.0 caps, for the before / after table. */
    public static final long LEGACY_MAX_EXP = 2000L;

    /** The tier of a monster by its maximum health: 0 weak (under 20), 1 common (under 50), 2 strong (under 150), 3 elite (under 400), 4 boss. */
    public static int tier(double maxHealth) {
        if (maxHealth < 20.0) return 0;
        if (maxHealth < 50.0) return 1;
        if (maxHealth < 150.0) return 2;
        if (maxHealth < 400.0) return 3;
        return 4;
    }

    private static final double[] TIER_STONE_FACTOR = {1.0, 1.5, 2.5, 4.0, 7.0};

    /** The experience of the kill: 5 + 1.2 per maximum health, up to 600 for one monster (a boss counts double, still below the cap per kill). 2.0.0: 3 + 0.8 per health, up to 400. A zombie (20 health) is worth 29. */
    public static long killExp(double maxHealth, boolean boss) {
        double e = 5.0 + 1.2 * Math.max(0.0, maxHealth);
        e = Math.min(600.0, e) * (boss ? 2.0 : 1.0);
        return Math.min(MAX_EXP_PER_KILL, Math.round(e));
    }

    /** The base value of one stone or crystal by the path of its item id (0 when it is not one): magic_stone 8, low crystal 10, medium 30, high 90 (one and a half times the 2.0.0 values 5, 6, 20, 60). */
    public static int stoneBase(String itemId) {
        if (itemId == null) return 0;
        String p = itemId.toLowerCase(Locale.ROOT);
        int colon = p.indexOf(':');
        if (colon >= 0) p = p.substring(colon + 1);
        return switch (p) {
            case "magic_stone" -> 8;
            case "low_quality_magic_crystal" -> 10;
            case "medium_quality_magic_crystal" -> 30;
            case "high_quality_magic_crystal" -> 90;
            default -> p.contains("magic_crystal") ? 18 : p.contains("magic_stone") ? 8 : 0;
        };
    }

    /** The experience of {@code count} stones of a kind taken from a monster of {@code mobTier}: the base times the tier factor (1, 1.5, 2.5, 4, 7). */
    public static long stoneExp(String itemId, int count, int mobTier) {
        int base = stoneBase(itemId);
        if (base <= 0 || count <= 0) return 0L;
        double f = TIER_STONE_FACTOR[Math.max(0, Math.min(TIER_STONE_FACTOR.length - 1, mobTier))];
        return Math.min(MAX_EXP_PER_ABSORB, Math.round(base * f * count));
    }

    /** The experience of an ordinary drop: two per item plus three per rarity step (0 common, 1 uncommon, 2 rare, 3 epic). */
    public static long dropExp(int count, int rarityOrdinal) {
        if (count <= 0) return 0L;
        return Math.min(MAX_EXP_PER_ABSORB, (long) count * (2 + 3L * Math.max(0, Math.min(3, rarityOrdinal))));
    }

    /** A kill's total, never above the per-kill cap. */
    public static long capKill(long total) { return Math.max(0L, Math.min(MAX_EXP_PER_KILL, total)); }
}
