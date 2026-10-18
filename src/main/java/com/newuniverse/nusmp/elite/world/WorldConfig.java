package com.newuniverse.nusmp.elite.world;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Server config of the world engine: {@code nusmp-elite-world-server.toml}. The master switch {@code enabled} is ON by default but every rate is conservative: the density moves slowly,
 * ores convert one at a time with a lifetime cap per chunk, few spawns are touched, and a calamity needs a very high density for a very long time. Reads fall back to the defaults before
 * the config is loaded (the pattern of the other configs of the mod), so a test or an early call never throws.
 */
public final class WorldConfig {
    private WorldConfig() {}

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    // ---- magicule
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    public static final ModConfigSpec.IntValue RADIUS_CHUNKS, UPDATE_TICKS, SYNC_TICKS;
    public static final ModConfigSpec.DoubleValue RISE_PER_MIN, FALL_PER_MIN, EVENT_DECAY_PER_MIN;
    // ---- crystals
    public static final ModConfigSpec.BooleanValue CRYSTALS;
    public static final ModConfigSpec.DoubleValue GROWTH_CHANCE, SEED_CHANCE, MOTHER_CHANCE;
    public static final ModConfigSpec.IntValue MOTHER_CAP, SEED_RADIUS;
    // ---- ore conversion
    public static final ModConfigSpec.BooleanValue ORES;
    public static final ModConfigSpec.DoubleValue ORE_THRESHOLD;
    public static final ModConfigSpec.IntValue ORE_INTERVAL, ORE_PER_INTERVAL, ORE_LIFETIME;
    // ---- mob replacement
    public static final ModConfigSpec.BooleanValue MOBS, MOB_MARKER;
    public static final ModConfigSpec.DoubleValue MOB_THRESHOLD, MOB_CHANCE, MOB_MAX_CHANCE;
    // ---- calamities
    public static final ModConfigSpec.BooleanValue CALAMITIES, CALAMITY_AUTO, CALAMITY_RITUAL, CALAMITY_BROADCAST;
    public static final ModConfigSpec.IntValue CALAMITY_CHECK_TICKS, CALAMITY_MAX_ACTIVE, CALAMITY_MIN_DISTANCE, CALAMITY_MAX_DISTANCE;
    public static final ModConfigSpec.DoubleValue CALAMITY_HEALTH, CALAMITY_DAMAGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Master switch of the whole world engine (chunk magicule, crystals, ore conversion, touched mobs, calamities). false = nothing here runs.")
                .define("enabled", true);

        b.push("magicule");
        DIMENSIONS = b.comment("Dimensions where the density is simulated and the world effects run. The dungeon dimensions are never listed here and are refused anyway.")
                .defineListAllowEmpty("dimensions", List.of("minecraft:overworld"), () -> "minecraft:overworld", o -> o instanceof String);
        RADIUS_CHUNKS = b.comment("Chunks around each player whose density is updated.").defineInRange("radiusChunks", 3, 0, 8);
        UPDATE_TICKS = b.comment("Ticks between two update passes.").defineInRange("updateTicks", 100, 20, 2400);
        SYNC_TICKS = b.comment("Ticks between two sync checks per player (a packet is sent only when the value moved).").defineInRange("syncTicks", 40, 10, 1200);
        RISE_PER_MIN = b.comment("How fast a chunk's density RISES toward its target, per minute (0.02 = a rich chunk needs most of an hour).").defineInRange("risePerMinute", 0.02, 0.0, 1.0);
        FALL_PER_MIN = b.comment("How fast it FALLS toward a lower target, per minute.").defineInRange("fallPerMinute", 0.06, 0.0, 1.0);
        EVENT_DECAY_PER_MIN = b.comment("How fast an event bump (a fallen calamity, a harvested Mother Rock) fades, per minute.").defineInRange("eventDecayPerMinute", 0.01, 0.0, 1.0);
        b.pop();

        b.push("crystals");
        CRYSTALS = b.comment("Magic Ore crystals grow (random ticks, driven by the chunk's density) and Mother Rocks seed buds.").define("growth", true);
        GROWTH_CHANCE = b.comment("Base chance per random tick that a bud ripens to the next stage, at density 1 (it is lower below).").defineInRange("growthChance", 0.25, 0.0, 1.0);
        SEED_CHANCE = b.comment("Base chance per random tick that a charged Mother Rock seeds a new bud nearby.").defineInRange("seedChance", 0.15, 0.0, 1.0);
        MOTHER_CHANCE = b.comment("Chance per random tick that a qualifying cluster (dense chunk, a grove around it) matures into a Mother Rock.").defineInRange("motherChance", 0.04, 0.0, 1.0);
        MOTHER_CAP = b.comment("Most Mother Rocks a chunk can have through growth (placed ones count, and are never removed).").defineInRange("motherRocksPerChunk", 2, 0, 16);
        SEED_RADIUS = b.comment("How far from a Mother Rock a new bud may sprout, in blocks.").defineInRange("seedRadius", 4, 1, 8);
        b.pop();

        b.push("oreConversion");
        ORES = b.comment("In dense chunks ordinary ores slowly turn into magic ore (data/nusmp/elite/world/ore_conversion.json).").define("enabled", true);
        ORE_THRESHOLD = b.comment("Lowest chunk density where conversion runs (an entry of the map may ask for more).").defineInRange("densityThreshold", 0.70, 0.0, 1.0);
        ORE_INTERVAL = b.comment("Ticks in one budget window of a chunk.").defineInRange("intervalTicks", 2400, 20, 72000);
        ORE_PER_INTERVAL = b.comment("Blocks one chunk may convert per window.").defineInRange("perInterval", 1, 0, 16);
        ORE_LIFETIME = b.comment("Blocks one chunk may convert in all, ever, so a rich chunk is never eaten whole.").defineInRange("perChunkLifetime", 24, 0, 4096);
        b.pop();

