package com.newuniverse.nusmp.core.discovery;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config of the discovery system: world/serverconfig/nusmp-discovery-server.toml */
public final class DiscoveryConfig {
    private DiscoveryConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_ALL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("discovery");
        SHOW_ALL = b.comment("true = every player sees every item, relic, page and entry in the Codex, the grimoire and the command lists from the start",
                "(the old behaviour). false (default) = a thing stays hidden (shown as ???) until the player has found, crafted or been given it,",
                "or unlocked it. Players in creative mode always see everything.").define("showAll", false);
        b.pop();
        SPEC = b.build();
    }

    /** The switch, false while the config is not loaded yet. */
    public static boolean showAll() {
        try { return SHOW_ALL.get(); } catch (RuntimeException notLoaded) { return false; }
    }
}
