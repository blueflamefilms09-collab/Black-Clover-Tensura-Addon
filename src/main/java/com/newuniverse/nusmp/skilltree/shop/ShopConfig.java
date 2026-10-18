package com.newuniverse.nusmp.skilltree.shop;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Black Market, {@code nusmp-skilltree-shop-server.toml}, section {@code [shop]}. The economy is data (the price table and the stock file), but a
 * server owner also needs a few blunt levers that do not require editing a datapack: switch the shop off, make the market turn over faster or slower, make everything dearer or
 * cheaper in one number, make the witch pay more or less for goods, and cap how many books one player can buy in a day so a single rich player cannot empty a shelf. It is its own file
 * (not more keys in the core's config) because the shop is a separate system with a separate builder, so a server owner who wants the trees but not the market flips one file.
 * Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader
 * goes through {@link #get}, which answers the default while the spec is not loaded yet (a client, a test, the first tick of a server).
 */
public final class ShopConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue RESTOCK_DAYS, MAX_BUY_PER_DAY;
    public static final ModConfigSpec.DoubleValue PRICE_SCALE, SELL_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("shop");
        ENABLED = b.comment("The Black Market: sneak + right click on the old witch opens her shop. Off: she only talks.").define("enabled", true);
        RESTOCK_DAYS = b.comment("In-game days (24000 ticks of game time each) between two restocks of the shelf. Prices are rolled again at every restock.")
                .defineInRange("restockDays", 3, 1, 365);
        PRICE_SCALE = b.comment("Multiplier on every price the player PAYS (1.0 = the stock file's prices). 0.5 halves them, 2.0 doubles them.")
                .defineInRange("priceScale", 1.0, 0.01, 100.0);
        SELL_SCALE = b.comment("Multiplier on every price the witch PAYS for goods (1.0 = the price table). 0 stops her buying anything.")
                .defineInRange("sellScale", 1.0, 0.0, 100.0);
        MAX_BUY_PER_DAY = b.comment("Most books one player may buy per in-game day (24000 ticks). 0 = no limit.")
                .defineInRange("maxBuyPerDay", 0, 0, 100);
        b.pop();
        SPEC = b.build();
    }

    private ShopConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static int restockDays() { return get(RESTOCK_DAYS); }

    public static double priceScale() { return get(PRICE_SCALE); }

    public static double sellScale() { return get(SELL_SCALE); }

    public static int maxBuyPerDay() { return get(MAX_BUY_PER_DAY); }
}
