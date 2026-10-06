package com.newuniverse.nusmp.core.magic.grimoire;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/** The hip -> hand trip, the page flip curve and the spell archetypes, all headless. */
class GrimoireCarryTest {
    private static final float EPS = 1e-4f;

    @Test
    void summonStartsAtTheHipAndEndsAtTheHand() {
        assertEquals(GrimoireCarry.HIP, GrimoireCarry.trip(true, 0f, false));
        GrimoireCarry.Pose end = GrimoireCarry.trip(true, GrimoireCarry.TRAVEL_TICKS, false);
        assertEquals(GrimoireCarry.HAND.up(), end.up(), EPS);
        assertEquals(GrimoireCarry.HAND.forward(), end.forward(), EPS);
        assertEquals(GrimoireCarry.HAND.scale(), end.scale(), EPS);
    }

    @Test
    void stowIsTheTripBackwards() {
        for (float age = 0; age <= GrimoireCarry.TRAVEL_TICKS; age += 0.5f) {
            GrimoireCarry.Pose there = GrimoireCarry.trip(true, age, false), back = GrimoireCarry.trip(false, GrimoireCarry.TRAVEL_TICKS - age, false);
            assertEquals(there.up(), back.up(), 1e-3f);
            assertEquals(there.yaw(), back.yaw(), 1e-2f);
        }
    }

    @Test
    void tripIsSmoothAndMonotonic() {
        float last = -1;
        for (float age = -2; age <= GrimoireCarry.TRAVEL_TICKS + 2; age += 0.25f) {
            float t = GrimoireCarry.travel(age);
            assertTrue(t >= last - EPS && t >= 0 && t <= 1);
            if (last >= 0) assertTrue(t - last < 0.1f, "no jumps");
            last = t;
        }
    }

    @Test
    void bookArcsUpOutOfTheHip() {
        GrimoireCarry.Pose mid = GrimoireCarry.blend(GrimoireCarry.HIP, GrimoireCarry.HAND, 0.5f);
        assertTrue(mid.up() > (GrimoireCarry.HIP.up() + GrimoireCarry.HAND.up()) / 2);
    }

    @Test
    void pagesTurnInOrderAndNeverPop() {
        float total = GrimoireCarry.flipDuration();
        for (int k = 0; k < GrimoireCarry.FLIP_PAGES; k++) {
            assertEquals(0f, GrimoireCarry.pageAlpha(k, 0f), EPS);
            assertEquals(0f, GrimoireCarry.pageAlpha(k, total + 1), EPS);
            assertEquals(GrimoireCarry.FLIP_MAX_DEG, GrimoireCarry.pageAngle(k, total + 1), EPS);
            float last = -1;
            for (float a = 0; a <= total; a += 0.1f) {
                float deg = GrimoireCarry.pageAngle(k, a);
                assertTrue(deg >= last - EPS && deg <= GrimoireCarry.FLIP_MAX_DEG + EPS);
                last = deg;
            }
        }
        assertTrue(GrimoireCarry.pageAngle(0, 3f) > GrimoireCarry.pageAngle(1, 3f), "first page leads");
        assertTrue(total < 20, "a flip is quick (rapid rustle)");
    }

    @Test
    void archetypesFromPageIds() {
        assertEquals(SpellArchetype.OFFENSE, SpellArchetype.of("exploding_fireball"));
        assertEquals(SpellArchetype.DEFENSE, SpellArchetype.of("mercury_shield"));
        assertEquals(SpellArchetype.DEFENSE, SpellArchetype.of("wall"));
        assertEquals(SpellArchetype.BUFF, SpellArchetype.of("thunder_boots"));
        assertEquals(SpellArchetype.DEBUFF, SpellArchetype.of("ice_prison"));
        assertEquals(SpellArchetype.DEBUFF, SpellArchetype.of("chrono_stasis"));
        assertEquals(SpellArchetype.OFFENSE, SpellArchetype.of(null));
        for (SpellArchetype a : SpellArchetype.values()) {
            int seed = SpellArchetype.packSeed(35, a);
            assertEquals(35, SpellArchetype.magicOf(seed));
            assertEquals(a, SpellArchetype.archetypeOf(seed));
            assertTrue(new File("src/main/resources/assets/nusmp/" + a.texture()).isFile(), "badge texture " + a);
        }
        assertNull(SpellArchetype.archetypeOf(7), "old cards carry no badge");
    }
}
