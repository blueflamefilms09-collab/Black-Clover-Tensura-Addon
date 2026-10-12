package com.newuniverse.nusmp.entity.zogratis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A boss's move list, checked once (pure): null entries and repeated ids are dropped (the first one stays), because the scheduler
 * keeps cooldowns by id. The order is the order the boss class wrote them in, and an index in this list is what the client is told
 * when a move is being wound up.
 */
public final class MoveBook<M extends MoveSpec> {
    private final List<M> moves;
    private final List<String> dropped;

    private MoveBook(List<M> moves, List<String> dropped) {
        this.moves = List.copyOf(moves);
        this.dropped = List.copyOf(dropped);
    }

    public static <M extends MoveSpec> MoveBook<M> of(List<? extends M> raw) {
        List<M> out = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (raw != null) {
            for (M m : raw) {
                if (m == null) dropped.add("<null>");
                else if (!seen.add(m.id())) dropped.add(m.id());
                else out.add(m);
            }
        }
        return new MoveBook<>(out, dropped);
    }

    public List<M> moves() { return moves; }
    public int size() { return moves.size(); }

    /** The move at an index, or null. */
    public M get(int index) { return index >= 0 && index < moves.size() ? moves.get(index) : null; }

    /** The index of a move id, or -1. */
    public int indexOf(String id) {
        for (int i = 0; i < moves.size(); i++) if (moves.get(i).id().equals(id)) return i;
        return -1;
    }

    public M byId(String id) { return get(indexOf(id)); }

    /** Ids that were dropped (repeats, nulls). */
    public List<String> dropped() { return dropped; }

    /** The moves a boss of this power may use. */
    public List<M> usableAt(int power) {
        List<M> out = new ArrayList<>();
        for (M m : moves) if (m.minPower() <= power) out.add(m);
        return out;
    }

    public List<M> ultimates() {
        List<M> out = new ArrayList<>();
        for (M m : moves) if (m.ultimate()) out.add(m);
        return out;
    }
}
