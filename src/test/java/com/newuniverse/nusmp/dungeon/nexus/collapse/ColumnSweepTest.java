package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The columns come down a few at a time, under a budget, only where the chunk is loaded. */
class ColumnSweepTest {
    private static final int FLOOR = 10, TOP = 30;

    /** A world of solid blocks in a set; chunks listed in `unloaded` (x >> 4, z >> 4 as "x,z") are not there. */
    private static final class World implements ColumnSweep.Sink {
        final Set<String> solids = new HashSet<>();
        final Set<String> unloaded = new HashSet<>();
        final List<int[]> taken = new ArrayList<>();
        final List<Boolean> asEntity = new ArrayList<>();
        boolean refuse;

        static String k(int x, int y, int z) { return x + "," + y + "," + z; }

        void put(int x, int y, int z) { solids.add(k(x, y, z)); }

        @Override
        public boolean loaded(int x, int z) { return !unloaded.contains((x >> 4) + "," + (z >> 4)); }

        @Override
        public boolean solid(int x, int y, int z, boolean wipe) { return solids.contains(k(x, y, z)); }

        @Override
        public boolean freeBelow(int x, int y, int z) { return y - 1 > FLOOR && !solids.contains(k(x, y - 1, z)); }

        @Override
        public boolean remove(int x, int y, int z, boolean entity) {
            if (refuse) return false;
            solids.remove(k(x, y, z));
            taken.add(new int[]{x, y, z});
            asEntity.add(entity);
            return true;
        }
    }

    private static CollapseSweep order(int half) {
        return CollapseSweep.build(Footprint.rect(0, 0, half, half), 0, new int[]{half}, new int[]{0}, 3);
    }

    @Test
    void aColumnIsWorkedFromTheFloorUpAndEveryBlockOfItGoes() {
        CollapseSweep o = order(2);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        int x = o.x(0), z = o.z(0);
        for (int y : new int[]{FLOOR + 1, FLOOR + 5, FLOOR + 6, FLOOR + 7, TOP}) w.put(x, y, z);
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        int next = sw.run(w, 0, 1, 10_000, 99, false, null, p);
        assertEquals(1, next);
        assertEquals(5, p.removed);
        assertTrue(w.solids.isEmpty());
        for (int i = 1; i < w.taken.size(); i++) assertTrue(w.taken.get(i)[1] > w.taken.get(i - 1)[1], "bottom-up");
        assertEquals(List.of(false, true, true, true, true), w.asEntity.subList(0, 5), "the block on the floor crumbles, every block above a gap or a taken block falls");
        assertEquals(4, p.entities);
        assertTrue(sw.maxColumnCost() >= p.units);
        assertEquals((TOP + 2 - FLOOR) * CollapsePlan.PROBE + 5 * CollapsePlan.REMOVE, p.units);
    }

    @Test
    void theEntityAllowanceIsHonouredAndTheRestCrumble() {
        CollapseSweep o = order(3);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        for (int i = 0; i < o.size(); i++) for (int y = FLOOR + 3; y <= FLOOR + 9; y++) w.put(o.x(i), y, o.z(i));
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        sw.run(w, 0, o.size(), 1_000_000, 5, false, null, p);
        assertEquals(5, p.entities, "no more than allowed fall");
        assertEquals(o.size() * 7, p.removed, "all of them are gone though");
        int entities = 0;
        for (boolean b : w.asEntity) if (b) entities++;
        assertEquals(5, entities);
        assertTrue(w.solids.isEmpty());
    }

    @Test
    void theWipeNeverStartsAFallingBlock() {
        CollapseSweep o = order(2);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        for (int i = 0; i < o.size(); i++) for (int y = FLOOR + 3; y <= FLOOR + 9; y += 2) w.put(o.x(i), y, o.z(i));
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        sw.run(w, 0, o.size(), 1_000_000, 50, true, null, p);
        assertEquals(0, p.entities);
        assertFalse(w.asEntity.contains(true));
        assertTrue(w.solids.isEmpty());
    }

    @Test
    void theBudgetStopsTheSweepBetweenColumnsAndTheNextTickCarriesOn() {
        CollapseSweep o = order(8);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        for (int i = 0; i < o.size(); i++) w.put(o.x(i), FLOOR + 5, o.z(i));
        int budget = 300, cursor = 0, ticks = 0;
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        while (cursor < o.size()) {
            int before = cursor;
            cursor = sw.run(w, cursor, o.size(), budget, 0, false, null, p);
            assertTrue(p.units < budget + sw.maxColumnCost(), "at most the budget plus one column");
            assertTrue(cursor > before, "always makes progress");
            ticks++;
        }
        assertEquals(o.size(), cursor);
        assertTrue(ticks > 5 && ticks < o.size(), "spread over several ticks: " + ticks);
        assertTrue(w.solids.isEmpty());
    }

    @Test
    void anUnloadedColumnStopsTheSweepWhereItIsUntilItLoads() {
        CollapseSweep o = order(40);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        for (int i = 0; i < o.size(); i++) w.put(o.x(i), FLOOR + 5, o.z(i));
        int stop = o.size() / 2;
        w.unloaded.add((o.x(stop) >> 4) + "," + (o.z(stop) >> 4));
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        int cursor = 0;
        for (int t = 0; t < 400 && cursor < o.size(); t++) cursor = sw.run(w, cursor, o.size(), 5000, 0, false, null, p);
        assertTrue(p.blocked);
        assertTrue(cursor <= stop, "never past the unloaded column");
        int left = w.solids.size();
        assertTrue(left > 0);
        assertEquals(cursor, sw.run(w, cursor, o.size(), 5000, 0, false, null, p), "still waiting");
        assertEquals(0, p.removed);
        w.unloaded.clear();
        for (int t = 0; t < 400 && cursor < o.size(); t++) cursor = sw.run(w, cursor, o.size(), 5000, 0, false, null, p);
        assertEquals(o.size(), cursor);
        assertTrue(w.solids.isEmpty(), "and carries on once it loads");
    }

