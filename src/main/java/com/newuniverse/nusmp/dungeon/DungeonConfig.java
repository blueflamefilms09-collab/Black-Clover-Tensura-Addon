package com.newuniverse.nusmp.dungeon;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config of the Living Dungeon: world/serverconfig/nusmp-dungeon-server.toml (docs/living_dungeon.md). */
public final class DungeonConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, BROADCAST, HAZARDS, NO_LOOT, LIFE, UNBREAKABLE, SENTIENT, JUGGERNAUT;
    public static final ModConfigSpec.DoubleValue MAW_THRESHOLD, REGROW_DAYS, ROOM_SCALE, JUGGERNAUT_THREAT, STONE_SCALE;
    public static final ModConfigSpec.IntValue MAW_MINUTES, MAW_COOLDOWN_MINUTES, SIM_RADIUS, ROOM_REBIRTH_MINUTES, BORN_CAP, JUGGERNAUT_COOLDOWN_MINUTES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("dungeon");
        ENABLED = b.comment("The Living Dungeon: the dimension's ecosystem, its hazards and the Dungeon Maw portals that open in the Overworld.").define("enabled", true);
        HAZARDS = b.comment("Atmospheric hazards of the Dungeon's tiers (heat, suffocation, magicule radiation, conductive water).").define("hazards", true);
        BROADCAST = b.comment("Ominous server-wide messages: a Maw opens or closes, the Golden City seal breaks.").define("broadcast", true);
        LIFE = b.comment("The Dungeon's ecosystem breeds its own creatures (slimes, Living Armors, Direwolves, Basilisks). false = nothing is ever born there; only what you spawn lives.").define("life", true);
        NO_LOOT = b.comment("Nothing that dies in the Dungeon drops items (players aside): a mob leaves a Carcass, and meat is cut from it with a cleaver.").define("noLoot", true);
        UNBREAKABLE = b.comment("Every block of the Dungeon is unbreakable by any means: mining (creative too), explosions, mob griefing, tools, pistons. false = the Dungeon can be dug and blown up again (mined terrain then grows back, see regrowDays).").define("unbreakable", true);
        MAW_THRESHOLD = b.comment("Ambient magicule density of a region of 4x4 chunks that tears open a Dungeon Maw. Every spell adds cost%^2/100; it decays on its own. 0 = Maws never open by themselves.")
                .defineInRange("mawThreshold", 600.0, 0.0, 1000000.0);
        MAW_MINUTES = b.comment("How long a Dungeon Maw stays open if its roots are not chopped down.").defineInRange("mawMinutes", 20, 1, 1440);
        MAW_COOLDOWN_MINUTES = b.comment("The least time between two Maws opening by themselves anywhere on the server.").defineInRange("mawCooldownMinutes", 30, 0, 1440);
        REGROW_DAYS = b.comment("In-game days before a block mined in the Dungeon grows back (stone, ores, dripstone).").defineInRange("regrowDays", 2.0, 0.05, 365.0);
        SIM_RADIUS = b.comment("Chunks round a player in the Dungeon whose ecosystem is simulated.").defineInRange("simRadius", 3, 1, 8);
        SENTIENT = b.comment("The Dungeon is a sentient, hostile entity (the DanMachi rules): nothing breeds, mobs are born from the walls of each room a player enters, over-threatening the Dungeon brings a Juggernaut, the dead turn to ash and drop Magic Stones. false = the closed ecosystem of 1.18-1.21 (hunger, breeding, carcasses).")
                .define("sentient", true);
        ROOM_SCALE = b.comment("Multiplies the mobs born in each room (the design's 5-8 on the Upper Catacombs, never more than 8 in a room).").defineInRange("roomPopulation", 1.0, 0.25, 3.0);
        ROOM_REBIRTH_MINUTES = b.comment("A populated room whose born mobs are all dead fills again this many minutes after its last birth, when a player is inside again. 0 = rooms never refill.").defineInRange("roomRebirthMinutes", 30, 0, 1440);
        BORN_CAP = b.comment("How many born mobs alive within 64 blocks of one player stop the walls from birthing more for them.").defineInRange("maxBornPerPlayer", 28, 4, 200);
        JUGGERNAUT = b.comment("The Dungeon's immune system: players who hurt it too much (its walls, explosions, massive spells) are answered with a Juggernaut born from the walls.").define("juggernaut", true);
        JUGGERNAUT_THREAT = b.comment("Threat that brings a Juggernaut. A refused blow at a wall is 4, a refused explosion 1 per block (40 at most), a spell that costs c percent of the caster's magicules (15 or more) c*c/10; it fades 1.5 a second.")
                .defineInRange("juggernautThreat", 100.0, 20.0, 1000.0);
        JUGGERNAUT_COOLDOWN_MINUTES = b.comment("The least time between two Juggernauts sent at the same player.").defineInRange("juggernautCooldownMinutes", 10, 1, 240);
        STONE_SCALE = b.comment("Multiplies the Magic Stones and crystals the dead of the Dungeon leave (0 = none).").defineInRange("stoneDrops", 1.0, 0.0, 5.0);
        b.pop();
        SPEC = b.build();
    }

    private DungeonConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
