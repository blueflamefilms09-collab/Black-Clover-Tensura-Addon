package com.newuniverse.nusmp.dragonballs;

import java.util.List;
import java.util.Random;

/**
 * Why: the wish table and its caps, pure, so a test can prove that no wish is unbounded and that the forbidden ones are refused. Shenron grants a CURATED list, never a free-form wish: a wish is one of
 * the {@link Wish} rows, found by id (the clickable menu, {@code /dragonwish <id>}) or by a keyword in what the summoner types. Nothing here can kill, edit the world, destroy another player's items,
 * control a mind or give immortality ({@link #forbidden}). Every number is a cap: permanent power is counted in LEVELS with a lifetime ceiling per player ({@link #POWER_CAP_MAX}), coins are one fixed
 * amount per wish, items are a fixed table, skills a short list, revive only reaches players who died inside the revive window. docs/dragon_balls.md has the table.
 */
public final class WishRules {
    private WishRules() {}

    public enum Kind { REVIVE, POWER, COINS, SKILL, ITEM, WEAPON, SAIYAN }

    /** One row of the table. {@code superOnly}: only Super Shenron grants it. {@code keywords}: whole words or phrases looked for in the typed wish. */
    public record Wish(String id, Kind kind, boolean superOnly, List<String> keywords) {}

    /** The table, in the order the menu shows it and the order keywords are tried (weapon before power so "power pole" is the weapon). */
    public static final List<Wish> ALL = List.of(
            new Wish("revive", Kind.REVIVE, false, List.of("revive", "resurrect", "bring back", "back to life", "come back to life")),
            new Wish("weapon", Kind.WEAPON, true, List.of("weapon", "power pole", "ruyi", "staff")),
            new Wish("saiyan", Kind.SAIYAN, false, List.of("saiyan", "become a saiyan", "turn me into a saiyan")),
            new Wish("skill", Kind.SKILL, false, List.of("skill", "ability", "technique")),
            new Wish("coins", Kind.COINS, false, List.of("money", "coins", "coin", "zeni", "rich", "gold", "wealth")),
            new Wish("item", Kind.ITEM, false, List.of("item", "treasure", "rare item", "gift")),
            new Wish("power", Kind.POWER, false, List.of("power", "power up", "stronger", "strength", "strong")));

    public static List<Wish> forSet(BallSet set) {
        return ALL.stream().filter(w -> set == BallSet.SUPER || !w.superOnly()).toList();
    }

    public static Wish byId(String id) {
        if (id == null) return null;
        String k = DbRules.norm(id);
        for (Wish w : ALL) if (w.id().equals(k)) return w;
        return null;
    }

    /** The wish a typed sentence asks for in this set, or null. The id itself ("revive") always matches. */
    public static Wish match(BallSet set, String text) {
        String t = DbRules.norm(text);
        if (t.isEmpty()) return null;
        for (Wish w : forSet(set)) {
            if (DbRules.contains(t, w.id())) return w;
            for (String k : w.keywords()) if (DbRules.contains(t, k)) return w;
        }
        return null;
    }

    // ---------------------------------------------------------------- what is never granted
    /** Words that make a wish a refusal (checked FIRST, before any wish matches): immortality, mass death, world edits, mind control, deleting others' things. Whole words / phrases. */
    public static final List<String> FORBIDDEN = List.of(
            "immortal", "immortality", "live forever", "never die", "invincible", "invincibility",
            "kill everyone", "kill all", "everyone die", "mass kill", "genocide", "destroy", "erase", "delete", "wipe", "annihilate", "explode",
            "mind control", "control", "enslave", "brainwash", "obey", "slave",
            "world", "universe", "planet", "server", "ban", "steal", "take their items", "remove their items");

    public static boolean forbidden(String text) { return DbRules.matchesAny(text, FORBIDDEN); }

    // ---------------------------------------------------------------- caps
    /** How many wishes a set may grant at most, whatever the setting says. */
    public static final int MAX_WISHES = 3;

    public static int wishCount(BallSet set, int configured) { return Math.max(1, Math.min(MAX_WISHES, configured)); }

    /** The lifetime ceiling of permanent power, in levels (config {@code powerCapLevels}, never above this). */
    public static final int POWER_CAP_DEFAULT = 10, POWER_CAP_MAX = 20;

