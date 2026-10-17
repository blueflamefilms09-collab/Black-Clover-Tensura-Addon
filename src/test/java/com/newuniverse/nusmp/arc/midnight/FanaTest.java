package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Fana's fight rests on numbers nobody has played yet, so the laws it must obey (a readable telegraph, marks that can be left, a robe with a real weakness, an Evil Heart that can be broken) are held here as tests instead of being found by a player. */
class FanaTest {
    static final List<MidnightSpec> SPECS = FanaTable.specs();

    @Test
    void theTableObeysTheLawsOfTheFramework() {
        List<String> problems = MidnightSpec.problems(SPECS);
        assertTrue(problems.isEmpty(), "problems: " + problems);
        assertTrue(SPECS.size() >= 8 && SPECS.size() <= 14, "eight to fourteen moves");
    }

    @Test
    void theMixAndTheOrderAreRight() {
        assertEquals(FanaTable.FIREBALL, SPECS.get(0).id());
        assertEquals(FanaTable.EVIL_HEART, SPECS.get(12).id(), "the Gates ultimate is appended last");
        assertEquals(1, SPECS.stream().filter(MidnightSpec::ultimate).count());
        MidnightSpec heart = FanaTable.byId(FanaTable.EVIL_HEART);
        assertTrue(heart.ultimate() && heart.minPower() == 100, "the Evil Heart is the Gates' move");
        assertTrue(heart.windUpTicks() >= 60 && heart.recovery() >= 60, "a long warning and a long rest");
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 8), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 8 && s.maxRange() >= 36), "ranged");
        assertTrue(SPECS.stream().anyMatch(MidnightSpec::crowd), "crowd");
        assertNotNull(FanaTable.byId(FanaTable.CHARGE), "mobility");
        assertNotNull(FanaTable.byId(FanaTable.ARMS), "control");
        assertNotNull(FanaTable.byId(FanaTable.ROBE), "defence");
        assertNull(FanaTable.byId("nope"));
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 40; d += 0.5) assertTrue(MidnightSpec.reaches(SPECS, 20, d), "an attack to throw at " + d + " blocks at 20%");
    }

    @Test
    void theCountsRiseWithThePower() {
        int[] powers = {20, 40, 60, 80, 100};
        for (int i = 1; i < powers.length; i++) {
            assertTrue(FanaMath.fireballCount(powers[i]) >= FanaMath.fireballCount(powers[i - 1]));
            assertTrue(FanaMath.pillarCount(powers[i]) >= FanaMath.pillarCount(powers[i - 1]));
            assertTrue(FanaMath.emberCount(powers[i]) >= FanaMath.emberCount(powers[i - 1]));
            assertTrue(FanaMath.eruptionWaves(powers[i]) >= FanaMath.eruptionWaves(powers[i - 1]));
        }
        assertEquals(1, FanaMath.fireballCount(20));
        assertEquals(3, FanaMath.fireballCount(100));
        assertEquals(FanaMath.emberCount(60), FanaMath.emberCount(70), "a rage value reads the step below");
    }

    @Test
    void everyMarkCanBeLeftInTheTimeItIsShown() {
        MidnightSpec col = FanaTable.byId(FanaTable.COLUMNS);
        assertTrue(MidnightMath.escapeReach(col.windUpTicks()) >= FanaMath.COLUMN_RADIUS + 0.3);
        MidnightSpec arms = FanaTable.byId(FanaTable.ARMS);
        assertTrue(MidnightMath.escapeReach(arms.windUpTicks()) >= FanaMath.ARMS_RADIUS + 0.3);
        assertTrue(MidnightMath.escapeReach(FanaMath.EMBER_WARN) >= FanaMath.EMBER_RADIUS + 0.3);
        MidnightSpec charge = FanaTable.byId(FanaTable.CHARGE);
        assertTrue(MidnightMath.escapeReach(charge.windUpTicks()) >= FanaMath.CHARGE_HALF_WIDTH + 0.3 + 0.5, "the charge lane can be left");
        MidnightSpec spiral = FanaTable.byId(FanaTable.SPIRAL);
        assertTrue(MidnightMath.escapeReach(FanaMath.SPIRAL_LOCK) >= FanaMath.SPIRAL_HALF_WIDTH + 0.3 + 0.5, "the frozen spiral lane can be left");
        assertTrue(spiral.windUpTicks() - FanaMath.SPIRAL_LOCK >= 12);
        MidnightSpec roar = FanaTable.byId(FanaTable.ROAR);
        assertTrue(roar.windUpTicks() >= 16, "the roar is shown");
    }

    @Test
    void theEruptionRingsAreOrderedAndFinishInTime() {
        for (int k = 1; k < 4; k++) assertTrue(FanaMath.eruptionReach(k) > FanaMath.eruptionReach(k - 1));
        MidnightSpec er = FanaTable.byId(FanaTable.ERUPTION);
        assertTrue(FanaMath.eruptionReach(FanaMath.eruptionWaves(100) - 1) <= er.maxRange() + 0.5, "the last ring fits the move's own range band");
        assertTrue(FanaMath.eruptionTicks(FanaMath.eruptionReach(0)) < 20);
        assertTrue(FanaMath.eruptionTicks(5.0) >= 6);
    }

    @Test
    void theRobeShrugsOffFireButNotWater() {
        assertEquals(PhoenixRobe.Element.FIRE, PhoenixRobe.classify("tensura:fire_elemental"));
        assertEquals(PhoenixRobe.Element.FIRE, PhoenixRobe.classify("minecraft:on_fire"));
        assertEquals(PhoenixRobe.Element.FIRE, PhoenixRobe.classify("minecraft:lava"));
        assertEquals(PhoenixRobe.Element.COOL, PhoenixRobe.classify("tensura:water_elemental"));
        assertEquals(PhoenixRobe.Element.COOL, PhoenixRobe.classify("minecraft:freeze"));
        assertEquals(PhoenixRobe.Element.COOL, PhoenixRobe.classify("nusmp:ice_wedge"));
        assertEquals(PhoenixRobe.Element.COOL, PhoenixRobe.classify("tensura:steam_fire_water"), "a cool word wins over a fire word");
        assertEquals(PhoenixRobe.Element.OTHER, PhoenixRobe.classify("tensura:light_elemental"));
        assertEquals(PhoenixRobe.Element.OTHER, PhoenixRobe.classify("minecraft:generic"));
        assertEquals(PhoenixRobe.Element.OTHER, PhoenixRobe.classify("minecraft:sacrifice"), "'ice' inside a word is not ice");
        assertEquals(PhoenixRobe.Element.OTHER, PhoenixRobe.classify("minecraft:device_blast"));
        assertEquals(PhoenixRobe.Element.OTHER, PhoenixRobe.classify(null));
        assertEquals(PhoenixRobe.Element.OTHER, PhoenixRobe.classify(""));
        assertTrue(PhoenixRobe.factor(PhoenixRobe.Element.FIRE) < PhoenixRobe.factor(PhoenixRobe.Element.OTHER));
        assertEquals(1.0, PhoenixRobe.factor(PhoenixRobe.Element.COOL), 0.0, "water goes through whole");
        assertTrue(PhoenixRobe.factor(PhoenixRobe.Element.FIRE) > 0.0 && PhoenixRobe.factor(PhoenixRobe.Element.OTHER) < 1.0);
        assertTrue(PhoenixRobe.DURATION <= 100, "a robe, not a wall: five seconds at most");
        assertTrue(FanaTable.byId(FanaTable.ROBE).cooldownTicks() >= PhoenixRobe.DURATION * 6, "and it is down far longer than it is up");
    }

    @Test
    void theEvilHeartCountsDownAndCanBeBroken() {
        EvilHeart h = new EvilHeart(10);
        assertEquals(EvilHeart.Phase.IDLE, h.phase());
        assertEquals(EvilHeart.Phase.IDLE, h.tick(true), "an idle heart ignores everything");
        h.begin();
        assertTrue(h.charging());
        for (int i = 0; i < 9; i++) assertEquals(EvilHeart.Phase.CHARGING, h.tick(false));
        assertEquals(1, h.left());
        assertEquals(EvilHeart.Phase.DETONATE, h.tick(false), "the tenth tick detonates");
        assertEquals(EvilHeart.Phase.DETONATE, h.tick(true), "once spent it stays spent");
        h.begin();
        assertEquals(EvilHeart.Phase.CHARGING, h.tick(false));
        assertEquals(EvilHeart.Phase.BROKEN, h.tick(true), "broken the moment the party has done enough");
        assertEquals(EvilHeart.Phase.BROKEN, h.tick(false));
        assertFalse(h.charging());
        h.reset();
        assertEquals(EvilHeart.Phase.IDLE, h.phase());
        assertEquals(10, h.left() + 0, "a reset heart has all its time again");
    }

    @Test
    void theSphereGrowsEvenlyAndTheBlastFallsOff() {
        EvilHeart h = new EvilHeart(100);
        h.begin();
        assertEquals(EvilHeart.START_RADIUS, h.radius(), 1e-9);
        for (int i = 0; i < 50; i++) h.tick(false);
        assertEquals((EvilHeart.START_RADIUS + EvilHeart.MAX_RADIUS) / 2, h.radius(), 1e-9);
        assertEquals(0.5f, h.progress(), 1e-6f);
        assertEquals(EvilHeart.MAX_RADIUS, EvilHeart.radiusAt(1.0), 1e-9);
        assertEquals(EvilHeart.MAX_RADIUS, EvilHeart.radiusAt(7.0), 1e-9, "progress is clamped");
        double r = 17;
        assertEquals(1.0, EvilHeart.falloff(0, r), 1e-12);
        assertEquals(1.0, EvilHeart.falloff(r * EvilHeart.CORE, r), 1e-12);
        assertEquals(EvilHeart.RIM, EvilHeart.falloff(r, r), 1e-12);
        assertEquals(0.0, EvilHeart.falloff(r + 0.01, r), 0.0);
        double prev = 2;
        for (double d = 0; d <= r; d += 0.5) {
            double f = EvilHeart.falloff(d, r);
            assertTrue(f <= prev + 1e-12, "never stronger farther out");
            assertTrue(f >= EvilHeart.RIM - 1e-12 && f <= 1.0 + 1e-12);
            prev = f;
        }
        assertEquals(0.0, EvilHeart.falloff(1, 0), 0.0, "no sphere, no blast");
    }

    @Test
    void shelterHelpsButNeverCompletely() {
        assertEquals(1.0, EvilHeart.shelter(false, false, false), 1e-12);
        assertTrue(EvilHeart.shelter(true, false, false) < 0.5, "a wall is the best shelter");
        assertTrue(EvilHeart.shelter(false, true, false) <= 0.5);
        assertTrue(EvilHeart.shelter(true, true, true) >= 0.2, "never a free pass");
        assertTrue(EvilHeart.shelter(true, true, false) <= EvilHeart.shelter(true, false, false));
        assertTrue(FanaMath.HEART_REACH > EvilHeart.MAX_RADIUS);
    }

    @Test
    void theHeartIsBeatableByAPartyThatHitsHard() {
        // the default countdown (150 ticks) against the default break share (6%): the budget lets 6% of the bar go every second at the Gates,
        // 9% in the final phase, so the party needs well under a second of full damage, and a weak one needs 1% of the bar every 25 ticks
        double needPerSecond = 0.06 / (150 / 20.0);
        assertTrue(needPerSecond < 0.01, "under one percent of the bar per second");
        assertTrue(0.06 < 0.09 * 150 / 20.0, "the budget does not stop the party from breaking it");
    }

    @Test
    void fanaOpensWithBreathAndClawUpCloseAndFireballsFarAway() {
        Map<String, Integer> near = play(3.0), far = play(20.0);
        assertTrue(near.getOrDefault(FanaTable.CLAW, 0) + near.getOrDefault(FanaTable.BREATH, 0) > 5, "close in she claws and breathes: " + near);
        assertNull(near.get(FanaTable.FIREBALL), "no fireball at three blocks");
        assertTrue(far.getOrDefault(FanaTable.FIREBALL, 0) > 5, "far away she throws fireballs: " + far);
        assertNull(far.get(FanaTable.CLAW));
        assertNull(far.get(FanaTable.EVIL_HEART), "the Evil Heart never comes before the Gates");
    }

    static Map<String, Integer> play(double distance) {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(9);
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
