package com.newuniverse.nusmp.client.playermodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of phase 2's performance and posture work: the level of detail by distance, the stances, the poses of the Goliath's moves and the puppets' throw. */
class PlayerLodStanceTest {
    @Test
    void theLevelOfDetailGrowsWithDistanceAndNeverBackwards() {
        double detail = PlayerLod.sqr(24), far = PlayerLod.sqr(64);
        assertEquals(PlayerLod.FULL, PlayerLod.level(0, detail, far));
        assertEquals(PlayerLod.FULL, PlayerLod.level(detail, detail, far), "exactly at the detail distance is still full");
        assertEquals(PlayerLod.NEAR, PlayerLod.level(detail + 1, detail, far));
        assertEquals(PlayerLod.NEAR, PlayerLod.level(far, detail, far));
        assertEquals(PlayerLod.FAR, PlayerLod.level(far + 1, detail, far));
        int last = -1;
        for (double d = 0; d < 20000; d += 37) {
            int level = PlayerLod.level(d, detail, far);
            assertTrue(level >= last, "the level never drops as the distance grows");
            last = level;
        }
        assertEquals(PlayerLod.FAR, PlayerLod.level(detail + 0.5, detail, detail / 2), "a far distance below the detail distance is raised to it: the near band collapses");
        assertEquals(PlayerLod.FULL, PlayerLod.level(detail - 0.5, detail, detail / 2));
    }

    @Test
    void theHairPaletteCacheGivesTheSameTintAsTheComputationAndReusesIt() {
        for (int rgb : new int[]{0x123456, 0xFFFFFF, 0x000000, 0xB02A2A, 0x1C1A22}) {
            assertEquals(HairPalette.of(rgb), HairPalette.cached(rgb));
            assertTrue(HairPalette.cached(rgb) == HairPalette.cached(rgb), "the cached tint is one object");
        }
        assertEquals(HairPalette.cached(0x123456), HairPalette.cached(0xFF123456), "the alpha byte is ignored");
    }

    @Test
    void whatEachLevelDraws() {
        assertTrue(PlayerLod.outerLayer(PlayerLod.FULL) && PlayerLod.hair(PlayerLod.FULL) && PlayerLod.glow(PlayerLod.FULL));
        assertFalse(PlayerLod.outerLayer(PlayerLod.NEAR) || PlayerLod.hair(PlayerLod.NEAR));
        assertTrue(PlayerLod.glow(PlayerLod.NEAR), "the glow stays until the far distance");
        assertFalse(PlayerLod.glow(PlayerLod.FAR) || PlayerLod.outerLayer(PlayerLod.FAR) || PlayerLod.hair(PlayerLod.FAR));
    }

    @Test
    void stancesLayerUnderTheBodyAndLeaveTheSwingAlone() {
        PoseState p = new PoseState();
        Stances.apply(Stances.ZOMBIE, p, true, false, true, 10f);
        assertTrue(p.blend[PoseState.ARM_R] > 0.5f && p.target[PoseState.ARM_R][0] < -1f, "a hunting zombie reaches forward");
        p.clear();
        Stances.apply(Stances.ZOMBIE, p, true, true, true, 10f);
        assertEquals(0f, p.blend[PoseState.ARM_R], "no pull on the arms during a swing");
        p.clear();
        Stances.apply(Stances.ZOMBIE, p, false, false, false, 10f);
        assertTrue(p.target[PoseState.ARM_R][0] > -1f && p.blend[PoseState.ARM_R] > 0f, "a shambling zombie holds its arms low");
        p.clear();
        Stances.apply(Stances.HUNCH, p, false, false, false, 0f);
        assertTrue(p.add[PoseState.BODY][0] > 0.2f, "the back bends forward");
        assertTrue(p.add[PoseState.HEAD][0] < 0f, "and the head juts to make up for it");
        p.clear();
        Stances.apply("", p, true, false, true, 1f);
        Stances.apply(null, p, true, false, true, 1f);
        Stances.apply("a stance nobody wrote", p, true, false, true, 1f);
        assertFalse(p.any(), "no stance, no pose");
        assertTrue(Stances.known("") && Stances.known(null) && Stances.known("hunch") && Stances.known("zombie") && !Stances.known("sway"));
    }

    @Test
    void aBodyAboveAStanceWinsTheArm() {
        PoseState p = new PoseState();
        Stances.apply(Stances.ZOMBIE, p, true, false, true, 0f);
        PosePatterns.cast(p, 1f, true, 0f);                                      // a big cast pulls the arms with a stronger amount and a different target
        assertTrue(p.target[PoseState.ARM_R][0] < -2f, "the cast's overhead pose replaced the zombie's reach");
    }

    @Test
    void everyGoliathMovePosesBothArmsOrALegAndStaysFinite() {
        for (String clip : new String[]{"slam", "stomp", "roar", "fist", "line", "rubble", "cast_big"}) {
            for (float t = 0f; t <= 1.0001f; t += 0.05f) {
                PoseState p = new PoseState();
                assertTrue(DungeonPoses.goliath(clip, p, t, 40f), clip);
                if (t >= 0.1f) assertTrue(p.any(), clip + " at " + t);
                for (int part = 0; part < PoseState.PARTS; part++) for (int k = 0; k < 3; k++) {
                    assertTrue(Float.isFinite(p.target[part][k]) && Float.isFinite(p.add[part][k]), clip + " is not finite at " + t);
                    assertTrue(Math.abs(p.target[part][k]) < 3.5f, clip + " turns a part past 3.5 radians at " + t);
                }
            }
        }
        assertFalse(DungeonPoses.goliath("walk", new PoseState(), 0.5f, 0f), "a name that is not a move falls back to the generic cast");
    }

    @Test
    void theSlamRaisesTheFistsFirstAndBringsThemDownAtTheLanding() {
        PoseState up = new PoseState(), down = new PoseState();
        DungeonPoses.slam(up, DungeonPoses.LANDS_AT * 0.95f);
        DungeonPoses.slam(down, 1f);
        assertTrue(up.target[PoseState.ARM_R][0] < -2.4f, "overhead before the blow");
        assertTrue(down.target[PoseState.ARM_R][0] > -0.6f, "down after it");
    }

    @Test
    void theRiseHasTheFistsUpAndSettlesWhenTheBodyHasCleared() {
        PoseState a = new PoseState(), b = new PoseState();
        DungeonPoses.rise(a, 0.1f);
        DungeonPoses.rise(b, 1f);
        assertTrue(a.target[PoseState.ARM_R][0] < -2.5f);
        assertFalse(b.any(), "nothing once it stands");
    }

    @Test
    void theThrowWindsUpThenReleasesForward() {
        PoseState wind = new PoseState(), release = new PoseState();
        DungeonPoses.throwArm(wind, 0.5f, 0f);
        DungeonPoses.throwArm(release, 1f, 0f);
        assertTrue(wind.target[PoseState.ARM_R][0] < -2.3f, "the arm is up and back at the end of the wind-up");
        assertTrue(release.target[PoseState.ARM_R][0] > -1.3f, "and forward at the release");
    }
}
