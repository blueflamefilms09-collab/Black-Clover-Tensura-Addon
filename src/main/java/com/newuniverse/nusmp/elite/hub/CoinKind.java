package com.newuniverse.nusmp.elite.hub;

import java.util.Locale;

/**
 * Why: the owner's brief makes Tensura Reincarnated's coins the one currency of the dungeon, the quests and the shops, so the four coin items of that mod need ONE table that says what each
 * is worth, and every other class (the wallet, the loot roll, the shop prices, the quest rewards, the tests) reads it. The unit is the lowest coin, the bronze coin (value 1); every price,
 * reward and balance in this package is a whole number of bronze coins, so there is no fractional money anywhere. The ratios are NOT invented here: the mod already has one place that pays out
 * Tensura coins ({@code relics/wave/Wave16Wallet}: "a gold coin is a hundred of them", gold = 100 silver) and the Black Market's gold is an abstract purse that never touches coin items, so
 * the table extends that single rule downwards and upwards by the same factor: 100 bronze = 1 silver, 100 silver = 1 gold, 100 gold = 1 stellar gold. {@code CoinKindTest} reads
 * {@code Wave16Tune.java} and fails if its gold-coin rule ever changes, so the two cannot drift apart. The last step (the stellar gold coin) has no precedent in the repo and is the
 * assumption to check against the real mod. Pure: no Minecraft type, the item ids are plain strings.
 */
public enum CoinKind {
    /** The lowest coin: value 1. */
    BRONZE("bronze_coin", 1L),
    SILVER("silver_coin", 100L),
    GOLD("gold_coin", 10_000L),
    /** The highest coin: value 1,000,000. */
    STELLAR_GOLD("stellar_gold_coin", 1_000_000L);

    /** The namespace of the coin items. */
    public static final String NAMESPACE = "tensura";

    /** The number of kinds. */
    public static final int COUNT = values().length;

    private final String path;
    private final long value;

    CoinKind(String path, long value) {
        this.path = path;
        this.value = value;
    }

    /** The item's registry path inside {@link #NAMESPACE}. */
    public String path() { return path; }

    /** The full item id, {@code tensura:gold_coin}. */
    public String itemId() { return NAMESPACE + ":" + path; }

    /** What one coin of this kind is worth, in bronze coins. */
    public long value() { return value; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The kind whose item id this is ({@code tensura:silver_coin}), or null. */
    public static CoinKind ofItemId(String itemId) {
        if (itemId == null) return null;
        for (CoinKind k : values()) if (k.itemId().equals(itemId)) return k;
        return null;
    }
}
