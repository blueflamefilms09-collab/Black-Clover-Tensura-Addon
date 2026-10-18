package com.newuniverse.nusmp.heroweapon.weapon.mirror;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Mirror's Glass Shield stands a pane of glass in the air, and four other things need to know where the panes are: the Prism Ray bounces off one,
 * the Chain Binding Mirror hangs its chains from them, a new pane must push the oldest out when two already stand, and the panes must vanish when their time
 * is up. That bookkeeping (positions, facing, charges left, expiry, eviction) is plain data with simple rules, so it is kept here without any game class and
 * tested; the game side only reads and writes it. A pane is a disc of glass facing the unit normal (nx, ny, nz); {@code until} and the times passed in are the
 * game clock in ticks.
 */
public final class PaneBook {
    /** One standing pane. {@code charges} counts the shots it can still turn back; {@code broken} is set when it has been used up or evicted. */
    public static final class Pane {
        public final int id;
        public final double x, y, z, nx, ny, nz, radius;
        public final long until;
        public int charges;
        public boolean broken;

        Pane(int id, double x, double y, double z, double nx, double ny, double nz, double radius, long until, int charges) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.radius = radius;
            this.until = until;
            this.charges = charges;
        }

        /** True while the pane stands: not broken and not out of time. */
        public boolean standing(long now) {
            return !broken && now < until;
        }
    }

    private final int max;
    private final List<Pane> panes = new ArrayList<>();
    private int nextId = 1;

    public PaneBook(int max) {
        this.max = Math.max(1, max);
    }

    /**
     * Makes room for one more pane: forgets the ones that fell down or ran out of time, then, if the book is still full, evicts the oldest standing panes. The
     * evicted panes (marked broken) are returned so the caller can shatter them; the others are simply gone.
     */
    public List<Pane> makeRoom(long now) {
        panes.removeIf(p -> !p.standing(now));
        List<Pane> evicted = new ArrayList<>();
        while (panes.size() >= max) {
            Pane old = panes.remove(0);
            old.broken = true;
            evicted.add(old);
        }
        return evicted;
    }

    /** Stands a new pane (call {@link #makeRoom} first). */
    public Pane place(long now, double x, double y, double z, double nx, double ny, double nz, double radius, int life, int charges) {
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9) {
            nx = 0;
            ny = 0;
            nz = 1;
            len = 1;
        }
        Pane p = new Pane(nextId++, x, y, z, nx / len, ny / len, nz / len, radius, now + life, charges);
        panes.add(p);
        return p;
    }

    /** The panes that stand now, oldest first (a copy). */
    public List<Pane> live(long now) {
        List<Pane> out = new ArrayList<>();
        for (Pane p : panes) if (p.standing(now)) out.add(p);
        return out;
    }

    /** The standing pane nearest to the point and within {@code within} of it, or null. */
    public Pane nearest(double x, double y, double z, long now, double within) {
        Pane best = null;
        double bestD = within * within;
        for (Pane p : panes) {
            if (!p.standing(now)) continue;
            double dx = p.x - x, dy = p.y - y, dz = p.z - z, d = dx * dx + dy * dy + dz * dz;
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** Forgets every pane (the weapon was put away for good, or the wielder left). */
    public void clear() {
        panes.clear();
    }

    public int size(long now) {
        return live(now).size();
    }
}
