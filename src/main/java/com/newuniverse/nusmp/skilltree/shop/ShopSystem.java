package com.newuniverse.nusmp.skilltree.shop;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Why: the one entry point of the Black Market: the witch's shop (stock, gold ledger, buy and sell packets, restock), the config {@code nusmp-skilltree-shop-server.toml}, the self-tests
 * {@code tree.shop.*}. Owned by the shop builder, who replaces this stub.
 */
public final class ShopSystem {
    private ShopSystem() {}

    public static void init(IEventBus modBus, ModContainer modContainer) {}
}
