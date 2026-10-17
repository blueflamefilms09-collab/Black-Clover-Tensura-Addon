package com.newuniverse.nusmp.labyrinth.mega.gen;

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
