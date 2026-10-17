package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Patolli is fast by design, so his fight is only fair if every strike is drawn long enough to read and the Overreach counter works as written; the table, his counts and that rule are held here. */
class PatolliTest {
    static final List<MidnightSpec> SPECS = PatolliTable.specs();

    @Test
    void theTableObeysTheLawsOfTheFramework() {
        List<String> problems = MidnightSpec.problems(SPECS);
        assertTrue(problems.isEmpty(), "problems: " + problems);
        assertTrue(SPECS.size() >= 8 && SPECS.size() <= 14, "eight to fourteen moves");
    }

    @Test
    void theFirstMovesKeepTheirPlaces() {
        // the client reads move names by index: the order below is part of the save format of the fight
        assertEquals(PatolliTable.LIGHT_SWORD, SPECS.get(0).id());
        assertEquals(PatolliTable.JUDGMENT_WHIP, SPECS.get(1).id());
        assertEquals(PatolliTable.LIGHT_STEP, SPECS.get(2).id());
        assertEquals(PatolliTable.VERDICT, SPECS.get(10).id());
        assertEquals(PatolliTable.DEMON_DANCE, SPECS.get(11).id(), "the Gates move is appended last");
        assertNull(PatolliTable.byId("nope"));
        assertNotNull(PatolliTable.byId(PatolliTable.LIGHT_SHAFT));
    }

