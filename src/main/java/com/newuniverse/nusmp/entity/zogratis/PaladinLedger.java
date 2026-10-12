package com.newuniverse.nusmp.entity.zogratis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Who Lucius has turned into a Soul Paladin and what state each one is in (pure, generic over the id: a UUID in the game). A Paladin
 * is <b>UP</b> (fighting) or <b>DOWNED</b> (struck down: it kneels with a soul tether to Lucius and rises on its own after a delay,
 * the revive timer). Cutting the soul or the death of Lucius takes the Paladin out of the ledger for good. The ledger is bounded
 * by the cap the caller asks for when adding.
 */
public final class PaladinLedger<K> {
    public enum State { UP, DOWNED }

    private static final class Entry {
        State state = State.UP;
        long riseAt;
        int falls;
    }

    private final Map<K, Entry> map = new LinkedHashMap<>();

    /** Adds a new Paladin if there are fewer than 'cap' and it is not there already. */
    public boolean add(K id, int cap) {
        if (id == null || map.containsKey(id) || map.size() >= Math.max(0, cap)) return false;
        map.put(id, new Entry());
        return true;
    }

    public boolean contains(K id) { return map.containsKey(id); }
    public int size() { return map.size(); }
    public boolean isEmpty() { return map.isEmpty(); }

    public State stateOf(K id) {
        Entry e = map.get(id);
        return e == null ? null : e.state;
    }

    public int standing() { return count(State.UP); }
    public int downed() { return count(State.DOWNED); }

    private int count(State s) {
        int n = 0;
        for (Entry e : map.values()) if (e.state == s) n++;
        return n;
    }

    /** A Paladin was struck down: it kneels for 'reviveTicks' ticks. False if it is unknown or already down. */
    public boolean down(K id, long now, int reviveTicks) {
        Entry e = map.get(id);
        if (e == null || e.state == State.DOWNED) return false;
        e.state = State.DOWNED;
        e.riseAt = now + Math.max(1, reviveTicks);
        e.falls++;
        return true;
    }

    /** A downed Paladin gets up now (the timer ran out, or Lucius raised it). False if it was not down. */
    public boolean raise(K id) {
        Entry e = map.get(id);
        if (e == null || e.state != State.DOWNED) return false;
        e.state = State.UP;
        e.riseAt = 0;
        return true;
    }

    /** The downed Paladins whose timer has run out by 'now', oldest first. */
    public List<K> due(long now) {
        List<K> out = new ArrayList<>();
        for (Map.Entry<K, Entry> en : map.entrySet()) if (en.getValue().state == State.DOWNED && now >= en.getValue().riseAt) out.add(en.getKey());
        return out;
    }

    /** Every downed Paladin, oldest first. */
    public List<K> downedIds() {
        List<K> out = new ArrayList<>();
        for (Map.Entry<K, Entry> en : map.entrySet()) if (en.getValue().state == State.DOWNED) out.add(en.getKey());
        return out;
    }

    public List<K> ids() { return new ArrayList<>(map.keySet()); }

    /** Ticks until a downed Paladin rises, 0 if it is up or unknown. */
    public int riseIn(K id, long now) {
        Entry e = map.get(id);
        return e == null || e.state != State.DOWNED ? 0 : (int) Math.max(0, e.riseAt - now);
    }

    /** How many times this Paladin has been struck down. */
    public int falls(K id) {
        Entry e = map.get(id);
        return e == null ? 0 : e.falls;
    }

    /** The soul was cut, or the Paladin is gone: it leaves the ledger. */
    public boolean remove(K id) { return map.remove(id) != null; }

    public void clear() { map.clear(); }
}
