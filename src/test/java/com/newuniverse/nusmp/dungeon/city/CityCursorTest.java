package com.newuniverse.nusmp.dungeon.city;

import com.newuniverse.nusmp.dungeon.city.CityPlan.Op;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Building a city a little at a time, and picking it up again after a restart, ends in the same city. */
class CityCursorTest {
    /** A world of one palette entry per position; counts what it was asked to write. */
    private static final class MapSink implements CityCursor.Sink {
        final Map<Long, Integer> world = new HashMap<>();
        int writes;

        @Override
        public boolean place(Op op, int x, int y, int z) {
            long key = (((long) x + 1_000_000L) * 4_000_021L + ((long) z + 1_000_000L)) * 128L + (y + 64);
            int want = op.kind() == CityPlan.Kind.CARVE ? 0 : 1 + op.pal().ordinal() + 100 * op.variant();
            Integer cur = world.get(key);
            if (cur != null && cur == want) return false;
            world.put(key, want);
            writes++;
            return true;
        }
    }

    @Test
    void slicesOfAnySizeBuildTheSameCity() {
        CityPlan plan = CityPlan.of(2, 5);
        MapSink whole = new MapSink();
        CityCursor all = new CityCursor(plan, 0, 0);
        all.run(whole, Long.MAX_VALUE);
        assertTrue(all.done());
        assertEquals(plan.ops.size(), all.op());
        for (long slice : new long[]{12_000L, 4_000L, 777L}) {
            MapSink sliced = new MapSink();
            CityCursor c = new CityCursor(plan, 0, 0);
            int ticks = 0;
            while (!c.done()) {
                long spent = c.run(sliced, slice);
                assertTrue(spent < slice + CityCursor.WRITE, "a slice never runs far past its budget");
                ticks++;
            }
            assertEquals(whole.world, sliced.world, "slice " + slice);
            assertTrue(ticks > 1);
        }
    }

    @Test
    void aSliceWritesAtMostBudgetOverWriteBlocks() {
        CityPlan plan = CityPlan.of(0, 0);
        MapSink sink = new MapSink();
        CityCursor c = new CityCursor(plan, 0, 0);
        int before = 0;
        while (!c.done()) {
            c.run(sink, 12_000L);
            assertTrue(sink.writes - before <= 12_000 / CityCursor.WRITE + 1, "writes in one slice: " + (sink.writes - before));
            before = sink.writes;
        }
    }

    @Test
    void resumingFromSavedProgressEndsInTheSameCity() {
        CityPlan plan = CityPlan.of(-3, 4);
        MapSink whole = new MapSink();
        new CityCursor(plan, 0, 0).run(whole, Long.MAX_VALUE);
        for (int stopAfter : new int[]{1, 7, 40}) {
            MapSink world = new MapSink();
            CityCursor c = new CityCursor(plan, 0, 0);
            for (int i = 0; i < stopAfter; i++) c.run(world, 12_000L);
            int savedOp = c.op();
            CityCursor again = CityCursor.resume(plan, savedOp);
            assertTrue(again.op() <= savedOp && again.op() >= savedOp - CityCursor.REWIND, "rewinds a little");
            assertEquals(plan.phaseOf(Math.min(savedOp, plan.ops.size() - 1)), plan.phaseOf(again.op()), "never back into an earlier phase");
            while (!again.done()) again.run(world, 12_000L);
            assertEquals(whole.world, world.world, "stopped after " + stopAfter);
        }
    }

    @Test
    void aFinishedCityStaysFinished() {
        CityPlan plan = CityPlan.of(1, 1);
        CityCursor done = CityCursor.resume(plan, plan.ops.size());
        assertTrue(done.done());
        MapSink sink = new MapSink();
        assertEquals(0L, done.run(sink, 1000L));
        assertEquals(0, sink.writes);
        CityCursor beyond = new CityCursor(plan, plan.ops.size() + 50, 99);
        assertTrue(beyond.done());
    }
}
