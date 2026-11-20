package com.newuniverse.nusmp.tooltip;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: ranks are now shown the way the relics show them (stars, grade name in the grade colour, place; docs/rank_audit.md). The old plain text ("Rank: God Grade #4", "rank S beast", "Low Devil")
 * stays one switch away (owner rule 2): {@code rankDisplay.rankDisplayClassic}, default false. It only changes what is drawn (a CLIENT config, own file {@code nusmp-rank-display-client.toml}); the
 * server decides nothing from it. A value read before the config is loaded (a unit test, a dedicated server) gives the default false, which is the new look.
 */
public final class RankDisplayConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CLASSIC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("rankDisplay");
        CLASSIC = b.comment("true: ranks are shown as plain text again (Rank: God Grade #4, rank S beast, Low Devil) instead of the relic card (stars, grade, place).")
                .define("rankDisplayClassic", false);
        b.pop();
        SPEC = b.build();
    }

    private RankDisplayConfig() {}

    public static boolean classic() {
        try { return CLASSIC.get(); } catch (IllegalStateException unloaded) { return false; }
    }
}
