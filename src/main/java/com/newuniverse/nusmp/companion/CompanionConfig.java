package com.newuniverse.nusmp.companion;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the switches of the companion system, {@code nusmp-companions-server.toml}, section {@code [companions]}. They change how the game plays (who may be asked to walk with a player, how many, whether they
 * cross dimensions and whether the pouch heals), so they are SERVER settings; what only changes looks (the speech bubbles over heads) is a client setting in {@code nusmp-companions-client.toml}. Every value is a plain
 * {@code define} / {@code defineInRange} (never a list-validated one: that throws while the server is still loading) and every reader goes through {@link #get}, which answers the default while the spec is
 * not loaded yet.
 */
public final class CompanionConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, TRAVEL, POUCH_USE, BUBBLES;
    public static final ModConfigSpec.IntValue MAX_FOLLOWERS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("companions");
        ENABLED = b.comment("Master switch of the companion system: when off, companions open no talk screen (the sample companion falls back to its old chat talk), nobody follows, no bubble is sent and the protection rule is off.")
                .define("enabled", true);
        MAX_FOLLOWERS = b.comment("How many companions one player can have walking with them at the same time.")
                .defineInRange("maxFollowersPerPlayer", 2, 1, 6);
        TRAVEL = b.comment("Followers are carried after their player through a dimension change (a portal, a command, another mod's crossing).")
                .define("travel", true);
        POUCH_USE = b.comment("A companion that walks with a player may use a healing potion or food from her pouch on them when their health is low (the player can still switch it off for each companion).")
                .define("pouchUse", true);
        BUBBLES = b.comment("The server sends speech bubbles (a small packet to the players near the speaker). Off: companions still talk in the screen, nothing floats over their heads.")
                .define("bubbles", true);
        b.pop();
        SPEC = b.build();
    }

    private CompanionConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static int maxFollowers() { return get(MAX_FOLLOWERS); }

    public static boolean travel() { return enabled() && get(TRAVEL); }

    public static boolean pouchUse() { return enabled() && get(POUCH_USE); }

    public static boolean bubbles() { return enabled() && get(BUBBLES); }
}
