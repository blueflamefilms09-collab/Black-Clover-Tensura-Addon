package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Relics of Many Worlds, wave 10: the ten weapons' pure rules (the d8 buffs, the wall, the axe's way home, dispelling, insults, flail arcs, shards, finger, needles). */
class Wave10RulesTest {
    @Test
    void theD8HasEightDistinctBuffsAndWraps() {
        assertEquals(8, Wave10Rules.buffCount());
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 1; i <= 8; i++) {
            Wave10Rules.Buff b = Wave10Rules.buff(i);
            assertTrue(seen.add(b.effect()), "buff " + i + " is new");
            assertTrue(b.ticks() >= 600 && b.ticks() <= 1800, "a buff lasts 30 to 90 seconds");
            assertTrue(b.amp() >= 0 && b.amp() <= 1);
        }
        assertEquals(Wave10Rules.buff(1), Wave10Rules.buff(9), "wraps");
        assertEquals(Wave10Rules.buff(8), Wave10Rules.buff(0), "wraps below");
    }

    @Test
    void aPokeNeverKills() {
        assertTrue(Wave10Rules.pokeHurts(20f));
        assertTrue(Wave10Rules.pokeHurts(2.5f));
        assertFalse(Wave10Rules.pokeHurts(2.0f));
        assertFalse(Wave10Rules.pokeHurts(0.5f));
    }

    @Test
    void theWallIsABoxAcrossTheLookDirection() {
        // wall at the origin facing +z: 9 wide along x, 1.4 deep along z, 4 tall
        assertTrue(Wave10Rules.inWall(0, 1, 0, 0, 0, 0, 0, 1));
        assertTrue(Wave10Rules.inWall(4.4, 3.5, 0.6, 0, 0, 0, 0, 1));
        assertFalse(Wave10Rules.inWall(4.6, 1, 0, 0, 0, 0, 0, 1), "past the end");
        assertFalse(Wave10Rules.inWall(0, 1, 0.8, 0, 0, 0, 0, 1), "past the depth");
        assertFalse(Wave10Rules.inWall(0, 4.6, 0, 0, 0, 0, 0, 1), "over the top");
        assertFalse(Wave10Rules.inWall(0, -1, 0, 0, 0, 0, 0, 1), "under the floor");
        // turned: facing +x, the long side runs along z
        assertTrue(Wave10Rules.inWall(0, 1, 4.0, 0, 0, 0, 1, 0));
        assertFalse(Wave10Rules.inWall(4.0, 1, 0, 0, 0, 0, 1, 0));
        assertEquals(1, Wave10Rules.wallSide(0, 2, 0, 0, 0, 1));
        assertEquals(-1, Wave10Rules.wallSide(0, -2, 0, 0, 0, 1));
        assertEquals(0, Wave10Rules.wallSide(3, 0, 0, 0, 0, 1));
    }

    @Test
    void theAxeCutsWhatLiesOnTheWayHome() {
        assertEquals(0.0, Wave10Rules.distToSegment(5, 0, 0, 0, 0, 0, 10, 0, 0), 1e-9, "on the line");
        assertEquals(2.0, Wave10Rules.distToSegment(5, 2, 0, 0, 0, 0, 10, 0, 0), 1e-9, "beside it");
        assertEquals(3.0, Wave10Rules.distToSegment(-3, 0, 0, 0, 0, 0, 10, 0, 0), 1e-9, "beyond the start");
        assertEquals(5.0, Wave10Rules.distToSegment(13, 4, 0, 0, 0, 0, 10, 0, 0), 1e-9, "beyond the end");
        assertEquals(1.0, Wave10Rules.distToSegment(1, 0, 0, 0, 0, 0, 0, 0, 0) , 1e-9, "a zero-length segment is a point");
    }

    @Test
    void dispelMagicEndsLowSpellsAndRollsForHighOnes() {
        assertTrue(Wave10Rules.dispelled(0, true, 0.99));
        assertTrue(Wave10Rules.dispelled(1, true, 0.99), "amplifier 1 is still a low spell");
        assertTrue(Wave10Rules.dispelled(2, false, 0.59));
        assertFalse(Wave10Rules.dispelled(2, false, 0.61));
        assertTrue(Wave10Rules.dispelled(3, true, 0.29));
        assertFalse(Wave10Rules.dispelled(3, true, 0.31), "infernal targets are harder to strip");
        assertTrue(Wave10Rules.infernalPath("greater_daemon"));
        assertTrue(Wave10Rules.infernalPath("lesser_demon"));
        assertFalse(Wave10Rules.infernalPath("zombie"));
        assertFalse(Wave10Rules.infernalPath(null));
    }

    @Test
    void theLightCyclesThroughItsColours() {
        assertEquals(Wave10Rules.LIGHT_COLOURS[0], Wave10Rules.lightColour(0));
        assertEquals(Wave10Rules.LIGHT_COLOURS[0], Wave10Rules.lightColour(Wave10Rules.LIGHT_COLOURS.length));
        assertEquals(Wave10Rules.LIGHT_COLOURS[Wave10Rules.LIGHT_COLOURS.length - 1], Wave10Rules.lightColour(-1));
    }

    @Test
    void theSwordSwearsAtWhateverItHit() {
        assertTrue(Wave10Rules.insult("Ogre", 0).contains("Ogre"));
        for (int i = 0; i < 8; i++) assertTrue(Wave10Rules.insult("Troll", i).contains("Troll"), "insult " + i);
        assertEquals(Wave10Rules.insult("X", 3), Wave10Rules.insult("X", 11), "wraps");
        assertTrue(Wave10Rules.insult(null, 1).contains("thing"));
        for (int r = 0; r < 4; r++) assertTrue(Wave10Rules.psychic(r) >= 1 && Wave10Rules.psychic(r) <= 4, "1d4");
        assertEquals(13, Wave10Rules.WIS_DC);
    }

    @Test
    void theFlailOnlyHitsWhatIsBehind() {
        // facing +z (0, 1): something at -z is behind, +z is in front, +x is at the side
        assertTrue(Wave10Rules.inRear(0, 1, 0, -2));
        assertFalse(Wave10Rules.inRear(0, 1, 0, 2));
        assertFalse(Wave10Rules.inRear(0, 1, 2, 0), "the side is not the rear");
        assertTrue(Wave10Rules.inRear(0, 1, 1.5, -1), "behind and a little to the side");
        assertTrue(Wave10Rules.inFront(0, 1, 0, 2));
        assertFalse(Wave10Rules.inFront(0, 1, 0, -2));
        assertFalse(Wave10Rules.inFront(0, 1, 2, 0), "a creature at your side takes full blows");
        assertFalse(Wave10Rules.inRear(0, 0, 1, 1), "no look direction: nothing is behind");
        assertEquals(1, Wave10Rules.flailTargets(false));
        assertEquals(2, Wave10Rules.flailTargets(true));
        assertTrue(Wave10Rules.FLAIL_REACH_WHIP > Wave10Rules.FLAIL_REACH_SWORD);
        assertTrue(Wave10Rules.FRONT_SCALE > 0f && Wave10Rules.FRONT_SCALE < 1f);
    }

    @Test
    void onlyAHeavyBlastShattersTheXiphos() {
        assertTrue(Wave10Rules.shatters(15f, true));
        assertTrue(Wave10Rules.shatters(24f, true));
        assertFalse(Wave10Rules.shatters(14.9f, true));
        assertFalse(Wave10Rules.shatters(40f, false), "a sword blow does not shatter it");
        assertTrue(Wave10Rules.shardDamage(30, true) > Wave10Rules.shardDamage(30, false));
        assertTrue(Wave10Rules.shardDamage(6, true) >= 6f);
        assertTrue(Wave10Rules.shardDamage(1000, true) <= 20f, "capped");
    }

    @Test
    void fingerOfDeathScalesAndASaveHalvesIt() {
        assertEquals(14f + 2.4f, Wave10Rules.fingerDamage(20, false), 1e-4);
        assertEquals((14f + 2.4f) / 2f, Wave10Rules.fingerDamage(20, true), 1e-4);
        assertEquals(60f, Wave10Rules.fingerDamage(2000, false), 1e-4, "capped at 60");
        assertEquals(14f, Wave10Rules.fingerDamage(-5, false), 1e-4);
        assertTrue(Wave10Rules.humanoidPath("villager"));
        assertTrue(Wave10Rules.humanoidPath("zombified_piglin"));
        assertTrue(Wave10Rules.humanoidPath("goblin"));
        assertFalse(Wave10Rules.humanoidPath("cow"));
        assertFalse(Wave10Rules.humanoidPath(null));
    }

    @Test
    void theNeedlesFanOutEvenly() {
        int n = Wave10Rules.NEEDLES;
        double sum = 0, min = 1e9, max = -1e9;
        for (int i = 0; i < n; i++) {
            double[] o = Wave10Rules.needleOffset(i, n);
            sum += o[0];
            min = Math.min(min, o[0]);
            max = Math.max(max, o[0]);
            assertTrue(Math.abs(o[1]) < Wave10Rules.NEEDLE_SPREAD_DEG, "a small pitch wobble only");
        }
        assertEquals(0.0, sum, 1e-9, "the fan is centred");
        assertTrue(max - min > 0 && max - min < 4 * Wave10Rules.NEEDLE_SPREAD_DEG);
        assertEquals(0.0, Wave10Rules.needleOffset(0, 1)[0], 1e-9, "one needle is dead on");
        // four needles of 2.2 are at most 8.8: under half a 20-health player (45% heavy cap per hit applies to each)
        assertTrue(n * Wave10Rules.NEEDLE_DAMAGE <= 9.0f);
    }

    @Test
    void theSwingPullIsBounded() {
        assertEquals(0.5, Wave10Rules.swingSpeed(0), 1e-9, "never under 0.5");
        assertEquals(1.5, Wave10Rules.swingSpeed(100), 1e-9, "never over 1.5");
        assertTrue(Wave10Rules.swingSpeed(12) > Wave10Rules.swingSpeed(4));
    }
}
