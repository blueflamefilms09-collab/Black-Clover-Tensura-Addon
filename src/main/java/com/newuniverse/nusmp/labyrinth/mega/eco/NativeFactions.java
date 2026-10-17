package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.Locale;

/**
 * Why: which rooms get a native faction, and which faction, is the one decision the generator hands us when a sector is placed, and it has
 * to be the same decision every time the same sector is rebuilt (a server that stopped mid-generation re-runs the saved plan). The rule is
 * separated from the listener so it can be tested: rooms that only carry traffic (corridors, junctions, links, the Way Onward) and the safe rooms
 * (Landings, Break Levels) belong to nobody, because a monster has no business claiming a place where players rest and a hallway is nobody's
 * home; every other room belongs to the faction its template names with a {@code nusmp:faction;faction=<id>} marker, and when the template names
 * none, to a faction drawn from the region's theme with the seed {@code sectorSeed ^ roomId}. A marker that names an unknown faction (a typo,
 * or a datapack that removed it) falls back to the draw instead of leaving the room unowned. Pure (no Minecraft types).
 */
public final class NativeFactions {
    private NativeFactions() {}

    /** Does a room of this kind get a native faction? Not the safe rooms, not circulation, not the door caps that are never stored as rooms. */
    public static boolean eligible(RoomKind kind) {
        return kind != null && !kind.isSafe() && !kind.isCirculation() && kind != RoomKind.CAP;
    }

    /**
     * The native faction of one room: the marker's faction when it is a known one, else the seeded theme-weighted draw. Null only when no
     * faction is loaded at all (then nothing is assigned, and the next sector tries again).
     */
    public static String choose(FactionGraph graph, long sectorSeed, int roomId, String themeId, String markerFaction) {
        if (graph == null || graph.size() == 0) return null;
        if (markerFaction != null) {
            String hint = markerFaction.trim().toLowerCase(Locale.ROOT);
            if (graph.knows(hint)) return hint;
        }
        return graph.pickNative(sectorSeed ^ roomId, themeId);
    }
}
