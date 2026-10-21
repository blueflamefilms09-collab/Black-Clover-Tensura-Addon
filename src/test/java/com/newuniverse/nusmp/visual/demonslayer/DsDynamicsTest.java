package com.newuniverse.nusmp.visual.demonslayer;

import com.newuniverse.nusmp.client.DemonSlayerMesh;
import com.newuniverse.nusmp.visual.genesis.VuPose;
import com.newuniverse.nusmp.visual.genesis.VuSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The stack-driven animation kinds of the upgraded Genesis Demon-Slayer: fracture (the four shards of Black Meteorite), fracture_glow (the light between them), resonance (the bloom). */
class DsDynamicsTest {
    static VuSpec.Group group(String name, String animJson) {
        String json = "{\"id\":\"t\",\"groups\":{\"" + name + "\":{\"pivot\":[0,10,0],\"anim\":[" + animJson + "]}}}";
        return VuSpec.parse(json).groups.get(name);
    }

    @Test
    void aShardDriftsOutAlongItsAxisInProportionToTheFracture() {
        VuSpec.Group g = group("s1", "{\"kind\":\"fracture\",\"axis\":[0,1,0],\"amp\":4.0,\"deg\":0,\"rate\":0,\"phase\":0}");
        VuPose none = new VuPose().reset(), half = new VuPose().reset(), full = new VuPose().reset();
        DsDynamics.apply(g, none, 3f, 0f, 0);
        DsDynamics.apply(g, half, 3f, 0.5f, 0);
        DsDynamics.apply(g, full, 3f, 1f, 0);
        assertTrue(none.identity(), "no fracture, no movement");
        assertEquals(2.0f, half.oy, 1e-4f);
        assertEquals(4.0f, full.oy, 1e-4f);
        assertTrue(Math.abs(full.ox) <= 0.18f + 1e-4f, "the shiver stays small");
    }

    @Test
    void aShardTurnsAboutZAndXByTheFracture() {
        VuSpec.Group g = group("s2", "{\"kind\":\"fracture\",\"axis\":[0,1,0],\"amp\":1.0,\"deg\":10,\"rate\":6}");
        VuPose p = new VuPose().reset();
        DsDynamics.apply(g, p, 0f, 1f, 0);
        assertFalse(p.identity());
        // the rotation about z by 10 degrees shows in r[1] (row 0, col 1) = -sin(10 deg) up to the turn about x
        assertEquals(-Math.sin(Math.toRadians(10.0)), p.r[1], 0.01);
    }

    @Test
    void theLightBetweenTheShardsFollowsTheFractureAndFlickers() {
        VuSpec.Group g = group("crack_1", "{\"kind\":\"fracture_glow\",\"phase\":0.3}");
        VuPose off = new VuPose().reset(), on = new VuPose().reset();
        DsDynamics.apply(g, off, 5f, 0f, 0);
        DsDynamics.apply(g, on, 5f, 1f, 0);
        assertEquals(0f, off.alpha, 0f);
        assertTrue(on.alpha > 0.55f && on.alpha <= 1f, "alpha " + on.alpha);
        for (float t = 0; t < 40; t += 0.7f) {
            float a = DsDynamics.glowAlpha(t, 0.3f, 1f);
            assertTrue(a >= 0.6f - 1e-4f && a <= 1f, "flicker range " + a);
        }
    }

    @Test
    void theBloomGrowsAndBrightensWithTheResonance() {
        VuSpec.Group g = group("bloom", "{\"kind\":\"resonance\",\"amp\":0.34,\"min\":0.22,\"max\":1.0}");
        float prevScale = 0f;
        float prevPeak = 0f;
        for (int r = 0; r <= 4; r++) {
            VuPose p = new VuPose().reset();
            DsDynamics.apply(g, p, 0f, 0f, r);
            assertTrue(p.scale > prevScale, "scale grows at resonance " + r);
            prevScale = p.scale;
            float peak = 0f;
            for (float t = 0; t < 80; t += 0.5f) peak = Math.max(peak, DsDynamics.bloomAlpha(t, r, 0.22f, 1.0f));
            assertTrue(peak > prevPeak, "the brightest moment grows with the resonance " + r);
            prevPeak = peak;
        }
        assertEquals(1f + 0.34f * 4, prevScale, 1e-4f);
    }

    @Test
    void theBloomAlsoQuickensWithTheResonance() {
        // the pulse runs at 0.25 + 0.12 x resonance radians a tick, as in DemonSlayerMesh#bloom: count the sign changes of the alpha around its mean over 200 ticks
        int calm = peaks(0), strong = peaks(4);
        assertTrue(strong > calm, "peaks " + calm + " vs " + strong);
    }

    private static int peaks(int res) {
        int n = 0;
        float prev = DsDynamics.bloomAlpha(0f, res, 0.2f, 1f), prev2 = prev;
        for (float t = 0.25f; t < 200f; t += 0.25f) {
            float a = DsDynamics.bloomAlpha(t, res, 0.2f, 1f);
            if (prev > prev2 && prev > a) n++;
            prev2 = prev;
            prev = a;
        }
        return n;
    }

    @Test
    void theResonanceIsClamped() {
        assertEquals(0, DsDynamics.clampResonance(-3));
        assertEquals(4, DsDynamics.clampResonance(99));
        VuSpec.Group g = group("clover", "{\"kind\":\"resonance\",\"amp\":0.1,\"min\":0,\"max\":1}");
        VuPose p = new VuPose().reset();
        DsDynamics.apply(g, p, 0f, 0f, 50);
        assertEquals(1.4f, p.scale, 1e-4f);
    }

    @Test
    void theHookDoesNothingUnlessASwordIsBeingDrawn() {
        VuSpec.Group g = group("s1", "{\"kind\":\"fracture\",\"axis\":[0,1,0],\"amp\":4.0}");
        VuPose p = new VuPose().reset();
        assertFalse(DsDynamics.active());
        DsDynamics.apply(g, p, 0f);
        assertTrue(p.identity());
        DsDynamics.begin(1f, 2);
        try {
            assertTrue(DsDynamics.active());
            DsDynamics.apply(g, p, 0f);
            assertTrue(p.oy > 3.9f);
        } finally {
            DsDynamics.end();
        }
        assertFalse(DsDynamics.active());
    }

    @Test
    void otherKindsAreLeftToTheClockDrivenPose() {
        assertFalse(DsDynamics.isDynamic("spin"));
        assertTrue(DsDynamics.isDynamic("fracture") && DsDynamics.isDynamic("fracture_glow") && DsDynamics.isDynamic("resonance"));
        VuSpec.Group g = group("orbit", "{\"kind\":\"spin\",\"axis\":[0,1,0],\"dps\":30}");
        VuPose p = new VuPose().reset();
        DsDynamics.apply(g, p, 10f, 1f, 4);
        assertTrue(p.identity(), "apply only touches the stack-driven kinds");
    }

    @Test
    void theOldFractureCurveIsTheOneTheShardsFollow() {
        assertEquals(0f, DemonSlayerMesh.fracture(0f));
        assertEquals(0f, DemonSlayerMesh.fracture(DemonSlayerMesh.FRACTURE_TICKS + 1f));
        float mid = DemonSlayerMesh.fracture(DemonSlayerMesh.FRACTURE_TICKS * 0.5f);
        assertTrue(mid > 0.9f, "the shards are fully apart in the middle: " + mid);
    }
}
