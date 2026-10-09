package com.newuniverse.nusmp.entity.riven;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/nusmp-riven.toml (common config). */
public final class RivenConfig {
    private RivenConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_RESEARCH, ALLOW_CODEX_INBOX;
    public static final ModConfigSpec.ConfigValue<String> RESEARCH_ENDPOINT;
    public static final ModConfigSpec.IntValue BASE_HEALTH, PER_PLAYER_HEALTH, REPLAN_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENABLE_RESEARCH = b.comment("Admin-only /nusmp riven research. Off by default. The server never loads code from anywhere: results are only ever candidate codex JSON files.")
                .define("enableResearch", false);
        RESEARCH_ENDPOINT = b.comment("Optional HTTP endpoint that returns skill text. Empty = none (drop JSON files into config/nusmp/codex-inbox/ instead).")
                .define("researchEndpoint", "");
        BASE_HEALTH = b.defineInRange("baseHealth", 600, 20, 1_000_000);
        PER_PLAYER_HEALTH = b.comment("Extra health for each additional player in the arena (up to 4 extra).").defineInRange("perPlayerHealth", 200, 0, 1_000_000);
        REPLAN_TICKS = b.comment("How long Riven commits to a kill plan before he rescans his target.").defineInRange("replanTicks", 40, 5, 400);
        ALLOW_CODEX_INBOX = b.comment("Lets ops accept skill files from config/nusmp/codex-inbox/ with /nusmp riven accept <file>.").define("allowCodexInbox", true);
        SPEC = b.build();
    }
}