    @Test
    void theMixIsMeleeRangedAreaMobilityControlAndOneUltimate() {
        assertEquals(1, SPECS.stream().filter(MidnightSpec::ultimate).count());
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 9), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 4 && s.maxRange() >= 34), "ranged");
        assertTrue(SPECS.stream().anyMatch(MidnightSpec::crowd), "crowd");
        assertNotNull(PatolliTable.byId(PatolliTable.LIGHT_STEP), "mobility");
        assertNotNull(PatolliTable.byId(PatolliTable.BLINDING_FLASH), "control");
        MidnightSpec v = PatolliTable.byId(PatolliTable.VERDICT);
        assertTrue(v.ultimate() && v.minPower() >= 60, "the Verdict needs 60%");
        assertTrue(v.windUpTicks() >= 60, "long enough to be broken: three seconds");
        assertTrue(PatolliTable.byId(PatolliTable.DEMON_DANCE).minPower() == 100, "the Dance is the Dark Elf's");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 38; d += 0.5) assertTrue(MidnightSpec.reaches(SPECS, 20, d), "an attack to throw at " + d + " blocks at 20%");
    }

    @Test
    void theCountsRiseWithThePower() {
        int[] powers = {20, 40, 60, 80, 100};
        for (int i = 1; i < powers.length; i++) {
            assertTrue(PatolliMath.bladeCount(powers[i]) >= PatolliMath.bladeCount(powers[i - 1]));
            assertTrue(PatolliMath.orbCount(powers[i]) >= PatolliMath.orbCount(powers[i - 1]));
            assertTrue(PatolliMath.salvoCount(powers[i]) >= PatolliMath.salvoCount(powers[i - 1]));
            assertTrue(PatolliMath.lampBlades(powers[i]) >= PatolliMath.lampBlades(powers[i - 1]));
            assertTrue(PatolliMath.danceCuts(powers[i]) >= PatolliMath.danceCuts(powers[i - 1]));
        }
        assertEquals(3, PatolliMath.bladeCount(20));
        assertEquals(6, PatolliMath.bladeCount(100));
        assertEquals(PatolliMath.bladeCount(40), PatolliMath.bladeCount(50), "a power between steps (a rage value) reads the step below");
        assertEquals(PatolliMath.bladeCount(20), PatolliMath.bladeCount(0), "under the opening power it is the opening count");
        assertEquals(PatolliMath.bladeCount(100), PatolliMath.bladeCount(250), "and over it the last");
    }

    @Test
    void aSpreadOfSwordsNeverWrapsAroundTheFoe() {
        for (int n = 1; n <= 8; n++) assertTrue(PatolliMath.bladeSpread(n) <= 1.2 + 1e-12);
        assertEquals(0.0, PatolliMath.bladeSpread(1), 1e-12);
        assertTrue(PatolliMath.bladeSpread(6) > PatolliMath.bladeSpread(3));
    }

    @Test
    void everyMarkCanBeLeftInTheTimeItIsShown() {
        assertTrue(MidnightMath.escapeReach(PatolliMath.ARROW_WARN) >= PatolliMath.ARROW_RADIUS + 0.3, "arrows: radius plus a body");
        assertTrue(MidnightMath.escapeReach(PatolliMath.ORB_WARN) >= PatolliMath.ORB_BEAM_HALF_WIDTH + 0.3 + 0.5, "an orb's beam can be sidestepped");
        assertTrue(MidnightMath.escapeReach(PatolliMath.LAMP_WARN) >= 1.4, "a lamp sword's line can be sidestepped");
        assertTrue(MidnightMath.escapeReach(PatolliMath.SHAFT_LOCK) >= PatolliMath.SHAFT_HALF_WIDTH + 0.3 + 0.5, "the frozen shaft can be sidestepped");
        assertTrue(MidnightMath.escapeReach(PatolliMath.VOLLEY_LOCK) >= 1.5, "the frozen volley lanes can be sidestepped");
        MidnightSpec step = PatolliTable.byId(PatolliTable.LIGHT_STEP);
        assertTrue(MidnightMath.escapeReach(step.windUpTicks()) >= PatolliMath.STEP_STRIKE_RADIUS + 0.3, "the arrival ring of the Light Step");
        MidnightSpec shaft = PatolliTable.byId(PatolliTable.LIGHT_SHAFT);
        assertTrue(shaft.windUpTicks() - PatolliMath.SHAFT_LOCK >= 12, "the aim is free for a while, the frozen line is shown for the framework's minimum tell");
    }

    @Test
    void theFlashAndTheVerdictHaveRealCounters() {
        assertEquals(1f, PatolliMath.verdictFactor(false, false, false), 0f);
        assertTrue(PatolliMath.verdictFactor(true, false, false) < 0.4f, "cover is the strongest cut");
        assertTrue(PatolliMath.verdictFactor(false, true, false) < 1f);
        assertTrue(PatolliMath.verdictFactor(false, false, true) < 1f);
        assertTrue(PatolliMath.verdictFactor(true, true, true) >= 0.2f, "never a free pass");
        assertTrue(PatolliMath.verdictFactor(true, true, false) <= PatolliMath.verdictFactor(true, false, false), "more protection never raises the blow");
        assertTrue(PatolliMath.verdictFactor(true, true, true) <= PatolliMath.verdictFactor(true, true, false));
        assertTrue(PatolliMath.FLASH_VIEW < Math.PI / 2, "someone looking sideways is not blinded");
    }

    @Test
    void overreachNeedsThreeEmptyStrikesInARow() {
        Overreach o = new Overreach(PatolliMath.OVERREACH_LIMIT);
        assertFalse(o.record(false));
        assertFalse(o.record(false));
        assertEquals(2, o.whiffs());
        assertTrue(o.record(false), "the third empty strike opens him");
        assertEquals(0, o.whiffs(), "and the count starts again");
        assertFalse(o.record(false));
        assertFalse(o.record(true), "a strike that lands clears the count");
        assertEquals(0, o.whiffs());
        assertFalse(o.record(false));
        assertFalse(o.record(false));
        assertFalse(o.record(true));
        assertFalse(o.record(false));
        assertEquals(1, o.whiffs());
        o.reset();
        assertEquals(0, o.whiffs());
        assertEquals(1, new Overreach(0).limit(), "a limit of zero is one");
        assertTrue(new Overreach(0).record(false));
    }

    @Test
    void theLightStepIsTheBreadAndButterAtMidRange() {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(5);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 6000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(14.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            MidnightSpec m = SPECS.get(idx);
            sch.commit(m, s, (int) Math.round(m.cooldownTicks() * tu.cooldownMultiplier()));
            used.merge(m.id(), 1, Integer::sum);
        }
        assertTrue(used.getOrDefault(PatolliTable.LIGHT_STEP, 0) > 5, "he steps in a lot: " + used);
        assertTrue(used.getOrDefault(PatolliTable.LIGHT_SWORD, 0) > 5, "and throws swords: " + used);
        assertNull(used.get(PatolliTable.JUDGMENT_WHIP), "no whip at fourteen blocks");
        assertNull(used.get(PatolliTable.DEMON_DANCE), "no Dance before the Gates");
        assertNull(used.get(PatolliTable.VERDICT), "no Verdict at 40%");
    }
}
