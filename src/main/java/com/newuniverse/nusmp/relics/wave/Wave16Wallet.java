package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * Wave 16: the Wallet of the Slayer. A creature the wearer's own hand kills pays Challenge Rating x 2d6 "gold pieces" into the wallet's purse (a
 * number on the wallet itself); the purse is paid out as Tensura's coins (a gold coin is a hundred of them, a silver coin one; gold nuggets if Tensura is
 * absent) or spent on a blood tithe that heals. Players, summons and pets never pay, so there is nothing to farm.
 */
final class Wave16Wallet {
    private Wave16Wallet() {}

    static int purse(ItemStack s) { return Wave6Kit.tag(s).getInt(Wave16Tune.S_WALLET); }

    private static void setPurse(ItemStack s, int gp) {
        int v = Math.max(0, Math.min(Wave16Tune.WALLET_CAP, gp));
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt(Wave16Tune.S_WALLET, v));
    }

    /** The kill: the wearer's blow felled {@code victim}. */
    static void reward(ServerPlayer p, LivingEntity victim) {
        if (victim instanceof Player || victim.getPersistentData().hasUUID(Wave16Tune.OWNER) || Wave16Kit.innocent(victim)) return;
        ItemStack s = Wave6Kit.find(p, Wave16Tune.ID_WALLET);
        if (s.isEmpty()) return;
        int gp = Wave16Rules.gp(victim.getMaxHealth(), p.getRandom().nextInt(6) + 1, p.getRandom().nextInt(6) + 1);
        setPurse(s, purse(s) + gp);
        Wave6Kit.hint(p, "+" + gp + " gp  (purse " + purse(s) + ")");
    }

    /** Count Coin: the purse is paid out in coins, at most 64 of each kind a time. */
    static void withdraw(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave6Kit.find(p, Wave16Tune.ID_WALLET);
        if (s.isEmpty()) s = c.stack();
        int gp = purse(s);
        if (gp <= 0) { Fx.say(p, "The purse is empty. It fills with the lives you take."); return; }
        Optional<Item> gold = Wave16Kit.item("tensura:gold_coin"), silver = Wave16Kit.item("tensura:silver_coin");
        int paid = 0;
        if (gold.isPresent() && silver.isPresent()) {
            int g = Math.min(64, Wave16Rules.goldCoins(gp));
            int sv = Math.min(64, gp - g * Wave16Tune.GOLD_COIN_GP);
            give(p, gold.get(), g);
            give(p, silver.get(), sv);
            paid = g * Wave16Tune.GOLD_COIN_GP + sv;
        } else {
            int n = Math.min(64, gp);
            give(p, Items.GOLD_NUGGET, n);
            paid = n;
        }
        setPurse(s, gp - paid);
        Fx.say(p, "You count out " + paid + " gp. " + (gp - paid) + " gp stay in the purse.");
    }

    private static void give(ServerPlayer p, Item item, int n) {
        if (n <= 0) return;
        ItemStack st = new ItemStack(item, n);
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    /** Blood Tithe: 300 gp from the purse close your wounds. */
    static void tithe(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave6Kit.find(p, Wave16Tune.ID_WALLET);
        if (s.isEmpty()) s = c.stack();
        int gp = purse(s);
        if (gp < Wave16Tune.WALLET_TITHE) { Fx.say(p, "The tithe is " + Wave16Tune.WALLET_TITHE + " gp; the purse holds " + gp + "."); return; }
        setPurse(s, gp - Wave16Tune.WALLET_TITHE);
        Fx.heal(p, 12f);
        Fx.shield(p, 4f, 600);
        Wave16Kit.puff(p.serverLevel(), p.position().add(0, 1.0, 0), 0x606068, 24, 0.5);
        Fx.say(p, "The wallet drinks " + Wave16Tune.WALLET_TITHE + " gp and closes your wounds.");
    }
}
