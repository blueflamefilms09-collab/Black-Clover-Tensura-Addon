package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Why: when the Dungeon Master does nothing for a minute, the log must say why in one line, or the owner is left guessing between "the handler never ran",
 * "the dice said no", "everything is on cooldown" and "the rolls came up but no place was found". This collects the outcome of every pass since the last report
 * (a count per reason and the latest detail of each) and writes the line: nothing when an encounter did spawn in the window (the Dungeon Master was not idle), a
 * "no party was rolled for" line when no pass happened at all while players were in the dungeon. Pure; the Director feeds it and prints the line.
 */
public final class IdleReport {
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final Map<String, String> detail = new LinkedHashMap<>();
    private int passes;
    private int spawned;
    private int playerTicks;

    /** A pass of the Director with a party: the reason name (an {@code EncounterDirector.Kind}) and a short detail. */
    public void pass(String reason, String text, boolean spawnedOne) {
        passes++;
        if (spawnedOne) spawned++;
        counts.merge(reason, 1, Integer::sum);
        detail.put(reason, text == null ? "" : text);
    }

    /** One tick of the handler with players in the dungeon (so "no passes at all" can be told from "no players"). */
    public void playersPresent() { playerTicks++; }

    public int passes() { return passes; }

    public int spawned() { return spawned; }

    /** Were players in the dungeon during the window? */
    public boolean worthReporting() { return playerTicks > 0; }

    /** The line for the window, or null when the Director was not idle (an encounter spawned) or nobody was in the dungeon. Does not reset. */
    public String line(int seconds) {
        if (playerTicks == 0 || spawned > 0) return null;
        if (passes == 0) {
            return String.format(Locale.ROOT, "idle for %d s with players in the dungeon: no party was rolled for at all (the handler saw players on %d ticks; read the gates with /multiverse mega dm why)", seconds, playerTicks);
        }
        StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "idle for %d s: %d passes, no encounter:", seconds, passes));
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            b.append(' ').append(e.getKey()).append(" x").append(e.getValue());
            String d = detail.get(e.getKey());
            if (d != null && !d.isEmpty()) b.append(" (").append(d.length() > 140 ? d.substring(0, 140) + "..." : d).append(')');
            b.append(';');
        }
        return b.toString();
    }

    public void reset() {
        counts.clear();
        detail.clear();
        passes = 0;
        spawned = 0;
        playerTicks = 0;
    }
}
