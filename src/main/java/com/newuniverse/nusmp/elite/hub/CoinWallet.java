package com.newuniverse.nusmp.elite.hub;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.api.EliteHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the owner's brief makes the Tensura coins the universal currency, and the rest of the 1.83.0 job (nations, the forge, the council) reads money through {@link EliteHooks#wallet}, so
 * somebody has to turn "the coins in a player's inventory" into one number and back. This is that class: {@code balance} counts the coins of every kind in the whole inventory (hotbar, bag, armour
 * and off-hand slots) and adds them up in bronze coins with {@link CoinMath#value}; {@code charge} asks {@link CoinMath#plan} which coins to take (the small ones first, so a saved gold coin stays)
 * and gives the change back in the best mix; {@code give} pays an amount in the best mix, into the inventory and, for what does not fit, onto the ground at the player's feet. The charge is atomic:
 * it is planned first from a count of the inventory and executed second, and nothing is taken when the player cannot pay. It needs the four Tensura coin items; if the Tensura mod is missing
 * (or has renamed one) the wallet says so ONCE in the log and answers 0 / false, so nothing is ever charged in coins that cannot be paid back as coins. Coins inside Tensura's coin pouches are
 * NOT counted (the pouch is an item with its own storage whose API this mod does not use): they have to be taken out first. Server thread only.
 */
public final class CoinWallet implements EliteHooks.Wallet {
    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    /** The item of a coin kind, or null when the Tensura mod does not have it. */
    static Item item(CoinKind kind) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(kind.itemId())).orElse(null);
    }

    /** Do all four coin items exist? Logs once when they do not. */
    public static boolean available() {
        for (CoinKind k : CoinKind.values()) {
            if (item(k) == null) {
                if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] elite hub: the coin item {} does not exist (is Tensura Reincarnated loaded?): the coin wallet is off", k.itemId());
                return false;
            }
        }
        return true;
    }

    /** The coins in the inventory, by kind (index = ordinal). */
    public static long[] count(Player player) {
        long[] out = new long[CoinKind.COUNT];
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            for (CoinKind k : CoinKind.values()) {
                Item it = item(k);
                if (it != null && s.is(it)) {
                    out[k.ordinal()] += s.getCount();
                    break;
                }
            }
        }
        return out;
    }

    @Override
    public long balance(ServerPlayer player) {
        if (player == null || !available()) return 0L;
        return CoinMath.value(count(player));
    }

    @Override
    public boolean charge(ServerPlayer player, long coins) {
        if (player == null || coins < 0 || !available()) return false;
        if (coins == 0) return true;
        CoinMath.Charge plan = CoinMath.plan(coins, count(player));
        if (plan == null) return false;
        for (CoinKind k : CoinKind.values()) remove(player, item(k), plan.take()[k.ordinal()]);
        if (plan.change() > 0) give(player, plan.change());
        player.getInventory().setChanged();
        return true;
    }

    @Override
    public void give(ServerPlayer player, long coins) {
        if (player == null || coins <= 0 || !available()) return;
        long[] mix = CoinMath.mix(coins);
        for (int k = CoinKind.COUNT - 1; k >= 0; k--) {
            for (ItemStack stack : stacks(CoinKind.values()[k], mix[k])) {
                if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
            }
        }
    }

    /** {@code n} coins of a kind as stacks that fit (empty list when the kind has no item). */
    public static List<ItemStack> stacks(CoinKind kind, long n) {
        List<ItemStack> out = new ArrayList<>();
        Item it = item(kind);
        if (it == null || n <= 0) return out;
        int max = Math.max(1, new ItemStack(it).getMaxStackSize());
        long left = Math.min(n, 1_000_000L);                        // a sane bound on stacks for an absurd amount
        while (left > 0) {
            int c = (int) Math.min(left, max);
            out.add(new ItemStack(it, c));
            left -= c;
        }
        return out;
    }

    /** The stacks that pay an amount in the best mix, biggest coin first. */
    public static List<ItemStack> stacksFor(long amount) {
        List<ItemStack> out = new ArrayList<>();
        long[] mix = CoinMath.mix(amount);
        for (int k = CoinKind.COUNT - 1; k >= 0; k--) out.addAll(stacks(CoinKind.values()[k], mix[k]));
        return out;
    }

    private static void remove(Player player, Item item, long n) {
        if (item == null || n <= 0) return;
        Inventory inv = player.getInventory();
        long left = n;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !s.is(item)) continue;
            int take = (int) Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
    }
}