    @Test
    void whenAskedItPassesOverAnUnloadedColumnAndNotesIt() {
        CollapseSweep o = order(40);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        for (int i = 0; i < o.size(); i++) w.put(o.x(i), FLOOR + 5, o.z(i));
        w.unloaded.add((o.x(200) >> 4) + "," + (o.z(200) >> 4));
        BitSet skipped = new BitSet();
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        int cursor = 0;
        for (int t = 0; t < 400 && cursor < o.size(); t++) cursor = sw.run(w, cursor, o.size(), 5000, 0, false, skipped, p);
        assertEquals(o.size(), cursor, "the sweep went all the way round");
        assertTrue(skipped.cardinality() > 0, "and noted what it passed over");
        assertEquals(skipped.cardinality(), w.solids.size(), "exactly the unloaded columns still stand");
        for (int i = skipped.nextSetBit(0); i >= 0; i = skipped.nextSetBit(i + 1)) assertTrue(w.solids.contains(World.k(o.x(i), FLOOR + 5, o.z(i))));
        assertFalse(sw.one(w, skipped.nextSetBit(0), 0, p), "still not loaded");
        assertTrue(p.blocked);
        w.unloaded.clear();
        int first = skipped.nextSetBit(0);
        assertTrue(sw.one(w, first, 0, p));
        assertEquals(1, p.removed);
        assertFalse(w.solids.contains(World.k(o.x(first), FLOOR + 5, o.z(first))));
        assertFalse(sw.one(w, -1, 0, p));
        assertFalse(sw.one(w, o.size(), 0, p));
    }

    @Test
    void theWipeNeverPassesOverAnUnloadedColumnEvenIfAskedTo() {
        CollapseSweep o = order(10);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        for (int i = 0; i < o.size(); i++) w.put(o.x(i), FLOOR + 5, o.z(i));
        w.unloaded.add((o.x(30) >> 4) + "," + (o.z(30) >> 4));
        BitSet skipped = new BitSet();
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        int cursor = sw.run(w, 0, o.size(), 1_000_000, 0, true, skipped, p);
        assertTrue(cursor <= 30);
        assertTrue(p.blocked);
        assertTrue(skipped.isEmpty());
    }

    @Test
    void aRefusedRemovalIsNotCounted() {
        CollapseSweep o = order(1);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        w.put(o.x(0), FLOOR + 4, o.z(0));
        w.refuse = true;
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        sw.run(w, 0, 1, 1000, 5, false, null, p);
        assertEquals(0, p.removed);
        assertEquals(0, p.entities);
        assertEquals(1, w.solids.size());
    }

    @Test
    void aSweepStartedPastTheEndOrBelowZeroIsHarmless() {
        CollapseSweep o = order(1);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        assertTrue(sw.run(w, o.size() + 50, o.size() + 100, 1000, 0, false, null, p) >= o.size(), "nothing to do past the end");
        assertEquals(1, sw.run(w, -5, 1, 1000, 0, false, null, p));
        assertEquals(0, sw.run(w, 0, 0, 1000, 0, false, null, p), "an empty range does nothing");
    }

    @Test
    void aDripTakesTheLowestBlockOfTheHighestRunAndLetsItFall() {
        CollapseSweep o = order(2);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        int x = o.x(3), z = o.z(3);
        w.put(x, FLOOR + 2, z);                                     // a lower run: left alone
        for (int y = TOP - 2; y <= TOP; y++) w.put(x, y, z);       // the roof, three thick
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        assertTrue(sw.dripTop(w, 3, p));
        assertEquals(1, p.removed);
        assertEquals(1, p.entities, "the exposed underside falls");
        assertArrayEquals(new int[]{x, TOP - 2, z}, w.taken.get(0));
        assertEquals(3, w.solids.size());
        assertTrue(w.solids.contains(World.k(x, TOP - 1, z)) && w.solids.contains(World.k(x, FLOOR + 2, z)));
    }

    @Test
    void aDripOnAPillarJustCrumblesAndOnAnEmptyColumnDoesNothing() {
        CollapseSweep o = order(2);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        int x = o.x(0), z = o.z(0);
        for (int y = FLOOR + 1; y <= TOP; y++) w.put(x, y, z);     // floor to roof
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        assertTrue(sw.dripTop(w, 0, p));
        assertEquals(0, p.entities, "a pillar has nothing under it to fall into");
        assertTrue(w.solids.contains(World.k(x, TOP, z)), "the roof of a pillar stays");
        assertFalse(w.solids.contains(World.k(x, FLOOR + 1, z)));
        assertFalse(sw.dripTop(w, 1, p), "nothing in that column");
        assertEquals(0, p.removed);
        assertFalse(sw.dripTop(w, -1, p));
        assertFalse(sw.dripTop(w, o.size(), p));
    }

    @Test
    void aDripOnAnUnloadedColumnWaits() {
        CollapseSweep o = order(2);
        ColumnSweep sw = new ColumnSweep(o, FLOOR + 1, TOP + 2);
        World w = new World();
        w.put(o.x(0), TOP, o.z(0));
        w.unloaded.add((o.x(0) >> 4) + "," + (o.z(0) >> 4));
        ColumnSweep.Pass p = new ColumnSweep.Pass();
        assertFalse(sw.dripTop(w, 0, p));
        assertTrue(p.blocked);
        assertEquals(1, w.solids.size());
    }
}
