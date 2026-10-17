package com.newuniverse.nusmp.avatar.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the Avatar's looting AI makes four decisions that have to be fair and predictable, and so are kept pure and tested: may it take this item at all (never
 * something a player has just dropped, never clutter into full pockets), how much does it want it (an armour upgrade, a rare item or a stack of valuables
 * first), how far does it look (a Merchant sees rare drops from far away and walks to them before a player can), and what does it hand to its creator
 * (valuables, when asked or when its pockets are nearly full). The game-facing goal ({@link AvatarLootGoal}) only gathers the facts about each dropped item.
 */
public final class LootRules {
    private LootRules() {}

    /** The rarity ordinal of "rare" (vanilla Rarity: COMMON 0, UNCOMMON 1, RARE 2, EPIC 3). */
    public static final int RARE = 2;

    /**
     * What the goal knows about one dropped item.
     *
     * @param rarity        the item's rarity ordinal
     * @param upgrade       it would replace worn gear or a held weapon (see {@code GearScore})
     * @param valuable      it is a valuable item at any count (diamond, netherite, a totem, ...)
     * @param valuableStack it is a stack of something that is valuable in numbers (eight gold ingots)
     * @param treasure      it is a relic or a magic stone of this mod
     * @param ageTicks      how long it has lain on the ground
     * @param byPlayer      a player threw or dropped it
     * @param distance      blocks from the Avatar
     * @param grave         it lies where a player died a little while ago (their belongings: never taken)
     */
    public record Drop(int rarity, boolean upgrade, boolean valuable, boolean valuableStack, boolean treasure, int ageTicks, boolean byPlayer, double distance, boolean grave) {
        /** A drop that is not a grave item (what every drop was before graves were told apart). */
        public Drop(int rarity, boolean upgrade, boolean valuable, boolean valuableStack, boolean treasure, int ageTicks, boolean byPlayer, double distance) {
            this(rarity, upgrade, valuable, valuableStack, treasure, ageTicks, byPlayer, distance, false);
        }
    }

    /** Is the item a "rare drop" in the sense of the Merchant's greed: rarity RARE or better, a stack of valuables, a relic or a magic stone? */
    public static boolean isRare(Drop d) { return d.rarity() >= RARE || d.valuableStack() || d.treasure(); }

    /**
     * May the Avatar take it? Never an item a player threw less than {@code graceTicks} ago (it is theirs to take back), never what lies in a player's grave; and into full pockets only what is an
     * upgrade or is worth swapping for (a rare drop).
     */
    public static boolean allowed(Drop d, int graceTicks, boolean roomInPockets) {
        if (d.grave()) return false;
        if (d.byPlayer() && d.ageTicks() < Math.max(0, graceTicks)) return false;
        return roomInPockets || d.upgrade() || isRare(d);
    }

    /** How much the Avatar wants it: higher is first. Distance lowers the want; a Merchant wants rare drops two and a half times as much. */
    public static double priority(Drop d, boolean merchant) {
        double p = 1.0;
        if (d.upgrade()) p += 2.0;
        if (d.valuable()) p += 1.5;
        if (d.rarity() >= RARE) p += 1.0 + (d.rarity() - RARE);
        if (d.valuableStack()) p += 1.5;
        if (d.treasure()) p += 1.0;
        if (merchant && isRare(d)) p *= 2.5;
        return p / (1.0 + Math.max(0.0, d.distance()) * 0.08);
    }

    /** Is the item inside the distance the Avatar will go for it? A Merchant sees rare drops from {@code merchantRadius}; everyone else only from {@code base}. */
    public static boolean withinReach(Drop d, double base, double merchantRadius, boolean merchant) {
        double reach = merchant && isRare(d) ? Math.max(base, merchantRadius) : base;
        return d.distance() <= reach;
    }

    /** The speed to walk at: a Merchant runs for a rare drop, everybody else walks. */
    public static double speed(Drop d, boolean merchant) { return merchant && isRare(d) ? 1.45 : 1.1; }

    /** Index of the drop to go for (the highest priority among the allowed ones in reach), or -1. */
    public static int pick(List<Drop> drops, int graceTicks, boolean roomInPockets, double base, double merchantRadius, boolean merchant) {
        int best = -1;
        double bestP = 0.0;
        for (int i = 0; i < drops.size(); i++) {
            Drop d = drops.get(i);
            if (!allowed(d, graceTicks, roomInPockets) || !withinReach(d, base, merchantRadius, merchant)) continue;
            double p = priority(d, merchant);
            if (p > bestP) { bestP = p; best = i; }
        }
        return best;
    }

    // ---------------------------------------------------------------- handing loot over
    /** What an Avatar carries, for the hand-over decision: how many of its pockets are free, and whether anything in them is worth giving. */
    public record Pockets(int free, int size, int valuableStacks) {}

    /**
     * Does the Avatar give its valuables to its creator now? Never without the setting or when the creator is not close; on request always (if there is anything
     * to give); unasked, once the pockets are nearly full and there is something worth handing over.
     */
    public static boolean shouldHandOver(boolean enabled, boolean creatorNear, boolean requested, Pockets p) {
        if (!enabled || !creatorNear || p.valuableStacks() <= 0) return false;
        return requested || p.free() <= Math.max(1, p.size() / 6);
    }

    // ---------------------------------------------------------------- what is valuable
    private static final Set<String> VALUABLE = Set.of("diamond", "emerald", "netherite_ingot", "netherite_scrap", "ancient_debris", "nether_star",
            "enchanted_golden_apple", "totem_of_undying", "heart_of_the_sea", "echo_shard", "elytra", "diamond_block", "emerald_block", "netherite_block",
            "gold_block", "beacon", "dragon_egg", "trial_key", "ominous_trial_key", "ominous_bottle", "enchanted_book", "name_tag", "saddle", "wither_skeleton_skull");
    private static final Set<String> VALUABLE_IN_NUMBERS = Set.of("gold_ingot", "raw_gold", "amethyst_shard", "lapis_lazuli", "quartz", "blaze_rod", "ender_pearl",
            "iron_ingot", "raw_iron");

    /** Is a registry path ("diamond", "gold_ingot") valuable at any count? Mod items go through their rarity, not this list. */
    public static boolean isValuable(String path) { return path != null && VALUABLE.contains(path.toLowerCase(Locale.ROOT)); }

    /** Is a stack of {@code count} of this path a "stack of valuables" (eight or more of something that is valuable in numbers)? */
    public static boolean isValuableStack(String path, int count) {
        return path != null && count >= 8 && VALUABLE_IN_NUMBERS.contains(path.toLowerCase(Locale.ROOT));
    }

    /** The indices of the pockets worth handing over: the valuable ones and the rare ones, at most {@code limit} of them. */
    public static List<Integer> handOverIndices(List<Boolean> worthGiving, int limit) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < worthGiving.size() && out.size() < limit; i++) if (Boolean.TRUE.equals(worthGiving.get(i))) out.add(i);
        return out;
    }
}
