package com.newuniverse.nusmp.shishou;

import java.util.Locale;
import java.util.Set;

/**
 * Why: feeding Shishou from the inventory (2.1.0): the wielder puts a stack on the Shishou item in an inventory screen (or Shishou on a stack) and he devours it as experience, like the Djinn Seal is put on a
 * vessel. The rules are one pure place so they are tested and the client and the server can never disagree: WHAT is food ({@link #classify}: magic stones and crystals, monster drops and cores; a tool, armour,
 * a block, a container or an unlisted item is {@code NONE} and the click stays vanilla), what is refused although it looks like food ({@link Kind#VALUABLE}: rare or enchanted things), how many items one click
 * eats ({@link #take}), what they are worth ({@link #value}, the same per-stone table as Predation: a stone is worth its 魔石値 times the tier factor) and which monster a drop stands for when Skill Eater may steal
 * a skill from it ({@link #sourceId}, {@link #stealChance}). No game classes: the glue ({@code ShishouFeedService}) describes the stack as an {@link Item}.
 */
public final class ShishouFeed {
    private ShishouFeed() {}

    /** What a stack is to him: a stone (worth by the table), a drop (worth by count and rarity), a valuable he will not eat, or nothing he takes (the click stays vanilla). */
    public enum Kind { STONE, DROP, VALUABLE, NONE }

    /** What the glue knows of a stack: its id, count, rarity ordinal (0 common .. 3 epic) and the flags that make it not food. */
    public record Item(String id, int count, int rarity, boolean enchanted, boolean tool, boolean armour, boolean block, boolean container) {}

    /** The outcome of one click. */
    public enum Verdict { OK, NOT_FOOD, VALUABLE, NOT_OWNER, DISABLED, TOO_FAST, FULL_LEVEL, EMPTY }

    /** The most items one click eats (config {@code feed.feedPerClick}, never above a full stack), a right click eats one. */
    public static final int HARD_CAP = 64, PER_CLICK_DEFAULT = 64;
    /** Skill Eater on a drop: half the chance the table gives for the monster itself, and at most this many independent rolls per click (the more items, the more rolls). */
    public static final double ITEM_STEAL_SHARE = 0.5;
    public static final int STEAL_ROLLS_MAX = 4;
    /** The tier of monster a fed stone is valued as (a common monster: factor 1.5). */
    public static final int STONE_TIER = 1;

    private static final Set<String> DROP_IDS = Set.of("rotten_flesh", "bone", "string", "spider_eye", "gunpowder", "slime_ball", "ender_pearl", "blaze_rod", "blaze_powder", "ghast_tear",
            "phantom_membrane", "prismarine_shard", "prismarine_crystals", "magma_cream", "leather", "feather", "ink_sac", "glow_ink_sac", "rabbit_hide", "rabbit_foot", "scute", "arrow", "breeze_rod",
            "poisonous_potato", "porkchop", "beef", "chicken", "mutton", "rabbit", "cod", "salmon");
    private static final String[] DROP_WORDS = {"_core", "_scale", "_horn", "_fang", "_claw", "_hide", "_tooth", "_tusk", "_bone", "_flesh", "_shell", "_feather", "_venom", "_silk", "_fur", "_blood", "_ichor", "_gland"};
    private static final String[] NOT_FOOD_WORDS = {"sword", "axe", "pickaxe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots", "shield", "bow", "crossbow", "trident", "spear", "staff", "wand", "katana", "armor", "armour"};

    /** The path of an item id (without the namespace), lower case. */
    public static String path(String id) {
        if (id == null) return "";
        String p = id.toLowerCase(Locale.ROOT);
        int c = p.indexOf(':');
        return c >= 0 ? p.substring(c + 1) : p;
    }

    /** Is it Shishou himself or something that must never be eaten (a spawn egg, a bucket of a creature...)? Only by id, as the pure rule. */
    public static boolean never(String id) {
        String p = path(id);
        return p.equals("shishou") || p.endsWith("_spawn_egg") || p.endsWith("_bucket") || p.contains("shulker_box") || p.equals("bundle");
    }

