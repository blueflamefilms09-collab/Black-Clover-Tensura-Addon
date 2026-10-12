package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** A whole collapse, tick by tick, against a world made of sets: the timeline, the budgets, the corridors, the restart. */
class CollapseRunnerTest {
    private static final int FLOOR = -52, TOP = -13, START = 5000;

    /** A hall: a three-thick roof, pillars every twelve blocks, a platform in the middle, a heap by each exit. */
    private static final class Hall implements ColumnSweep.Sink {
        final Footprint fp = Footprint.oval(0, 0, 47, 47);
        final Set<Long> solids = new HashSet<>();
        final Set<Long> unloadedChunks = new HashSet<>();
        final List<long[]> removals = new ArrayList<>();      // {tick, x, z, entity}
        long tick;

        static long k(int x, int y, int z) { return ((long) (x + 5000) * 100_000L + (z + 5000)) * 1000L + (y + 200); }

        static long chunk(int x, int z) { return ((long) (x >> 4) << 32) | ((z >> 4) & 0xFFFFFFFFL); }

        Hall() {
            for (int x = -50; x <= 50; x++) {
                for (int z = -50; z <= 50; z++) {
                    if (!fp.contains(x, z, 1)) continue;
                    for (int y = TOP - 2; y <= TOP; y++) solids.add(k(x, y, z));
                    if (x % 12 == 0 && z % 12 == 0) for (int y = FLOOR + 1; y < TOP - 2; y++) solids.add(k(x, y, z));
                    if (Math.abs(x) < 6 && Math.abs(z) < 6) solids.add(k(x, FLOOR + 6, z));
                    if (Math.abs(x - 40) <= 2 && Math.abs(z) <= 2) solids.add(k(x, FLOOR + 1, z));
                }
            }
        }

        @Override
        public boolean loaded(int x, int z) { return !unloadedChunks.contains(chunk(x, z)); }

        @Override
        public boolean solid(int x, int y, int z, boolean wipe) { return solids.contains(k(x, y, z)); }

        @Override
        public boolean freeBelow(int x, int y, int z) { return y - 1 > FLOOR && !solids.contains(k(x, y - 1, z)); }

        @Override
        public boolean remove(int x, int y, int z, boolean entity) {
            if (!solids.remove(k(x, y, z))) return false;
            removals.add(new long[]{tick, x, z, entity ? 1 : 0});
            return true;
        }
    }

    private static final class Log implements CollapseRunner.Events {
        final List<String> events = new ArrayList<>();
        final List<CollapsePlan.Stage> stages = new ArrayList<>();
        final List<Integer> lines = new ArrayList<>(), waves = new ArrayList<>();
        final List<Long> linesLeft = new ArrayList<>();
        final List<long[]> dusts = new ArrayList<>();         // {tick, x, z}
        int expelFirst, expelLater, cracks;
        long tick;

        @Override
        public void stage(CollapsePlan.Stage to) { stages.add(to); }

        @Override
        public void line(int index, long ticksLeft) {
            lines.add(index);
            linesLeft.add(ticksLeft);
        }

        @Override
        public void crack(int x, int z) { cracks++; }

        @Override
        public void dust(int x, int z) { dusts.add(new long[]{tick, x, z}); }

        @Override
        public void wave(int index) { waves.add(index); }

        @Override
        public void expel(boolean first) {
            if (first) expelFirst++;
            else expelLater++;
        }
    }

    private static CollapseState state() {
        CollapseState s = new CollapseState();
        s.id = 42;
        s.startTick = START;
        s.fp = Footprint.oval(0, 0, 47, 47);
        s.floorY = FLOOR;
        s.topY = TOP;
        s.nexus = true;
        s.zone = NexusZone.MONARCH_VAULT;
        for (int[] e : new int[][]{{43, FLOOR + 1, 0}, {-43, FLOOR + 1, 0}, {0, FLOOR + 1, 43}}) {
            s.exits.add(e);
            s.gates.add(new int[0]);
        }
        return s;
    }

    private static CollapseRunner runner(CollapseState s) {
        CollapseSweep o = s.order();
        return new CollapseRunner(s, o, s.sweep(o));
    }