    /** Levels one power wish adds: Earth 1, Super 2. */
    public static int powerLevels(BallSet set) { return set == BallSet.SUPER ? 2 : 1; }

    /** One level is +2 max health (one heart) and +0.5 attack damage, so the default ceiling (10 levels) is +20 health and +5 attack. */
    public static double healthPerLevel() { return 2.0; }

    public static double attackPerLevel() { return 0.5; }

    /** The levels a wish may really add: never below 0, never past the cap. */
    public static int addable(int current, int add, int cap) {
        int c = Math.max(0, Math.min(POWER_CAP_MAX, cap));
        return Math.max(0, Math.min(add, c - Math.max(0, current)));
    }

    /** Coins of one wish, in bronze coins (CoinKind: 10,000 = one gold coin, 1,000,000 = one stellar gold coin). Earth: 20 gold coins. Super: 100 gold coins. */
    public static long coins(BallSet set) { return set == BallSet.SUPER ? 1_000_000L : 200_000L; }

    // ---------------------------------------------------------------- curated tables
    public record Gift(String id, int count, int weight) {}

    public static final List<Gift> EARTH_ITEMS = List.of(
            new Gift("minecraft:enchanted_golden_apple", 2, 4), new Gift("minecraft:totem_of_undying", 1, 4), new Gift("minecraft:diamond", 16, 4),
            new Gift("minecraft:netherite_ingot", 2, 2), new Gift("minecraft:elytra", 1, 1), new Gift("minecraft:experience_bottle", 32, 3),
            new Gift("anime_multiverse:dragon_radar", 1, 3));

    public static final List<Gift> SUPER_ITEMS = List.of(
            new Gift("minecraft:enchanted_golden_apple", 8, 4), new Gift("minecraft:totem_of_undying", 3, 4), new Gift("minecraft:diamond_block", 8, 3),
            new Gift("minecraft:netherite_ingot", 8, 3), new Gift("minecraft:nether_star", 1, 2), new Gift("minecraft:elytra", 1, 2),
            new Gift("minecraft:experience_bottle", 64, 3));

    /** The skills a wish may grant (Tensura skill ids that already exist in this mod's own relic data). Earth picks one at random; Super lets the summoner name one (a word of its id in the wish). */
    public static final List<String> EARTH_SKILLS = List.of("tensura:strengthen_body", "tensura:self_regeneration", "tensura:magic_sense", "tensura:danger_sense", "tensura:thought_acceleration", "tensura:farsight");

    public static final List<String> SUPER_SKILLS = List.of("tensura:eye_of_truth", "tensura:thought_acceleration", "tensura:magic_resistance", "tensura:abnormal_condition_resistance",
            "tensura:danger_sense", "tensura:self_regeneration", "tensura:strengthen_body", "tensura:coercion");

    /** The weapon Super Shenron grants (the Dragon Ball relic that already exists: Ruyi Power Pole, God grade), once per player. */
    public static final String WEAPON_RELIC = "ruyi_power_pole";

    public static List<Gift> items(BallSet set) { return set == BallSet.SUPER ? SUPER_ITEMS : EARTH_ITEMS; }

    public static List<String> skills(BallSet set) { return set == BallSet.SUPER ? SUPER_SKILLS : EARTH_SKILLS; }

    public static Gift pick(List<Gift> table, Random r) {
        int total = 0;
        for (Gift g : table) total += Math.max(0, g.weight());
        if (total <= 0) return table.get(0);
        int roll = r.nextInt(total);
        for (Gift g : table) {
            roll -= Math.max(0, g.weight());
            if (roll < 0) return g;
        }
        return table.get(table.size() - 1);
    }

    /** The skill of the list the typed wish names ("skill eye of truth" -> tensura:eye_of_truth), or null. */
    public static String namedSkill(List<String> list, String text) {
        for (String id : list) {
            String path = id.substring(id.indexOf(':') + 1);
            if (DbRules.contains(text, path.replace('_', ' '))) return id;
        }
        return null;
    }

    /** Human name of a skill id ("eye_of_truth" -> "Eye Of Truth"). */
    public static String skillName(String id) {
        String[] w = id.substring(id.indexOf(':') + 1).split("_");
        StringBuilder b = new StringBuilder();
        for (String s : w) if (!s.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
        return b.toString();
    }
}
