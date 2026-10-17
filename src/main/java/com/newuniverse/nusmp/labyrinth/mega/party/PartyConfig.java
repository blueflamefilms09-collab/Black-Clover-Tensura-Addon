package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the dungeon parties (1.58.0), {@code world/serverconfig/nusmp-labyrinth-mega-party-server.toml}, section {@code [party]}. Its own file (the pattern of
 * {@code DndConfig}, {@code HubConfig} and {@code MegaConfig}) so no two builders edit one spec and a server owner who wants the dungeon as it was before parties flips one
 * word: {@code mode = "shared"}. Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is
 * still loading, so the mode is a string that {@link PartyPolicy.Mode#parse} reads leniently) and every reader goes through {@link #get}, which answers the default while the
 * spec is not loaded yet (a chunk or a self-test can ask before the server config is read).
 */
public final class PartyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> MODE;
    public static final ModConfigSpec.IntValue MAX_PARTIES;
    public static final ModConfigSpec.BooleanValue USE_FTB_TEAMS, GUARD, OPS_BYPASS, TEAM_INHERITS_LANE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("party");
        MODE = b.comment("\"party\" (default): the Mega-Dungeon is instanced. Every party (an FTB Team, or a single player without one) gets its own Holy and Abyssal chain of floors on its own lane,",
                        "and only that party ever stands in it; the Hub stays one shared town. \"shared\": everybody is party 0 and walks the same floors, the way the dungeon worked before 1.58.0.",
                        "Parties are only separated in the floor model (floors.perSector = 1); with the old ten-floor sectors the dungeon stays shared whatever this says.")
                .define("mode", "party");
        MAX_PARTIES = b.comment("The most parties the dungeon gives lanes to (a lane is 4096 blocks from the next; with the default 64 the farthest lane is at z = 558 048 and the nearest at z = -258 048,",
                        "far inside the vanilla world border; 256 is the most the lane maths knows). A party that finds the book full is told so and cannot enter until an admin raises this or frees a lane.")
                .defineInRange("maxParties", 64, 1, 256);
        USE_FTB_TEAMS = b.comment("Teammates share a party: the FTB Team of a player is their party (through the same soft bridge the squads use; the mod works without FTB Teams installed).",
                        "false (or no FTB Teams): every player is a party of one.")
                .define("useFtbTeams", true);
        GUARD = b.comment("A player whose party is not the party of the floor they stand in is sent out of the dungeon (to the Hub) within a second. The backstop for teleports and bugs, like the Directional Lock's hard guard.")
                .define("guard", true);
        OPS_BYPASS = b.comment("Operators (permission level 2) and spectators are never sent out of a floor of another party, so an admin can look at any party's floors.")
                .define("opsBypass", true);
        TEAM_INHERITS_LANE = b.comment("A new team takes over the lane of one of its members who explored alone before (the owner first), so founding a party does not leave the floors of the founder behind.",
                        "false: a new team always starts on a fresh lane.")
                .define("teamInheritsLane", true);
        b.pop();
        SPEC = b.build();
    }

    private PartyConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The configured mode ({@code party} unless the file says {@code shared}). */
    public static PartyPolicy.Mode mode() { return PartyPolicy.Mode.parse(get(MODE)); }

    /** The most parties, kept inside the range the lane maths knows. */
    public static int maxParties() { return Math.max(1, Math.min(MegaGrid.MAX_PARTIES, get(MAX_PARTIES))); }
}
