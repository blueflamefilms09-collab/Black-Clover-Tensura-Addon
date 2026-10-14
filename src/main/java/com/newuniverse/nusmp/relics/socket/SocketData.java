package com.newuniverse.nusmp.relics.socket;

import java.util.ArrayList;
import java.util.List;

/**
 * What the {@code nusmp:socket} component says, as plain data: the bound ids in order and the game time each was bound at. Immutable; every change
 * returns a new value. Pure (the NBT form is {@link SocketComponents}'s).
 */
public record SocketData(List<Entry> entries) {
    /** One bound Core: its socket id ("spirit:fire") and the game time of the binding. */
    public record Entry(String id, long boundAt) {}

    public static final SocketData EMPTY = new SocketData(List.of());
    /** No relic has more sockets than this. */
    public static final int MAX = 2;

    public SocketData {
        entries = List.copyOf(entries);
    }

    public int size() { return entries.size(); }

    public boolean isEmpty() { return entries.isEmpty(); }

    public boolean has(String id) {
        for (Entry e : entries) if (e.id().equals(id)) return true;
        return false;
    }

    public SocketData with(Entry e) {
        List<Entry> l = new ArrayList<>(entries);
        l.add(e);
        return new SocketData(l);
    }

    /** Without the entry at this index (unchanged if the index is out of range). */
    public SocketData without(int index) {
        if (index < 0 || index >= entries.size()) return this;
        List<Entry> l = new ArrayList<>(entries);
        l.remove(index);
        return new SocketData(l);
    }
}
