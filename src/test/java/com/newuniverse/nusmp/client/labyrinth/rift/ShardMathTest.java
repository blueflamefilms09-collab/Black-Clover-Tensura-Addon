package com.newuniverse.nusmp.client.labyrinth.rift;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the reality shards promise a vertex budget, a level of detail by distance, a closed-form motion with no state, shards that stay near the tear, and an ending (they shrink, split and fade);
 * the parallax layers promise the same budget and a mask that shrinks with depth. All of it is arithmetic, pinned here.
 */
class ShardMathTest {
    @Test
    void theBudgetHoldsAndFallsWithDistance() {
        assertTrue(ShardMath.vertices(TearGeometry.FULL) <= ShardMath.BUDGET, "full: " + ShardMath.vertices(TearGeometry.FULL));
        assertTrue(ShardMath.vertices(TearGeometry.FULL) > ShardMath.vertices(TearGeometry.MIDDLE));
        assertTrue(ShardMath.vertices(TearGeometry.MIDDLE) > ShardMath.vertices(TearGeometry.FAR));
        assertEquals(0, ShardMath.vertices(TearGeometry.NONE));
        assertEquals(0, ShardMath.count(TearGeometry.NONE));
        assertEquals(14, ShardMath.count(TearGeometry.FULL));
    }

    @Test
    void aShardIsAFunctionOfSeedNumberAndTickOnly() {
        float[] a = new float[ShardMath.OUT_LEN], b = new float[ShardMath.OUT_LEN];
        for (int i = 0; i < 14; i++) {
            for (float t = 0; t < 900; t += 37.3f) {
                boolean va = ShardMath.state(4242L, i, t, 1.2f, 2.3f, a), vb = ShardMath.state(4242L, i, t, 1.2f, 2.3f, b);
                assertEquals(va, vb);
                if (va) for (int k = 0; k < ShardMath.OUT_LEN; k++) assertEquals(a[k], b[k], 0f, "index " + k);
            }
        }
        // another seed gives another swarm
        boolean differs = false;
        for (int i = 0; i < 14; i++) {
            ShardMath.state(1L, i, 100f, 1.2f, 2.3f, a);
            ShardMath.state(2L, i, 100f, 1.2f, 2.3f, b);
            differs |= a[ShardMath.X] != b[ShardMath.X];
        }
        assertTrue(differs);
    }

    @Test
    void shardsStayNearTheTearAndNothingIsNotANumber() {
        float[] s = new float[ShardMath.OUT_LEN];
        float rx = 1.2f, ry = 2.3f;
        float reach = 1.35f * 1.6f * Math.max(rx, ry) + 1.5f;
        int seen = 0;
        for (long seed = 0; seed < 40; seed++) {
            for (int i = 0; i < 14; i++) {
                for (float t = 0; t < 2400; t += 11.7f) {
                    if (!ShardMath.state(seed * 977L, i, t, rx, ry, s)) continue;
                    seen++;
                    for (int k = 0; k < ShardMath.OUT_LEN; k++) assertFalse(Float.isNaN(s[k]) || Float.isInfinite(s[k]), "index " + k);
                    assertTrue(Math.abs(s[ShardMath.X]) <= reach && Math.abs(s[ShardMath.Y]) <= reach && Math.abs(s[ShardMath.Z]) <= reach, "near the tear: " + s[0] + "," + s[1] + "," + s[2]);
                    assertTrue(s[ShardMath.SIZE] > 0f && s[ShardMath.SIZE] <= ShardMath.MAX_SIZE + 1e-4f);
                    assertTrue(s[ShardMath.ALPHA] > 0f && s[ShardMath.ALPHA] <= 1f);
                    assertTrue(s[ShardMath.SPLIT] >= 0f && s[ShardMath.SPLIT] <= 1f);
                }
            }
        }
        assertTrue(seen > 5000, "the swarm is visible most of the time: " + seen);
    }

    @Test
    void aShardGrowsInThenBreaksIntoTwoAndFades() {
        float[] s = new float[ShardMath.OUT_LEN];
        float maxSize = 0f, prevSize = 0f, minAlphaSeen = 1f;
        boolean grew = false, split = false, shrankWhileSplit = false, gone = false;
        for (float t = 0; t < 2 * (ShardMath.LIFE_MIN + ShardMath.LIFE_SPAN); t += 0.5f) {
            boolean vis = ShardMath.state(99L, 3, 1000f + t, 1f, 2f, s);
            if (!vis) { gone = true; prevSize = 0f; continue; }
            maxSize = Math.max(maxSize, s[ShardMath.SIZE]);
            minAlphaSeen = Math.min(minAlphaSeen, s[ShardMath.ALPHA]);
            if (s[ShardMath.SIZE] > prevSize + 1e-6f) grew = true;
            if (s[ShardMath.SPLIT] > 0.5f) {
                split = true;
                if (s[ShardMath.SIZE] < prevSize) shrankWhileSplit = true;
            }
            prevSize = s[ShardMath.SIZE];
        }
        assertTrue(grew, "it grows in");
        assertTrue(split, "it breaks apart");
        assertTrue(shrankWhileSplit, "it shrinks while it breaks");
        assertTrue(gone, "it disappears at the end of its cycle and is born again");
        assertTrue(minAlphaSeen < 0.3f, "it fades: " + minAlphaSeen);
        assertTrue(maxSize > 0.07f, "it reaches its size: " + maxSize);
    }

    @Test
    void thePiecesCountFollowsTheSplit() {
        assertEquals(1, ShardMath.pieces(0f));
        assertEquals(1, ShardMath.pieces(0.02f));
        assertEquals(2, ShardMath.pieces(0.5f));
    }

    @Test
    void theParallaxMaskShrinksWithDepthAndTheBudgetHolds() {
        assertEquals(2, ParallaxMath.layers(TearGeometry.FULL));
        assertEquals(1, ParallaxMath.layers(TearGeometry.MIDDLE));
        assertEquals(0, ParallaxMath.layers(TearGeometry.FAR));
        assertEquals(0, ParallaxMath.layers(TearGeometry.NONE));
        assertTrue(ParallaxMath.vertices(TearGeometry.FULL) <= 256, "" + ParallaxMath.vertices(TearGeometry.FULL));
        assertTrue(ParallaxMath.inset(0) < 1f && ParallaxMath.inset(1) < ParallaxMath.inset(0) && ParallaxMath.inset(1) > 0.5f, "a deeper layer is narrower, never gone");
        // the deeper layer slides faster against the viewing angle than the near one
        float dNear = Math.abs(ParallaxMath.u(0, 0f, 1.0f, 0f) - ParallaxMath.u(0, 0f, 0f, 0f));
        float dDeep = Math.abs(ParallaxMath.u(1, 0f, 1.0f, 0f) - ParallaxMath.u(1, 0f, 0f, 0f));
        assertTrue(dDeep > dNear, "parallax: " + dNear + " < " + dDeep);
        float vNear = Math.abs(ParallaxMath.v(0, 0f, 2f, 0f) - ParallaxMath.v(0, 0f, 0f, 0f));
        float vDeep = Math.abs(ParallaxMath.v(1, 0f, 2f, 0f) - ParallaxMath.v(1, 0f, 0f, 0f));
        assertTrue(vDeep > vNear);
    }
}