    @Test
    void aWholeCollapseRunsInOrderAndLeavesNothingStanding() {
        CollapseState s = state();
        Hall hall = new Hall();
        Log log = new Log();
        CollapseRunner r = runner(s);
        int total = hall.solids.size();
        long end = -1;
        int maxUnits = 0;
        for (long t = 0; t < CollapsePlan.TOTAL + 200; t++) {
            hall.tick = log.tick = t;
            long now = START + t;
            CollapsePlan.Stage st = r.tick(now, hall, log);
            assertSame(CollapsePlan.stageAt(t), st);
            int cap = st == CollapsePlan.Stage.WIPE ? CollapsePlan.WIPE_UNITS + r.sweepColumnCost() : CollapsePlan.sweepUnits(t) + 5 * r.sweepColumnCost();
            assertTrue(r.lastUnits() <= cap, "bounded work at " + t + ": " + r.lastUnits() + " > " + cap);
            assertTrue(r.lastEntities() <= CollapsePlan.entityBudget(t) + 2 + 2, "bounded entities at " + t + ": " + r.lastEntities());
            assertTrue(r.fallingAlive(now) <= CollapsePlan.MAX_ALIVE, "bounded falling blocks at " + t);
            maxUnits = Math.max(maxUnits, r.lastUnits());
            if (r.ended()) {
                end = t;
                break;
            }
        }
        assertTrue(end >= CollapsePlan.TOTAL, "the end is never before the clock runs out: " + end);
        assertTrue(end <= CollapsePlan.TOTAL + 60, "and comes soon after: " + end);
        assertTrue(hall.solids.isEmpty(), "nothing is left");
        assertEquals(total, hall.removals.size());
        assertEquals(List.of(CollapsePlan.Stage.CRACKS, CollapsePlan.Stage.ROLLING, CollapsePlan.Stage.FINAL, CollapsePlan.Stage.WIPE), log.stages);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), log.lines, "every countdown mark once, in order");
        for (int i = 0; i < log.lines.size(); i++) assertEquals((long) CollapsePlan.MARKS[i], log.linesLeft.get(i), "each said on time: " + i);
        assertEquals(List.of(0, 1, 2, 3, 4), log.waves, "every guard wave once, in order");
        assertEquals(1, log.expelFirst, "the expulsion starts once");
        assertTrue(log.expelLater >= 1, "and catches whoever walks back in while the wipe runs");
        assertTrue(log.cracks > 100);
        assertTrue(maxUnits <= CollapsePlan.WIPE_UNITS + r.sweepColumnCost());
    }

    @Test
    void theRollNeverTouchesTheCorridorsAndTheyOnlyGetTheTelegraphedDrips() {
        CollapseState s = state();
        Hall hall = new Hall();
        Log log = new Log();
        CollapseRunner r = runner(s);
        CollapseSweep o = r.order();
        int corridorHits = 0;
        for (long t = 0; t < CollapsePlan.TOTAL; t++) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
        }
        for (long[] rem : hall.removals) {
            int idx = o.indexOf((int) rem[1], (int) rem[2]);
            if (idx < 0 || !o.spared(idx)) continue;
            corridorHits++;
            assertTrue(rem[0] >= CollapsePlan.DRIP_FROM + CollapsePlan.DRIP_LEAD, "no corridor block falls before the last half minute, at " + rem[0]);
            assertTrue(rem[0] <= CollapsePlan.TOTAL - 100, "and none in the last five seconds, at " + rem[0]);
            boolean warned = false;
            for (long[] d : log.dusts) if (d[1] == rem[1] && d[2] == rem[2] && rem[0] - d[0] >= CollapsePlan.DRIP_LEAD) warned = true;
            assertTrue(warned, "a second of dust at that column first: " + rem[1] + "," + rem[2]);
        }
        assertTrue(corridorHits > 0 && corridorHits <= CollapsePlan.MAX_CORRIDOR_DRIPS, "a handful and no more: " + corridorHits);
        int roof = 0;
        for (long[] rem : hall.removals) {
            if (rem[0] >= CollapsePlan.TOTAL) continue;
            int idx = o.indexOf((int) rem[1], (int) rem[2]);
            if (idx >= 0 && !o.spared(idx)) roof++;
        }
        assertTrue(roof > hall.removals.size() / 2, "most of the hall came down during the roll");
        assertFalse(hall.solids.isEmpty(), "the corridors and the last of it stand until the wipe");
    }

    @Test
    void theFrontStartsAtTheFarSideAndReachesTheExitsLast() {
        CollapseState s = state();
        Hall hall = new Hall();
        Log log = new Log();
        CollapseRunner r = runner(s);
        CollapseSweep o = r.order();
        for (long t = 0; t < CollapsePlan.FRONT_END + 5; t++) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
        }
        double[] sum = new double[6];
        int[] n = new int[6];
        for (long[] rem : hall.removals) {
            if (rem[0] < CollapsePlan.CRACKS_END) continue;
            int idx = o.indexOf((int) rem[1], (int) rem[2]);
            if (idx < 0) continue;
            int slot = (int) Math.min(5, (rem[0] - CollapsePlan.CRACKS_END) * 6 / (CollapsePlan.FRONT_END - CollapsePlan.CRACKS_END));
            sum[slot] += idx;
            n[slot]++;
        }
        double prev = -1;
        for (int i = 0; i < 6; i++) {
            assertTrue(n[i] > 0, "something falls in every sixth of the roll");
            double avg = sum[i] / n[i];
            assertTrue(avg > prev, "the average place in the order only moves on, slice " + i + ": " + avg + " after " + prev);
            prev = avg;
        }
        assertEquals(100, r.percent(), "everything is swept by the time the front is through");
    }

    @Test
    void aRestartFromASavedMomentFinishesTheSameAndSaysNothingTwice() {
        for (long cut : new long[]{0, 1, 599, 600, 1199, 1200, 1500, 2500, 2999, 3100, 3590, 3599, 3600, 3610}) {
            CollapseState s = state();
            Hall hall = new Hall();
            Log log = new Log();
            CollapseRunner r = runner(s);
            for (long t = 0; t < cut; t++) {
                hall.tick = log.tick = t;
                r.tick(START + t, hall, log);
            }
            CollapseState saved = s.copy();                            // the server stops here and comes back
            CollapseRunner again = runner(saved);
            CollapseRunner cur = again;
            for (long t = cut; t < CollapsePlan.TOTAL + 200; t++) {
                hall.tick = log.tick = t;
                cur.tick(START + t, hall, log);
                if (cur.ended()) break;
            }
            assertTrue(cur.ended(), "ends after a restart at " + cut);
            assertTrue(hall.solids.isEmpty(), "nothing left after a restart at " + cut);
            assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), log.lines, "no mark twice or missed after a restart at " + cut);
            assertEquals(List.of(0, 1, 2, 3, 4), log.waves, "no wave twice or missed after a restart at " + cut);
            assertEquals(1, log.expelFirst, "one expulsion after a restart at " + cut);
        }
    }

    @Test
    void aLongStallJumpsStraightToTheWipeAndSaysOnlyTheNewestMark() {
        CollapseState s = state();
        Hall hall = new Hall();
        Log log = new Log();
        CollapseRunner r = runner(s);
        r.tick(START + 5, hall, log);
        r.tick(START + 200_000, hall, log);                            // the server was off for a day
        assertEquals(List.of(0, 9), log.lines, "3:00 at the start, then only 0:01, not the six in between");
        assertEquals(1, log.expelFirst);
        long t = 200_000;
        while (!r.ended() && t < 200_100) r.tick(START + ++t, hall, log);
        assertTrue(r.ended());
        assertTrue(hall.solids.isEmpty());
        assertTrue(log.waves.size() <= 1, "no guard waves for a collapse that is already over");
    }

    @Test
    void anUnloadedPartOfTheHallDoesNotFreezeTheFallAndTheWipeWaitsForIt() {
        CollapseState s = state();
        Hall hall = new Hall();
        Log log = new Log();
        CollapseRunner r = runner(s);
        long chunk = Hall.chunk(-20, 20);
        hall.unloadedChunks.add(chunk);
        boolean blocked = false;
        for (long t = 0; t < CollapsePlan.TOTAL - 1; t++) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
            blocked |= r.blocked();
        }
        assertTrue(blocked);
        assertEquals(100, r.percent(), "the rest of the hall came down on time");
        assertTrue(r.skipped() > 0, "the unloaded columns were passed over, not waited for");
        int standingElsewhere = 0;
        for (long k : hall.solids) {
            int z = (int) (k / 1000L % 100_000L) - 5000, x = (int) (k / 1000L / 100_000L) - 5000;
            if (Hall.chunk(x, z) != chunk && !r.order().spared(r.order().indexOf(x, z))) standingElsewhere++;
        }
        assertEquals(0, standingElsewhere, "nothing but the unloaded part and the corridors still stands");
        for (long t = CollapsePlan.TOTAL - 1; t < CollapsePlan.TOTAL + 300; t++) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
        }
        assertFalse(r.ended(), "the wipe waits for the chunk: it must not leave a standing hall behind");
        assertFalse(hall.solids.isEmpty());
        hall.unloadedChunks.clear();
        long t = CollapsePlan.TOTAL + 300;
        while (!r.ended() && t < CollapsePlan.TOTAL + 600) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
            t++;
        }
        assertTrue(r.ended(), "and finishes the moment it loads");
        assertTrue(hall.solids.isEmpty());
        assertEquals(1, log.expelFirst, "the expulsion was not repeated");
    }

    @Test
    void aColumnPassedOverIsTakenAsSoonAsItsChunkLoadsDuringTheRoll() {
        CollapseState s = state();
        Hall hall = new Hall();
        Log log = new Log();
        CollapseRunner r = runner(s);
        long chunk = Hall.chunk(-20, 20);
        hall.unloadedChunks.add(chunk);
        long t = 0;
        for (; t < CollapsePlan.FRONT_END - 100; t++) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
        }
        int before = r.skipped();
        assertTrue(before > 0);
        hall.unloadedChunks.clear();
        for (; t < CollapsePlan.FRONT_END - 100 + 200; t++) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
        }
        assertEquals(0, r.skipped(), "the passed-over columns were taken once they loaded");
        assertTrue(t < CollapsePlan.TOTAL);
        for (long k : hall.solids) {
            int z = (int) (k / 1000L % 100_000L) - 5000, x = (int) (k / 1000L / 100_000L) - 5000;
            assertTrue(r.order().spared(r.order().indexOf(x, z)), "only the corridors still stand before the wipe");
        }
    }

    @Test
    void theWipeTakesWhatTheRollPassedOver() {
        CollapseState s = state();
        Hall hall = new Hall();
        hall.unloadedChunks.add(Hall.chunk(0, 0));
        Log log = new Log();
        CollapseRunner r = runner(s);
        for (long t = 0; t < CollapsePlan.TOTAL - 5; t++) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
        }
        assertTrue(r.skipped() > 0, "the roll passed over the unloaded chunk");
        assertTrue(hall.solids.contains(Hall.k(0, FLOOR + 6, 0)), "the platform in the middle still stands");
        hall.unloadedChunks.clear();
        long t = CollapsePlan.TOTAL - 5;
        while (!r.ended() && t < CollapsePlan.TOTAL + 200) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
            t++;
        }
        assertTrue(r.ended());
        assertTrue(hall.solids.isEmpty(), "everything, including the part the roll passed over");
    }

    @Test
    void aCollapseWithoutExitsStillRunsAndNothingIsSpared() {
        CollapseState s = state();
        s.exits.clear();
        s.gates.clear();
        Hall hall = new Hall();
        Log log = new Log();
        CollapseRunner r = runner(s);
        assertEquals(r.order().size(), r.order().open());
        for (long t = 0; t < CollapsePlan.TOTAL + 100 && !r.ended(); t++) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
        }
        assertTrue(r.ended());
        assertTrue(hall.solids.isEmpty());
    }

    @Test
    void aClockThatRunsBackwardsJustWaits() {
        CollapseState s = state();
        Hall hall = new Hall();
        Log log = new Log();
        CollapseRunner r = runner(s);
        r.tick(START - 500, hall, log);
        assertEquals(0, r.lastRemoved());
        assertEquals(0, r.percent());
        assertEquals(List.of(CollapsePlan.Stage.CRACKS), log.stages);
    }

    @Test
    void theFrontAndTheProgressReadBackSensibly() {
        CollapseState s = state();
        Hall hall = new Hall();
        Log log = new Log();
        CollapseRunner r = runner(s);
        assertEquals(0, r.percent());
        int[] f0 = r.front();
        assertNotNull(f0);
        for (long t = 0; t < 2400; t++) {
            hall.tick = log.tick = t;
            r.tick(START + t, hall, log);
        }
        assertTrue(r.percent() > 10 && r.percent() < 90, "partway: " + r.percent());
        assertTrue(r.fallingAlive(START + 2399) > 0);
        assertEquals(0, state().cursor);
    }
}
