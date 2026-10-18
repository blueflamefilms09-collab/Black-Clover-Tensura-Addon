package com.newuniverse.nusmp.elite.hub;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Dungeon Map is "fog-of-war aware", which needs the server to remember WHICH ROOMS each player has stood in, and that memory has to follow the player (a data attachment, copied on death)
 * and stay small however long a server runs. A room is addressed by its sector id and its room id packed into one long ({@link #key}), so a party's separate floors never collide. The set keeps
 * the most recent {@link #MAX_VISITED} rooms (the oldest are forgotten first: a floor you left a hundred floors ago matters less than the one you are on), and a second small set remembers which
 * sectors the player has already MAPPED (for {@code map.mode = per_floor}: opening the map again on a floor you paid for is free). Pure, so it is unit tested; the codec is in
 * {@link HubAttachments}.
 */
public final class MapMemory {
    /** The most visited rooms and mapped sectors kept per player. */
    public static final int MAX_VISITED = 8192, MAX_MAPPED = 1024;

    public record Data(List<Long> visited, List<Long> mapped) {
        public static final Data EMPTY = new Data(List.of(), List.of());
    }

    private final LinkedHashSet<Long> visited = new LinkedHashSet<>();
    private final LinkedHashSet<Long> mapped = new LinkedHashSet<>();

    /** The packed address of a room. */
    public static long key(int sectorId, int roomId) { return ((long) sectorId << 32) | (roomId & 0xFFFFFFFFL); }

    /** Notes that the player stood in the room; true when that is news. The newest rooms are kept when the set is full. */
    public boolean visit(int sectorId, int roomId) {
        long k = key(sectorId, roomId);
        if (visited.contains(k)) return false;
        visited.add(k);
        trim(visited, MAX_VISITED);
        return true;
    }

    public boolean visited(int sectorId, int roomId) { return visited.contains(key(sectorId, roomId)); }

    public int visitedCount() { return visited.size(); }

    /** Notes that the player mapped this sector (a map was used on it). True when that is news. */
    public boolean markMapped(int sectorId) {
        if (!mapped.add((long) sectorId)) return false;
        trim(mapped, MAX_MAPPED);
        return true;
    }

    public boolean isMapped(int sectorId) { return mapped.contains((long) sectorId); }

    public boolean hasContent() { return !visited.isEmpty() || !mapped.isEmpty(); }

    private static void trim(Set<Long> set, int max) {
        Iterator<Long> it = set.iterator();
        while (set.size() > max && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    public Data toData() { return new Data(new ArrayList<>(visited), new ArrayList<>(mapped)); }

    public static MapMemory of(Data d) {
        MapMemory m = new MapMemory();
        if (d == null) return m;
        if (d.visited() != null) for (Long l : d.visited()) if (l != null) m.visited.add(l);
        if (d.mapped() != null) for (Long l : d.mapped()) if (l != null) m.mapped.add(l);
        trim(m.visited, MAX_VISITED);
        trim(m.mapped, MAX_MAPPED);
        return m;
    }
}
