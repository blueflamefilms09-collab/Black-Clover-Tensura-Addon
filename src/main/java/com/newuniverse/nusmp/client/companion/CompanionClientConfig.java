package com.newuniverse.nusmp.client.companion;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: what only changes LOOKS of the companion system is a client setting ({@code nusmp-companions-client.toml}): whether speech bubbles are drawn over heads. (What changes how the game plays is the server
 * config {@code companions.*}.) A plain {@code define}, read through {@link #bubbles} which answers the default while the spec is not loaded yet.
 */
public final class CompanionClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue BUBBLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("companions");
        BUBBLES = b.comment("Draw the speech bubbles over the heads of companions (at most 24 at a time, only within 24 blocks). Off: you still read everything in the talk screen.")
                .define("bubbles", true);
        b.pop();
        SPEC = b.build();
    }

    private CompanionClientConfig() {}

    public static boolean bubbles() {
        try { return BUBBLES.get(); } catch (IllegalStateException notLoaded) { return BUBBLES.getDefault(); }
    }
}
