package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: a party's floors stand on a LANE (a z coordinate, {@link MegaGrid#laneZ}) and the lane is picked by a small integer, the party index. The index has to be
 * handed out once, the first time a party goes into the dungeon, remembered for ever, never given to a second party by accident, and unaffected by who is online. This
 * is that book, and nothing else: a map from the party's key to its index with the rules of handing out, as plain Java so every rule is tested without a game
 * ({@code MegaParties} saves it, {@code PartyService} asks it). The rules: an index is the lowest one that nobody holds and that no existing floor already uses
 * ({@code reserved}, so a lost book can never hand the lane of somebody's floors to a stranger); there are never more than {@code maxParties} of them; an index may
 * be held by several keys only when an admin put them there on purpose (to merge two groups); and a NEW team may take over the lane of one of its members who played
 * alone before (the owner first), so that a player who has been exploring on their own and then founds a party does not leave their floors behind. In that case the
 * member's own entry is removed, because two keys on one lane would let a player who later leaves the team walk back into the team's floors.
 */
public final class PartyIndex {
    /** One party of the book: its key, whether the key is a team's, the index of its lane and the name it was last seen under (for the admin's lists only). */
    public record Entry(UUID key, boolean team, int index, String label) {}

    /**
     * The answer of {@link #allocate}: the index (negative when none is free), whether the key was new, and, when a new team took over the lane of a member who played
     * alone, that member's id.
     */
    public record Allocation(int index, boolean created, UUID inheritedFrom) {
        public boolean ok() { return index >= 0; }

        public boolean inherited() { return inheritedFrom != null; }
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public int size() { return entries.size(); }

    public Entry get(UUID key) { return key == null ? null : entries.get(key); }

    /** The index of a party, or -1 when the book has never heard of it. */
    public int indexOf(UUID key) {
        Entry e = get(key);
        return e == null ? -1 : e.index();
    }

    /** Every party in the book, the lowest index first, ties in the order they were added. */
    public List<Entry> entries() {
        List<Entry> out = new ArrayList<>(entries.values());
        out.sort(Comparator.comparingInt(Entry::index));
        return out;
    }

    /** The parties that hold an index (normally one; several after an admin merged them). */
    public List<Entry> holders(int index) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries.values()) if (e.index() == index) out.add(e);
        return out;
    }

    public boolean holds(int index) {
        for (Entry e : entries.values()) if (e.index() == index) return true;
        return false;
    }

    /** The lowest index below {@code maxParties} that nobody holds and that is not in {@code reserved}, or -1 when there is none. */
    public int lowestFree(int maxParties, Set<Integer> reserved) {
        int limit = Math.min(Math.max(0, maxParties), MegaGrid.MAX_PARTIES);
        for (int i = 0; i < limit; i++) {
            if (holds(i)) continue;
            if (reserved != null && reserved.contains(i)) continue;
            return i;
        }
        return -1;
    }

    /**
     * The index of a party, handing one out the first time it is asked for. {@code label} is the name to show for it; {@code reserved} the indices that floors already
     * use although no key holds them (never handed out); {@code inheritFrom} the players, most deserving first, whose own lane a NEW team may take over (empty or null for
     * a solo key, for a key that is known already, and when the server switched the idea off). A key that is known keeps its index, whatever else changed. Index -1 means
     * the book is full for {@code maxParties}.
     */
    public Allocation allocate(PartyKey key, String label, int maxParties, Set<Integer> reserved, List<UUID> inheritFrom) {
        Entry known = get(key.id());
        if (known != null) return new Allocation(known.index(), false, null);
        String name = label == null ? "" : label;
        if (key.team() && inheritFrom != null) {
            for (UUID candidate : inheritFrom) {
                Entry c = get(candidate);
                if (c == null || c.team() || c.index() < 0 || c.index() >= MegaGrid.MAX_PARTIES) continue;
                if (holders(c.index()).size() != 1) continue;                      // a lane that two keys share is an admin's merge: a new team does not move into it
                entries.remove(candidate);
                entries.put(key.id(), new Entry(key.id(), true, c.index(), name));
                return new Allocation(c.index(), true, candidate);
            }
        }
        int free = lowestFree(maxParties, reserved);
        if (free < 0) return new Allocation(-1, false, null);
        entries.put(key.id(), new Entry(key.id(), key.team(), free, name));
        return new Allocation(free, true, null);
    }

    /**
     * Puts a party on an index by hand (the admin's {@code party set}): the key is added when it is new and moves when it is not, and it may share the index with
     * another party (that is how two groups are merged). Returns the index it had before, or -1 when it was new. Throws for an index outside the lane maths: callers check.
     */
    public int assign(PartyKey key, String label, int index) {
        if (index < 0 || index >= MegaGrid.MAX_PARTIES) throw new IllegalArgumentException("party index " + index + " is outside 0.." + (MegaGrid.MAX_PARTIES - 1));
        Entry old = get(key.id());
        String name = label != null && !label.isEmpty() ? label : old == null ? "" : old.label();
        entries.put(key.id(), new Entry(key.id(), key.team(), index, name));
        return old == null ? -1 : old.index();
    }

    /** Remembers the name a party is shown under. True when it changed. */
    public boolean relabel(UUID key, String label) {
        Entry e = get(key);
        if (e == null || label == null || label.isEmpty() || label.equals(e.label())) return false;
        entries.put(key, new Entry(e.key(), e.team(), e.index(), label));
        return true;
    }

    /** Takes a party out of the book (its lane stays, with its floors; the index is free again unless floors use it). True when it was there. */
    public boolean forget(UUID key) { return entries.remove(key) != null; }

    /** Puts an entry back when the book is loaded; an entry with an impossible index or a key that is already there is ignored. */
    public void restore(Entry e) {
        if (e == null || e.key() == null || e.index() < 0 || e.index() >= MegaGrid.MAX_PARTIES || entries.containsKey(e.key())) return;
        entries.put(e.key(), e);
    }
}
