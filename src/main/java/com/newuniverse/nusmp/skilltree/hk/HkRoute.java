package com.newuniverse.nusmp.skilltree.hk;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Why: the four routes of The Exiled Heavy Knight (Oath of the Heavy Armor, Basic Swordsmanship, Lady Luck, Smoldering Fang of Insanity) are named in three places that must never disagree (the JSON file, the ManasCore
 * skill ids it points at, and the passives that look a node up by id), and a typo in any of them would make a technique silently uncastable or a passive silently dead. So every id is
 * spelled once, here, as a constant that the skills, the passives and the self-test all read. The class ids are the {@code class_id} of the files: the single-active-route rule keys on
 * them, so a player who equips Lady Luck gets nothing from the Oath and the Swordsmanship. The fallback table holds the level-1 value of every number a technique reads from its file; the
 * file is the truth (a datapack can retune it) and the table only keeps a technique from throwing when a key is missing. It is plain data with no Minecraft type, so the pure tests can read it.
 */
public final class HkRoute {
    private HkRoute() {}

    /** The franchise tag of all three routes (the {@code magic} field of the files; the screen groups the routes by it). */
    public static final String FRANCHISE = "exiled_heavy_knight";

    public static final String OATH = "nusmp:oath_of_the_heavy_armor", SWORD = "nusmp:basic_swordsmanship", LUCK = "nusmp:lady_luck", FANG = "nusmp:smoldering_fang_of_insanity";
    public static final String OATH_CLASS = "heavy_armor", SWORD_CLASS = "swordsmanship", LUCK_CLASS = "lady_luck", FANG_CLASS = "smoldering_fang";

    // Oath of the Heavy Armor
    public static final String PROVOKE = "provoke", IRON_HIDE = "iron_hide", SHIELD_WALL = "shield_wall", BULWARK = "bulwark", SHIELD_BASH = "shield_bash", UNYIELDING = "unyielding";
    // Basic Swordsmanship
    public static final String POWER_STRIKE = "power_strike", SWORD_MASTERY = "sword_mastery", SWIFT_STEP = "swift_step", CRESCENT_CUT = "crescent_cut", RIPOSTE = "riposte";
    // Lady Luck
    public static final String FORTUNES_FAVOR = "fortunes_favor", CATS_PAW = "cats_paw", LUCKY_STRIKE = "lucky_strike", GAMBLERS_COIN = "gamblers_coin", TREASURE_NOSE = "treasure_nose", JACKPOT = "jackpot";

    // Smoldering Fang of Insanity (a berserker's fang, a book of its own)
    public static final String EMBER_BITE = "ember_bite", RABID_FANGS = "rabid_fangs", ASH_SKIN = "ash_skin", INSANITY = "insanity", HOWL_OF_RUIN = "howl_of_ruin", FANG_OF_INSANITY = "fang_of_insanity";

    /** The ManasCore skill ids (registry path under {@code nusmp}). */
    public static final String SKILL_PROVOKE = "hk_provoke", SKILL_SHIELD_WALL = "hk_shield_wall", SKILL_SHIELD_BASH = "hk_shield_bash", SKILL_POWER_STRIKE = "hk_power_strike",
            SKILL_SWIFT_STEP = "hk_swift_step", SKILL_CRESCENT_CUT = "hk_crescent_cut", SKILL_GAMBLERS_COIN = "hk_gamblers_coin", SKILL_JACKPOT = "hk_jackpot",
            SKILL_EMBER_BITE = "hk_ember_bite", SKILL_INSANITY = "hk_insanity", SKILL_HOWL_OF_RUIN = "hk_howl_of_ruin", SKILL_FANG_OF_INSANITY = "hk_fang_of_insanity";

    /** The passive ids the files list under {@code passives}. */
    public static final Set<String> PASSIVES = Set.of("hk_iron_hide", "hk_bulwark", "hk_unyielding", "hk_sword_mastery", "hk_riposte", "hk_fortunes_favor", "hk_cats_paw", "hk_lucky_strike", "hk_treasure_nose", "hk_rabid_fangs", "hk_ash_skin");

