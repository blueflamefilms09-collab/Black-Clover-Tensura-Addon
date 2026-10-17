package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.MoveRow;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Diane's two sizes are only fair if each always has something to throw at every range and the framework's scheduler plays them the way the design says, so a whole fight is played through it, at each size and power. */
class DianeTableTest {
    static final List<MoveRow> ROWS = DianeTable.rows();

    @Test
    void theTableIsWellFormed() {
        assertEquals(11, ROWS.size());
        Set<String> ids = new HashSet<>();
        for (MoveRow r : ROWS) {
            assertTrue(ids.add(r.id()), "unique id " + r.id());
            assertFalse(r.label().isBlank());
            assertTrue(r.minPower() >= 20 && r.minPower() <= 60, r.id() + " is available before the Gates");
            assertTrue(r.windUpTicks() >= 14, r.id() + " has a telegraph of at least 14 ticks");
            assertTrue(Math.round(r.windUpTicks() * 0.75f) >= 8, r.id() + " keeps 8 ticks of tell even at the Gates");
            assertTrue(r.cooldownTicks() >= 70 && r.weight() >= 1);
            assertTrue(r.minRange() >= 0 && r.maxRange() > r.minRange());
            assertTrue(r.recovery() >= 8, "a beat to breathe after every move");
            assertTrue(r.group() >= DianeTable.ANY && r.group() <= DianeTable.HUMAN);
        }
        assertEquals(DianeTable.GIDEON_SMASH, ROWS.get(0).id(), "the first moves keep their places (the client reads names by index)");
        assertEquals(DianeTable.DROLE_DANCE, ROWS.get(4).id());
        assertEquals(DianeTable.SHRINK, ROWS.get(10).id());
        assertNull(DianeTable.byId("nope"));
        assertSame(ROWS.get(2), DianeTable.byId(DianeTable.QUEEN_EMBRACE));
    }

    @Test
    void theSizeChangesAreFreeStartedByTheClockAndNeverDrawn() {
        assertTrue(DianeTable.isSizeChange(DianeTable.GROW) && DianeTable.isSizeChange(DianeTable.SHRINK));
        assertFalse(DianeTable.isSizeChange(DianeTable.GIDEON_SMASH));
        assertEquals(0.0, DianeTable.byId(DianeTable.GROW).manaCost(), 1e-12, "growing costs no mana");
        assertEquals(0.0, DianeTable.byId(DianeTable.SHRINK).manaCost(), 1e-12);
        assertEquals(DianeTable.HUMAN, DianeTable.byId(DianeTable.GROW).group(), "she grows from human size");
        assertEquals(DianeTable.GIANT, DianeTable.byId(DianeTable.SHRINK).group(), "she shrinks from giant size");
        for (boolean giant : new boolean[]{false, true})
            for (MoveRow r : DianeTable.drawable(giant)) assertFalse(DianeTable.isSizeChange(r.id()), r.id());
        assertTrue(DianeTable.byId(DianeTable.GROW).windUpTicks() >= 24, "the growth is a quake you can see coming");
    }

    @Test
    void eachSizeHasItsOwnMovesAndTheEarthMagicWorksAtBoth() {
        List<MoveRow> giant = DianeTable.drawable(true), human = DianeTable.drawable(false);
        assertTrue(giant.stream().anyMatch(r -> r.id().equals(DianeTable.GIDEON_SMASH)) && giant.stream().noneMatch(r -> r.id().equals(DianeTable.RUSH_ROCK)));
        assertTrue(human.stream().anyMatch(r -> r.id().equals(DianeTable.RUSH_ROCK)) && human.stream().noneMatch(r -> r.id().equals(DianeTable.GIDEON_SMASH)));
        for (String both : new String[]{DianeTable.QUEEN_EMBRACE, DianeTable.RISING_METEOR}) {
            assertTrue(giant.stream().anyMatch(r -> r.id().equals(both)) && human.stream().anyMatch(r -> r.id().equals(both)), both + " is Creation, at any size");
        }
        assertTrue(giant.stream().anyMatch(MoveRow::ultimate) && human.stream().noneMatch(MoveRow::ultimate), "the Dance is a giant's");
        assertTrue(human.size() >= 6, "a Diane who cannot grow still has six moves to fight with: " + human.size());
    }

