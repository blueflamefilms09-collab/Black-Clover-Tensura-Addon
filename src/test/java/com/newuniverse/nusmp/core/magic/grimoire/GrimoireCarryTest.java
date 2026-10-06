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
    void bookOpensAfterArrivingAndClosesBeforeLeaving() {
        assertEquals(0, GrimoireCarry.openStep(0f));
        assertEquals(0, GrimoireCarry.openStep(GrimoireCarry.TRAVEL_TICKS - 0.1f), "closed while it travels");
        assertEquals(1, GrimoireCarry.openStep(GrimoireCarry.TRAVEL_TICKS));
        assertEquals(GrimoireCarry.OPEN_STEPS, GrimoireCarry.openStep(GrimoireCarry.TRAVEL_TICKS + 100));
        int last = 0;
        for (float a = 0; a < GrimoireCarry.TRAVEL_TICKS + 20; a += 0.5f) { int s = GrimoireCarry.openStep(a); assertTrue(s >= last); last = s; }
        assertEquals(GrimoireCarry.OPEN_STEPS, GrimoireCarry.closeStep(0f));
        assertEquals(0, GrimoireCarry.closeStep(GrimoireCarry.CLOSE_TICKS));
        last = GrimoireCarry.OPEN_STEPS;
        for (float a = 0; a < GrimoireCarry.CLOSE_TICKS + 2; a += 0.5f) { int s = GrimoireCarry.closeStep(a); assertTrue(s <= last && s >= 0); last = s; }
        assertEquals(GrimoireCarry.OPEN_STEPS, com.newuniverse.nusmp.grimoire.GrimoireBookPlan.OPEN_STEPS);
    }

    @Test
    void pagesTurnFromTheRightBlockToTheLeftThroughTheReadersSide() {
        float open = 34f, end = GrimoireCarry.flipDuration() + 1;
        assertEquals(-open, GrimoireCarry.pageHeading(0, 0f, open, false), EPS);
        assertEquals(-(180f - open), GrimoireCarry.pageHeading(0, end, open, false), 0.01f);
        assertEquals(-(180f - open), GrimoireCarry.pageHeading(0, 0f, open, true), EPS);
        assertEquals(-open, GrimoireCarry.pageHeading(0, end, open, true), 0.01f);
        for (float a = 0; a < end; a += 0.25f) {
            float h = GrimoireCarry.pageHeading(1, a, open, false);
            assertTrue(h <= -open + EPS && h >= -(180f - open) - EPS, "stays between the page blocks, on the reader's side");
        }
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
        int ult = SpellArchetype.packSeed(12, SpellArchetype.DEBUFF, true);
        assertEquals(12, SpellArchetype.magicOf(ult));
        assertEquals(SpellArchetype.DEBUFF, SpellArchetype.archetypeOf(ult));
        assertTrue(SpellArchetype.isUltimate(ult));
        assertFalse(SpellArchetype.isUltimate(SpellArchetype.packSeed(12, SpellArchetype.DEBUFF)));
        // every magic has all three icons on disk
        String[] magics = {"flame", "explosion", "magma", "water", "ice", "mercury", "mist", "wind", "star", "storm", "earth", "plant", "sand",
                "light", "lightning", "sword", "dark", "shadow", "poison", "spatial", "mirror", "gravity", "time", "sealing", "reinforcement",
                "beast", "bone", "blood", "creation", "copy", "illusion", "dream", "anti_magic", "steel", "thread"};
        for (String m : magics) {
            for (int seed : new int[]{SpellArchetype.packSeed(0, SpellArchetype.OFFENSE), SpellArchetype.packSeed(0, SpellArchetype.BUFF), ult}) {
                String path = SpellArchetype.iconPath(m.toUpperCase(), seed);
                assertTrue(new File("src/main/resources/assets/nusmp/" + path).isFile(), "icon " + path);
            }
        }
        assertEquals("textures/skill/icons/water_buff.png", SpellArchetype.iconPath("WATER", SpellArchetype.packSeed(3, SpellArchetype.DEFENSE)));
        assertEquals("textures/skill/grimoire/water.png", SpellArchetype.iconPath("WATER", 3));
    }
}
