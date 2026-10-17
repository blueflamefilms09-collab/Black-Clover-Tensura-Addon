package com.newuniverse.nusmp.labyrinth.mega;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Mega-Dungeon core, {@code world/serverconfig/nusmp-labyrinth-mega-server.toml}, section {@code [mega]}. A separate
 * spec (the pattern of {@link com.newuniverse.nusmp.labyrinth.LabyrinthConfig} and {@code DndConfig}). Only plain {@code define} /
 * {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading). Until 1.58.0 it also held
 * {@code layout} (Mega-Dungeon or the 1.35.0 Spire) and {@code legacyGates} (may the old Dungeon Gate arches and Maws lead in); the Mega-Dungeon is the
 * only dungeon now and both keys are gone (an old file that still has them is simply not read). Every reader goes through {@link #get}, which answers the default while the spec is not loaded yet (world-gen and block registration run
 * before SERVER configs are read). The other Mega builders keep THEIR switches in their own config classes (the DndConfig pattern), so no two
 * builders edit one spec; the keys here are the generator's and the gateway's.
 */
public final class MegaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue EXPANSION, LAST_LANDING, EXPAND_AFTER_VAULT;
    public static final ModConfigSpec.IntValue PIECES_PER_TICK, MAX_PIECES, MAX_SECTORS, ASSEMBLE_TRIES, MAX_LINK_MODULES, GEN_BUDGET_MS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("mega");
        EXPANSION = b.comment("Opening a sector's Gatekeeper door with the Labyrinth Guardian's Mana Core generates the next sector. false = the dungeon only grows with /multiverse mega expand.")
                .define("expansion", true);
        LAST_LANDING = b.comment("Entering takes you to the deepest Landing you ever reached (sneak while entering = sector 0). false = always sector 0.")
                .define("lastLanding", true);
        EXPAND_AFTER_VAULT = b.comment("1.55.0: the next sector is generated when the Crucible of the unsealed sector is resolved (or a player reaches the Way Onward), not the moment the Gatekeeper opens, so the relic taken can decide the next sector's theme. false = the 1.54.0 behaviour.")
                .define("expandAfterVault", true);
        PIECES_PER_TICK = b.comment("Jigsaw pieces placed per server tick while a sector is being generated (higher = faster, laggier).")
                .defineInRange("piecesPerTick", 3, 1, 16);
        MAX_PIECES = b.comment("Upper bound on the pieces of one sector (rooms, corridors and door caps together).")
                .defineInRange("maxPiecesPerSector", 450, 100, 2000);
        MAX_SECTORS = b.comment("The dungeon stops growing after this many sectors.")
                .defineInRange("maxSectors", 500, 1, 10000);
        ASSEMBLE_TRIES = b.comment("How many seeds the generator tries before it gives up on a sector layout that passes the audit and uses the fallback.")
                .defineInRange("assembleTries", 24, 1, 64);
        MAX_LINK_MODULES = b.comment("Longest straight tunnel (in 9-block modules) between two sectors when the next region would collide with an old one. 0 = never build links.")
                .defineInRange("maxLinkModules", 40, 0, 60);
        GEN_BUDGET_MS = b.comment("Milliseconds of server time one tick may spend placing the pieces of a sector (at least one piece is always placed, and never more than piecesPerTick). Lower = smoother, but the sector takes longer to appear.")
                .defineInRange("genBudgetMs", 30, 5, 200);
        b.pop();
        SPEC = b.build();
    }

    private MegaConfig() {}

    /** The value, or its default while the server config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
