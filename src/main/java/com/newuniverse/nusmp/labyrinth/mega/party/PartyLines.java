package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: {@code /multiverse mega party ...} is how an admin looks at the parties of the dungeon, and the headless CI smoke test drives it through the server console and
 * PARSES what comes back ("party 1 team key=... name=... holy=1 abyssal=0 online=0"). A format that drifts by one word turns a working feature into a failed pipeline, and the
 * words are the only interface the script has, so every line the command prints for the script is built here, on plain values, where a test can pin the exact text (the same
 * arrangement as {@code gen.CommandLines}). The command only collects the numbers. Refusals use the generator's {@code verb: refused (reason)} shape.
 */
public final class PartyLines {
    private PartyLines() {}

    /** {@code party: mode=<party|shared> instanced=<true|false> maxParties=<n> known=<n> sectors=<n>}: the first line of {@code party list}. */
    public static String header(String mode, boolean instanced, int maxParties, int known, int sectors) {
        return "party: mode=" + mode + " instanced=" + instanced + " maxParties=" + maxParties + " known=" + known + " sectors=" + sectors;
    }

    /**
     * {@code party <index> <team|solo|unowned> key=<uuid|none> name=<name> holy=<n> abyssal=<n> online=<n>}: one party of the list. {@code unowned} is a lane that has floors
     * but no party in the book (a lost book, or a party an admin moved away from it); {@code holy} and {@code abyssal} are the floors built on each road, {@code online}
     * the players who are inside its floors now.
     */
    public static String row(int index, String kind, String key, String name, int holy, int abyssal, int online) {
        return "party " + index + " " + kind + " key=" + (key == null || key.isEmpty() ? "none" : key) + " name=" + clean(name) + " holy=" + holy + " abyssal=" + abyssal + " online=" + online;
    }

    /** {@code party of <player>: index=<n|none> kind=<team|solo> key=<uuid> name=<name> where=<party <n> floor <f>|party <n>|outside>}. */
    public static String of(String player, int index, String kind, String key, String name, String where) {
        return "party of " + player + ": index=" + (index < 0 ? "none" : Integer.toString(index)) + " kind=" + kind + " key=" + key + " name=" + clean(name) + " where=" + where;
    }

    /** {@code party set: <player> is in party <index> now (was <before|new>, key <uuid>, <kind>)}. */
    public static String set(String player, int index, int before, String key, String kind) {
        return "party set: " + player + " is in party " + index + " now (was " + (before < 0 ? "new" : Integer.toString(before)) + ", key " + key + ", " + kind + ")";
    }

    /** {@code party tp: party <index> <holy|abyssal> floor <label> (sector <id>)}. */
    public static String tp(int index, Polarity road, int floor, int sectorId) {
        return "party tp: party " + index + " " + road.id() + " floor " + FloorMap.label(floor) + " (sector " + sectorId + ")";
    }

    /** {@code party <verb>: refused (<reason>)}. */
    public static String refused(String verb, String reason) { return "party " + verb + ": refused (" + reason + ")"; }

    /** A name on one line: no line break and no empty name (a name is shown after {@code name=} and the script splits on spaces only for the numbers behind it). */
    static String clean(String name) {
        if (name == null || name.isBlank()) return "-";
        return name.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
