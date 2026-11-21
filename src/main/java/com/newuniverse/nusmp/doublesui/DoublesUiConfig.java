package com.newuniverse.nusmp.doublesui;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the Real Double selector changes one rule of the game (a sneak-cast trades places with the double you picked, not always the nearest), so it is a SERVER setting file,
 * {@code nusmp-doubles-ui-server.toml}, with a switch that gives the old rule back. The lead registers it. Plain define only; each accessor answers the default until the file is loaded.
 */
public final class DoublesUiConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED, ACTIVE_TRADES;
    private static final ModConfigSpec.IntValue SELECT_GAP_MS, SYNC_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("doublesUi");
        ENABLED = b.comment("Send each player the roster of their Real Doubles and accept the selection keys. false = no roster packet and no selection (the old game, no stack on screen).").define("enabled", true);
        ACTIVE_TRADES = b.comment("A sneak-cast of Real Double trades places with the double the player selected. false = always the nearest double, as before.").define("selectedDoubleTrades", true);
        SELECT_GAP_MS = b.comment("The least milliseconds between two selection requests of one player (extra presses are ignored).").defineInRange("selectGapMillis", 100, 0, 5000);
        SYNC_TICKS = b.comment("How often (game ticks) the server looks for a change in a player's doubles to send. Only players who have doubles are looked at.").defineInRange("syncEveryTicks", 10, 5, 100);
        b.pop();
        SPEC = b.build();
    }

    private DoublesUiConfig() {}

    public static boolean enabled() { return SPEC.isLoaded() ? ENABLED.get() : true; }
    public static boolean selectedDoubleTrades() { return SPEC.isLoaded() ? ACTIVE_TRADES.get() : true; }
    public static int selectGapMillis() { return SPEC.isLoaded() ? SELECT_GAP_MS.get() : 100; }
    public static int syncEveryTicks() { return SPEC.isLoaded() ? SYNC_TICKS.get() : 10; }
}
