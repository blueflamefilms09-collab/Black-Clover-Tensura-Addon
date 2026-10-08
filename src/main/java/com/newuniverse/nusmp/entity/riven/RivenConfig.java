package com.newuniverse.nusmp.entity.riven;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/nusmp-riven.toml */
public final class RivenConfig {
    private RivenConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_RESEARCH, ALLOW_INBOX;
    public static final ModConfigSpec.ConfigValue<String> RESEARCH_ENDPOINT;
    public static final ModConfigSpec.IntValue BASE_HEALTH, PER_PLAYER_HEALTH, REPLAN_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENABLE_RESEARCH = b.comment("Allow /nusmp riven research to call researchEndpoint (off the server thread). Default off.")
                .define("enableResearch", false);
        RESEARCH_ENDPOINT = b.comment("https endpoint that returns plain text about an anime ability (?anime=..&ability=..). Empty = research does nothing but explain the inbox.")
                .define("researchEndpoint", "");
        BASE_HEALTH = b.comment("Riven Remake's health with one player.").defineInRange("baseHealth", 600, 100, 100000);
        PER_PLAYER_HEALTH = b.comment("Extra health for each additional player in the arena (+20% of the base by default).")
                .defineInRange("perPlayerHealth", 200, 0, 100000);
        REPLAN_TICKS = b.comment("How often he rescans his target and replans, in ticks.").defineInRange("replanTicks", 40, 10, 400);
        ALLOW_INBOX = b.comment("Allow /nusmp riven accept to move a skill file from config/nusmp/codex-inbox into the live codex.")
                .define("allowCodexInbox", true);
        SPEC = b.build();
    }
}
