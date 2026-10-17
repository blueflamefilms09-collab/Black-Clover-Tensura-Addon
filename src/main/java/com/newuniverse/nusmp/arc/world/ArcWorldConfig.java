package com.newuniverse.nusmp.arc.world;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the switches of the places where the characters of the arcs appear (the story arenas and the Chronicle, the Promenade of the Hub and the friends who
 * stand on it, the saga rewards, how hard a floor boss grows with depth), {@code nusmp-arc-world-server.toml}, section {@code [arc]}. The three switches the
 * brief names for the layer as a whole ({@code storyGate}, {@code floorBosses}, {@code floorBossChance}) already live in {@code ArcConfig}; this file holds
 * only what this part adds, in its own file so it ships and is documented with the part. Every value is a plain {@code define} / {@code defineInRange}
 * (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through {@link #get}, which answers
 * the default while the spec is not loaded yet.
 */
public final class ArcWorldConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ARENAS, ARENA_REPLAY, SAGA_REWARDS, CHRONICLE, HUB_NPCS, HUB_NPCS_ALWAYS;
    public static final ModConfigSpec.IntValue ARENA_COOLDOWN_MINUTES;
    public static final ModConfigSpec.DoubleValue FLOOR_SCALE;
    public static final ModConfigSpec.ConfigValue<String> HUB_GUIDE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("arc");
        ARENAS = b.comment("The story arenas, the Chronicle Sigil and the lectern of the Chronicle. Off: the sigil does nothing and nobody can be carried to an arena.")
                .define("arenas", true);
        ARENA_REPLAY = b.comment("A boss that a player has already defeated can be fought again from the Chronicle (the story credit and the grimoire are given once only).")
                .define("arenaReplay", true);
        ARENA_COOLDOWN_MINUTES = b.comment("Minutes a story arena rests after its boss fell before it answers again.")
                .defineInRange("arenaCooldownMinutes", 10, 1, 240);
        SAGA_REWARDS = b.comment("The player whose victory closes a saga (every boss of it defeated) is given its reward once: items and a commemorative book.")
                .define("sagaRewards", true);
        CHRONICLE = b.comment("The Sanctuary Hub gets the Promenade (a paved ring round the plaza) and the Chronicle (a rotunda with the lectern of the story).")
                .define("chronicle", true);
        HUB_NPCS = b.comment("The friendly characters of the arcs stand on the posts of the Promenade when the story has brought them there.")
                .define("hubNpcs", true);
        HUB_NPCS_ALWAYS = b.comment("True: every friendly character stands in the Hub from the start. False: one stands there only when some player who is online has defeated the bosses it follows.")
                .define("hubNpcsAlwaysThere", false);
        HUB_GUIDE = b.comment("The id of the friendly character who stands nearest the Chronicle (the guide of the story). Empty or not in the roster: nobody is singled out.")
                .define("hubGuide", "eina_tulle");
        FLOOR_SCALE = b.comment("How much stronger (health and damage, as a share) a boss of the arcs gets for every floor it stands below the first floor it was designed for. 0 = none. Capped well below the Guardian's own curve.")
                .defineInRange("floorBossScalePerFloor", 0.03, 0.0, 0.25);
        b.pop();
        SPEC = b.build();
    }

    private ArcWorldConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean arenas() { return com.newuniverse.nusmp.arc.ArcConfig.enabled() && get(ARENAS); }

    public static boolean arenaReplay() { return get(ARENA_REPLAY); }

    public static int arenaCooldownMinutes() { return get(ARENA_COOLDOWN_MINUTES); }

    public static long arenaCooldownTicks() { return arenaCooldownMinutes() * 1200L; }

    public static boolean sagaRewards() { return get(SAGA_REWARDS); }

    public static boolean chronicle() { return com.newuniverse.nusmp.arc.ArcConfig.enabled() && get(CHRONICLE); }

    public static boolean hubNpcs() { return com.newuniverse.nusmp.arc.ArcConfig.enabled() && get(HUB_NPCS); }

    public static boolean hubNpcsAlways() { return get(HUB_NPCS_ALWAYS); }

    public static String hubGuide() {
        String g = get(HUB_GUIDE);
        return g == null ? "" : g.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static double floorScale() { return get(FLOOR_SCALE); }
}
