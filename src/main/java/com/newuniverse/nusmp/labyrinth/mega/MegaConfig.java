package com.newuniverse.nusmp.labyrinth.mega;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Mega-Dungeon core, {@code world/serverconfig/nusmp-labyrinth-mega-server.toml}, section {@code [mega]}. A separate
 * spec (the pattern of {@link com.newuniverse.nusmp.labyrinth.LabyrinthConfig} and {@code DndConfig}) because the owner's rule is "add the new,
 * keep the old": the Spire's file keeps its keys byte for byte, and a server that wants the 1.35 Spire back flips ONE key here
 * ({@code layout = "spire"}). Only plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while
 * the server is still loading); the {@code layout} String gets a Predicate validator, which NeoForge resolves to the default on a bad value.
 * Every reader goes through {@link #get}, which answers the default while the spec is not loaded yet (world-gen and block registration run
 * before SERVER configs are read). The other Mega builders keep THEIR switches in their own config classes (the DndConfig pattern), so no two
 * builders edit one spec; the keys here are the generator's and the gateway's.
 */
public final class MegaConfig {
    public static final ModConfigSpec SPEC;
    /** "mega" (the singular expanding Mega-Dungeon, default) or "spire" (the 1.35.0 floating Spire of the Labyrinth). */
    public static final ModConfigSpec.ConfigValue<String> LAYOUT;
    public static final ModConfigSpec.BooleanValue EXPANSION, LEGACY_GATES, LAST_LANDING, EXPAND_AFTER_VAULT;
    public static final ModConfigSpec.IntValue PIECES_PER_TICK, MAX_PIECES, MAX_SECTORS, ASSEMBLE_TRIES, MAX_LINK_MODULES, GEN_BUDGET_MS;

    public static final String LAYOUT_MEGA = "mega", LAYOUT_SPIRE = "spire";
    /** The default layout: the Mega-Dungeon replaces the Spire as the default dungeon (the owner's decision). It was left at "spire" by mistake in 1.55.0 and 1.56.0; flipped in 1.57.0. A server config that already says "spire" keeps it. */
    public static final String DEFAULT_LAYOUT = LAYOUT_MEGA;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("mega");
        LAYOUT = b.comment("Which Labyrinth the Gates, the Dungeon Maws and /multiverse labyrinth enter lead to: \"mega\" = the singular, expanding Mega-Dungeon in the nusmp:mega_dungeon dimension (1.54.0, default); \"spire\" = the floating Spire of 1.35.0 in nusmp:labyrinth. Both stay registered; only the routing changes (and labyrinth.engine = \"legacy\" still wins over both).")
                .define("layout", DEFAULT_LAYOUT, o -> o instanceof String s && (s.equals(LAYOUT_MEGA) || s.equals(LAYOUT_SPIRE)));
        EXPANSION = b.comment("Opening a sector's Gatekeeper door with the Labyrinth Guardian's Mana Core generates the next sector. false = the dungeon only grows with /multiverse mega expand.")
                .define("expansion", true);
        LEGACY_GATES = b.comment("The old Dungeon Gate arches and Dungeon Maws also lead into the Mega-Dungeon. false = they refuse and point at the Gateway (the one structure of this layout).")
                .define("legacyGates", true);
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
