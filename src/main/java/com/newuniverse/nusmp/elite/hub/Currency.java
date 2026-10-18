package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the owner asked that the generic Magic Stone economy be REPLACED by Tensura's coins, without deleting the old one. The Mega-Dungeon pays Magic Stones from three places that existed before
 * this job (the 1.36.0 economy's {@code EconomyDrops}, the faction extras {@code FactionDrops} and the Mob Foundry's {@code FoundryDrops}), so each of them asks this small seam one question,
 * {@link #stonesSuppressed()}, and does nothing when the answer is yes; the coin drops that take their place are added by {@link CoinLootModifier} (mobs, chests) and {@link CoinBossDrops}
 * (the decennial Guardian). Under {@code economy.currency = magic_stones} the answer is always no and not one line of the old path runs differently. The other half of the seam is the money
 * itself: {@link #balance}, {@link #charge} and {@link #give} take an amount in bronze coins and move it with whichever wallet the mode picks ({@link CoinWallet} or {@link StoneWallet}); the
 * shop, the quests and the {@code EliteHooks.wallet} hook (the other 1.83.0 systems) all go through here, never to a wallet directly. When the Hub economy is switched off
 * ({@code hub.enabled = false}) every answer is "no money, no change". Server thread only.
 */
public final class Currency {
    private Currency() {}

    /** The hook instance other systems read: it follows the mode and the master switch at every call. */
    public static final EliteHooks.Wallet HOOK = new EliteHooks.Wallet() {
        @Override public long balance(ServerPlayer player) { return Currency.balance(player); }

        @Override public boolean charge(ServerPlayer player, long coins) { return Currency.charge(player, coins); }

        @Override public void give(ServerPlayer player, long coins) { Currency.give(player, coins); }
    };

    private static final CoinWallet COINS = new CoinWallet();
    private static final StoneWallet STONES = new StoneWallet();

    /** The configured currency. */
    public static CurrencyMode mode() { return CurrencyMode.parse(HubConfig.get(HubConfig.CURRENCY)); }

    /** True when the Hub economy is on and pays and charges Tensura coins. */
    public static boolean coins() { return HubConfig.enabled() && mode() == CurrencyMode.TENSURA_COINS; }

    /** True when the Hub economy is on and runs on Magic Stones (the old behaviour). */
    public static boolean stones() { return HubConfig.enabled() && mode() == CurrencyMode.MAGIC_STONES; }

    /**
     * Must the dungeon's old stone and crystal drops stay home? Yes exactly when coins are the currency and the owner did not ask to keep the stones too
     * ({@code economy.keepStoneDrops}). The three old drop listeners call this first; with Magic Stones as the currency it is always false.
     */
    public static boolean stonesSuppressed() { return coins() && !HubConfig.get(HubConfig.KEEP_STONE_DROPS); }

    /**
     * The same question for a drop in a given level: only the Mega-Dungeon is switched to coins. The 1.36.0 economy also pays in the older Labyrinth Spire dimension, where nothing pays coins,
     * so there the stones keep dropping whatever the currency is.
     */
    public static boolean stonesSuppressed(net.minecraft.world.level.Level level) { return stonesSuppressed() && com.newuniverse.nusmp.labyrinth.mega.MegaDungeon.in(level); }

    private static EliteHooks.Wallet wallet() { return stones() ? STONES : COINS; }

    /** The player's money in bronze coins (0 when the economy is off). */
    public static long balance(ServerPlayer p) { return HubConfig.enabled() ? wallet().balance(p) : 0L; }

    /** Takes {@code coins} bronze from the player; false (and nothing changes) when they cannot pay or the economy is off. */
    public static boolean charge(ServerPlayer p, long coins) { return HubConfig.enabled() && wallet().charge(p, coins); }

    /** Pays {@code coins} bronze to the player; nothing when the economy is off. */
    public static void give(ServerPlayer p, long coins) { if (HubConfig.enabled()) wallet().give(p, coins); }

    /** The word for the money in the current mode, for messages: {@code coins} or {@code Magic Stones}. */
    public static String unitName() { return stones() ? "Magic Stones" : "coins"; }
}
