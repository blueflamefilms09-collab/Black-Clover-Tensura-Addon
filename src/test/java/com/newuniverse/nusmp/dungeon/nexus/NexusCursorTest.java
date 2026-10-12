package com.newuniverse.nusmp.dungeon.nexus;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Building a Nexus a little at a time, and picking it up again after a restart, ends in the same Nexus. */
class NexusCursorTest {
    /** A plan of every kind of op, in four phases: a carved cavern, a floor and a wall, details, a late fill of the air that is left. */
    static ZonePlan sample(int cellX, int cellZ) {
        int cx = NexusZone.centre(cellX), cz = NexusZone.centre(cellZ);
        ZonePlan.Builder b = ZonePlan.builder(NexusZone.MONARCH_VAULT, cellX, cellZ, cx, cz).floorY(-52).topY(-36).footprint(Footprint.Shape.OVAL, 30, 22);
        NexusOps o = b.ops();
        o.carveDisc(28, 20, -51, -37);
        o.phase();
        o.disc("minecraft:obsidian", "", 30, 22, -55, -52);
        o.discHollow("minecraft:blackstone", "", 30, 22, 2, -51, -36);
        o.phase();
        for (int i = 0; i < 6; i++) {
            o.frame(i * 4 - 10, 0, i % 2 == 0, i % 3 == 0 ? -1 : 1, 1);
            o.put("minecraft:stone_brick_stairs", "facing=north,half=bottom", 2, -51, 3);
            o.box("minecraft:gold_block", 0, -51, 0, 1, -50, 1);
        }
        o.resetFrame();
        o.air(-3, -51, -3, 3, -49, 3);
        o.phase();
        o.fillAir("minecraft:cobweb", "", -20, -51, -10, 20, -45, 10);
        b.centralRelic(cx, -51, cz);
        return b.build();
    }

    /** A world of one block per position, with the same rules as the real sink: carve clears, fill-air fills only air, boxes replace. */
    static final class MapSink implements NexusCursor.Sink {
        final Map<Long, String> world = new HashMap<>();
        int writes;

        static long key(int x, int y, int z) { return (((long) x + 1_000_000L) * 4_000_021L + ((long) z + 1_000_000L)) * 512L + (y + 128); }

        @Override
        public boolean place(NexusOp op, int x, int y, int z) {
            long k = key(x, y, z);
            String cur = world.getOrDefault(k, NexusOp.AIR);
            String want = op.block() + "[" + op.props() + "]";
            switch (op.kind()) {
                case CARVE -> {
                    if (cur.equals(NexusOp.AIR)) return false;
                    world.remove(k);
                }
                case FILL_AIR -> {
                    if (!cur.equals(NexusOp.AIR)) return false;
                    world.put(k, want);
                }
                default -> {
                    if (op.block().equals(NexusOp.AIR)) {
                        if (cur.equals(NexusOp.AIR)) return false;
                        world.remove(k);
                    } else {
                        if (cur.equals(want)) return false;
                        world.put(k, want);
                    }
                }
            }
            writes++;
            return true;
        }
    }

    /** The stone a natural cave has where a Nexus is carved: a solid block, so the carve has something to clear. */
    private static MapSink naturalWorld(ZonePlan plan) {
        MapSink w = new MapSink();
        for (int x = plan.footprint.minX(); x <= plan.footprint.maxX(); x++)
            for (int z = plan.footprint.minZ(); z <= plan.footprint.maxZ(); z++)
                for (int y = -56; y <= -34; y++) w.world.put(MapSink.key(x, y, z), "minecraft:stone[]");
        return w;
    }

    @Test
    void slicesOfAnySizeBuildTheSameNexus() {
        ZonePlan plan = sample(2, 5);
        MapSink whole = naturalWorld(plan);
        NexusCursor all = new NexusCursor(plan, 0, 0);
        all.run(whole, Long.MAX_VALUE);
        assertTrue(all.done());
        assertEquals(plan.ops.size(), all.op());
        assertTrue(whole.writes > 3000, "a real amount of building: " + whole.writes);
        for (long slice : new long[]{12_000L, 4_000L, 777L}) {
            MapSink sliced = naturalWorld(plan);
            NexusCursor c = new NexusCursor(plan, 0, 0);
            int ticks = 0;
            while (!c.done()) {
                long spent = c.run(sliced, slice);
                assertTrue(spent < slice + NexusCursor.WRITE, "a slice never runs far past its budget");
                ticks++;
            }
            assertEquals(whole.world, sliced.world, "slice " + slice);
            assertTrue(ticks > 1);
        }
    }

