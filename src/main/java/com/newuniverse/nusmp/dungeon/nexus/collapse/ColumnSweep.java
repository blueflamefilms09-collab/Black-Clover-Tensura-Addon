package com.newuniverse.nusmp.dungeon.nexus.collapse;

import java.util.BitSet;

/**
 * Brings the columns of a {@link CollapseSweep} down a few at a time. A column is worked bottom-up and always to the end, so the only
 * position to save is the index of the next column (a column that was left half done is simply done again: removed blocks read as air,
 * which costs one probe each). Working upward means that when a block is taken the one under it is already gone: a whole stack of
 * ceiling comes down as falling blocks, not just its underside, for as many as the tick's entity allowance covers; the rest crumble.
 * The budget is a soft cap checked between columns, so one tick spends at most `units` plus the cost of one column. Pure: the world is
 * reached through a {@link Sink}.
 */
public final class ColumnSweep {
    /** The world, as the sweep needs it. All coordinates are block coordinates. */
    public interface Sink {
        /** Is the chunk of this column loaded? An unloaded column stops the sweep where it is. */
        boolean loaded(int x, int z);

        /** Is there a block here that the sweep (or, with `wipe`, the final wipe) takes? Air, fluids, fixtures, anything unbreakable and the things left for the wipe are not. */
        boolean solid(int x, int y, int z, boolean wipe);

        /** Is there free space under this block, so that it falls rather than crumbles in place? */
        boolean freeBelow(int x, int y, int z);

        /** Takes the block away: as a falling block entity if `asEntity`, else it simply crumbles. False if it could not be taken. */
        boolean remove(int x, int y, int z, boolean asEntity);
    }

    /** What one pass did. */
    public static final class Pass {
        public int units, removed, entities;
        /** The pass stopped at a column whose chunk is not loaded. */
        public boolean blocked;

        void reset() {
            units = 0;
            removed = 0;
            entities = 0;
            blocked = false;
        }
    }

    private final CollapseSweep order;
    private final int yMin, yMax;

    /** The blocks swept are those from yMin to yMax inclusive in each column (the floor itself, at yMin - 1, stays). */
    public ColumnSweep(CollapseSweep order, int yMin, int yMax) {
        this.order = order;
        this.yMin = yMin;
        this.yMax = Math.max(yMin, yMax);
    }

    /** The most one column can cost: a probe and a removal for every block of it. */
    public int maxColumnCost() { return (yMax - yMin + 1) * (CollapsePlan.PROBE + CollapsePlan.REMOVE); }

    /**
     * Works the columns from `cursor` up to (not including) `limit` until about `units` are spent; `entities` is how many blocks may be
     * started as falling entities, the rest crumble. Returns the cursor to carry on from. A column whose chunk is not loaded stops the
     * sweep where it is (the final wipe must not leave anything behind); but given a `skipped` set (and not wiping) it is passed over
     * and its index noted there instead, so one far chunk cannot freeze the whole fall: {@link #one} takes it later, or the wipe does.
     */
    public int run(Sink sink, int cursor, int limit, int units, int entities, boolean wipe, BitSet skipped, Pass out) {
        out.reset();
        int end = Math.min(limit, order.size());
        int c = Math.max(0, cursor);
        while (c < end && out.units < units) {
            int x = order.x(c), z = order.z(c);
            if (!sink.loaded(x, z)) {
                out.blocked = true;
                if (skipped == null || wipe) break;
                skipped.set(c);
                out.units += CollapsePlan.PROBE;                                         // a look costs something, so the loop always ends
                c++;
                continue;
            }
            column(sink, x, z, entities, wipe, out);
            c++;
        }
        return c;
    }

    /** Works exactly column `index`, if its chunk is loaded. False if there is no such column or it is not loaded (then `out.blocked` says so). */
    public boolean one(Sink sink, int index, int entities, Pass out) {
        out.reset();
        if (index < 0 || index >= order.size()) return false;
        int x = order.x(index), z = order.z(index);
        if (!sink.loaded(x, z)) {
            out.blocked = true;
            return false;
        }
        column(sink, x, z, entities, false, out);
        return true;
    }

    private void column(Sink sink, int x, int z, int entities, boolean wipe, Pass out) {
        for (int y = yMin; y <= yMax; y++) {
            out.units += CollapsePlan.PROBE;
            if (!sink.solid(x, y, z, wipe)) continue;
            boolean asEntity = !wipe && out.entities < entities && sink.freeBelow(x, y, z);
            out.units += CollapsePlan.REMOVE;
            if (sink.remove(x, y, z, asEntity)) {
                out.removed++;
                if (asEntity) out.entities++;
            }
        }
    }

    /**
     * Shakes one block of column i loose: the lowest block of the topmost run of blocks (what hangs over the open air, if the column has
     * a ceiling), as a falling block if there is free space under it, else it just crumbles. True if there was a block and it went.
     */
    public boolean dripTop(Sink sink, int i, Pass out) {
        out.reset();
        if (i < 0 || i >= order.size()) return false;
        int x = order.x(i), z = order.z(i);
        if (!sink.loaded(x, z)) {
            out.blocked = true;
            return false;
        }
        int y = yMax;
        while (y >= yMin) {                                                              // down to the top of the highest run
            out.units += CollapsePlan.PROBE;
            if (sink.solid(x, y, z, false)) break;
            y--;
        }
        if (y < yMin) return false;
        while (y > yMin) {                                                               // and down the run to its lowest block
            out.units += CollapsePlan.PROBE;
            if (!sink.solid(x, y - 1, z, false)) break;
            y--;
        }
        boolean asEntity = sink.freeBelow(x, y, z);
        out.units += CollapsePlan.REMOVE;
        if (!sink.remove(x, y, z, asEntity)) return false;
        out.removed++;
        if (asEntity) out.entities++;
        return true;
    }
}