        b.push("mobReplacement");
        MOBS = b.comment("A share of the natural spawns of ordinary monsters in dense chunks become magicule-touched (stronger, marked, better drops). Never in the dungeon dimensions or the Hub.")
                .define("enabled", true);
        MOB_THRESHOLD = b.comment("Lowest chunk density where spawns can be touched.").defineInRange("densityThreshold", 0.60, 0.0, 1.0);
        MOB_CHANCE = b.comment("Base chance that a natural spawn is touched at the threshold (it grows with the density).").defineInRange("chance", 0.06, 0.0, 1.0);
        MOB_MAX_CHANCE = b.comment("The chance is never above this, whatever the density.").defineInRange("maxChance", 0.25, 0.0, 1.0);
        MOB_MARKER = b.comment("Touched mobs glow (a visible outline) so they can be told at a glance. false = only the custom name marks them.").define("glowMarker", true);
        b.pop();

        b.push("calamities");
        CALAMITIES = b.comment("The calamity system: bosses with phases that wake from the world's magicule (data/nusmp/elite/world/calamities/*.json).").define("enabled", true);
        CALAMITY_AUTO = b.comment("Calamities may wake by themselves when a trigger of their file is met. false = only by ritual or command.").define("autoSpawn", true);
        CALAMITY_RITUAL = b.comment("A Calamity Core used on a charged Mother Rock starts a calamity.").define("ritual", true);
        CALAMITY_BROADCAST = b.comment("Announcements (omen, rising, phases, defeat) go to every player on the server, not only to those near.").define("broadcast", true);
        CALAMITY_CHECK_TICKS = b.comment("Ticks between two checks of the director.").defineInRange("checkTicks", 100, 20, 2400);
        CALAMITY_MAX_ACTIVE = b.comment("Calamities alive (or announced) at once on the whole server.").defineInRange("maxActive", 1, 1, 4);
        CALAMITY_MIN_DISTANCE = b.comment("Nearest a calamity rises to the player it targets, in blocks.").defineInRange("minDistance", 28, 8, 128);
        CALAMITY_MAX_DISTANCE = b.comment("Farthest a calamity rises from that player, in blocks.").defineInRange("maxDistance", 44, 12, 160);
        CALAMITY_HEALTH = b.comment("Multiplier of the health of every calamity boss.").defineInRange("healthMultiplier", 1.0, 0.1, 20.0);
        CALAMITY_DAMAGE = b.comment("Multiplier of the damage of every calamity boss (the hit cap on players still applies).").defineInRange("damageMultiplier", 1.0, 0.1, 5.0);
        b.pop();

        SPEC = b.build();
    }

    // ---------------------------------------------------------------- readers (safe before the config is loaded)

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static List<? extends String> dimensions() { return get(DIMENSIONS); }

    public static int radiusChunks() { return get(RADIUS_CHUNKS); }

    public static int updateTicks() { return get(UPDATE_TICKS); }

    public static int syncTicks() { return get(SYNC_TICKS); }

    public static double risePerMinute() { return get(RISE_PER_MIN); }

    public static double fallPerMinute() { return get(FALL_PER_MIN); }

    public static double eventDecayPerMinute() { return get(EVENT_DECAY_PER_MIN); }

    public static boolean crystals() { return enabled() && get(CRYSTALS); }

    public static double growthChance() { return get(GROWTH_CHANCE); }

    public static double seedChance() { return get(SEED_CHANCE); }

    public static double motherChance() { return get(MOTHER_CHANCE); }

    public static int motherCap() { return get(MOTHER_CAP); }

    public static int seedRadius() { return get(SEED_RADIUS); }

    public static boolean ores() { return enabled() && get(ORES); }

    public static double oreThreshold() { return get(ORE_THRESHOLD); }

    public static int oreInterval() { return get(ORE_INTERVAL); }

    public static int orePerInterval() { return get(ORE_PER_INTERVAL); }

    public static int oreLifetime() { return get(ORE_LIFETIME); }

    public static boolean mobs() { return enabled() && get(MOBS); }

    public static double mobThreshold() { return get(MOB_THRESHOLD); }

    public static double mobChance() { return get(MOB_CHANCE); }

    public static double mobMaxChance() { return get(MOB_MAX_CHANCE); }

    public static boolean mobMarker() { return get(MOB_MARKER); }

    public static boolean calamities() { return enabled() && get(CALAMITIES); }

    public static boolean calamityAuto() { return calamities() && get(CALAMITY_AUTO); }

    public static boolean calamityRitual() { return calamities() && get(CALAMITY_RITUAL); }

    public static boolean calamityBroadcast() { return get(CALAMITY_BROADCAST); }

    public static int calamityCheckTicks() { return get(CALAMITY_CHECK_TICKS); }

    public static int calamityMaxActive() { return get(CALAMITY_MAX_ACTIVE); }

    public static int calamityMinDistance() { return get(CALAMITY_MIN_DISTANCE); }

    public static int calamityMaxDistance() { return Math.max(get(CALAMITY_MAX_DISTANCE), get(CALAMITY_MIN_DISTANCE) + 4); }

    public static double calamityHealth() { return get(CALAMITY_HEALTH); }

    public static double calamityDamage() { return get(CALAMITY_DAMAGE); }
}
