package com.newuniverse.nusmp.dungeon.nexus.relic;

import java.util.Map;

/**
 * What the Glutton's Chalice makes of the Dungeon's raw monster meat (docs/living_dungeon.md section 7): the sickness is gone, the meat
 * becomes a rich stew (immense saturation) and the wearer breathes the Dungeon's bad air for a few minutes. Pure.
 */
public final class DigestTable {
    private DigestTable() {}

    /** Nutrition of each raw cut of the Dungeon (DungeonFood), by item path; anything else that counts as raw meat is worth {@link #OTHER}. */
    private static final Map<String, Integer> NUTRITION = Map.of(
            "raw_slime_jelly", 2, "raw_armor_flank", 4, "raw_basilisk_fillet", 5, "raw_direwolf_haunch", 5, "raw_monster_meat", 3, "ruined_meat", 1);
    public static final int OTHER = 3;

    /** Food points of a full stomach, and the longest the immunity can be stacked to (ticks). */
    public static final int FOOD_FULL = 20;
    public static final long IMMUNITY_CAP = 12000L;
    public static final int IMMUNITY_BASE = 1800, IMMUNITY_PER_POINT = 300;
    /** A shut-out of the bad air is re-lent this many ticks ahead each second, so it ends within moments of the chalice coming off. */
    public static final int WARD_AHEAD = 40;

    public static int nutrition(String itemPath) { return NUTRITION.getOrDefault(itemPath, OTHER); }

    /** Food points the stew adds: double the meat's nutrition. */
    public static int foodGain(int nutrition) { return Math.max(0, nutrition) * 2; }

    /** The food level after the meal, capped at a full stomach. */
    public static int foodAfter(int food, int nutrition) { return Math.min(FOOD_FULL, Math.max(0, food) + foodGain(nutrition)); }

    /** Ticks of immunity one meal buys. */
    public static int immunityTicks(int nutrition) { return IMMUNITY_BASE + IMMUNITY_PER_POINT * Math.max(0, nutrition); }

    /** The end of the immunity after a meal at {@code now}: added on to what is left, never more than the cap from now. */
    public static long extend(long now, long until, int nutrition) {
        long base = until > now ? until : now;
        return Math.min(now + IMMUNITY_CAP, base + immunityTicks(nutrition));
    }

    public static boolean active(long now, long until) { return now < until && until - now <= IMMUNITY_CAP; }
}
