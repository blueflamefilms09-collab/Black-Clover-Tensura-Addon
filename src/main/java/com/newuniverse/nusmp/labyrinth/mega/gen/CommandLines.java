package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.List;
import java.util.Locale;

/**
 * Why: the headless CI smoke test drives the Mega-Dungeon with {@code /multiverse mega ...} through the server console and RCON and PARSES what comes back
 * ("expand: queued sector 0", "audit 0 OK", the status lines). A format that drifts by one word turns a working dungeon into a failed pipeline, and the
 * words are the only interface the script has, so every line the command prints for the script is built here, on plain values, where a test can pin the
 * exact text. {@code MegaCommand} only collects the numbers.
 */
public final class CommandLines {
    private CommandLines() {}

    /** The first status line: {@code mega: expansionLevel=<n> sectors=<n> job=<idle|<percent>%>}. A progress below zero means no job is queued or running. */
    public static String status(int expansionLevel, int sectors, float progress) {
        return "mega: expansionLevel=" + expansionLevel + " sectors=" + sectors + " job=" + (progress < 0f ? "idle" : percent(progress) + "%");
    }

    /** {@code sector <id> <STATE> theme=<theme id> threat=<n> rooms=<n> edges=<n> pieces=<n> relics=<n> region=<x1>,<z1>,<x2>,<z2>}. */
    public static String sector(int id, String state, String theme, int threat, int rooms, int edges, int pieces, int relics, int x1, int z1, int x2, int z2) {
        return "sector " + id + " " + state + " theme=" + theme + " threat=" + threat + " rooms=" + rooms + " edges=" + edges + " pieces=" + pieces + " relics=" + relics
                + " region=" + x1 + "," + z1 + "," + x2 + "," + z2;
    }

    /**
     * The sector line followed by the road it belongs to (1.55.0): {@code <sector line> road=<holy|abyssal> chain=<k> floors=<first>..<last>}, the floors signed
     * ({@code +1..+10}, {@code -11..-20}). The words before {@code road=} are exactly {@link #sector}'s, so a script that parses the 1.54.0 line still does.
     */
    public static String sectorOnRoad(String sectorLine, Polarity road, int chainIndex, int firstFloor, int lastFloor) {
        return sectorLine + " road=" + road.id() + " chain=" + chainIndex + " floors=" + FloorMap.label(firstFloor) + ".." + FloorMap.label(lastFloor);
    }

    /**
     * The header of one road in {@code status}: {@code road <holy|abyssal>: sectors=<n> ids=<id,id> floors=<first>..<last> state=<STATE of its newest sector>}, or
     * {@code road <id>: not opened} when the road has no sector yet. {@code ids} are the global sector ids in chain order; the floors run from the first floor of the
     * road's first sector to the last floor of its newest one.
     */
    public static String road(Polarity road, List<Integer> ids, int firstFloor, int lastFloor, String newestState) {
        if (ids.isEmpty()) return "road " + road.id() + ": not opened";
        StringBuilder b = new StringBuilder("road ").append(road.id()).append(": sectors=").append(ids.size()).append(" ids=");
        for (int i = 0; i < ids.size(); i++) b.append(i == 0 ? "" : ",").append(ids.get(i));
        return b.append(" floors=").append(FloorMap.label(firstFloor)).append("..").append(FloorMap.label(lastFloor)).append(" state=").append(newestState).toString();
    }

    /** {@code <verb>: queued sector <id> on the <holy|abyssal> road (floors <first>..<last>)}: {@link #queued}'s words first, then where the sector goes. */
    public static String queuedOn(String verb, int id, Polarity road, int firstFloor, int lastFloor) {
        return queued(verb, id) + " on the " + road.id() + " road (floors " + FloorMap.label(firstFloor) + ".." + FloorMap.label(lastFloor) + ")";
    }

    /**
     * The stairs of a floor of the floor model, the line {@code /multiverse mega sector <id>} prints after the landmarks and the CI smoke test parses:
     * {@code   stairs back=<x,y,z|none> forward=<x,y,z|none> forwardPad=<x,y,z|none> leadsOn=<sector id|none> leadsBack=<sector id|hub|none> boss=<true|false> climbs=<up|down>}.
     * {@code leadsOn} is the id of the sector the forward stairs lead to, {@code leadsBack} the one the stairs back lead to ({@code hub} from the first floor), {@code none}
     * where that floor does not exist (yet). {@code climbs} is the way the FORWARD stairs go: {@code up} on the Holy road, {@code down} on the Abyssal road.
     */
    public static String stairs(String back, String forward, String forwardPad, StairLinks.Dest backDest, StairLinks.Dest forwardDest, Polarity road, boolean boss, int forwardId, int backId) {
        String leadsBack = backDest.hub() ? "hub" : backId < 0 ? "none" : Integer.toString(backId);
        return "  stairs back=" + (back == null ? "none" : back) + " forward=" + (forward == null ? "none" : forward) + " forwardPad=" + (forwardPad == null ? "none" : forwardPad)
                + " leadsOn=" + (forwardId < 0 ? "none" : Integer.toString(forwardId)) + " leadsBack=" + leadsBack + " boss=" + boss + " climbs=" + (road == Polarity.ABYSSAL ? "down" : "up");
    }

    /** {@code audit <id> OK} or {@code audit <id> FAIL: <failure>; <failure>}. */
    public static String audit(int id, List<String> failures) {
        return failures.isEmpty() ? "audit " + id + " OK" : "audit " + id + " FAIL: " + String.join("; ", failures);
    }

    /** {@code <verb>: queued sector <id>}. */
    public static String queued(String verb, int id) { return verb + ": queued sector " + id; }

    /** {@code <verb>: refused (<reason>)}. */
    public static String refused(String verb, String reason) { return verb + ": refused (" + reason + ")"; }

    /** {@code plan: <path>}. */
    public static String plan(String path) { return "plan: " + path; }

    /** One line of the room listing: {@code <kind> <count>}, the kind in lower case ({@code set_piece}). */
    public static String roomKind(RoomKind kind, int count) { return kind.name().toLowerCase(Locale.ROOT) + " " + count; }

    /** The progress of a job as a whole percent, clamped to 0..100. */
    public static int percent(float progress) { return Math.max(0, Math.min(100, Math.round(progress * 100f))); }
}
