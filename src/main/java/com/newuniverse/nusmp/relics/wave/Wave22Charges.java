package com.newuniverse.nusmp.relics.wave;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntUnaryOperator;

/**
 * Why: the cards say "this staff has N charges, it regains some at dawn". The count, the day it was last brought up to date and the "sulking" flag
 * live in the item's custom data (so a charge cannot be duplicated by dropping or storing the item), and the dawn refill is applied lazily the first
 * time the item is used on a new Minecraft day, by the pure rule in {@link Wave22Rules#dawn}.
 * <p>
 * Wave 22: the charges of the five charged wands and staves, kept on the stack.
 */
final class Wave22Charges {
    private Wave22Charges() {}

    static final String K_CH = "nusmp_w22_ch", K_DAY = "nusmp_w22_day", K_LOCK = "nusmp_w22_lock";

    /** The store of the stack, brought up to today (and saved if it changed). Tells the player when the wand sulks. */
    static Wave22Rules.Store load(ServerPlayer p, ItemStack stack, Wave22Rules.Kind kind) {
        long today = Wave22Rules.day(p.level().getDayTime());
        Wave22Rules.Store cur;
        boolean known = Wave0Util.stackLong(stack, K_DAY, Long.MIN_VALUE) != Long.MIN_VALUE;
        if (!known) {
            cur = Wave22Rules.fresh(kind, today);
            save(stack, cur);
            return cur;
        }
        cur = new Wave22Rules.Store(Wave0Util.stackInt(stack, K_CH, kind.max()), Wave0Util.stackLong(stack, K_DAY, today), Wave0Util.stackInt(stack, K_LOCK, 0) != 0);
        IntUnaryOperator roll = n -> 1 + p.getRandom().nextInt(n);
        Wave22Rules.Dawn d = Wave22Rules.dawn(kind, cur, today, roll);
        if (d.store() != cur) {
            save(stack, d.store());
            if (d.sulked()) Wave0Util.note(p, "The wand sulks: it refills nothing this dawn.", ChatFormatting.GRAY);
            cur = d.store();
        }
        return cur;
    }

    static void save(ItemStack stack, Wave22Rules.Store s) {
        Wave0Util.edit(stack, t -> {
            t.putInt(K_CH, s.charges());
            t.putLong(K_DAY, s.day());
            t.putInt(K_LOCK, s.locked() ? 1 : 0);
        });
    }

    /**
     * Spends {@code cost} charges. False (and a line on the action bar) when there are not enough or the wand sleeps. {@code d20Last} is true for the
     * wands that risk the d20 on their last charge (Serpent Head Staff, Wand of Spider Summoning).
     */
    static boolean spend(ServerPlayer p, ItemStack stack, Wave22Rules.Kind kind, int cost, boolean d20Last) {
        Wave22Rules.Store s = load(p, stack, kind);
        if (s.locked()) {
            Wave0Util.note(p, "The wand is dormant until the next dawn.", ChatFormatting.GRAY);
            return false;
        }
        if (!Wave22Rules.canSpend(s, cost)) {
            Wave0Util.note(p, "Not enough charges (" + s.charges() + "/" + kind.max() + ", needs " + cost + ").", ChatFormatting.GRAY);
            return false;
        }
        Wave22Rules.Store next = Wave22Rules.spend(s, cost);
        if (d20Last && next.charges() == 0 && Wave22Rules.lastChargeLocks(1 + p.getRandom().nextInt(20))) {
            next = Wave22Rules.lock(next);
            Wave0Util.note(p, "The last charge fades and the wand falls dormant until the next dawn.", ChatFormatting.DARK_GRAY);
        } else {
            Wave0Util.note(p, "Charges " + next.charges() + "/" + kind.max(), ChatFormatting.AQUA);
        }
        save(stack, next);
        return true;
    }
}