    /** Effect colours, 0xAARRGGBB (for {@code VfxSpawn}). */
    public static final int STEEL = 0xFF8E9AAF, BLADE = 0xFFC9D2E0, GOLD = 0xFFF2C94C, EMBER = 0xFFB3341F, ASH = 0xFF4A4452;

    private static final Map<String, Map<String, Double>> FALLBACK = new HashMap<>();

    private static void node(String id, Object... keyValue) {
        Map<String, Double> m = new HashMap<>();
        for (int i = 0; i + 1 < keyValue.length; i += 2) m.put((String) keyValue[i], ((Number) keyValue[i + 1]).doubleValue());
        FALLBACK.put(id, m);
    }

    static {
        node(PROVOKE, "radius", 8, "targets", 4, "resist_seconds", 3, "cost", 0.02, "cooldown", 200);
        node(IRON_HIDE, "armor", 2, "knockback_resist", 0.1);
        node(SHIELD_WALL, "seconds", 6, "resist_amp", 1, "absorb_hearts", 4, "cost", 0.03, "cooldown", 500);
        node(BULWARK, "reduction", 0.2);
        node(SHIELD_BASH, "damage", 5, "range", 3, "half_angle", 35, "knockback", 0.9, "slow_seconds", 2, "targets", 4, "cost", 0.02, "cooldown", 120);
        node(UNYIELDING, "absorb_hearts", 4, "seconds", 5, "rest", 2400);
        node(POWER_STRIKE, "damage", 5, "reach", 3.5, "knockback", 0.4, "cost", 0.015, "cooldown", 80);
        node(SWORD_MASTERY, "damage_bonus", 0.5);
        node(SWIFT_STEP, "distance", 5, "resist_ticks", 20, "cost", 0.015, "cooldown", 100);
        node(CRESCENT_CUT, "damage", 5, "range", 4, "half_angle", 50, "knockback", 0.5, "targets", 5, "cost", 0.02, "cooldown", 140);
        node(RIPOSTE, "bonus", 0.3, "window", 40);
        node(FORTUNES_FAVOR, "luck", 1);
        node(CATS_PAW, "chance", 0.1, "luck_bonus", 0.01, "rest", 600);
        node(LUCKY_STRIKE, "chance", 0.05, "luck_bonus", 0.01);
        node(GAMBLERS_COIN, "seconds", 12, "heads_chance", 0.55, "cost", 0.015, "cooldown", 600);
        node(TREASURE_NOSE, "chance", 0.1);
        node(JACKPOT, "emeralds", 2, "xp", 30, "cost", 0.04, "cooldown", 3600);
        node(EMBER_BITE, "damage", 5, "range", 3, "wither_seconds", 3, "life_steal", 0.1, "cost", 0.012, "cooldown", 80);
        node(RABID_FANGS, "bonus", 0.15);
        node(ASH_SKIN, "chance", 0.2, "seconds", 3, "rest", 100);
        node(INSANITY, "seconds", 8, "strength_amp", 0, "speed_amp", 0, "vulnerability", 0.25, "cost", 0.02, "cooldown", 600);
        node(HOWL_OF_RUIN, "radius", 6, "targets", 4, "weak_seconds", 6, "slow_seconds", 3, "knockback", 0.6, "cost", 0.02, "cooldown", 300);
        node(FANG_OF_INSANITY, "damage", 18, "reach", 6, "wither_seconds", 6, "heal_pct", 0.35, "exhaust_seconds", 4, "cost", 0.04, "cooldown", 3600);
    }

    /** The shipped level-1 value of a node's table (0 when unknown). */
    public static double fallback(String nodeId, String key) {
        Map<String, Double> m = FALLBACK.get(nodeId);
        Double v = m == null ? null : m.get(key);
        return v == null ? 0.0 : v;
    }
}
