package com.newuniverse.nusmp.dungeon;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config of the Living Dungeon: world/serverconfig/nusmp-dungeon-server.toml (docs/living_dungeon.md). */
public final class DungeonConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, BROADCAST, HAZARDS;
    public static final ModConfigSpec.DoubleValue MAW_THRESHOLD, REGROW_DAYS;
    public static final ModConfigSpec.IntValue MAW_MINUTES, MAW_COOLDOWN_MINUTES, SIM_RADIUS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("dungeon");
        ENABLED = b.comment("The Living Dungeon: the dimension's ecosystem, its hazards and the Dungeon Maw portals that open in the Overworld.").define("enabled", true);
        HAZARDS = b.comment("Atmospheric hazards of the Dungeon's tiers (heat, suffocation, magicule radiation, conductive water).").define("hazards", true);
        BROADCAST = b.comment("Ominous server-wide messages: a Maw opens or closes, the Golden City seal breaks.").define("broadcast", true);
        MAW_THRESHOLD = b.comment("Ambient magicule density of a region of 4x4 chunks that tears open a Dungeon Maw. Every spell adds cost%^2/100; it decays on its own. 0 = Maws never open by themselves.")
                .defineInRange("mawThreshold", 600.0, 0.0, 1000000.0);
        MAW_MINUTES = b.comment("How long a Dungeon Maw stays open if its roots are not chopped down.").defineInRange("mawMinutes", 20, 1, 1440);
        MAW_COOLDOWN_MINUTES = b.comment("The least time between two Maws opening by themselves anywhere on the server.").defineInRange("mawCooldownMinutes", 30, 0, 1440);
        REGROW_DAYS = b.comment("In-game days before a block mined in the Dungeon grows back (stone, ores, dripstone).").defineInRange("regrowDays", 2.0, 0.05, 365.0);
        SIM_RADIUS = b.comment("Chunks round a player in the Dungeon whose ecosystem is simulated.").defineInRange("simRadius", 3, 1, 8);
        b.pop();
        SPEC = b.build();
    }

    private DungeonConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
