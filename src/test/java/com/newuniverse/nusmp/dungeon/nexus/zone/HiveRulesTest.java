package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.HiveRules.Budget;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveRules.Stats;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Kind;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** What the swarm is made of, what it is called, and the budget that keeps its blows on one player from ever being a stun-lock. */
class HiveRulesTest {
    @Test
    void everyKindHasAMutationThatIsFiniteAndVariesALittle() {
        for (Kind k : Kind.values()) {
            Stats lo = HiveRules.mutate(k, 0.0), mid = HiveRules.mutate(k, 0.5), hi = HiveRules.mutate(k, 1.0);
            assertTrue(lo.health() > 1.0 && hi.health() < 3.0, "tougher than the plain creature, but not a boss: " + k);
            assertTrue(lo.health() < hi.health(), "individuals differ");
            assertEquals(mid.health(), (lo.health() + hi.health()) / 2, 1e-9);
            assertTrue(hi.health() / lo.health() < 1.2, "by a few per cent only");
            assertTrue(mid.attack() >= 1.0 && mid.attack() <= 1.4, "modest damage: " + k);
            assertTrue(mid.speed() >= 0.9 && mid.speed() <= 1.2, "speed: " + k);
            assertTrue(mid.scale() >= 1.0 && mid.scale() <= 1.3);
        }
        assertEquals(HiveRules.mutate(Kind.ORC, 0.5), HiveRules.mutate(Kind.ORC, 0.5));
        assertEquals(HiveRules.mutate(Kind.ORC, -4.0), HiveRules.mutate(Kind.ORC, 0.0), "a roll out of range is clamped");
        assertEquals(HiveRules.mutate(Kind.ORC, 7.0), HiveRules.mutate(Kind.ORC, 1.0));
        assertTrue(HiveRules.mutate(Kind.ORC, 0.5).speed() < HiveRules.mutate(Kind.HOUND, 0.5).speed(), "orcs are the slow ones");
    }

    @Test
    void totalsNeverShrinkAndNeverPassTheirCaps() {
        assertEquals(36.0, HiveRules.total(20.0, 1.8, HiveRules.MAX_HEALTH), 1e-9);
        assertEquals(HiveRules.MAX_HEALTH, HiveRules.total(150.0, 2.2, HiveRules.MAX_HEALTH), 1e-9, "capped");
        assertEquals(200.0, HiveRules.total(200.0, 2.0, HiveRules.MAX_HEALTH), 1e-9, "never below what it was");
        assertEquals(6.0, HiveRules.total(6.0, 1.0, HiveRules.MAX_ATTACK), 1e-9);
        assertEquals(HiveRules.MAX_ATTACK, HiveRules.total(13.0, 1.25, HiveRules.MAX_ATTACK), 1e-9);
    }

    @Test
    void theNamesAndIdsAreOneToOne() {
        Set<String> ids = new HashSet<>(), names = new HashSet<>();
        for (Kind k : Kind.values()) {
            assertTrue(HiveRules.typeId(k).startsWith("tensura:"));
            assertTrue(ids.add(HiveRules.typeId(k)));
            assertTrue(names.add(HiveRules.name(k)));
            assertTrue(HiveRules.name(k).startsWith("Mutated "));
            assertSame(k, HiveRules.kindOf(HiveRules.typeId(k)));
        }
        assertNull(HiveRules.kindOf("minecraft:zombie"));
        assertNull(HiveRules.kindOf(""));
        assertEquals("tensura:hound_dog", HiveRules.typeId(Kind.HOUND));
    }

    @Test
    void aPlayersPowerGrowsWithHealthAndArmourButStaysInRange() {
        assertEquals(1.0, HiveRules.power(20.0, 0.0), 1e-9);
        assertTrue(HiveRules.power(40.0, 20.0) > HiveRules.power(20.0, 0.0));
        assertTrue(HiveRules.power(20.0, 20.0) > HiveRules.power(20.0, 0.0));
        assertEquals(4.0, HiveRules.power(1000.0, 1000.0), 1e-9);
        assertEquals(0.6, HiveRules.power(1.0, 0.0), 1e-9);
    }

    @Test
    void aBlowIsCappedAtAShareOfTheVictimsHealth() {
        Budget b = new Budget();
        assertEquals(2.0, b.take(0, 50.0, 20.0), 1e-9, "10 per cent of 20 health");
        assertEquals(1.0, new Budget().take(0, 1.0, 20.0), 1e-9, "small blows pass whole");
        assertEquals(0.0, new Budget().take(0, 0.0, 20.0));
        assertEquals(0.0, new Budget().take(0, -3.0, 20.0));
        assertEquals(HiveRules.MIN_HIT, new Budget().take(0, 50.0, 8.0), 1e-9, "a frail victim is not made immune: at least one point lands");
        assertEquals(10.0, new Budget().take(0, 999.0, 100.0), 1e-9, "scales with the victim");
    }

    @Test
    void theBlowsOfAnySecondTogetherAreCappedToo() {
        Budget b = new Budget();
        double total = 0;
        for (int tick = 0; tick < HiveRules.WINDOW; tick++) total += b.take(tick, 9.0, 20.0);
        assertEquals(HiveRules.WINDOW_SHARE * 20.0, total, 1e-9, "four points a second at most from the whole swarm");
        assertEquals(0.0, b.take(HiveRules.WINDOW - 1, 9.0, 20.0), "the rest is turned away");
        assertTrue(b.take(HiveRules.WINDOW, 9.0, 20.0) > 0, "as the first blows leave the window there is room again");
        // a sliding window: no stretch of WINDOW ticks, wherever it starts, holds more than the share
        Budget s = new Budget();
        double[] dealt = new double[400];
        for (int t = 0; t < dealt.length; t++) dealt[t] = s.take(t, 1.0 + (t % 7), 20.0);
        for (int start = 0; start + HiveRules.WINDOW <= dealt.length; start++) {
            double sum = 0;
            for (int t = start; t < start + HiveRules.WINDOW; t++) sum += dealt[t];
            assertTrue(sum <= HiveRules.WINDOW_SHARE * 20.0 + 1e-9, "window at " + start + " took " + sum);
        }
    }

    @Test
    void aSwarmOfFortyCannotKillAPlayerInASecond() {
        for (double max : new double[]{10, 20, 40, 100, 400}) {
            Budget b = new Budget();
            double dealt = 0;
            for (int blow = 0; blow < 200; blow++) dealt += b.take(blow / 10, 25.0, max);               // ten blows a tick for twenty ticks
            assertTrue(dealt <= HiveRules.WINDOW_SHARE * max + 1e-9, "max " + max + " took " + dealt);
            assertTrue(dealt < max, "never the whole of it in a second");
        }
    }

    @Test
    void theClockGoingBackwardsResetsTheWindow() {
        Budget b = new Budget();
        b.take(1000, 99.0, 20.0);
        b.take(1005, 99.0, 20.0);
        assertTrue(b.take(0, 99.0, 20.0) > 0, "a world that was reloaded does not carry a stale budget");
    }
}
