package com.newuniverse.nusmp.skilltree.shop;

import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Why: a player's gold has to be saved with the player, copied to the new body when they die, and visible to the client's copy of the tree, and the skill tree's data attachment
 * already does all three. The design (section 8 of {@code docs/skilltree/design.md}) therefore keeps the purse in the tree's own {@code extra("market")} sub-compound instead of
 * registering a second attachment: one save entry, one copy on death, no new registry entry to freeze, and an old save without it simply reads 0. This class is the only code that
 * touches that compound; every change goes through {@link GoldLedger}, so a purse can never go negative or overflow. It also keeps the per-player daily purchase counter in the same
 * compound (the day it was written on and how many were bought that day), because that counter belongs to the player, not to the world's shelf. Reads never create the compound,
 * so a player who has never seen the shop does not get a saved entry just because something asked for their gold. Server thread only for writes.
 */
public final class MarketGold {
    private MarketGold() {}

    /** The name of the tree's sub-compound this system owns. */
    public static final String SYSTEM = "market";
    private static final String GOLD = "gold", LIMIT_DAY = "limit_day", LIMIT_COUNT = "limit_n";

    /** The player's gold (0 for a player who has none). */
    public static long get(Player p) {
        IPlayerSkillTree tree = SkillTreeApi.of(p);
        return tree.hasExtra(SYSTEM) ? GoldLedger.clamp(tree.extra(SYSTEM).getLong(GOLD)) : 0;
    }

    /** Sets the gold (clamped to a legal purse) and returns what was stored. */
    public static long set(Player p, long gold) {
        long v = GoldLedger.clamp(gold);
        if (v == 0 && !SkillTreeApi.of(p).hasExtra(SYSTEM)) return 0;
        SkillTreeApi.of(p).extra(SYSTEM).putLong(GOLD, v);
        return v;
    }

    /** Adds gold (a negative amount removes it); returns the new purse. */
    public static long add(Player p, long amount) {
        return set(p, GoldLedger.add(get(p), amount));
    }

    /** Pays {@code price}: true and the purse is lower, or false and nothing changed. */
    public static boolean take(Player p, long price) {
        long purse = get(p);
        if (!GoldLedger.canAfford(purse, price)) return false;
        set(p, GoldLedger.take(purse, price));
        return true;
    }

    /** How many purchases the player has made on the in-game day {@code today}. */
    public static int boughtToday(Player p, long today) {
        IPlayerSkillTree tree = SkillTreeApi.of(p);
        if (!tree.hasExtra(SYSTEM)) return 0;
        CompoundTag t = tree.extra(SYSTEM);
        return ShopRules.boughtToday(t.getLong(LIMIT_DAY), t.getInt(LIMIT_COUNT), today);
    }

    /** Counts one purchase on the day {@code today} (the counter starts again on a new day). */
    public static void recordPurchase(Player p, long today) {
        CompoundTag t = SkillTreeApi.of(p).extra(SYSTEM);
        int n = ShopRules.boughtToday(t.getLong(LIMIT_DAY), t.getInt(LIMIT_COUNT), today);
        t.putLong(LIMIT_DAY, today);
        t.putInt(LIMIT_COUNT, n + 1);
    }

    /** Forgets the player's daily purchase counter (the self-test uses it to leave its stand-in player as it found him). */
    public static void clearCounter(Player p) {
        IPlayerSkillTree tree = SkillTreeApi.of(p);
        if (!tree.hasExtra(SYSTEM)) return;
        tree.extra(SYSTEM).remove(LIMIT_DAY);
        tree.extra(SYSTEM).remove(LIMIT_COUNT);
    }
}
