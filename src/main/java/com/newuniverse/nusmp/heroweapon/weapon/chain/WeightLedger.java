package com.newuniverse.nusmp.heroweapon.weapon.chain;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Vassal Chain is heavy, and its heavy links (the passive of every form) must be felt in a fight: each melee blow hangs another link's weight on the creature it
 * strikes, and at the third the weight takes hold and the creature is slowed. That is a small counter per creature with a time limit and a size limit, as plain data on
 * the game clock with tests; the game side only feeds it blows and applies the slow when it answers "taken hold". It is NOT the Sloth curse (that is a form with its own
 * techniques and a price): the links belong to every form of the weapon.
 */
public final class WeightLedger {
    /** The third blow in a row (each within {@link #LIFE} ticks of the last) makes the weight take hold. */
    public static final int MAX_STACKS = 3, LIFE = 60;
    /** At most this many creatures carry weight at once; the oldest is forgotten first. */
    public static final int MAX_TARGETS = 16;

    private record Stack(int n, long until) {}

    private final Map<UUID, Stack> stacks = new LinkedHashMap<>();

    /** Hangs one more link on the creature; returns the count it now carries, or {@link #MAX_STACKS} (and clears it) when the weight has taken hold. */
    public int add(UUID id, long now) {
        prune(now);
        Stack old = stacks.remove(id);
        int n = old == null ? 1 : old.n() + 1;
        if (n >= MAX_STACKS) return MAX_STACKS;
        while (stacks.size() >= MAX_TARGETS) {
            Iterator<UUID> it = stacks.keySet().iterator();
            it.next();
            it.remove();
        }
        stacks.put(id, new Stack(n, now + LIFE));
        return n;
    }

    /** How many links the creature carries now. */
    public int stacks(UUID id, long now) {
        Stack s = stacks.get(id);
        return s != null && now < s.until() ? s.n() : 0;
    }

    public int size(long now) {
        prune(now);
        return stacks.size();
    }

    private void prune(long now) {
        stacks.values().removeIf(s -> now >= s.until());
    }

    public void clear() {
        stacks.clear();
    }
}
