package com.newuniverse.nusmp.dungeon.placed;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the player-placed block exemption, {@code world/serverconfig/nusmp-dungeon-placed-server.toml}, section {@code [placed]}. Its own
 * small spec (the {@code GuardConfig} pattern) so a server owner who finds the exemption too generous switches it off without touching the Dungeon
 * Guard's own switch, and the two caps are tuned in one place. Only plain {@code define} / {@code defineInRange}; every reader goes through {@link #get},
 * which answers the default while the spec is not loaded yet (block and entity registration run before SERVER configs are read).
 */
public final class PlacedConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue EXEMPT, SYNC, NOTICE;
    public static final ModConfigSpec.IntValue CAP, PER_CHUNK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("placed");
        EXEMPT = b.comment("A block a player placed in the Labyrinth (the Mega-Dungeon and the old Spire) can be broken and picked up again: torches, crafting stations, sleeping bags, backpacks, bridges. The blocks the dungeon generated stay unbreakable. false = every block of the dungeon is unbreakable, as before 1.107.0.")
                .define("exempt", true);
        CAP = b.comment("Most player-placed blocks remembered per dimension. When it is reached, the OLDEST placed block becomes part of the dungeon's stone again (it can no longer be broken); the newest always works.")
                .defineInRange("cap", PlacedSet.DEFAULT_CAP, PlacedSet.MIN_CAP, PlacedSet.MAX_CAP);
        PER_CHUNK = b.comment("Most player-placed blocks remembered per chunk (16 x 16 columns), the same oldest-first rule. Stops one chunk from using the whole cap.")
                .defineInRange("perChunk", PlacedSet.DEFAULT_PER_CHUNK, 1, PlacedSet.MAX_CAP);
        SYNC = b.comment("Tell the clients which nearby blocks are player-placed, so the crack animation starts on them. false = clients predict the old behaviour (no crack on any dungeon block) and a placed block can only be broken in creative mode or with the server's help.")
                .define("sync", true);
        NOTICE = b.comment("Tell a player (action bar, at most once a few seconds) when the cap made the dungeon reclaim the oldest block they placed.")
                .define("notice", true);
        b.pop();
        SPEC = b.build();
    }

    private PlacedConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