    @Test
    void aSliceWritesAtMostBudgetOverWriteBlocks() {
        ZonePlan plan = sample(0, 0);
        MapSink sink = naturalWorld(plan);
        NexusCursor c = new NexusCursor(plan, 0, 0);
        int before = 0;
        while (!c.done()) {
            c.run(sink, 12_000L);
            assertTrue(sink.writes - before <= 12_000 / NexusCursor.WRITE + 1, "writes in one slice: " + (sink.writes - before));
            before = sink.writes;
        }
    }

    @Test
    void buildingIsIdempotent() {
        ZonePlan plan = sample(-3, 4);
        MapSink world = naturalWorld(plan);
        new NexusCursor(plan, 0, 0).run(world, Long.MAX_VALUE);
        Map<Long, String> built = new HashMap<>(world.world);
        long spent = new NexusCursor(plan, 0, 0).run(world, Long.MAX_VALUE);
        assertEquals(built, world.world, "building again changes nothing");
        assertTrue(spent > 0);
    }

    @Test
    void resumingFromSavedProgressEndsInTheSameNexus() {
        ZonePlan plan = sample(-3, 4);
        MapSink whole = naturalWorld(plan);
        new NexusCursor(plan, 0, 0).run(whole, Long.MAX_VALUE);
        for (int stopAfter : new int[]{1, 3, 7, 12}) {
            MapSink world = naturalWorld(plan);
            NexusCursor c = new NexusCursor(plan, 0, 0);
            for (int i = 0; i < stopAfter; i++) c.run(world, 5_000L);
            int savedOp = c.op();
            NexusCursor again = NexusCursor.resume(plan, savedOp);
            assertTrue(again.op() <= savedOp && again.op() >= savedOp - NexusCursor.REWIND, "rewinds a little");
            assertEquals(plan.phaseOf(Math.min(savedOp, plan.ops.size() - 1)), plan.phaseOf(again.op()), "never back into an earlier phase");
            assertEquals(0, again.cell());
            while (!again.done()) again.run(world, 5_000L);
            assertEquals(whole.world, world.world, "stopped after " + stopAfter);
        }
    }

    @Test
    void resumingPastTheEndOrFromNothingIsSafe() {
        ZonePlan plan = sample(1, 1);
        assertTrue(NexusCursor.resume(plan, plan.ops.size()).done());
        assertTrue(NexusCursor.resume(plan, plan.ops.size() + 50).done());
        NexusCursor fresh = NexusCursor.resume(plan, 0);
        assertEquals(0, fresh.op());
        assertFalse(fresh.done());
        assertEquals(0, NexusCursor.resume(plan, -4).op());
        NexusCursor clamped = new NexusCursor(plan, 10_000, 99);
        assertTrue(clamped.done());
        assertEquals(0, clamped.cell());
    }

    @Test
    void anEmptyPlanIsDoneAtOnce() {
        ZonePlan plan = ZonePlan.builder(NexusZone.GOLDEN_CITY, 0, 0, 64, 64).floorY(-52).topY(-34).footprint(Footprint.Shape.RECT, 10, 10).build();
        NexusCursor c = new NexusCursor(plan, 0, 0);
        assertTrue(c.done());
        assertEquals(0, c.run((op, x, y, z) -> { throw new AssertionError("nothing to place"); }, 1000));
        assertTrue(NexusCursor.resume(plan, 0).done());
    }

    @Test
    void theCursorVisitsEveryCellOfEveryOpOnceInOrder() {
        ZonePlan plan = sample(4, 4);
        List<String> seen = new ArrayList<>();
        NexusCursor c = new NexusCursor(plan, 0, 0);
        while (!c.done()) c.run((op, x, y, z) -> { seen.add(plan.ops.indexOf(op) + ":" + x + "," + y + "," + z); return false; }, 3_000L);
        long expected = 0;
        for (NexusOp op : plan.ops) expected += op.volume();
        assertEquals(expected, seen.size());
        assertEquals(seen.size(), new java.util.HashSet<>(seen).size(), "no cell of an op twice");
        int last = -1;
        for (String s : seen) {
            int idx = Integer.parseInt(s.substring(0, s.indexOf(':')));
            assertTrue(idx >= last, "ops in order");
            last = idx;
        }
    }
}
