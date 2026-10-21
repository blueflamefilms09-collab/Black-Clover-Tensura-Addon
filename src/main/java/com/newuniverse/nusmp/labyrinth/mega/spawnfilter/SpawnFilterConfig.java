package com.newuniverse.nusmp.labyrinth.mega.spawnfilter;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Why: server config of the Dungeon's spawn filter, {@code world/serverconfig/nusmp-labyrinth-mega-spawnfilter-server.toml}, section {@code [spawn]}: the
 * creature blacklist (the Gorgon by default, more can be added), and the themed palette that keeps a floor's creatures to the factions its Region Theme
 * likes. Its own spec so the filter never edits another builder's config, plain {@code define} calls only, and every reader goes through {@link #get}, which
 * answers the default while the spec is not loaded yet.
 */
public final class SpawnFilterConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static final ModConfigSpec.BooleanValue ADMIN_BYPASS, THEME_FILTER;
    public static final ModConfigSpec.DoubleValue THEME_MIN_WEIGHT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spawn");
        BLACKLIST = b.comment("Entity ids that never exist in any dungeon dimension (the Mega-Dungeon and its pockets, the Spire, the Sanctuary): they are refused when they spawn, when they are added by another mod, and removed when a saved chunk holds one. 'namespace:path' is exact, 'namespace:*' is every creature of a mod, a bare 'path' is that path in any namespace. Ice and Fire's Gorgon is on the list by default. The tag nusmp:dungeon/banned is read too.")
                .defineListAllowEmpty("blacklist", SpawnBlacklist.DEFAULT, () -> "iceandfire:gorgon", o -> o instanceof String);
        ADMIN_BYPASS = b.comment("true = an operator's own /summon or spawn egg may bring a blacklisted creature in anyway. false (default) = never, not even for an operator.")
                .define("blacklistAllowsAdmin", false);
        THEME_FILTER = b.comment("A floor only gets creatures of the factions its Region Theme likes (the themeWeights of data/nusmp/mega/factions: a weight of 0 means never native). Creatures without a faction, bosses, owned and named creatures and an operator's are never filtered. false = any faction on any floor, as before 1.107.0.")
                .define("themeFilter", true);
        THEME_MIN_WEIGHT = b.comment("How much a faction must like the floor's theme to count as native: 0 = any weight above zero, 1 = at least the neutral weight, higher = only the favourites. A theme where no faction reaches the bar filters nothing.")
                .defineInRange("themeMinWeight", 0.0, 0.0, 10.0);
        b.pop();
        SPEC = b.build();
    }

    private SpawnFilterConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
