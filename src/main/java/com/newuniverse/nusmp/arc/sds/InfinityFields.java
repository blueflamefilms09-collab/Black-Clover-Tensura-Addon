package com.newuniverse.nusmp.arc.sds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Why: Infinity is the power that stops the time of Merlin's own spells, so they never end. In the fight that is the field she lays on the ground: it does
 * not run out on a timer, it lasts until she is staggered (a burst of damage), struck by anti-magic, or dead. Left alone that would fill the room, so
 * she may keep two at once (three at the Gates) and a new one replaces the oldest. This pure list is the whole bookkeeping: which fields stand, which one
 * a new field pushes out, whether a point is inside one. The damage a field does grows the longer you stay in it ({@link MerlinTable#fieldTick}), which
 * makes leaving it the first answer and breaking it the second.
 */
public final class InfinityFields {
    /** One field on the ground: its id (for the glue's zone to check it still stands), centre and radius. */
    public record Field(int id, double x, double z, double radius, long born) {
        public boolean contains(double px, double pz) { return Math.hypot(px - x, pz - z) <= radius; }
    }

    private final List<Field> fields = new ArrayList<>();
    private int nextId = 1;

    /** Lays a field. If {@code cap} fields already stand the oldest is pushed out and returned (null when nothing was). */
    public Field add(double x, double z, double radius, long now, int cap) {
        Field pushed = null;
        int keep = Math.max(1, cap) - 1;
        while (fields.size() > keep) pushed = fields.remove(0);
        fields.add(new Field(nextId++, x, z, radius, now));
        return pushed;
    }

    public List<Field> all() { return Collections.unmodifiableList(new ArrayList<>(fields)); }

    public int count() { return fields.size(); }

    public boolean stands(int id) {
        for (Field f : fields) if (f.id() == id) return true;
        return false;
    }

    /** True if the point is inside any standing field. */
    public boolean inside(double px, double pz) {
        for (Field f : fields) if (f.contains(px, pz)) return true;
        return false;
    }

    /** Ends every field; returns how many stood. */
    public int clear() {
        int n = fields.size();
        fields.clear();
        return n;
    }
}
