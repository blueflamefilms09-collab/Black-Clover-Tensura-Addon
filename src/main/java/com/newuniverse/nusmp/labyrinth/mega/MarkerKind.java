package com.newuniverse.nusmp.labyrinth.mega;

import java.util.Locale;

/**
 * Why: the Mega-Dungeon's templates are built from VANILLA blocks only (the Vanilla Block Primacy Rule), so "where does a patrol node go",
 * "which cell is a seat", "where does the next sector attach" cannot be custom marker blocks. They are vanilla structure blocks in DATA mode
 * whose metadata string names a marker ({@code nusmp:seat;as=chair}): the game never places a DATA structure block (the Jigsaw pool element
 * ignores it), so a marker leaves no trace in the world, costs no custom block, and is read BEFORE placement through the public
 * {@code SinglePoolElement.getDataMarkers(...)}, which already applies the piece's position and rotation. Convention for the cell: the marker
 * stands in the AIR cell directly ABOVE the thing it describes (above the seat stair, above the hearth, above the pedestal), except where the
 * table below says otherwise. Unknown kinds are skipped with one log line (a typo must not break generation). Pure (no Minecraft types).
 */
public enum MarkerKind {
    /** Where a player stands on arrival in a Landing (floor cell + 1). */
    ARRIVAL,
    /** Optional override of a room's centre for patrols and spawn picking. */
    ROOM_CENTER,
    /** A candidate spawn cell for the encounter director (standing cell). args: weight (default 1). */
    SPAWN,
    /** An explicit patrol waypoint inside the room (standing cell). */
    PATROL,
    /** Above a seat block (stairs, sofa): a short-rest anchor. args: as (compat hint), facing. */
    SEAT,
    /** Above a cooking station (campfire, stove, smoker): a short-rest anchor. args: as. */
    HEARTH,
    /** Above a bed (the foot block): a long-rest anchor. args: as. */
    BED,
    /** Above a table / counter / storage block that a compat mod may replace. args: as (table, counter, cutting_board, storage). */
    FURNITURE,
    /** Where the Labyrinth Guardian appears (arena floor cell + 1). */
    GUARDIAN_SPAWN,
    /** The end of the Way Onward: the seal box to clear and the direction the next sector attaches. args: facing, w, h, d. Stands IN the seal's bottom-centre cell of its outer layer. */
    EXPANSION_SOCKET,
    /** The centre of the Crucible Vault floor (+1). */
    VAULT_CENTER,
    /** Above a relic pedestal slot. args: slot (0..2). */
    PEDESTAL,
    /** The native faction of the room. args: faction. */
    FACTION,
    /** The centre of a Gatekeeper door (the middle of the door slab). */
    GATEKEEPER,
    /** A hidden trap site the director may arm. args: type. */
    TRAP;

    /** The id as written after {@code nusmp:} in a metadata string ({@code guardian_spawn}). */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The kind named by {@code id} (case-insensitive, with or without the {@code nusmp:} prefix), or null when unknown. */
    public static MarkerKind byId(String id) {
        if (id == null) return null;
        String s = id.trim();
        if (s.startsWith("nusmp:")) s = s.substring(6);
        for (MarkerKind k : values()) if (k.id().equalsIgnoreCase(s)) return k;
        return null;
    }
}
