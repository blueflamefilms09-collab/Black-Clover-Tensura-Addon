package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Rades's table, his army and ward, the Soul Abductor's memory and the retreat rule. */
class RadesTest {
    static final List<MidnightSpec> SPECS = RadesTable.specs();

    @Test
    void theTableObeysTheLawsOfTheFramework() {
        List<String> problems = MidnightSpec.problems(SPECS);
        assertTrue(problems.isEmpty(), "problems: " + problems);
        assertTrue(SPECS.size() >= 8 && SPECS.size() <= 14, "eight to fourteen moves");
    }

    @Test
    void theMixAndTheOrderAreRight() {
        assertEquals(RadesTable.RAISE_DEAD, SPECS.get(0).id());
        assertEquals(RadesTable.BETRAYED_WRATH, SPECS.get(11).id(), "the Gates move is appended last");
        assertEquals(1, SPECS.stream().filter(MidnightSpec::ultimate).count());
        MidnightSpec legion = RadesTable.byId(RadesTable.LEGION);
        assertTrue(legion.ultimate() && legion.minPower() == 60, "the Legion needs 60%");
        assertTrue(legion.recovery() >= 60, "he is exposed after it");
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 8), "melee or close");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 6 && s.maxRange() >= 36), "ranged");
        assertTrue(SPECS.stream().anyMatch(MidnightSpec::crowd), "crowd");
        assertNotNull(RadesTable.byId(RadesTable.SPECTRAL_GATE), "mobility");
        assertNotNull(RadesTable.byId(RadesTable.DEAD_HANDS), "control");
        assertNotNull(RadesTable.byId(RadesTable.RAISE_DEAD), "summon");
        assertNotNull(RadesTable.byId(RadesTable.SOUL_ABDUCTOR));
        assertNull(RadesTable.byId("nope"));
    }

    @Test
    void thereIsNoDeadRangeAtTheOpeningOnceTheSummonsAreSetAside() {
        for (double d = 0; d <= 38; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()
                            && !s.id().equals(RadesTable.RAISE_DEAD)),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void theArmyGrowsWithThePowerAndObeysTheServerLimit() {
        int[] powers = {20, 40, 60, 80, 100};
        for (int i = 1; i < powers.length; i++) {
            assertTrue(RadesMath.thrallCap(powers[i], 12) >= RadesMath.thrallCap(powers[i - 1], 12));
            assertTrue(RadesMath.thrallHealth(powers[i]) > RadesMath.thrallHealth(powers[i - 1]));
            assertTrue(RadesMath.thrallAttack(powers[i]) > RadesMath.thrallAttack(powers[i - 1]));
            assertTrue(RadesMath.bulletCount(powers[i]) >= RadesMath.bulletCount(powers[i - 1]));
            assertTrue(RadesMath.thunderCount(powers[i]) >= RadesMath.thunderCount(powers[i - 1]));
            assertTrue(RadesMath.graveCount(powers[i]) >= RadesMath.graveCount(powers[i - 1]));
        }
        assertEquals(2, RadesMath.thrallCap(20, 12));
        assertEquals(6, RadesMath.thrallCap(100, 12));
        assertEquals(3, RadesMath.thrallCap(100, 3), "the server's limit wins");
        assertEquals(0, RadesMath.thrallCap(100, 0), "a limit of zero means no army");
        assertEquals(0, RadesMath.thrallCap(60, -4), "a negative limit is no army either");
        assertTrue(RadesMath.graveCount(100) <= RadesMath.thrallCap(100, 12), "one cast never calls more than the army may be");
        assertTrue(RadesMath.thrallHealth(100) < 80, "a corpse can be cut down");
        assertTrue(RadesMath.thrallHealth(20) >= 20, "but it is not paper");
    }

    @Test
    void theWardIsCappedAndAntiMagicCutsIt() {
        assertEquals(1.0, RadesMath.wardFactor(0, false), 1e-12);
        assertEquals(0.93, RadesMath.wardFactor(1, false), 1e-12);
        assertEquals(0.58, RadesMath.wardFactor(6, false), 1e-12, "six corpses: 42% soaked");
        assertEquals(0.58, RadesMath.wardFactor(12, false), 1e-12, "the ceiling holds");
        assertEquals(1.0, RadesMath.wardFactor(6, true), 1e-12, "a severed thread soaks nothing");
        assertEquals(1.0, RadesMath.wardFactor(-3, false), 1e-12);
        for (int n = 1; n <= 12; n++) assertTrue(RadesMath.wardFactor(n, false) <= RadesMath.wardFactor(n - 1, false), "more corpses never soak less");
        assertTrue(RadesMath.WARD_MAX < 0.5, "never more than half of a blow");
        assertTrue(RadesMath.SEVER_TICKS >= 100, "long enough to use");
        // the ward at its best against a party at its worst still lets a fight finish
        assertTrue(RadesMath.wardFactor(RadesMath.thrallCap(100, 12), false) > 0.5);
    }

    @Test
    void marksAreFair() {
        MidnightSpec th = RadesTable.byId(RadesTable.ALFRED);
        assertTrue(MidnightMath.escapeReach(RadesMath.THUNDER_WARN) >= RadesMath.THUNDER_RADIUS + 0.3);
        assertTrue(th.windUpTicks() >= RadesMath.THUNDER_WARN, "the strike marks show for the whole wind-up");
        MidnightSpec burst = RadesTable.byId(RadesTable.CORPSE_BURST);
        assertTrue(MidnightMath.escapeReach(burst.windUpTicks()) >= RadesMath.BURST_RADIUS + 0.3);
        MidnightSpec hands = RadesTable.byId(RadesTable.DEAD_HANDS);
        assertTrue(MidnightMath.escapeReach(hands.windUpTicks()) >= RadesMath.HANDS_RADIUS + 0.3);
        MidnightSpec thread = RadesTable.byId(RadesTable.SOUL_THREAD);
        assertTrue(thread.windUpTicks() - RadesMath.THREAD_LOCK >= 12);
        assertTrue(MidnightMath.escapeReach(RadesMath.THREAD_LOCK) >= 1.0);
        assertTrue(RadesMath.wailTicks(RadesMath.WAIL_RADIUS) >= 10 && RadesMath.wailTicks(RadesMath.WAIL_RADIUS) <= 20);
    }

    @Test
    void theSoulLedgerRemembersTheFallenForFifteenSeconds() {
        SoulLedger l = new SoulLedger();
        l.fell(100, 1, 64, 1);
        l.fell(200, 2, 64, 2);
        l.fell(250, 3, 64, 3);
        assertEquals(3, l.count(300, RadesMath.ABDUCT_WINDOW));
        assertEquals(2, l.count(450, RadesMath.ABDUCT_WINDOW), "tick 100 is older than 300 ticks at tick 450");
        List<double[]> back = l.take(450, RadesMath.ABDUCT_WINDOW, 5);
        assertEquals(2, back.size());
        assertEquals(3.0, back.get(0)[0], 0.0, "the newest comes back first");
        assertEquals(2.0, back.get(1)[0], 0.0);
        assertEquals(0, l.size(), "a corpse revived is forgotten");
        assertEquals(0, l.take(450, RadesMath.ABDUCT_WINDOW, 5).size());
    }

    @Test
    void theSoulLedgerRespectsTheMaximumAndIsBounded() {
        SoulLedger l = new SoulLedger();
        for (int i = 0; i < 10; i++) l.fell(100 + i, i, 0, 0);
        assertEquals(3, l.take(120, 300, 3).size(), "at most as many as asked");
        assertEquals(7, l.size());
        assertEquals(0, l.take(120, 300, 0).size());
        assertEquals(0, l.take(120, 300, -2).size());
        for (int i = 0; i < 500; i++) l.fell(1000 + i, 0, 0, 0);
        assertTrue(l.size() <= 32, "the memory is bounded");
        l.clear();
        assertEquals(0, l.size());
        assertEquals(0, l.count(5000, 300));
    }

    @Test
    void heRunsOnlyWhenReallyPressedAndOnlyWhenTheGateIsReady() {
        float max = 1000f;
        assertFalse(RadesMath.shouldRetreat(50f, max, 1000, 0), "5% is a scratch");
        assertFalse(RadesMath.shouldRetreat(119f, max, 1000, 0));
        assertTrue(RadesMath.shouldRetreat(120f, max, 1000, 0), "12% in the window is a burst");
        assertTrue(RadesMath.shouldRetreat(400f, max, 1000, 0));
        assertFalse(RadesMath.shouldRetreat(400f, max, 1000, 1001), "the gate is not ready");
        assertTrue(RadesMath.shouldRetreat(400f, max, 1001, 1001), "ready exactly at its tick");
        assertFalse(RadesMath.shouldRetreat(400f, 0f, 1000, 0), "no maximum, no retreat");
        // the damage budget lets 6% of the bar go each second (9% at the Gates): the window is three seconds, so 12% is within reach of a real party
        assertTrue(0.06 * (RadesMath.RETREAT_WINDOW / 20.0) > RadesMath.RETREAT_FRACTION);
        assertTrue(RadesMath.RETREAT_COOLDOWN >= 200, "he cannot run again and again");
    }

    @Test
    void closeInHeFightsWithHandsAndWailsAndFarAwayWithShotsAndThunder() {
        Map<String, Integer> near = play(2.5), far = play(25.0);
        assertTrue(near.getOrDefault(RadesTable.DEAD_HANDS, 0) + near.getOrDefault(RadesTable.WRAITH_WAIL, 0) > 5, "close in: " + near);
        assertNull(near.get(RadesTable.JIMMY), "no bullets at two and a half blocks");
        assertTrue(far.getOrDefault(RadesTable.JIMMY, 0) + far.getOrDefault(RadesTable.ALFRED, 0) > 5, "far away: " + far);
        assertNull(far.get(RadesTable.WRAITH_WAIL));
        assertNull(far.get(RadesTable.LEGION), "no Legion before 60%");
        assertNull(far.get(RadesTable.BETRAYED_WRATH), "no Wrath before the Gates");
    }

    static Map<String, Integer> play(double distance) {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(21);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 6000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(distance, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            MidnightSpec m = SPECS.get(idx);
            sch.commit(m, s, (int) Math.round(m.cooldownTicks() * tu.cooldownMultiplier()));
            used.merge(m.id(), 1, Integer::sum);
        }
        return used;
    }
}