    /** What the stack is to him. {@code NONE} means the click is left to the game. */
    public static Kind classify(Item it) {
        if (it == null || it.count() <= 0 || it.id() == null || it.id().isEmpty()) return Kind.NONE;
        if (it.tool() || it.armour() || it.block() || it.container() || never(it.id())) return Kind.NONE;
        String p = path(it.id());
        for (String w : NOT_FOOD_WORDS) if (p.endsWith(w) || p.contains(w + "_")) return Kind.NONE;
        boolean stone = ShishouPredation.stoneBase(it.id()) > 0;
        boolean drop = DROP_IDS.contains(p);
        if (!drop && !stone) for (String w : DROP_WORDS) if (p.contains(w)) {
            drop = true;
            break;
        }
        if (!stone && !drop) return Kind.NONE;
        if (it.enchanted() || it.rarity() >= 2) return Kind.VALUABLE;                          // a rare or enchanted thing is not eaten
        return stone ? Kind.STONE : Kind.DROP;
    }

    /** Will the click be taken over (client and server alike)? Only for the wielder (the stack's owner is the player) and only for a stack that is food or looks like it. */
    public static boolean takesOver(Item it, boolean ownerMatches) { return ownerMatches && classify(it) != Kind.NONE; }

    /** How many items one click eats: a right click ({@code whole = false}) one, a left click the whole stack, never above the per-click cap and a full stack. */
    public static int take(int count, boolean whole, int perClickCap) {
        if (count <= 0) return 0;
        return whole ? Math.min(count, Math.max(1, Math.min(HARD_CAP, perClickCap))) : 1;
    }

    /** The experience of {@code n} items of this kind: stones by their table value and the stone tier, drops by count and rarity. */
    public static long value(Item it, int n) {
        if (n <= 0) return 0L;
        Kind k = classify(it);
        if (k == Kind.STONE) return ShishouPredation.stoneExp(it.id(), n, STONE_TIER);
        if (k == Kind.DROP) return ShishouPredation.dropExp(n, it.rarity());
        return 0L;
    }

    /** The decision of one click, in the order the glue checks: nothing there, not food, a valuable, not the wielder, switched off, too fast, a sword at the cap. */
    public static Verdict check(Item it, boolean isOwner, boolean enabled, boolean rateOk, boolean atCap) {
        if (it == null || it.count() <= 0) return Verdict.EMPTY;
        Kind k = classify(it);
        if (k == Kind.NONE) return Verdict.NOT_FOOD;
        if (!isOwner) return Verdict.NOT_OWNER;
        if (!enabled) return Verdict.DISABLED;
        if (k == Kind.VALUABLE) return Verdict.VALUABLE;
        if (!rateOk) return Verdict.TOO_FAST;
        if (atCap) return Verdict.FULL_LEVEL;
        return Verdict.OK;
    }

    // ---------------------------------------------------------------- Skill Eater on items
    /**
     * The monster id a drop stands for when the steal table is asked (the table is keyed by monster): a blaze rod is a blaze, an ender pearl an enderman, a ghast tear a ghast, prismarine a guardian...
     * Any other item uses its own id (so a Tensura {@code flame_wolf_fang} hits the "flame" word row). A stone never maps (a stone has no species): empty. Bounded: a handful of equals and no scan of the table.
     */
    public static String sourceId(String itemId) {
        String p = path(itemId);
        if (ShishouPredation.stoneBase(itemId) > 0) return "";
        return switch (p) {
            case "blaze_rod", "blaze_powder" -> "minecraft:blaze";
            case "magma_cream" -> "minecraft:magma_cube";
            case "ghast_tear" -> "minecraft:ghast";
            case "prismarine_shard", "prismarine_crystals" -> "minecraft:guardian";
            case "ender_pearl" -> "minecraft:enderman";
            case "spider_eye", "string" -> "minecraft:spider";
            case "phantom_membrane" -> "minecraft:phantom";
            default -> itemId == null ? "" : itemId;
        };
    }

    /** The chance (0..1) that feeding {@code n} items steals from the row: half the table's chance per roll, up to {@link #STEAL_ROLLS_MAX} rolls (so a stack of 64 is four rolls, not 64). */
    public static double stealChance(ShishouStealTable.Row row, int scalePercent, int n) {
        if (row == null || n <= 0) return 0.0;
        double one = ShishouStealTable.chance(row, scalePercent) * ITEM_STEAL_SHARE;
        int rolls = Math.min(STEAL_ROLLS_MAX, n);
        return 1.0 - Math.pow(1.0 - one, rolls);
    }

    /** The rolls a click makes (for the glue's loop). */
    public static int stealRolls(int n) { return Math.max(0, Math.min(STEAL_ROLLS_MAX, n)); }
}
