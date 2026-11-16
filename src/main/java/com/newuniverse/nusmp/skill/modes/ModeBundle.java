package com.newuniverse.nusmp.skill.modes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * An ordered, immutable list of {@link ModeSpec}: the mode table of one multi-mode skill. The index is the mode number Tensura stores, so order is permanent (append only). {@link #next} walks the wheel
 * the way the mode key does and skips a mode the player has not unlocked (never an endless loop: it gives up after one full turn and stays where it is).
 */
public final class ModeBundle {
    private final String id;
    private final List<ModeSpec> modes;

    public ModeBundle(String id, List<ModeSpec> modes) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("no bundle id");
        if (modes == null || modes.isEmpty()) throw new IllegalArgumentException("no modes in " + id);
        Set<String> seen = new HashSet<>();
        for (ModeSpec m : modes) if (!seen.add(m.id())) throw new IllegalArgumentException("duplicate mode id " + m.id() + " in " + id);
        this.id = id;
        this.modes = List.copyOf(modes);
    }

    public String id() { return id; }
    public List<ModeSpec> modes() { return modes; }
    public int size() { return modes.size(); }

    /** The mode at a number, wrapped (a stale saved number never throws). */
    public ModeSpec at(int mode) { return modes.get(Math.floorMod(mode, modes.size())); }

    /** The number of a mode id, or -1. */
    public int index(String modeId) {
        for (int i = 0; i < modes.size(); i++) if (modes.get(i).id().equals(modeId)) return i;
        return -1;
    }

    /** The next (or previous) mode number whose {@code open} test passes; the same number when nothing else is open. */
    public int next(int current, boolean reverse, IntPredicate open) {
        int n = modes.size(), step = reverse ? -1 : 1, from = Math.floorMod(current, n);
        for (int k = 1; k <= n; k++) {
            int c = Math.floorMod(from + step * k, n);
            if (open.test(c)) return c;
        }
        return from;
    }
}
