package com.newuniverse.nusmp.heroweapon.weapon.sewingset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Sewing Set works with needles that stay where they were put. A needle shot into a foe leaves a stitch mark (the Scissor Cut cuts a marked foe deeper), a
 * needle shot into an ally leaves a beacon (the Jumping Needle jumps to it, the Stitch Mend reaches it from afar, Ariadne's Thread draws it in), and a needle planted in
 * the ground is a place to jump back to. The marks are a small table, bounded in size and in time, as plain data on the game clock with tests; the game side only reads
 * and writes it. This is NOT the red-thread bookkeeping of the Thread Magic ({@code thread/**}): there the thread binds and protects people the caster chose by name; here
 * a mark is a thing struck by a needle and it expires.
 */
public final class NeedleBoard {
    public enum Side { FOE, ALLY }

    /** One needle in a creature: whose, which side, when it falls out. */
    public record Mark(UUID id, Side side, long until) {}

    /** At most this many marks at once; a new one pushes the oldest out. */
    public static final int MAX_MARKS = 8;

    private final Map<UUID, Mark> marks = new LinkedHashMap<>();

    private boolean hasPlace;
    private double px, py, pz;
    private long placeUntil;
    private long lastDraw = Long.MIN_VALUE / 4;

    /** True (and remembered) when at least {@code every} ticks have passed since the threads to the beacons were last drawn: the hook that draws them runs on its own beat. */
    public boolean drawDue(long now, int every) {
        if (now - lastDraw < every) return false;
        lastDraw = now;
        return true;
    }

    // ------------------------------------------------------------------ marks
    /** Marks a creature (or refreshes its mark) for {@code life} ticks; when the table is full the oldest mark falls out. */
    public void mark(UUID id, Side side, long now, int life) {
        prune(now);
        marks.remove(id);
        while (marks.size() >= MAX_MARKS) {
            Iterator<UUID> it = marks.keySet().iterator();
            it.next();
            it.remove();
        }
        marks.put(id, new Mark(id, side, now + life));
    }

    public boolean has(UUID id, long now) {
        Mark m = marks.get(id);
        return m != null && now < m.until();
    }

    /** True and the mark is removed if the creature was marked (a Scissor Cut uses it up). */
    public boolean consume(UUID id, long now) {
        boolean marked = has(id, now);
        marks.remove(id);
        return marked;
    }

    /** The live marks of one side, oldest first. */
    public List<UUID> ids(Side side, long now) {
        List<UUID> out = new ArrayList<>();
        for (Mark m : marks.values()) if (m.side() == side && now < m.until()) out.add(m.id());
        return out;
    }

    /** Forgets the marks that have fallen out. */
    public void prune(long now) {
        marks.values().removeIf(m -> now >= m.until());
    }

    public int size(long now) {
        prune(now);
        return marks.size();
    }

    // ------------------------------------------------------------------ the needle in the ground
    /** Plants a needle at a place for {@code life} ticks (it replaces the old one). */
    public void plant(double x, double y, double z, long now, int life) {
        hasPlace = true;
        px = x;
        py = y;
        pz = z;
        placeUntil = now + life;
    }

    public boolean placeLive(long now) {
        return hasPlace && now < placeUntil;
    }

    public double placeX() { return px; }

    public double placeY() { return py; }

    public double placeZ() { return pz; }

    public void clearPlace() {
        hasPlace = false;
    }

    /** Forgets everything. */
    public void clear() {
        marks.clear();
        hasPlace = false;
    }
}