    @Test
    void theMixHasOneUltimateCloseWorkRangedAndCrowdMoves() {
        assertEquals(1, ROWS.stream().filter(MoveRow::ultimate).count());
        MoveRow ult = DianeTable.byId(DianeTable.DROLE_DANCE);
        assertTrue(ult.ultimate() && ult.minPower() >= 60);
        assertTrue(ult.windUpTicks() >= 56);
        assertTrue(ult.recovery() >= 40, "exposed afterwards");
        assertTrue(ult.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        for (boolean giant : new boolean[]{false, true}) {
            List<MoveRow> m = DianeTable.drawable(giant);
            assertTrue(m.stream().anyMatch(r -> r.maxRange() <= 11), "close work at either size");
            assertTrue(m.stream().anyMatch(r -> r.minRange() >= 4 && r.maxRange() >= 32), "ranged at either size");
            assertTrue(m.stream().anyMatch(MoveRow::crowd), "crowd moves at either size");
        }
    }

    @Test
    void thereIsNoDeadRangeAtTheOpeningAtEitherSize() {
        for (boolean giant : new boolean[]{false, true}) {
            List<MoveRow> m = DianeTable.drawable(giant);
            for (double d = 0; d <= 34; d += 0.5) {
                double dist = d;
                assertTrue(m.stream().anyMatch(r -> !r.ultimate() && r.minPower() <= 20 && dist >= r.minRange() && dist <= r.maxRange()
                                && !r.id().equals(DianeTable.HEAVY_METAL) && !r.id().equals(DianeTable.DIAMOND_SHIELD)),
                        "an attack to throw at " + d + " blocks at 20% (giant " + giant + ")");
            }
        }
    }

    @Test
    void theNumbersAreSane() {
        assertTrue(DianeTable.SMASH > DianeTable.SWING && DianeTable.SMASH >= DianeTable.SWEEP, "the giant's hammer is the heavier");
        assertTrue(DianeTable.SMASH_RADIUS > 3.5 && DianeTable.SWEEP_RANGE > DianeTable.SWING_RANGE, "a giant reaches further");
        assertTrue(DianeTable.SCALE_GIANT > 1.5 && DianeTable.SCALE_GIANT < 2.5);
        assertEquals(3.9, DianeSheet.HEIGHT * DianeTable.SCALE_GIANT, 0.01, "a giant is about 3.9 blocks tall");
        assertEquals(3, DianeTable.rockCount(1));
        assertEquals(6, DianeTable.rockCount(4));
        assertEquals(7, DianeTable.rockCount(40), "the rocks are capped");
        assertEquals(6, DianeTable.meteorCount(4));
        assertEquals(5, DianeTable.danceBeats(false));
        assertEquals(7, DianeTable.danceBeats(true));
        assertTrue(DianeTable.cutFraction(true) > DianeTable.cutFraction(false), "the Gates make her harder to cut down");
        assertTrue(DianeTable.cutFraction(true) < 0.2f);
        assertTrue(DianeTable.PREBEAT >= 6 && DianeTable.PREBEAT < DianeTable.BEAT_TICKS, "the foot rises before every stomp");
        assertTrue(WaveMath.ticksToReach(DianeTable.DANCE_RADIUS, DianeTable.DANCE_SPEED) < DianeTable.BEAT_TICKS, "a ring has run out before the next stomp");
        assertTrue(WaveMath.ticksToReach(DianeTable.QUEEN_LENGTH, DianeTable.QUEEN_SPEED) <= 32);
        assertTrue(DianeTable.METAL_TICKS >= 80 && DianeTable.METAL_TICKS <= 200);
        assertTrue(DianeTable.CRYSTAL_FRACTION <= 0.08f, "the shield is not a second health bar");
        assertTrue(DianeTable.GIANT_TAKES < 1f && DianeTable.CUT_TAKES > 1f);
    }

    /** Plays a fight at one size through the scheduler. */
    static Map<String, Integer> play(long seed, int power, int ticks, boolean giant) {
        List<MoveRow> rows = DianeTable.drawable(giant);
        MoveScheduler sch = new MoveScheduler(seed);
        sch.reset(0);
        Map<String, Integer> used = new HashMap<>();
        PowerCurve.Tuning tu = PowerCurve.tuning(power);
        long busyUntil = 0;
        String prev = "";
        int run = 0;
        for (long t = 20; t < ticks; t += 5) {
            if (t < busyUntil) continue;
            double dist = 2 + 17 * (1 + Math.sin(t / 97.0)) + 2 * Math.sin(t / 13.0);
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(rows, s);
            if (idx < 0) continue;
            MoveRow m = rows.get(idx);
            sch.commit(m, s, (int) Math.max(10, Math.round(m.cooldownTicks() * tu.cooldownMultiplier())));
            used.merge(m.id(), 1, Integer::sum);
            busyUntil = t + Math.max(8, Math.round(m.windUpTicks() * tu.windUpMultiplier())) + m.recovery();
            if (m.id().equals(prev)) run++;
            else { prev = m.id(); run = 1; }
            assertTrue(m.ultimate() || run <= MoveScheduler.REPEAT_LIMIT, "no move spammed: " + m.id());
        }
        return used;
    }

    @Test
    void atTheOpeningOnlyOpeningMovesAndNeverTheDance() {
        for (long seed = 1; seed <= 5; seed++)
            for (boolean giant : new boolean[]{false, true}) {
                Map<String, Integer> used = play(seed, 20, 20000, giant);
                for (String id : used.keySet()) assertTrue(DianeTable.byId(id).minPower() <= 20, id + " is not an opening move");
                assertNull(used.get(DianeTable.DROLE_DANCE));
                assertTrue(used.size() >= 3, "variety at 20% (giant " + giant + "): " + used.keySet());
            }
    }

    @Test
    void theDanceNeedsSixtyPercentAGiantAndTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 59, 20000, true).get(DianeTable.DROLE_DANCE));
            assertNotNull(play(seed, 60, 20000, true).get(DianeTable.DROLE_DANCE), "she dances at 60%");
            assertNull(play(seed, 100, 20000, false).get(DianeTable.DROLE_DANCE), "a human Diane does not dance");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(DianeTable.byId(DianeTable.DROLE_DANCE), early));
    }

    @Test
    void atTheGatesEveryDrawableMoveGetsUsedAtItsSizeAndTheDanceIsSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> giant = play(seed, 100, 60000, true), human = play(seed, 100, 60000, false);
            for (MoveRow r : DianeTable.drawable(true)) assertNotNull(giant.get(r.id()), r.id() + " is used by the giant at the Gates (seed " + seed + ")");
            for (MoveRow r : DianeTable.drawable(false)) assertNotNull(human.get(r.id()), r.id() + " is used by the human at the Gates (seed " + seed + ")");
            int dances = giant.get(DianeTable.DROLE_DANCE);
            assertTrue(dances >= 3);
            assertTrue(dances * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }
}
