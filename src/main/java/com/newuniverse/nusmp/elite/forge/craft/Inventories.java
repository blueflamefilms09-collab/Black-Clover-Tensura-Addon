package com.newuniverse.nusmp.elite.forge.craft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * Why: the forge counts and takes materials from the player's main inventory and off hand (never the armour slots), by item id. Counting and taking are kept in one
 * small class so that "have enough" and "take" can never disagree, and a recipe is paid in one all-or-nothing step ({@link #takeAll}).
 */
public final class Inventories {
    private Inventories() {}

    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ITEM.containsKey(rl) ? null : BuiltInRegistries.ITEM.get(rl);
    }

    private static void each(Inventory inv, Consumer<ItemStack> f) {
        for (ItemStack s : inv.items) f.accept(s);
        for (ItemStack s : inv.offhand) f.accept(s);
    }

    /** How many of the item the player holds (main inventory and off hand). */
    public static int count(Player player, Item item) {
        int[] n = {0};
        each(player.getInventory(), s -> {
            if (!s.isEmpty() && s.is(item)) n[0] += s.getCount();
        });
        return n[0];
    }

    /** Takes {@code count} of the item; the caller has checked {@link #count}. Returns false (and takes nothing) when there is not enough. */
    public static boolean take(Player player, Item item, int count) {
        if (count(player, item) < count) return false;
        int[] left = {count};
        each(player.getInventory(), s -> {
            if (left[0] > 0 && !s.isEmpty() && s.is(item)) {
                int t = Math.min(left[0], s.getCount());
                s.shrink(t);
                left[0] -= t;
            }
        });
        return left[0] == 0;
    }

    /** One row of a payment. */
    public record Row(Item item, int count) {}

    /** True when the player has every row. */
    public static boolean hasAll(Player player, List<Row> rows) {
        for (Row r : rows) if (count(player, r.item()) < r.count()) return false;
        return true;
    }

    /** Takes every row or nothing. */
    public static boolean takeAll(Player player, List<Row> rows) {
        if (!hasAll(player, rows)) return false;
        for (Row r : rows) take(player, r.item(), r.count());
        player.getInventory().setChanged();
        return true;
    }
}
