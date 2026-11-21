package com.newuniverse.nusmp.dragonballs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Why: the wish table and its caps, pure, so a test can prove that no wish is unbounded and that the forbidden ones are refused. Shenron grants a CURATED list, never a free-form wish: a wish is one of
 * the {@link Wish} rows, found by id (the clickable menu, {@code /dragonwish <id>}) or by a keyword in what the summoner types. Nothing here can kill, edit the world, destroy another player's items,
 * control a mind or give immortality ({@link #forbidden}). Every number is a cap: permanent power is counted in LEVELS with a lifetime ceiling per player ({@link #POWER_CAP_MAX}), coins are one fixed
 * amount per wish, items are a fixed table, skills a short list, revive only reaches players who died inside the revive window. docs/dragon_balls.md has the table.
 */
public final class WishRules {
    private WishRules() {}

    public enum Kind { REVIVE, POWER, COINS, SKILL, ITEM, WEAPON, SAIYAN, RACE, RANK }

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

    // ================================================================ OVERPOWERED (dragonBalls.overpowered): everything below is only used while that setting is on; the tables above are the old mode, unchanged
    /** The two wishes only the overpowered mode has (appended; the old table {@link #ALL} is untouched). */
    public static final List<Wish> OVERPOWERED_ONLY = List.of(
            new Wish("rank", Kind.RANK, false, List.of("rank", "rate", "promotion", "promote", "promoted", "knight", "stage")),
            new Wish("race", Kind.RACE, false, List.of("race", "become a", "become an", "become the", "turn me into", "turn into", "transform into", "reincarnate as", "evolve into", "evolve")));

    private static final List<String> OVERPOWERED_ORDER = List.of("revive", "weapon", "saiyan", "rank", "race", "skill", "coins", "item", "power");

    /** The table of a set. {@code overpowered} false is exactly {@link #forSet(BallSet)}; true adds Rank and Race and opens the weapon to both dragons. */
    public static List<Wish> forSet(BallSet set, boolean overpowered) {
        if (!overpowered) return forSet(set);
        List<Wish> out = new ArrayList<>();
        for (String id : OVERPOWERED_ORDER) {
            Wish w = byId(id, true);
            if (w != null) out.add(w);
        }
        return out;
    }

    public static Wish byId(String id, boolean overpowered) {
        Wish w = byId(id);
        if (w != null || !overpowered || id == null) return w;
        String k = DbRules.norm(id);
        for (Wish x : OVERPOWERED_ONLY) if (x.id().equals(k)) return x;
        return null;
    }

    /** The wish a typed sentence asks for; with {@code overpowered} false exactly {@link #match(BallSet, String)}. */
    public static Wish match(BallSet set, String text, boolean overpowered) {
        if (!overpowered) return match(set, text);
        String t = DbRules.norm(text);
        if (t.isEmpty()) return null;
        for (Wish w : forSet(set, true)) {
            if (DbRules.contains(t, w.id())) return w;
            for (String k : w.keywords()) if (DbRules.contains(t, k)) return w;
        }
        return null;
    }

    /** Wishes per summoning: at most 3 in the old mode, at most 10 while overpowered. */
    public static final int MAX_WISHES_OVERPOWERED = 10;

    public static int wishCount(BallSet set, int configured, boolean overpowered) {
        return overpowered ? Math.max(1, Math.min(MAX_WISHES_OVERPOWERED, configured)) : wishCount(set, configured);
    }

    /** Levels of power one wish adds: old mode Earth 1 / Super 2, overpowered Earth 2 / Super 4. */
    public static int powerLevels(BallSet set, boolean overpowered) { return overpowered ? powerLevels(set) * 2 : powerLevels(set); }

    /** The lifetime power ceiling: the configured one in the old mode, the hard maximum while overpowered. */
    public static int powerCap(int configured, boolean overpowered) { return overpowered ? POWER_CAP_MAX : Math.max(0, Math.min(POWER_CAP_MAX, configured)); }

    /** Coins of one wish in bronze: overpowered Earth 100 gold coins, Super 1000 gold coins (a gold coin is 10,000 bronze). */
    public static long coins(BallSet set, boolean overpowered) { return overpowered ? (set == BallSet.SUPER ? 10_000_000L : 1_000_000L) : coins(set); }

    /** Is a player who died {@code ageMs} ago inside the revive window? Overpowered: always (only the 256 recorded deaths limit it). */
    public static boolean reviveInWindow(boolean overpowered, long ageMs, int reviveMinutes) { return overpowered || (ageMs >= 0 && ageMs <= reviveMinutes * 60_000L); }

    // ---------------------------------------------------------------- the anime scatter
    /** Scatter the whole set now (at the first granted wish) only while overpowered, only after a wish was really granted, and only once per ritual. */
    public static boolean scatterNow(boolean overpowered, int wishesGranted, boolean alreadyScattered) { return overpowered && wishesGranted > 0 && !alreadyScattered; }

    /** Particles of the rising flash per ball and per set (the cap the self-test and the code share). */
    public static final int FLASH_PER_BALL = 16;

    public static int flashPerSet(int balls) { return Math.max(0, Math.min(DbRules.MAX_COUNT, balls)) * FLASH_PER_BALL; }

    // ---------------------------------------------------------------- the weapon (any relic up to God grade, once a week)
    public static final long WEEK_MS = 7L * 24 * 3600 * 1000;

    /** May the player wish for a weapon again? {@code lastMs} 0 = never. */
    public static boolean weaponReady(long lastMs, long nowMs) { return lastMs <= 0 || nowMs - lastMs >= WEEK_MS || nowMs < lastMs; }

    public static long weaponWaitMs(long lastMs, long nowMs) { return weaponReady(lastMs, nowMs) ? 0 : lastMs + WEEK_MS - nowMs; }

    /** Is a relic of this grade ordinal (Normal 0 .. God 5, Genesis 6, Akashic 7) grantable? Up to God; Genesis and Akashic only with the setting. */
    public static boolean relicGradeAllowed(int gradeOrdinal, boolean genesisRelics) { return gradeOrdinal <= 5 || genesisRelics; }

    // ---------------------------------------------------------------- the item wish (any registry item, one stack, never an operator item)
    private static final Set<String> ITEM_BLOCK = Set.of("minecraft:air", "minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block", "minecraft:command_block_minecart",
            "minecraft:barrier", "minecraft:light", "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw", "minecraft:debug_stick", "minecraft:knowledge_book", "minecraft:bedrock",
            "minecraft:end_portal_frame", "minecraft:reinforced_deepslate", "minecraft:spawner", "minecraft:trial_spawner", "minecraft:vault", "minecraft:test_block", "minecraft:test_instance_block",
            "minecraft:budding_amethyst", "minecraft:petrified_oak_slab", "minecraft:farmland", "minecraft:dirt_path", "minecraft:frogspawn", "minecraft:player_head", "minecraft:written_book");

    private static final List<String> ITEM_BLOCK_WORDS = List.of("command_block", "structure_block", "creative", "admin", "debug", "operator", "cheat", "dragon_ball_", "_spawn_egg", "spawn_egg_");

    /** True when the id is never given by an item wish (operator items, creative-only items, spawn eggs, the Dragon Balls themselves). A runtime check adds the operator tag and the relics ({@link DbWishesPlus}). */
    public static boolean itemBlocked(String id) {
        if (id == null || id.isBlank() || !id.contains(":")) return true;
        String k = id.toLowerCase(java.util.Locale.ROOT);
        if (ITEM_BLOCK.contains(k)) return true;
        for (String w : ITEM_BLOCK_WORDS) if (k.contains(w)) return true;
        return false;
    }

    /** One stack: the number asked for (0 = not said, the whole stack), at most 64 and at most the item's own stack size. */
    public static int itemCount(int requested, int maxStack) {
        int cap = Math.max(1, Math.min(64, maxStack));
        return requested <= 0 ? cap : Math.min(requested, cap);
    }

    /** The first whole number (up to 5 characters) in a sentence ("64 diamonds", "a stack of 10 apples"), or 0. */
    public static int numberIn(String text) {
        String t = DbRules.norm(text);
        for (String w : t.split(" ")) {
            if (w.isEmpty() || w.length() > 5 || !w.chars().allMatch(Character::isDigit)) continue;
            return Integer.parseInt(w);
        }
        return 0;
    }

    // ---------------------------------------------------------------- the rank wish (Magic Knight rank)
    /** Highest step a rank wish reaches (Senior 1st = 14 for Earth, Grand Magic Knight = 15 for Super; never the Wizard King, 16). */
    public static int rankCeiling(BallSet set) { return set == BallSet.SUPER ? 15 : 14; }

    /** Steps one rank wish climbs when no rank is named: Earth one full class group (5), Super all the way to its ceiling. */
    public static int rankSteps(BallSet set) { return set == BallSet.SUPER ? 16 : 5; }

    /** The step a rank wish ends on, or -1 when nothing would change (already at or above the ceiling or the named rank). {@code named} -1 = none named. Never lowers, never passes the ceiling. */
    public static int rankTarget(BallSet set, int current, int named) {
        int ceil = rankCeiling(set), cur = Math.max(0, current);
        int target = named >= 0 ? Math.min(named, ceil) : Math.min(cur + rankSteps(set), ceil);
        return target > cur ? target : -1;
    }

    // ---------------------------------------------------------------- the runtime lists (filled by WishCatalog; empty in a unit test)
    public static volatile Supplier<List<WishNames.Entry>> races = List::of, skills = List::of, items = List::of, relics = List::of;

    /**
     * The single words the voice recogniser should know: every keyword and id of the wishes, then the words of the race, skill, item and relic names that are in the game right now (read from the live lists),
     * lower case, de-duplicated, at most 2000. Wish words come first so a cap never cuts them.
     */
    public static List<String> spokenVocabulary() {
        LinkedHashSet<String> v = new LinkedHashSet<>();
        List<Wish> all = new ArrayList<>(ALL);
        all.addAll(OVERPOWERED_ONLY);
        for (Wish w : all) {
            v.addAll(WishNames.words(w.id()));
            for (String k : w.keywords()) v.addAll(WishNames.words(k));
        }
        v.addAll(List.of("cancel", "never", "mind", "farewell", "goodbye", "please"));
        int limit = 2000;
        for (Supplier<List<WishNames.Entry>> src : List.of(races, skills, items, relics)) {
            try {
                WishNames.addWords(src.get(), v, limit);
            } catch (RuntimeException ignored) {
                // a list that is not ready yet adds nothing
            }
        }
        List<String> out = new ArrayList<>(v);
        return out.size() <= limit ? out : new ArrayList<>(out.subList(0, limit));
    }
}
