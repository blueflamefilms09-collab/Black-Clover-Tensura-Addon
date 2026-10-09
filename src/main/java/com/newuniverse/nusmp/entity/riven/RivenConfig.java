package com.newuniverse.nusmp.entity.riven;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/nusmp-riven.toml */
public final class RivenConfig {
    private RivenConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_RESEARCH, ALLOW_INBOX, BORED_PORTAL;
    public static final ModConfigSpec.IntValue BORED_SECONDS, BORED_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BORED_CHANCE;
    public static final ModConfigSpec.ConfigValue<String> RESEARCH_ENDPOINT;
    public static final ModConfigSpec.IntValue BASE_HEALTH, PER_PLAYER_HEALTH, REPLAN_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENABLE_RESEARCH = b.comment("Allow /nusmp riven research to call researchEndpoint (off the server thread). Default off.")
                .define("enableResearch", false);
        RESEARCH_ENDPOINT = b.comment("https endpoint that returns plain text about an anime ability (?anime=..&ability=..). Empty = research does nothing but explain the inbox.")
                .define("researchEndpoint", "");
        BASE_HEALTH = b.comment("Riven Remake's health with one player.").defineInRange("baseHealth", 1200, 100, 100000);
        PER_PLAYER_HEALTH = b.comment("Extra health for each additional player in the arena, up to a four-player party.").defineInRange("perPlayerHealth", 400, 0, 100000);
        REPLAN_TICKS = b.comment("How often he rescans his target and replans, in ticks.").defineInRange("replanTicks", 40, 10, 400);
        ALLOW_INBOX = b.comment("Allow /nusmp riven accept to move a skill file from config/nusmp/codex-inbox into the live codex.")
                .define("allowCodexInbox", true);
        BORED_PORTAL = b.comment("Riven can get bored when the fight is too easy and open a portal (reinforcements + flank).")
                .define("boredPortal", true);
        BORED_SECONDS = b.comment("Seconds of him taking almost no damage before he counts as bored.").defineInRange("boredSeconds", 25, 5, 600);
        BORED_CHANCE = b.comment("Chance per second, once bored, that he opens the portal.").defineInRange("boredChance", 0.08, 0.0, 1.0);
        BORED_COOLDOWN = b.comment("Minimum seconds between portals.").defineInRange("boredCooldownSeconds", 90, 10, 3600);
        SPEC = b.build();
    }
}
