package com.newuniverse.nusmp.labyrinth;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Labyrinth, {@code world/serverconfig/nusmp-labyrinth-server.toml}, section {@code [labyrinth]}. A separate spec
 * (not more keys in {@link com.newuniverse.nusmp.dungeon.DungeonConfig}) because the owner's rule is "add the new, keep the old": the legacy
 * Dungeon's file keeps its keys and defaults byte for byte, and a server that wants the old caves back flips ONE key here
 * ({@code engine = "legacy"}) without touching anything else. Only plain {@code define} / {@code defineInRange} are used, never
 * {@code defineInList}, because a list-validated value throws on an unknown entry while the server is still loading; the {@code engine}
 * String gets a Predicate validator instead, which NeoForge resolves to the default on a bad value. Every reader goes through {@link #get},
 * which answers the default while the spec is not loaded yet (world-gen and block registration run before SERVER configs are read).
 */
public final class LabyrinthConfig {
    public static final ModConfigSpec SPEC;
    /** "labyrinth" (the Spire, default) or "legacy" (the Living Dungeon caves of 1.17-1.34): where Maws, Gates and the enter command lead. */
    public static final ModConfigSpec.ConfigValue<String> ENGINE;
    public static final ModConfigSpec.BooleanValue UNBREAKABLE, RELICS_EXCLUSIVE, VOID_RESCUE, BOSS_ROSTER_ENABLED;
    public static final ModConfigSpec.IntValue TRIAL_RESET_SECONDS;
    public static final ModConfigSpec.DoubleValue WARDEN_HEALTH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("labyrinth");
        ENGINE = b.comment("Which dungeon the Dungeon Maws, the Labyrinth Gates and /multiverse dungeon enter lead to: \"labyrinth\" = the Jigsaw-built Labyrinth Spire in the nusmp:labyrinth sky dimension (1.35.0, default); \"legacy\" = the Living Dungeon caves of 1.17-1.34 (nusmp:living_dungeon). Both stay registered; only the routing changes.")
                .define("engine", Labyrinth.ENGINE_LABYRINTH, o -> o instanceof String s && (s.equals(Labyrinth.ENGINE_LABYRINTH) || s.equals(Labyrinth.ENGINE_LEGACY)));
        UNBREAKABLE = b.comment("Every block of the Labyrinth dimension is unbreakable by any means (mining, creative too, explosions, pistons, mob griefing): the Dungeon Guard holds the whole dimension so nobody digs round a sealed door. false = the Spire can be mined.")
                .define("unbreakable", true);
        RELICS_EXCLUSIVE = b.comment("Relics of Many Worlds are found ONLY in the Labyrinth (vault chests, Relic Pedestals, boss drops). false = the 1.24 behaviour: relics may also roll in ordinary Overworld chests.")
                .define("relicsExclusive", true);
        VOID_RESCUE = b.comment("A player who falls off the Spire into the void is caught and put back on the arrival pad with 4 damage instead of dying. false = the void kills as usual.")
                .define("voidRescue", true);
        TRIAL_RESET_SECONDS = b.comment("Seconds after a Sealed Archway's trial is abandoned (every player left its box) before its barriers fall and it can be fought again.")
                .defineInRange("trialResetSeconds", 60, 10, 600);
        WARDEN_HEALTH = b.comment("Max health of the Floor Warden mini-boss that drops the Shadow Key.")
                .defineInRange("wardenHealth", 200.0, 20.0, 2000.0);
        BOSS_ROSTER_ENABLED = b.comment("The Peak Altar rolls its floor boss from the roster written into the structure (the Zogratis siblings, a Floor Warden). false = the altar always calls a Floor Warden.")
                .define("bossRosterEnabled", true);
        b.pop();
        SPEC = b.build();
    }

    private LabyrinthConfig() {}

    /** The value, or its default while the server config is not loaded yet (registration, world-gen threads, the client's own guesses). */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
