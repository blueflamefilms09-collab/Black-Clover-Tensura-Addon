package com.newuniverse.nusmp.dungeon.nexus;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The stages of a build run in order under one budget and resume at the stage that was running. */
class StageSequenceTest {
    /** A stage of `total` units, one unit of work per unit of budget; its position is the units done. */
    private static final class Fake implements StageSequence.Stage {
        final int total;
        int done;
        boolean stuck;

        Fake(int total, int done) {
            this.total = total;
            this.done = done;
        }

        @Override
        public long run(long budget) {
            if (stuck) return 0;
            long n = Math.min(budget, total - done);
            done += (int) n;
            return n;
        }

        @Override
        public boolean done() { return done >= total; }

        @Override
        public int op() { return done; }

        @Override
        public long cell() { return done * 10L; }
    }

    private static final class Rig {
        final int[] totals;
        final List<String> opened = new ArrayList<>();
        final List<Fake> stages = new ArrayList<>();

        /** A negative total is a stage that is waiting for something: it does no work and is never done. */
        Rig(int... totals) { this.totals = totals; }

        StageSequence.Opener opener() {
            return (index, op, cell) -> {
                opened.add(index + "@" + op + "/" + cell);
                Fake f = new Fake(totals[index] < 0 ? 5 : totals[index], op);
                f.stuck = totals[index] < 0;
                stages.add(f);
                return f;
            };
        }
    }

    @Test
    void stagesRunInOrderAndTheBudgetIsSharedAcrossThem() {
        Rig rig = new Rig(10, 0, 25);
        StageSequence seq = new StageSequence(3, rig.opener(), 0, 0, 0L);
        List<String> progress = new ArrayList<>();
        long total = 0;
        while (!seq.done()) {
            long spent = seq.run(7);
            total += spent;
            assertTrue(spent <= 7, "a slice stays inside its budget here");
            progress.add(seq.stage() + ":" + seq.op());
        }
        assertEquals(35, total, "every unit of work is done once");
        assertEquals(List.of("0@0/0", "1@0/0", "2@0/0"), rig.opened, "each stage is opened once, fresh, in order");
        assertEquals(3, seq.stage());
        assertEquals(0, seq.op());
        assertEquals(0, seq.cell());
        assertEquals("0:7", progress.get(0), "the first slice ends inside stage 0");
        assertEquals("2:4", progress.get(1), "the second finishes stage 0, passes the empty stage 1 at once and goes on in stage 2: " + progress);
    }

    @Test
    void aSliceMayCrossIntoTheNextStage() {
        Rig rig = new Rig(5, 5);
        StageSequence seq = new StageSequence(2, rig.opener(), 0, 0, 0L);
        assertEquals(8, seq.run(8));
        assertEquals(1, seq.stage());
        assertEquals(3, seq.op());
        assertEquals(30L, seq.cell());
        assertFalse(seq.done());
        assertEquals(2, seq.run(100));
        assertTrue(seq.done());
    }

    @Test
    void resumingOpensOnlyTheStageThatWasRunningAtItsSavedPosition() {
        Rig rig = new Rig(10, 10, 10);
        StageSequence seq = new StageSequence(3, rig.opener(), 1, 4, 40L);
        assertEquals(List.of("1@4/40"), rig.opened);
        assertEquals(1, seq.stage());
        assertEquals(4, seq.op());
        assertEquals(6 + 10, seq.run(1000), "the rest of stage 1 and all of stage 2");
        assertEquals(List.of("1@4/40", "2@0/0"), rig.opened);
        assertTrue(seq.done());
    }

    @Test
    void aSavedStagePastTheEndMeansEverythingIsDone() {
        Rig rig = new Rig(10, 10);
        StageSequence seq = new StageSequence(2, rig.opener(), 2, 5, 5L);
        assertTrue(seq.done());
        assertEquals(0, seq.run(100));
        assertTrue(rig.opened.isEmpty());
        assertTrue(new StageSequence(2, rig.opener(), 99, 0, 0L).done());
        assertTrue(new StageSequence(0, rig.opener(), 0, 0, 0L).done(), "no stages at all");
        assertEquals(0, new StageSequence(0, rig.opener(), 0, 0, 0L).run(100));
    }

    @Test
    void aStageThatIsWaitingEndsTheSliceInsteadOfSpinning() {
        Rig rig = new Rig(5, -1, 5);
        StageSequence seq = new StageSequence(3, rig.opener(), 0, 0, 0L);
        long spent = seq.run(1_000_000L);
        assertEquals(5, spent, "the first stage is done, the second waits");
        assertEquals(1, seq.stage());
        assertFalse(seq.done());
        assertEquals(0, seq.run(1_000_000L), "still waiting, still no spin");
    }

    /** Two plans as two stages of one build, as the Golden City hands its structure to the old city builder after its own ops. */
    private static StageSequence.Opener twoPlans(ZonePlan a, ZonePlan b, NexusCursorTest.MapSink world) {
        return (index, op, cell) -> {
            NexusCursor cursor = NexusCursor.resume(index == 0 ? a : b, op);
            return new StageSequence.Stage() {
                @Override
                public long run(long budget) { return cursor.run(world, budget); }

                @Override
                public boolean done() { return cursor.done(); }

                @Override
                public int op() { return cursor.op(); }

                @Override
                public long cell() { return cursor.cell(); }
            };
        };
    }

    @Test
    void aWholeBuildOfTwoStagesResumesAfterAnyRestartInTheSameWorld() {
        ZonePlan a = NexusCursorTest.sample(2, 5), b = NexusCursorTest.sample(3, 5);
        NexusCursorTest.MapSink whole = new NexusCursorTest.MapSink();
        StageSequence all = new StageSequence(2, twoPlans(a, b, whole), 0, 0, 0L);
        all.run(Long.MAX_VALUE);
        assertTrue(all.done());
        for (int[] pattern : new int[][]{{2}, {4, 3}, {3, 2, 5}, {6}, {3, 3, 3, 3}, {14, 4, 14}}) {   // a restart replays up to REWIND ops, so each stretch between restarts must reach further than that
            NexusCursorTest.MapSink world = new NexusCursorTest.MapSink();
            StageSequence seq = new StageSequence(2, twoPlans(a, b, world), 0, 0, 0L);
            int round = 0;
            while (!seq.done()) {
                int slices = pattern[round++ % pattern.length];
                for (int i = 0; i < slices && !seq.done(); i++) seq.run(12_000L);
                seq = new StageSequence(2, twoPlans(a, b, world), seq.stage(), seq.op(), seq.cell());       // a restart: only (stage, op, cell) survives
            }
            assertEquals(whole.world, world.world, "restarts " + java.util.Arrays.toString(pattern));
            assertTrue(round > 1, "it really was interrupted");
        }
    }

    @Test
    void negativeSavedPositionsAreClamped() {
        Rig rig = new Rig(10);
        StageSequence seq = new StageSequence(1, rig.opener(), -3, -7, -9L);
        assertEquals(List.of("0@0/0"), rig.opened);
        assertEquals(10, seq.run(100));
    }
}
