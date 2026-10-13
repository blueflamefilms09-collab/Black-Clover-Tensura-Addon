package com.newuniverse.nusmp.dungeon.sentient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;

/**
 * The Dungeon's memory of its rooms: for each room that was ever rolled, whether it is populated (BORN, with the time of its last birth) or
 * barren, and when a player last stood in it. It holds at most {@link #MAX_ROOMS} rooms and forgets the one longest unseen when it is full.
 * It saves as four parallel arrays, least recently seen first. Pure (RoomSave puts it into the world's data).
 */
public final class RoomBook {
    public static final int MAX_ROOMS = 100_000;
    /** The two states of a rolled room. */
    public static final byte BARREN = 1, BORN = 2;

    /** One room's record. */
    public static final class Room {
        public byte state;
        public long lastBirth, lastSeen;
    }

    private final int max;
    private final LinkedHashMap<Long, Room> rooms;

    public RoomBook() { this(MAX_ROOMS); }

    RoomBook(int max) {
        this.max = Math.max(1, max);
        this.rooms = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Room> eldest) { return size() > RoomBook.this.max; }
        };
    }

    public int size() { return rooms.size(); }

    /** The record of a room, or null if it was never rolled (or was forgotten). Looking marks the room as seen now. */
    public Room lookup(long key, long now) {
        Room r = rooms.get(key);
        if (r != null) r.lastSeen = now;
        return r;
    }

    /** Records a room's roll: BORN stamps the time of the birth. */
    public Room record(long key, byte state, long now) {
        Room r = rooms.computeIfAbsent(key, k -> new Room());
        r.state = state;
        r.lastSeen = now;
        if (state == BORN) r.lastBirth = now;
        return r;
    }

    /** Forgets the rooms whose key passes the test; returns how many. */
    public int forget(LongPredicate which) {
        int n = 0;
        for (Iterator<Map.Entry<Long, Room>> it = rooms.entrySet().iterator(); it.hasNext(); ) {
            if (which.test(it.next().getKey())) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    public int count(byte state) {
        int n = 0;
        for (Room r : rooms.values()) if (r.state == state) n++;
        return n;
    }

    // ---------------------------------------------------------------- saving
    /** The rooms as arrays: keys, last births, last sightings, states; least recently seen first. */
    public Saved toArrays() {
        int n = rooms.size();
        long[] keys = new long[n], births = new long[n], seen = new long[n];
        byte[] states = new byte[n];
        int i = 0;
        for (Map.Entry<Long, Room> e : rooms.entrySet()) {
            keys[i] = e.getKey();
            births[i] = e.getValue().lastBirth;
            seen[i] = e.getValue().lastSeen;
            states[i] = e.getValue().state;
            i++;
        }
        return new Saved(keys, births, seen, states);
    }

    /** Fills the book from saved arrays (mismatched lengths read as far as the shortest; states that are neither BARREN nor BORN are skipped). */
    public static RoomBook fromArrays(long[] keys, long[] births, long[] seen, byte[] states) {
        RoomBook b = new RoomBook();
        int n = Math.min(Math.min(keys.length, births.length), Math.min(seen.length, states.length));
        for (int i = 0; i < n; i++) {
            if (states[i] != BARREN && states[i] != BORN) continue;
            Room r = new Room();
            r.state = states[i];
            r.lastBirth = births[i];
            r.lastSeen = seen[i];
            b.rooms.put(keys[i], r);
        }
        return b;
    }

    /** The saved form of the book. */
    public record Saved(long[] keys, long[] births, long[] seen, byte[] states) {}

    /** The keys in the order the book keeps them (least recently seen first). */
    public List<Long> keys() { return new ArrayList<>(rooms.keySet()); }
}
