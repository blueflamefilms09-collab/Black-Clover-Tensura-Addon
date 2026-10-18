package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Why: {@code economy.currency = magic_stones} keeps the old behaviour of the dungeon (Magic Stones drop), and the Hub's shops and quests, which write every price in bronze coins, must still
 * work in that mode. This wallet is the Magic Stone side of the {@link Currency} seam: the balance is the number of Magic Stones in the inventory times {@code economy.stoneValue} bronze (default
 * 100, one silver), a charge takes the fewest whole stones that cover the price (there is no change in stones: a price is rounded UP to whole stones, never down), and a payment gives whole stones
 * (rounded down, at least one for any positive amount). The item is Tensura's own {@code tensura:magic_stone}, the one the dungeon's economy already drops; the mod's items, ids and saves are not
 * touched. Server thread only.
 */
public final class StoneWallet implements EliteHooks.Wallet {
    /** The Magic Stone's item id: the same one {@code EconomyDrops} drops. */
    public static final String STONE_ID = "tensura:magic_stone";

    static Item item() {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(STONE_ID)).orElse(null);
    }

    static long unit() { return Math.max(1, HubConfig.get(HubConfig.STONE_VALUE)); }

    /** The Magic Stones in the inventory. */
    public static long count(ServerPlayer p) {
        Item it = item();
        if (it == null) return 0L;
        long n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(it)) n += s.getCount();
        }
        return n;
    }

    /** How many whole stones a price needs (rounded up). */
    public static long stonesFor(long coins, long unit) {
        if (coins <= 0 || unit <= 0) return 0;
        return (coins + unit - 1) / unit;
    }

    @Override
    public long balance(ServerPlayer player) {
        return player == null ? 0L : CoinMath.mul(unit(), count(player));
    }

    @Override
    public boolean charge(ServerPlayer player, long coins) {
        if (player == null || coins < 0 || item() == null) return false;
        if (coins == 0) return true;
        long need = stonesFor(coins, unit());
        if (count(player) < need) return false;
        Inventory inv = player.getInventory();
        long left = need;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !s.is(item())) continue;
            int take = (int) Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
        inv.setChanged();
        return true;
    }

    @Override
    public void give(ServerPlayer player, long coins) {
        Item it = item();
        if (player == null || coins <= 0 || it == null) return;
        long stones = Math.max(1, coins / unit());
        int max = Math.max(1, new ItemStack(it).getMaxStackSize());
        stones = Math.min(stones, 1_000_000L);
        while (stones > 0) {
            ItemStack stack = new ItemStack(it, (int) Math.min(stones, max));
            stones -= stack.getCount();
            if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
        }
    }
}
